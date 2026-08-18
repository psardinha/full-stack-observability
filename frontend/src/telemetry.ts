import {metrics} from '@opentelemetry/api';
import {logs, SeverityNumber} from '@opentelemetry/api-logs';
import {ZoneContextManager} from '@opentelemetry/context-zone';
import {OTLPLogExporter} from '@opentelemetry/exporter-logs-otlp-http';
import {OTLPMetricExporter} from '@opentelemetry/exporter-metrics-otlp-http';
import {OTLPTraceExporter} from '@opentelemetry/exporter-trace-otlp-http';
import {registerInstrumentations} from '@opentelemetry/instrumentation';
import {FetchInstrumentation} from '@opentelemetry/instrumentation-fetch';
import {XMLHttpRequestInstrumentation} from '@opentelemetry/instrumentation-xml-http-request';
import {resourceFromAttributes} from '@opentelemetry/resources';
import {BatchLogRecordProcessor, LoggerProvider} from '@opentelemetry/sdk-logs';
import {MeterProvider, PeriodicExportingMetricReader} from '@opentelemetry/sdk-metrics';
import {BatchSpanProcessor, ParentBasedSampler, TraceIdRatioBasedSampler} from '@opentelemetry/sdk-trace-base';
import {WebTracerProvider} from '@opentelemetry/sdk-trace-web';
import {ATTR_SERVICE_NAME, ATTR_SERVICE_VERSION} from '@opentelemetry/semantic-conventions';
import {onCLS, onFCP, onINP, onLCP, onTTFB} from 'web-vitals';
import {environment} from './environments/environment';

declare global {
  interface Window {
    __otelBootstrapDone?: boolean;
  }
}

const FLUSH_EVENTS: Array<keyof WindowEventMap> = ['pagehide', 'beforeunload'];
const LOGGER_NAME = 'observability-client.app';
const HEALTH_METER_NAME = 'frontend.health';
const VITALS_ENDPOINT = `${environment.apiBaseUrl}/api/metrics/vitals`;

type JsonPrimitive = string | number | boolean | null;
type JsonLogContext = Record<string, JsonPrimitive | undefined>;
type JsonLogLevel = 'debug' | 'info' | 'warn' | 'error';

const LOG_SEVERITY_BY_LEVEL: Record<JsonLogLevel, SeverityNumber> = {
  debug: SeverityNumber.DEBUG,
  info: SeverityNumber.INFO,
  warn: SeverityNumber.WARN,
  error: SeverityNumber.ERROR,
};

const BASE_LOG_CONTEXT: JsonLogContext = {
  environment: environment.telemetry.deploymentEnvironment,
  service_name: environment.telemetry.serviceName,
  service_version: environment.telemetry.serviceVersion,
};

let tracerProviderRef: WebTracerProvider | undefined;
let meterProviderRef: MeterProvider | undefined;
let loggerProviderRef: LoggerProvider | undefined;
let appLoggerRef = logs.getLogger(LOGGER_NAME);
let pageLoadTotalRef: ReturnType<ReturnType<typeof metrics.getMeter>['createCounter']> | undefined;
let routeChangeTotalRef: ReturnType<ReturnType<typeof metrics.getMeter>['createCounter']> | undefined;
let routeChangeDurationMsRef: ReturnType<ReturnType<typeof metrics.getMeter>['createHistogram']> | undefined;
let jsErrorsTotalRef: ReturnType<ReturnType<typeof metrics.getMeter>['createCounter']> | undefined;
let pageLoadStatusRecorded = false;

export function getCurrentNavigationLabel(): string {
  const browserWindow = globalThis.window;

  if (!browserWindow)
    return '/';

  return `${browserWindow.location.pathname || '/'}${browserWindow.location.hash || ''}`;
}

function resolveNavigationLabel(navigation?: string): string {
  if (!navigation || navigation.trim().length === 0) {
    return getCurrentNavigationLabel();
  }

  try {
    const parsedUrl = new URL(navigation, globalThis.window?.location?.origin ?? 'http://localhost');
    return `${parsedUrl.pathname || '/'}${parsedUrl.hash || ''}`;
  } catch {
    return navigation;
  }
}

function sanitizeLogContext(context: JsonLogContext): Record<string, JsonPrimitive> {
  const sanitizedContext: Record<string, JsonPrimitive> = {};

  for (const [key, value] of Object.entries(context)) {
    if (value !== undefined) {
      sanitizedContext[key] = value;
    }
  }

  return sanitizedContext;
}

export function logJson(level: JsonLogLevel, message: string, context: JsonLogContext = {}): void {
  const mergedContext = sanitizeLogContext({ ...BASE_LOG_CONTEXT, ...context });
  const body = {
    timestamp: new Date().toISOString(),
    level,
    message,
    environment: mergedContext['environment'],
    context: mergedContext,
  };

  appLoggerRef.emit({
    severityNumber: LOG_SEVERITY_BY_LEVEL[level],
    severityText: level.toUpperCase(),
    body,
    attributes: mergedContext,
  });

  switch (level) {
    case 'debug':
      console.debug(message, body);
      break;
    case 'info':
      console.info(message, body);
      break;
    case 'warn':
      console.warn(message, body);
      break;
    case 'error':
      console.error(message, body);
      break;
  }
}

export function flushTelemetry(): void {
  if (tracerProviderRef) {
    void tracerProviderRef.forceFlush();
  }

  if (meterProviderRef) {
    void meterProviderRef.forceFlush();
  }

  if (loggerProviderRef) {
    void loggerProviderRef.forceFlush();
  }
}

function setupHealthMetrics(): void {
  const healthMeter = metrics.getMeter(HEALTH_METER_NAME);

  pageLoadTotalRef = healthMeter.createCounter('frontend_page_load_total', {
    description: 'Page load outcomes.',
  });

  routeChangeTotalRef = healthMeter.createCounter('frontend_route_change_total', {
    description: 'Route change outcomes.',
  });

  routeChangeDurationMsRef = healthMeter.createHistogram('frontend_route_change_duration_ms', {
    description: 'Route change duration in milliseconds.',
    unit: 'ms',
  });

  jsErrorsTotalRef = healthMeter.createCounter('frontend_js_errors_total', {
    description: 'Unhandled JavaScript errors.',
  });
}

function countJsError(): void {
  const page_route = getCurrentNavigationLabel();
  jsErrorsTotalRef?.add(1, {page_route});
}

export function markPageLoadSuccess(): void {
  if (pageLoadStatusRecorded)
    return;

  pageLoadTotalRef?.add(1, { status: 'success', page_route: getCurrentNavigationLabel() });
  pageLoadStatusRecorded = true;
}

export function markPageLoadError(): void {
  if (pageLoadStatusRecorded)
    return;

  pageLoadTotalRef?.add(1, {status: 'error', page_route: getCurrentNavigationLabel()});
  pageLoadStatusRecorded = true;
}

export function markRouteChangeSuccess(navigation: string): void {
  routeChangeTotalRef?.add(1, {status: 'success', page_route: resolveNavigationLabel(navigation)});
}

export function markRouteChangeError(navigation: string): void {
  routeChangeTotalRef?.add(1, {status: 'error', page_route: resolveNavigationLabel(navigation)});
}

export function recordRouteChangeDuration(durationMs: number, navigation: string): void {
  routeChangeDurationMsRef?.record(durationMs, {page_route: resolveNavigationLabel(navigation)});
}

function sendVitalToBackend(vitalType: string, value: number, pageRoute: string): void {
  try {
    const payload = new Blob([JSON.stringify({ type: vitalType, value, page_route: pageRoute })],
                             {type: 'application/json'});
    const result = navigator.sendBeacon(VITALS_ENDPOINT, payload);
    logJson('debug', 'Sent vital metric to backend', { vital: vitalType, value, page_route: pageRoute, beaconResult: result });
  } catch (error) {
    logJson('warn', 'Failed to send vital to backend', {vital: vitalType, page_route: pageRoute, error: String(error) });
  }
}

export function setupWebVitalsCollectors(): void {
  onLCP(metric => {
    const pageRoute = getCurrentNavigationLabel();
    logJson('debug', 'Web Vital: LCP', { vital: 'LCP', value: metric.value, pageRoute });
    sendVitalToBackend('lcp', metric.value, pageRoute);
  });

  onFCP(metric => {
    const pageRoute = getCurrentNavigationLabel();
    logJson('debug', 'Web Vital: FCP', { vital: 'FCP', value: metric.value, pageRoute });
    sendVitalToBackend('fcp', metric.value, pageRoute);
  });

  onCLS(metric => {
    const pageRoute = getCurrentNavigationLabel();
    logJson('debug', 'Web Vital: CLS', { vital: 'CLS', value: metric.value, pageRoute });
    sendVitalToBackend('cls', metric.value, pageRoute);
  });

  onINP(metric => {
    const pageRoute = getCurrentNavigationLabel();
    logJson('debug', 'Web Vital: INP', { vital: 'INP', value: metric.value, pageRoute });
    sendVitalToBackend('inp', metric.value, pageRoute);
  });

  onTTFB(metric => {
    const pageRoute = getCurrentNavigationLabel();
    logJson('debug', 'Web Vital: TTFB', { vital: 'TTFB', value: metric.value, pageRoute });
    sendVitalToBackend('ttfb', metric.value, pageRoute);
  });
}

function setupGlobalErrorHandlers(browserWindow: Window): void {
  const previousOnError = browserWindow.onerror;
  browserWindow.onerror = (...args) => {
    countJsError();
    markPageLoadError();

    if (typeof previousOnError === 'function')
      return previousOnError(...args);

    return false;
  };

  const previousUnhandledRejection = browserWindow.onunhandledrejection;
  browserWindow.onunhandledrejection = (event) => {
    countJsError();
    markPageLoadError();

    if (typeof previousUnhandledRejection === 'function')
      previousUnhandledRejection.call(browserWindow, event);
  };
}

export function bootstrapTelemetry(): void {
  const browserWindow = globalThis.window;

  if (!browserWindow || browserWindow.__otelBootstrapDone)
    return;

  browserWindow.__otelBootstrapDone = true;

  const telemetryConfig = environment.telemetry;

  const resource = resourceFromAttributes({
    [ATTR_SERVICE_NAME]: telemetryConfig.serviceName,
    [ATTR_SERVICE_VERSION]: telemetryConfig.serviceVersion,
    'deployment.environment.name': telemetryConfig.deploymentEnvironment,
  });

  const traceExporter = new OTLPTraceExporter({url: telemetryConfig.traceExportUrl});

  const tracerProvider = new WebTracerProvider({
    resource,
    sampler: new ParentBasedSampler({
      root: new TraceIdRatioBasedSampler(telemetryConfig.traceSampleRatio),
    }),
    spanProcessors: [new BatchSpanProcessor(traceExporter
//       , {
//   maxQueueSize: 100,          // buffer size
//   maxExportBatchSize: 20,     // how many per request
//   scheduledDelayMillis: 5000, // ⬅️ key: export every 5s
//   exportTimeoutMillis: 30000,
// }
    )],
  });

  tracerProvider.register({contextManager: new ZoneContextManager()});
  tracerProviderRef = tracerProvider;

  const metricExporter = new OTLPMetricExporter({url: telemetryConfig.metricExportUrl});

  const metricReader = new PeriodicExportingMetricReader({
    exporter: metricExporter,
    exportIntervalMillis: telemetryConfig.metricExportIntervalMillis,
    // exportTimeoutMillis: 5000
  });

  const meterProvider = new MeterProvider({resource, readers: [metricReader]});
  meterProviderRef = meterProvider;

  metrics.setGlobalMeterProvider(meterProvider);
  setupHealthMetrics();

  const logExporter = new OTLPLogExporter({url: telemetryConfig.logExportUrl});

  const loggerProvider = new LoggerProvider({resource,
                                             processors: [new BatchLogRecordProcessor({exporter: logExporter})]});
  loggerProviderRef = loggerProvider;

  logs.setGlobalLoggerProvider(loggerProvider);
  appLoggerRef = logs.getLogger(LOGGER_NAME, telemetryConfig.serviceVersion);

  registerInstrumentations({
    tracerProvider,
    meterProvider,
    instrumentations: [
      new FetchInstrumentation({
        propagateTraceHeaderCorsUrls: telemetryConfig.propagateTraceHeaderCorsUrls,
        ignoreUrls: telemetryConfig.ignoreUrls,
      }),
      new XMLHttpRequestInstrumentation({
        propagateTraceHeaderCorsUrls: telemetryConfig.propagateTraceHeaderCorsUrls,
        ignoreUrls: telemetryConfig.ignoreUrls,
      }),
    ],
  });

  setupGlobalErrorHandlers(browserWindow);

  for (const eventName of FLUSH_EVENTS) {
    browserWindow.addEventListener(eventName, () => {
      flushTelemetry();
    });
  }

  browserWindow.document.addEventListener('visibilitychange', () => {
    if (browserWindow.document.visibilityState === 'hidden') {
      flushTelemetry();
    }
  });

  setupWebVitalsCollectors();
  logJson('info', 'Telemetry bootstrap complete');
}

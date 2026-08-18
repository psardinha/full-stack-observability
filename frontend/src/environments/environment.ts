import { APP_VERSION } from './version';

export const environment = {
  production: true,
  apiBaseUrl: '',
  telemetry: {
    serviceName: 'observability-client',
    serviceVersion: APP_VERSION,
    deploymentEnvironment: 'production',
    traceExportUrl: 'http://localhost:8080/otel/v1/traces',
    metricExportUrl: 'http://localhost:8080/otel/v1/metrics',
    logExportUrl: 'http://localhost:8080/otel/v1/logs',    
    traceSampleRatio: 1,
    metricExportIntervalMillis: 10000,
    propagateTraceHeaderCorsUrls: [/^https?:\/\/localhost:8080\//],
    ignoreUrls: ['/otel/v1/traces', '/otel/v1/metrics', '/otel/v1/logs'],
  },
};

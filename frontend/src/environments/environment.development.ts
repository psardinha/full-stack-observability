import { APP_VERSION } from './version';

export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080',
  telemetry: {
    serviceName: 'observability-client',
    serviceVersion: APP_VERSION,
    deploymentEnvironment: 'development',
    traceExportUrl: 'http://localhost:8080/otel/v1/traces',
    metricExportUrl: 'http://localhost:8080/otel/v1/metrics',
    logExportUrl: 'http://localhost:8080/otel/v1/logs',
    traceSampleRatio: 1,
    metricExportIntervalMillis: 30000,
    propagateTraceHeaderCorsUrls: [/^https?:\/\/localhost:8080\//],
    ignoreUrls: [/^https?:\/\/localhost:8080\/otel\/v1\/(traces|metrics|logs)$/],
  },
};

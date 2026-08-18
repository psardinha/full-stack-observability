import {ChangeDetectionStrategy, Component, inject, signal} from '@angular/core';
import {FormControl, ReactiveFormsModule} from '@angular/forms';
import {HttpClient, HttpErrorResponse, HttpParams} from '@angular/common/http';
import {NavigationEnd, Router, RouterOutlet} from '@angular/router';
import {Span, SpanStatusCode, context, metrics, propagation, trace} from '@opentelemetry/api';
import {environment} from '../environments/environment';
import {logJson, getCurrentNavigationLabel} from '../telemetry';

type ReverseRequest = {
  input: string;
};

type ReverseResponse = {
  output: string;
};

type LengthResponse = {
  length: number;
};

type AppTelemetry = {
  tracer: ReturnType<typeof trace.getTracer>;
  operationAttemptsCounter: ReturnType<ReturnType<typeof metrics.getMeter>['createCounter']>;
  operationFailuresCounter: ReturnType<ReturnType<typeof metrics.getMeter>['createCounter']>;
  operationDurationMs: ReturnType<ReturnType<typeof metrics.getMeter>['createHistogram']>;
  mixFirstReplyCounter: ReturnType<ReturnType<typeof metrics.getMeter>['createCounter']>;
};

let appTelemetry: AppTelemetry | undefined;

function getAppTelemetry(): AppTelemetry {
  if (appTelemetry) {
    return appTelemetry;
  }

  const tracer = trace.getTracer('angular-frontend.operations');
  const meter = metrics.getMeter('angular-frontend.operations');

  appTelemetry = {
    tracer,
    operationAttemptsCounter: meter.createCounter('app.operation.attempts', {
      description: 'Number of attempts per operation.',
    }),
    operationFailuresCounter: meter.createCounter('app.operation.failures', {
      description: 'Number of failed operations.',
    }),
    operationDurationMs: meter.createHistogram('app.operation.duration.ms', {
      description: 'Operation duration in milliseconds.',
      unit: 'ms',
    }),
    mixFirstReplyCounter: meter.createCounter('app.mix.first_reply', {
      description: 'Tracks which partial reply was produced first in Mix.',
    }),
  };

  return appTelemetry;
}

function resolveApiUrl(path: string): string {
  return `${environment.apiBaseUrl}${path}`;
}

function closeSpan(span: Span, statusCode: SpanStatusCode, message?: string): void {
  if (statusCode === SpanStatusCode.ERROR) {
    span.setStatus({code: statusCode, message: message ?? 'Operation failed'});
  } else {
    span.setStatus({code: statusCode});
  }

  span.end();
}

@Component({
  selector: 'app-root',
  imports: [ReactiveFormsModule, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly reverseApiUrl = resolveApiUrl('/utils/reverse');
  private readonly lengthApiUrl = resolveApiUrl('/utils/length');

  protected readonly title = 'Trace Initiator';
  protected readonly argumentControl = new FormControl('', {nonNullable: true});
  protected readonly result = signal('No result yet.');
  protected readonly isLoading = signal(false);
  protected readonly isRouteExercisePage = signal(this.router.url.startsWith('/route-exercise'));

  constructor() {
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd)
        this.isRouteExercisePage.set(event.urlAfterRedirects.startsWith('/route-exercise'));
    });
  }

  protected goToRouteExercise(): void {
    void this.router.navigate(['/route-exercise'], {queryParams: {value: this.argumentControl.value}});
  }

  protected reverse(): void {
    const {tracer, operationAttemptsCounter, operationFailuresCounter, operationDurationMs} = getAppTelemetry();
    const operationName = 'reverse';
    const pageRoute = getCurrentNavigationLabel();
    const input = this.argumentControl.value.trim();
    const startedAt = performance.now();
    const reverseOperationSpan = tracer.startSpan('ui.operation.reverse');
    let   reverseContext = trace.setSpan(context.active(), reverseOperationSpan);
    const requestBaggage = propagation.createBaggage({operation: {value: operationName},
                                                      input_arg: {value: input}});
    reverseContext = propagation.setBaggage(reverseContext, requestBaggage);

    operationAttemptsCounter.add(1, {operation: operationName, page_route: pageRoute});
    this.isLoading.set(true);

    context.with(reverseContext, () => {
      this.http.post<ReverseResponse>(this.reverseApiUrl, {input} as ReverseRequest).subscribe({
        next: (response) => {
          this.result.set(response.output);
          logJson('info', 'Reverse operation completed', {
            operation: operationName,
            source: "frontend",
            page_route: pageRoute,
            original: input,
            reversed: response.output,
          });
          operationDurationMs.record(performance.now() - startedAt, 
                                     {operation: operationName, page_route: pageRoute});
          closeSpan(reverseOperationSpan, SpanStatusCode.OK);
          this.isLoading.set(false);
        },
        error: (error: HttpErrorResponse) => {
          this.result.set(this.getOperationError('Reverse', error));
          logJson('error', 'Reverse operation failed', {
            operation: operationName,
            source: "frontend",
            page_route: pageRoute,
            original: input,
            reversed: `HTTP_${error.status}`,
          });
          operationFailuresCounter.add(1, {operation: operationName, page_route: pageRoute});
          operationDurationMs.record(performance.now() - startedAt, {operation: operationName, page_route: pageRoute});
          closeSpan(reverseOperationSpan, SpanStatusCode.ERROR, `HTTP ${error.status}`);
          this.isLoading.set(false);
        },
      });
    });
  }

  protected length(): void {
    const {tracer, operationAttemptsCounter, operationFailuresCounter, operationDurationMs} = getAppTelemetry();
    const operationName = 'length';
    const pageRoute = getCurrentNavigationLabel();
    const value = this.argumentControl.value.trim();
    const startedAt = performance.now();
    const lengthOperationSpan = tracer.startSpan('ui.operation.length');
    let   lengthContext = trace.setSpan(context.active(), lengthOperationSpan);
    const requestBaggage = propagation.createBaggage({operation: {value: operationName},
                                                      input_arg: {value: value}});
    lengthContext = propagation.setBaggage(lengthContext, requestBaggage);

    operationAttemptsCounter.add(1, {operation: operationName, page_route: pageRoute});
    this.isLoading.set(true);
    const params = new HttpParams().set('string', value);

    context.with(lengthContext, () => {
      this.http.get<LengthResponse>(this.lengthApiUrl, {params}).subscribe({
        next: (response) => {
          this.result.set(String(response.length));
          logJson('info', 'Length operation completed', {
            operation: operationName,
            page_route: pageRoute,
            source: "frontend",
            original: value,
            length: response.length,
          });
          operationDurationMs.record(performance.now() - startedAt, {operation: operationName, page_route: pageRoute});
          closeSpan(lengthOperationSpan, SpanStatusCode.OK);
          this.isLoading.set(false);
        },
        error: (error: HttpErrorResponse) => {
          this.result.set(this.getOperationError('Length', error));
          operationFailuresCounter.add(1, {operation: operationName, page_route: pageRoute});
          operationDurationMs.record(performance.now() - startedAt, {operation: operationName, page_route: pageRoute});
          closeSpan(lengthOperationSpan, SpanStatusCode.ERROR, `HTTP ${error.status}`);
          this.isLoading.set(false);
        },
      });
    });
  }

  protected mix(): void {
    const {tracer, operationAttemptsCounter, operationFailuresCounter, 
           operationDurationMs, mixFirstReplyCounter} = getAppTelemetry();
    const operationName = 'mix';
    const pageRoute = getCurrentNavigationLabel();
    const value = this.argumentControl.value.trim();
    const startedAt = performance.now();
    const mixOperationSpan = tracer.startSpan('ui.operation.mix');
    const mixContext = trace.setSpan(context.active(), mixOperationSpan);

    operationAttemptsCounter.add(1, {operation: operationName, page_route: pageRoute});
    this.isLoading.set(true);

    const partialReplies: string[] = [];
    let completedOperations = 0;
    let firstReplyFrom: 'reverse' | 'length' | undefined;
    let hasAnyError = false;

    const appendReply = (source: 'reverse' | 'length', message: string): void => {
      if (!firstReplyFrom)
        firstReplyFrom = source;

      partialReplies.push(message);
      this.result.set(partialReplies.join(';'));
      completedOperations += 1;

      if (completedOperations === 2) {
        if (firstReplyFrom)
          mixFirstReplyCounter.add(1, {first_reply: firstReplyFrom, page_route: pageRoute});

        operationDurationMs.record(performance.now() - startedAt, {operation: operationName, page_route: pageRoute});

        if (hasAnyError) {
          operationFailuresCounter.add(1, {operation: operationName, page_route: pageRoute});
          closeSpan(mixOperationSpan, SpanStatusCode.ERROR, 'One or more sub-operations failed');
        } else
          closeSpan(mixOperationSpan, SpanStatusCode.OK);
        this.isLoading.set(false);
      }
    };

    context.with(mixContext, () => {
      this.http.post<ReverseResponse>(this.reverseApiUrl, {input: value} as ReverseRequest).subscribe({
        next: (reverseResponse) => {
          appendReply('reverse', `Reverted string: ${reverseResponse.output}`);
        },
        error: (error: HttpErrorResponse) => {
          hasAnyError = true;
          appendReply('reverse', this.getOperationError('Reverse', error));
        },
      });

      const params = new HttpParams().set('string', value);
      this.http.get<LengthResponse>(this.lengthApiUrl, {params}).subscribe({
        next: (lengthResponse) => {
          appendReply('length', `String length: ${lengthResponse.length}`);
        },
        error: (error: HttpErrorResponse) => {
          hasAnyError = true;
          appendReply('length', this.getOperationError('Length', error));
        },
      });
    });
  }

  protected exception(): void {
    this.argumentControl.setValue('ERROR');
    this.reverse();
  }

  
  private getOperationError(operationName: string, error: HttpErrorResponse): string {
    if (error.status === 0)
      return `${operationName} failed: backend unreachable or blocked by CORS policy.`;

    const body = typeof error.error === 'string' ? 
                 error.error :
                 error.error?.message ?? JSON.stringify(error.error);
    return `${operationName} failed (HTTP ${error.status}, ${body})`;
  }
}

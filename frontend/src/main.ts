import {bootstrapApplication} from '@angular/platform-browser';
import {NavigationCancel, NavigationEnd, NavigationError, NavigationStart, Router } from '@angular/router';
import {appConfig } from './app/app.config';
import {bootstrapTelemetry, logJson, markPageLoadError, markPageLoadSuccess, recordRouteChangeDuration,
	    markRouteChangeError, markRouteChangeSuccess} from './telemetry';

bootstrapTelemetry();
const appBootstrapStartedAt = performance.now();

void import('./app/app')
	.then(({ App }) => bootstrapApplication(App, appConfig))
	.then(appRef => {
		markPageLoadSuccess();

		const router = appRef.injector.get(Router);
		if (router.navigated) {
			const initialNavigation = `${globalThis.window?.location.pathname || '/'}${globalThis.window?.location.hash || ''}`;
			recordRouteChangeDuration(performance.now() - appBootstrapStartedAt, initialNavigation);
			markRouteChangeSuccess(initialNavigation);
		}

		const navigationStartById = new Map<number, number>();
		router.events.subscribe((event) => {
			if (event instanceof NavigationStart) {
				navigationStartById.set(event.id, performance.now());
			}

			if (event instanceof NavigationCancel) {
				navigationStartById.delete(event.id);
			}

			if (event instanceof NavigationEnd) {
				const startedAt = navigationStartById.get(event.id);
				if (startedAt !== undefined) {
					recordRouteChangeDuration(performance.now() - startedAt, event.urlAfterRedirects);
					navigationStartById.delete(event.id);
				}

				markRouteChangeSuccess(event.urlAfterRedirects);
			}

			if (event instanceof NavigationError) {
				const startedAt = navigationStartById.get(event.id);
				if (startedAt !== undefined) {
					recordRouteChangeDuration(performance.now() - startedAt, event.url);
					navigationStartById.delete(event.id);
				}

				markRouteChangeError(event.url);
			}
		});
	})
	.catch((err) => {
		markPageLoadError();
		logJson('error', 'Application bootstrap failed', {
			error_message: err instanceof Error ? err.message : String(err),
		});
	});

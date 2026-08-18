package com.utils.services.control;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.utils.services.Control;

/**
 * Records web vitals metrics (FCP, LCP, CLS, INP, FID, TTFB) from the frontend.
 * Metrics are exported as OpenTelemetry metrics via OTLP to the configured collector.
 * Multi-instance aggregation is handled by the collector/Prometheus via instance labels.
 */
@Control
public class VitalsMetricsServiceSimple {
  private static final Logger LOGGER = Logger.getLogger(VitalsMetricsServiceSimple.class.getName());
  private static final String METER_NAME = "stringUtils.frontend-vitals";
  private static final AttributeKey<String> VITAL_TYPE_ATTRIBUTE = AttributeKey.stringKey("vital_type");
  private static final AttributeKey<String> PAGE_ROUTE_ATTRIBUTE = AttributeKey.stringKey("page_route");

  private final DoubleHistogram fcpDurationHistogram = GlobalOpenTelemetry.getMeter(METER_NAME).
         histogramBuilder("frontend.vitals.fcp").
         setDescription("Distribution of First Contentful Paint (FCP) values received by the backend").
         setUnit("ms").
         build();

  private final DoubleHistogram lcpDurationHistogram = GlobalOpenTelemetry.getMeter(METER_NAME).
         histogramBuilder("frontend.vitals.lcp").
         setDescription("Distribution of Largest Contentful Paint (LCP) values received by the backend").
         setUnit("ms").
         build();

  private final DoubleHistogram inpDurationHistogram = GlobalOpenTelemetry.getMeter(METER_NAME).
         histogramBuilder("frontend.vitals.inp").
         setDescription("Distribution of Interaction to Next Paint (INP) values received by the backend").
         setUnit("ms").
         build();

  private final DoubleHistogram ttfbDurationHistogram = GlobalOpenTelemetry.getMeter(METER_NAME).
         histogramBuilder("frontend.vitals.ttfb").
         setDescription("Distribution of Time to First Byte (TTFB) values received by the backend").
         setUnit("ms").
         build();

  private final DoubleHistogram clsScoreHistogram = GlobalOpenTelemetry.getMeter(METER_NAME).
         histogramBuilder("frontend.vitals.cls").
         setDescription("Distribution of Cumulative Layout Shift (CLS) values received by the backend").
         setUnit("1").
         build();

  private final LongCounter vitalEventCounter = GlobalOpenTelemetry.getMeter(METER_NAME).
         counterBuilder("frontend.vitals.events").
         setDescription("Count of frontend web vital events received by the backend").
         setUnit("{event}").
         build();

  public void recordVital(String vitalType, double value,  String pageRoute) {
    if (vitalType == null || value < 0) {
      LOGGER.log(Level.WARNING, "Invalid vital metric: type={0}, value={1}", new Object[]{vitalType, value});
      return;
    }

    String normalizedType = vitalType.toLowerCase();
    recordOtelMetrics(normalizedType, value, pageRoute);

    LOGGER.log(Level.INFO, "Recorded {0}: value={1}ms", new Object[]{normalizedType, value});
  }

  void recordOtelMetrics(String vitalType, double value, String pageRoute) {
    AttributesBuilder attributesBuilder = Attributes.builder().
                                                     put(VITAL_TYPE_ATTRIBUTE, vitalType);
    if (pageRoute != null)
      attributesBuilder.put(PAGE_ROUTE_ATTRIBUTE, pageRoute);
    Attributes attributes = attributesBuilder.build();

    switch (vitalType) {
      case "fcp" -> fcpDurationHistogram.record(value, attributes);
      case "lcp" -> lcpDurationHistogram.record(value, attributes);
      case "inp" -> inpDurationHistogram.record(value, attributes);
      case "ttfb" -> ttfbDurationHistogram.record(value, attributes);
      case "cls" -> clsScoreHistogram.record(value, attributes);
      default -> {
                  LOGGER.log(Level.WARNING, "Ignoring unknown vital metric type={0}, value={1}, page_route={2}", 
                             new Object[]{vitalType, value, pageRoute != null ? pageRoute : "<no-page-route>>"});
                  return;
                 }
    }
    vitalEventCounter.add(1, attributes);
  }
}

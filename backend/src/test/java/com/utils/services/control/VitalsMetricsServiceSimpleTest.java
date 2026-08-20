package com.utils.services.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class VitalsMetricsServiceSimpleTest {
  @Mock
  private DoubleHistogram fcpDurationHistogram;

  @Mock
  private DoubleHistogram lcpDurationHistogram;

  @Mock
  private DoubleHistogram inpDurationHistogram;

  @Mock
  private DoubleHistogram ttfbDurationHistogram;

  @Mock
  private DoubleHistogram clsScoreHistogram;

  @Mock
  private LongCounter vitalEventCounter;

  private VitalsMetricsServiceSimple service;

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    service = new VitalsMetricsServiceSimple();
    setField(service, "fcpDurationHistogram", fcpDurationHistogram);
    setField(service, "lcpDurationHistogram", lcpDurationHistogram);
    setField(service, "inpDurationHistogram", inpDurationHistogram);
    setField(service, "ttfbDurationHistogram", ttfbDurationHistogram);
    setField(service, "clsScoreHistogram", clsScoreHistogram);
    setField(service, "vitalEventCounter", vitalEventCounter);
  }

  @Test
  void recordVital_shouldRecordFcp() {
    service.recordVital("FCP", 123.45, "/home");
    verify(fcpDurationHistogram).record(123.45, attributesWith("vital_type", "fcp", "page_route", "/home"));
    verify(vitalEventCounter).add(1, attributesWith("vital_type", "fcp", "page_route",
                        "/home"));
  }

  @Test
  void recordVital_shouldRecordLcp() {
    service.recordVital( "LCP", 456.78, "/home"); 
    verify(lcpDurationHistogram).record( 456.78, attributesWith( "vital_type", "lcp", "page_route", "/home"));
    verify(vitalEventCounter).add( 1, attributesWith( "vital_type", "lcp", "page_route", "/home"));
  }

  @Test
  void recordVital_shouldRecordInp() {
    service.recordVital( "INP", 250.0, "/products"); 
    verify(inpDurationHistogram).record( 250.0, attributesWith( "vital_type", "inp", "page_route", "/products")); 
    verify(vitalEventCounter).add( 1, attributesWith( "vital_type", "inp", "page_route", "/products"));
  }

  @Test
  void recordVital_shouldRecordTtfb() {
    service.recordVital( "TTFB", 80.5, "/"); 
    verify(ttfbDurationHistogram).record( 80.5, attributesWith( "vital_type", "ttfb", "page_route", "/"));
  }

  @Test
  void recordVital_shouldRecordCls() {
    service.recordVital( "CLS", 0.15, "/checkout"); 
    verify(clsScoreHistogram).record( 0.15, attributesWith( "vital_type", "cls", "page_route", "/checkout"));
  }

  @Test
  void recordVital_shouldNormalizeVitalTypeToLowerCase() {
    service.recordVital( "lCp", 123.0, "/home"); 
    verify(lcpDurationHistogram).record( 123.0, attributesWith( "vital_type", "lcp", "page_route", "/home"));
  }

  @Test
  void recordVital_shouldIgnoreNullVitalType() {
    service.recordVital( null, 123.0, "/home");
    verifyNoMetricsRecorded();
  }

  @Test
  void recordVital_shouldIgnoreNegativeValue() {
    service.recordVital( "LCP", -1.0, "/home");
    verifyNoMetricsRecorded();
  }

  @Test
  void recordVital_shouldAcceptZeroValue() {
    service.recordVital( "FCP", 0.0, "/home"); 
    verify(fcpDurationHistogram).record( 0.0, attributesWith( "vital_type", "fcp", "page_route", "/home")); 
    verify(vitalEventCounter).add( 1, attributesWith( "vital_type", "fcp", "page_route", "/home"));
  }

  @Test
  void recordVital_shouldIgnoreUnknownVitalType() {
    service.recordVital( "UNKNOWN", 123.0, "/home");
    verifyNoMetricsRecorded();
  }

  @Test
  void recordVital_shouldAllowNullPageRoute() {
    service.recordVital( "LCP", 123.0, null); 
    verify(lcpDurationHistogram).record( 123.0, attributesWith( "vital_type", "lcp")); 
    verify(vitalEventCounter).add( 1, attributesWith( "vital_type", "lcp"));
  }

  @Test
  void recordOtelMetrics_shouldRecordEventCounter() {
    service.recordOtelMetrics( "lcp", 123.0, "/home"); 
    verify(lcpDurationHistogram).record( 123.0, attributesWith( "vital_type", "lcp", "page_route", "/home")); 
    verify(vitalEventCounter).add( 1, attributesWith( "vital_type", "lcp", "page_route", "/home"));
  }

  @Test
  void recordOtelMetrics_shouldIgnoreUnknownType() {
    service.recordOtelMetrics( "unknown", 123.0, "/home");
    verifyNoMetricsRecorded();
  }

  private void verifyNoMetricsRecorded() {
    verify(fcpDurationHistogram, never()).record( org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.any(Attributes.class)); 
    verify(lcpDurationHistogram, never()).record( org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.any(Attributes.class));
    verify(inpDurationHistogram, never()).record( org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.any(Attributes.class));
    verify(ttfbDurationHistogram, never()).record( org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.any(Attributes.class));
    verify(clsScoreHistogram, never()).record( org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.any(Attributes.class));
    verify(vitalEventCounter, never()).add( org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any(Attributes.class));
  }

  private static Attributes attributesWith(String... values) {
    io.opentelemetry.api.common.AttributesBuilder builder = Attributes.builder();

    for (int i = 0; i < values.length; i += 2)
      builder.put(values[i], values[i + 1]);
    return builder.build();
  }

  private static void setField( Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
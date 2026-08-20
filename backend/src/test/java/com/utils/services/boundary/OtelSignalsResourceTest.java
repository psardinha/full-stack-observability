package com.utils.services.boundary;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.utils.services.control.OtelSignalForwarder;

@ExtendWith(MockitoExtension.class)
class OtelSignalsResourceTest {
  @Mock
  private OtelSignalForwarder otelSignalForwarder;

  @Mock
  private HttpHeaders headers;

  private OtelSignalsResource resource;

  @BeforeEach
  void setUp() {
    resource = new OtelSignalsResource();
    resource.otelSignalForwarder = otelSignalForwarder;
  }

  @Test
  void traces_shouldForwardTraceSignal() {
    byte[] payload = {1, 2, 3};
    Response expectedResponse = Response.ok().build();

    when(otelSignalForwarder.forwardTraceSignal(payload, headers)).thenReturn(expectedResponse);

    Response actualResponse = resource.traces(payload, headers);

    assertSame(expectedResponse, actualResponse);
    verify(otelSignalForwarder).forwardTraceSignal(payload, headers);
    verifyNoMoreInteractions(otelSignalForwarder);
  }

  @Test
 void metrics_shouldForwardMetricSignal() {
    byte[] payload = {4, 5, 6};
    Response expectedResponse = Response.ok().build();

    when(otelSignalForwarder.forwardMetricSignal(payload, headers)).thenReturn(expectedResponse);

    Response actualResponse = resource.metrics(payload, headers);

    assertSame(expectedResponse, actualResponse);
    verify(otelSignalForwarder).forwardMetricSignal(payload, headers);
    verifyNoMoreInteractions(otelSignalForwarder);
  }

  @Test
  void logs_shouldForwardLogSignal() {
    byte[] payload = {7, 8, 9};
    Response expectedResponse = Response.ok().build();

    when(otelSignalForwarder.forwardLogSignal(payload, headers)).thenReturn(expectedResponse);

    Response actualResponse = resource.logs(payload, headers);

    assertSame(expectedResponse, actualResponse);
    verify(otelSignalForwarder).forwardLogSignal(payload, headers);
    verifyNoMoreInteractions(otelSignalForwarder);
  }
}

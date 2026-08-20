package com.utils.services.boundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.utils.services.control.VitalsMetricsServiceSimple;
import com.utils.services.entity.VitalMetricRequest;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VitalsResourceTest {
  private Level originalLogLevel;

  @Mock
  private VitalsMetricsServiceSimple vitalsMetricsService;

  private VitalsResource resource;

  @BeforeEach
  void setUp() {
    resource = new VitalsResource(vitalsMetricsService);
    Logger logger = Logger.getLogger(VitalsResource.class.getName());
    originalLogLevel = logger.getLevel();
    logger.setLevel(Level.OFF);
  }

  @AfterEach
  void tearDown() {
    Logger.getLogger(VitalsResource.class.getName()).setLevel(originalLogLevel);
  }

  @Test
  void recordVital_shouldRecordValidRequest() {
    VitalMetricRequest request = new VitalMetricRequest("LCP", 123.45, "/home");
    Response response = resource.recordVital(request);

    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    assertThat(response.getEntity()).isEqualTo("{\"status\": \"success\"}");
    verify(vitalsMetricsService).recordVital("LCP", 123.45, "/home");
    response.close();
  }

  @Test
  void recordVital_shouldReturnBadRequestWhenRequestIsNull() {
    Response response = resource.recordVital(null);

    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    assertThat(response.getEntity()).isEqualTo("{\"error\": \"Invalid request\"}");
    verifyNoInteractions(vitalsMetricsService);
    response.close();
  }

  @Test
  void recordVital_shouldReturnBadRequestWhenTypeIsNull() {
    VitalMetricRequest request = new VitalMetricRequest(null, 123.45, "/home");
    Response response = resource.recordVital(request);

    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    assertThat(response.getEntity()).isEqualTo("{\"error\": \"Invalid request\"}");
    verifyNoInteractions(vitalsMetricsService);
    response.close();
  }

  @Test
  void recordVital_shouldReturnBadRequestWhenValueIsNull() {
    VitalMetricRequest request = new VitalMetricRequest("LCP", null, "/home");
    Response response = resource.recordVital(request);

    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    assertThat(response.getEntity()).isEqualTo("{\"error\": \"Invalid request\"}");
    verifyNoInteractions(vitalsMetricsService);
    response.close();
  }

  @Test
  void recordVital_shouldReturnInternalServerErrorWhenServiceThrowsException() {
    VitalMetricRequest request = new VitalMetricRequest("LCP", 123.45, "/home");
    doThrow(new RuntimeException("Service failure")).when(vitalsMetricsService).
                                                             recordVital("LCP", 123.45, "/home");
    Response response = resource.recordVital(request);

    assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    assertThat(response.getEntity()).isEqualTo("{\"error\": \"Service failure\"}");
    verify(vitalsMetricsService).recordVital("LCP", 123.45, "/home");
    response.close();
  }
}
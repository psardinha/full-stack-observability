package com.utils.services.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class OtelSignalForwarderTest {
  @Mock
  private HttpClient client;

  @Mock
  private jakarta.ws.rs.core.HttpHeaders inboundHeaders;

  @Mock
  private HttpResponse<byte[]> collectorResponse;

  private OtelSignalForwarder forwarder;

  private AutoCloseable mocks;

  @BeforeEach
  void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    forwarder = new OtelSignalForwarder();
    setPrivateField(forwarder, "client", client);
    setPrivateField( forwarder, "collectorBaseUrl", "http://collector:4318");
  }

  @AfterEach
  void tearDown() throws Exception {
    mocks.close();
    // Clear interrupted flag in case the interrupted test set it.
    Thread.interrupted();
  }

  private static void setPrivateField( Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  @Test
  void forwardTraceSignal_shouldForwardToTracesEndpoint() throws Exception {
    byte[] payload = "trace-payload".getBytes();

    java.net.http.HttpHeaders responseHeaders = java.net.http.HttpHeaders.of(Map.of("content-type", List.of("application/json")), 
                                                                             (name, value) -> true);

    when(collectorResponse.statusCode()).thenReturn(200);
    when(collectorResponse.headers()).thenReturn(responseHeaders);
    when(collectorResponse.body()).thenReturn("{\"status\":\"ok\"}".getBytes());
    when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(collectorResponse);
    Response response = forwarder.forwardTraceSignal(payload, inboundHeaders);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE)).isEqualTo("application/json");
    assertThat(response.getEntity()).isEqualTo("{\"status\":\"ok\"}".getBytes());
    verify(client).send( argThat(request -> request.uri().equals( URI.create( "http://collector:4318/v1/traces"))), any(HttpResponse.BodyHandler.class));
    response.close();
  }

  @Test
  void forwardMetricSignal_shouldForwardToMetricsEndpoint() throws Exception {
    when(collectorResponse.statusCode()).thenReturn(202);
    when(collectorResponse.headers()).thenReturn( java.net.http.HttpHeaders.of( Map.of(), (name, value) -> true));
    when(collectorResponse.body()).thenReturn(new byte[0]);
    when(client.send( any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(collectorResponse);

    Response response = forwarder.forwardMetricSignal( "metric".getBytes(), inboundHeaders);

    assertThat(response.getStatus()).isEqualTo(202);
    assertThat(response.getEntity()).isNull();
    verify(client).send( argThat(request -> request.uri().equals( URI.create( "http://collector:4318/v1/metrics"))), any(HttpResponse.BodyHandler.class));

    response.close();
  }

  @Test
  void forwardLogSignal_shouldForwardToLogsEndpoint() throws Exception {
    when(collectorResponse.statusCode()).thenReturn(204);
    when(collectorResponse.headers()).thenReturn( java.net.http.HttpHeaders.of( Map.of(), (name, value) -> true));
    when(collectorResponse.body()).thenReturn(null);
    when(client.send( any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(collectorResponse);

    Response response = forwarder.forwardLogSignal( "log".getBytes(), inboundHeaders);

    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(response.getEntity()).isNull();
    verify(client).send( argThat(request -> request.uri().equals( URI.create( "http://collector:4318/v1/logs"))), any(HttpResponse.BodyHandler.class));

    response.close();
  }

  @Test
  void forward_shouldCopyInboundHeaders() throws Exception {
    when(inboundHeaders.getHeaderString(HttpHeaders.CONTENT_TYPE)).thenReturn("application/json");
    when(inboundHeaders.getHeaderString(HttpHeaders.CONTENT_ENCODING)) .thenReturn("gzip"); 
    when(inboundHeaders.getHeaderString(HttpHeaders.AUTHORIZATION)) .thenReturn("Bearer token"); 
    when(collectorResponse.statusCode()).thenReturn(200); 
    when(collectorResponse.headers()).thenReturn( java.net.http.HttpHeaders.of( Map.of(), (name, value) -> true));
    when(collectorResponse.body()).thenReturn(new byte[0]);
    when(client.send( any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(collectorResponse);

    forwarder.forward( "/v1/traces", "payload".getBytes(), inboundHeaders);

    verify(client).send(argThat(request -> request.headers().
                                                   firstValue(HttpHeaders.CONTENT_TYPE).
                                                   orElse("").
                                                   equals("application/json")
                                           &&
                                           request.headers().
                                                   firstValue(HttpHeaders.CONTENT_ENCODING).
                                                   orElse("").
                                                   equals("gzip")
                                           &&
                                           request.headers().
                                                   firstValue(HttpHeaders.AUTHORIZATION).
                                                   orElse("").
                                                   equals("Bearer token")),
                                any(HttpResponse.BodyHandler.class));
  }

  @Test
  void forward_shouldNotCopyBlankInboundHeaders() throws Exception {
    when(inboundHeaders.getHeaderString(HttpHeaders.CONTENT_TYPE)).thenReturn(" "); 
    when(inboundHeaders.getHeaderString(HttpHeaders.CONTENT_ENCODING)).thenReturn("");
    when(inboundHeaders.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(null); 
    when(collectorResponse.statusCode()).thenReturn(200); 
    when(collectorResponse.headers()).thenReturn( java.net.http.HttpHeaders.of( Map.of(), (name, value) -> true)); 
    when(collectorResponse.body()).thenReturn(new byte[0]); 
    when(client.send( any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(collectorResponse); 

    forwarder.forward( "/v1/traces", new byte[0], inboundHeaders);
    verify(client).send(argThat(request -> request.headers().
                                                   firstValue(HttpHeaders.CONTENT_TYPE).
                                                   isEmpty()
                                           &&
                                           request.headers().
                                                   firstValue(HttpHeaders.CONTENT_ENCODING).
                                                   isEmpty()
                                           &&
                                           request.headers().
                                                   firstValue(HttpHeaders.AUTHORIZATION).
                                                   isEmpty()),
                                any(HttpResponse.BodyHandler.class));
  }

  @Test
  void forward_shouldHandleNullPayload() throws Exception {
    when(collectorResponse.statusCode()).thenReturn(200); 
    when(collectorResponse.headers()).thenReturn( java.net.http.HttpHeaders.of( Map.of(), (name, value) -> true));
    when(collectorResponse.body()).thenReturn(new byte[0]);
    when(client.send( any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(collectorResponse); 
    Response response = forwarder.forward( "/v1/traces", null, inboundHeaders);

    assertThat(response.getStatus()).isEqualTo(200);
    verify(client).send(argThat(request -> request.bodyPublisher().orElseThrow().contentLength() == 0), 
                        any(HttpResponse.BodyHandler.class));

    response.close();
  }

  @Test
  void forward_shouldReturnServiceUnavailableWhenInterrupted() throws Exception {
    when(client.send( any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(new InterruptedException("interrupted"));
    assertThatThrownBy(() -> forwarder.forward("/v1/traces", new byte[0], inboundHeaders)).
                                       isInstanceOf(WebApplicationException.class).
                                       satisfies(exception -> {
                                         WebApplicationException webException = (WebApplicationException) exception;
                                         assertThat(webException.getResponse().getStatus()).
                                                                 isEqualTo(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
                                         assertThat(webException.getMessage()).contains( "Interrupted while forwarding OTEL payload");
                                     });
    assertThat(Thread.currentThread().isInterrupted()).isTrue();
  }

  @Test
  void forward_shouldReturnServiceUnavailableWhenClientFails() throws Exception {
    when(client.send( any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).
                thenThrow(new RuntimeException("Connection failed"));

    assertThatThrownBy(() ->forwarder.forward( "/v1/traces", new byte[0], inboundHeaders)).
                                      isInstanceOf(WebApplicationException.class).
                                      satisfies(exception -> {
                                        WebApplicationException webException = (WebApplicationException) exception;

                                        assertThat(webException.getResponse().getStatus()).
                                                isEqualTo(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());

                                        assertThat(webException.getMessage()).contains("Unable to forward OTEL payload");
        });
  }

    @Test
  void copyHeaderIfPresent_shouldCopyHeader() {
    HttpRequest.Builder builder = HttpRequest.newBuilder(); 
    when(inboundHeaders.getHeaderString( HttpHeaders.AUTHORIZATION)).thenReturn("Bearer token"); 
    forwarder.copyHeaderIfPresent(inboundHeaders, builder, HttpHeaders.AUTHORIZATION); 
    HttpRequest request = builder .uri(URI.create("http://localhost")).GET().build(); 
    assertThat( request.headers() .firstValue(HttpHeaders.AUTHORIZATION)) .contains("Bearer token");
  }

  @Test
  void copyHeaderIfPresent_shouldIgnoreNullHeader() {
    HttpRequest.Builder builder = HttpRequest.newBuilder(); 
    when(inboundHeaders.getHeaderString( HttpHeaders.AUTHORIZATION)).thenReturn(null); 
    forwarder.copyHeaderIfPresent( inboundHeaders, builder, HttpHeaders.AUTHORIZATION); 
    HttpRequest request = builder.uri(URI.create("http://localhost")).GET().build(); 
    assertThat( request.headers().firstValue(HttpHeaders.AUTHORIZATION)).isEmpty();
 }

  @Test
  void copyHeaderIfPresent_shouldIgnoreBlankHeader() {
    HttpRequest.Builder builder = HttpRequest.newBuilder(); 
    when(inboundHeaders.getHeaderString( HttpHeaders.AUTHORIZATION)).thenReturn("   "); 
    forwarder.copyHeaderIfPresent( inboundHeaders, builder, HttpHeaders.AUTHORIZATION); 
    HttpRequest request = builder .uri(URI.create("http://localhost")).GET() .build(); 
    assertThat( request.headers().firstValue(HttpHeaders.AUTHORIZATION)).isEmpty();
  }

  @Test
  void copyResponseContentType_shouldCopyFirstValue() {
    Response.ResponseBuilder builder = Response.ok();
    forwarder.copyResponseContentType( Map.of( "content-type", List.of( "application/json", "application/octet-stream")), builder); 
    Response response = builder.build(); 
    assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE)).isEqualTo("application/json");

    response.close();
  }

  @Test
  void copyResponseContentType_shouldIgnoreMissingHeader() {
    Response.ResponseBuilder builder = Response.ok();
    forwarder.copyResponseContentType(Map.of(), builder);

    Response response = builder.build();
    assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE)).isNull();

    response.close();
  }

  @Test
  void copyResponseContentType_shouldIgnoreEmptyHeaderList() {
    Response.ResponseBuilder builder = Response.ok();
    forwarder.copyResponseContentType(Map.of("content-type", List.of()), builder);
    Response response = builder.build();

    assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE)).isNull();

    response.close();
  }

  @Test
  void initialize_shouldCreateClientAndExecutor() {
    OtelSignalForwarder testForwarder = new OtelSignalForwarder();
    testForwarder.initialize();
    assertThatCode(testForwarder::shutdown).doesNotThrowAnyException();
  }

  @Test
  void shutdown_shouldBeSafeBeforeInitialize() {
    OtelSignalForwarder testForwarder = new OtelSignalForwarder();
    testForwarder.shutdown();
  }
}
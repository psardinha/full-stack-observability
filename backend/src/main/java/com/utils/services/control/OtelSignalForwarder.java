package com.utils.services.control;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.utils.services.Control;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.Map;

@Control
public class OtelSignalForwarder {
  static final System.Logger LOGGER = System.getLogger(OtelSignalForwarder.class.getName());

  private HttpClient client;

  private ExecutorService otelForwarderExecutor;

  @ConfigProperty(name = "otel.proxy.max-threads", defaultValue = "2")
  private int maxForwarderThreads;

  @ConfigProperty(name = "otel.proxy.collector.url", defaultValue = "http://localhost:4318")
  private String collectorBaseUrl;

  @PostConstruct
  void initialize() {
    int threadCount = Math.max(1, maxForwarderThreads);
    otelForwarderExecutor = Executors.newFixedThreadPool(threadCount,
                                                         Thread.ofPlatform().name("otel-forwarder-", 0).
                                                                factory());
    client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).
                                     executor(this.otelForwarderExecutor).
                                     build();
  }

  @PreDestroy
  void shutdown() {
    if (this.otelForwarderExecutor != null)
      this.otelForwarderExecutor.shutdown();
  }

  public Response forwardTraceSignal(byte[] payload, HttpHeaders headers) {
    return this.forward("/v1/traces", payload, headers);
  }

  public Response forwardMetricSignal(byte[] payload, HttpHeaders headers) {
    return this.forward("/v1/metrics", payload, headers);
  }

  public Response forwardLogSignal(byte[] payload, HttpHeaders headers) {
    return this.forward("/v1/logs", payload, headers);
  }

  Response forward(String signalPath, byte[] payload, HttpHeaders inboundHeaders) {
    var requestBuilder = HttpRequest.newBuilder().uri(URI.create(this.collectorBaseUrl + signalPath)).
                                     timeout(Duration.ofSeconds(10)).
                                     POST(HttpRequest.BodyPublishers.ofByteArray(payload != null ? payload : new byte[0]));

    copyHeaderIfPresent(inboundHeaders, requestBuilder, HttpHeaders.CONTENT_TYPE);
    copyHeaderIfPresent(inboundHeaders, requestBuilder, HttpHeaders.CONTENT_ENCODING);
    copyHeaderIfPresent(inboundHeaders, requestBuilder, HttpHeaders.AUTHORIZATION);

    try {
      var collectorResponse = this.client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
      var response = Response.status(collectorResponse.statusCode());
      copyResponseContentType(collectorResponse.headers().map(), response);
      byte[] body = collectorResponse.body();
      if (body != null && body.length > 0)
        response.entity(body);
      return response.build();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new WebApplicationException("Interrupted while forwarding OTEL payload to collector", exception,
                                        Response.Status.SERVICE_UNAVAILABLE);
    } catch (Exception exception) {
      LOGGER.log(System.Logger.Level.WARNING, "Failed to forward OTEL payload: {0}", exception.getMessage());
      throw new WebApplicationException("Unable to forward OTEL payload to collector", exception,
                                        Response.Status.SERVICE_UNAVAILABLE);
    }
 }

  void copyHeaderIfPresent(HttpHeaders inboundHeaders, HttpRequest.Builder requestBuilder, String headerName) {
    var value = inboundHeaders.getHeaderString(headerName);
    if (value != null && !value.isBlank())
      requestBuilder.header(headerName, value);
  }

  void copyResponseContentType(Map<String, List<String>> responseHeaders, Response.ResponseBuilder responseBuilder) {
    var contentTypes = responseHeaders.get("content-type");
    if (contentTypes != null && !contentTypes.isEmpty())
      responseBuilder.header(HttpHeaders.CONTENT_TYPE, contentTypes.getFirst());
  }
}
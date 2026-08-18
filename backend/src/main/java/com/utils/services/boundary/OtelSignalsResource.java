package com.utils.services.boundary;

import com.utils.services.Boundary;
import com.utils.services.control.OtelSignalForwarder;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/otel/v1")
@Boundary
public class OtelSignalsResource {
  @Inject
  OtelSignalForwarder otelSignalForwarder;

  @POST
  @Path("/traces")
  @Consumes(MediaType.WILDCARD)
  @Produces(MediaType.WILDCARD)
  public Response traces(byte[] payload, @jakarta.ws.rs.core.Context HttpHeaders headers) {
    return this.otelSignalForwarder.forwardTraceSignal(payload, headers);
  }

  @POST
  @Path("/metrics")
  @Consumes(MediaType.WILDCARD)
  @Produces(MediaType.WILDCARD)
  public Response metrics(byte[] payload, @jakarta.ws.rs.core.Context HttpHeaders headers) {
    return this.otelSignalForwarder.forwardMetricSignal(payload, headers);
  }

  @POST
  @Path("/logs")
  @Consumes(MediaType.WILDCARD)
  @Produces(MediaType.WILDCARD)
  public Response logs(byte[] payload, @jakarta.ws.rs.core.Context HttpHeaders headers) {
    return this.otelSignalForwarder.forwardLogSignal(payload, headers);
  }
}
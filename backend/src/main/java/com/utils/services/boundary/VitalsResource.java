package com.utils.services.boundary;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.utils.services.Boundary;
import com.utils.services.control.VitalsMetricsServiceSimple;
import com.utils.services.entity.VitalMetricRequest;

@Path("/api/metrics/vitals")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Boundary
public class VitalsResource {
  private static final Logger LOGGER = Logger.getLogger(VitalsResource.class.getName());

  private final VitalsMetricsServiceSimple vitalsMetricsService;

  @Inject
  public VitalsResource(VitalsMetricsServiceSimple vitalsMetricsService) {
    this.vitalsMetricsService = vitalsMetricsService;
  }

  @POST
  public Response recordVital(VitalMetricRequest request) {
    try {
      LOGGER.log(Level.INFO, "Received vital metric request: {0}", request);

      if (request == null || request.type() == null || request.value() == null) {
        LOGGER.log(Level.WARNING, "Invalid vital metric request: {0}", request);
        return Response.status(Response.Status.BAD_REQUEST).
                        entity("{\"error\": \"Invalid request\"}").
                        build();
      }

      vitalsMetricsService.recordVital(request.type(), request.value(), request.pageRoute());
      return Response.ok().entity("{\"status\": \"success\"}").build();
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error recording vital metric", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).
                      entity("{\"error\": \"" + e.getMessage() + "\"}").
                      build();
    }
  }
}

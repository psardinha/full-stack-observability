package com.utils.services.boundary;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.utils.services.entity.LengthResponse;
import com.utils.services.entity.ReverseRequest;
import com.utils.services.entity.ReverseResponse;

@Path("/utils")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "service_uri")
public interface StringUtilsClient {
  @POST
  @Path("/reverse")
  ReverseResponse reverse(ReverseRequest request);

  @GET
  @Path("/length")
  LengthResponse length(@QueryParam("string") String string);
}

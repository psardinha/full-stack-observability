package com.utils.services.boundary;

import com.utils.services.Boundary;
import com.utils.services.control.StringUtils;
import com.utils.services.entity.LengthResponse;
import com.utils.services.entity.ReverseRequest;
import com.utils.services.entity.ReverseResponse;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/utils")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Boundary
public class StringUtilsResource {
  @Inject
  StringUtils stringUtils;

  @POST
  @Path("/reverse")
  public ReverseResponse reverse(ReverseRequest request) {
    if (request == null || request.input() == null)
      return new ReverseResponse(stringUtils.reverse(null));
    return new ReverseResponse(stringUtils.reverse(request.input()));
  }

  @GET
  @Path("/length")
  public LengthResponse length(@QueryParam("string") String string) {
    if (string == null)
      return new LengthResponse(0);
    return new LengthResponse(stringUtils.length(string));
  }
}
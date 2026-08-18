package com.utils.services.boundary;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import com.utils.services.entity.ErrorResponse;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

@Provider
public class BadRequestExceptionMapper implements ExceptionMapper<BadRequestException> {
  @Override
  public Response toResponse(BadRequestException exception) {
    return Response.status(Response.Status.BAD_REQUEST).
                    type(MediaType.APPLICATION_JSON).
                    entity(new ErrorResponse(exception.getMessage())).
                    build();
  }
}

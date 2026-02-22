package com.myapp.shopping.adapter.restapi;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;


@Provider
@Slf4j
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {
        log.error("Unhandled exception caught in REST layer", exception);

        if (exception instanceof IllegalArgumentException) {
            return build(Response.Status.BAD_REQUEST, "Invalid input data");
        }

        if (exception instanceof jakarta.ws.rs.NotFoundException) {
            return build(Response.Status.NOT_FOUND, "Resource not found");
        }


        return build(Response.Status.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private Response build(Response.Status status, String message) {
        return Response.status(status)
                .entity(new ErrorResponse(message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}


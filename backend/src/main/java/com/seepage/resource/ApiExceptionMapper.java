package com.seepage.resource;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

/** 统一错误响应：{ "message": "..." }，前端直接展示 */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception e) {
        if (e instanceof WebApplicationException wae) {
            int status = wae.getResponse().getStatus();
            String msg = e.getMessage() == null ? wae.getResponse().getStatusInfo().getReasonPhrase() : e.getMessage();
            return Response.status(status)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("message", msg))
                    .build();
        }
        return Response.status(500)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("message", "服务器错误: " + e.getMessage()))
                .build();
    }
}

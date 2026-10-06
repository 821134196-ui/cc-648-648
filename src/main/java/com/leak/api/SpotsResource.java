package com.leak.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/spots")
@Produces(MediaType.APPLICATION_JSON)
public class SpotsResource {

    @Inject
    DtoAssembler assembler;

    @GET
    public Object list() {
        return assembler.listSpots();
    }
}

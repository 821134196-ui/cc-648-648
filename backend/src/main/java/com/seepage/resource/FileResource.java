package com.seepage.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.nio.file.Files;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** 本地照片文件服务 */
@Path("/files")
public class FileResource {

    @ConfigProperty(name = "app.photo-dir")
    String photoDir;

    @GET
    @Path("/{name}")
    public Response get(@PathParam("name") String name) {
        if (name.contains("/") || name.contains("..") || name.contains("\\")) {
            throw new NotFoundException();
        }
        java.nio.file.Path base = java.nio.file.Path.of(photoDir).toAbsolutePath().normalize();
        java.nio.file.Path file = base.resolve(name).normalize();
        if (!file.startsWith(base) || !Files.exists(file)) {
            throw new NotFoundException();
        }
        String lower = name.toLowerCase();
        String type = lower.endsWith(".svg") ? "image/svg+xml"
                : lower.endsWith(".png") ? "image/png"
                : lower.endsWith(".jpg") || lower.endsWith(".jpeg") ? "image/jpeg"
                : lower.endsWith(".gif") ? "image/gif"
                : lower.endsWith(".webp") ? "image/webp"
                : "application/octet-stream";
        return Response.ok(file.toFile(), type).build();
    }
}

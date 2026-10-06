package com.leak.api;

import com.leak.service.PhotoStorage;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import java.nio.file.Files;

/** 读取本地存储的照片文件。 */
@Path("/photos/{fileName}")
public class PhotoResource {

    @Inject
    PhotoStorage storage;

    @GET
    public Response get(@PathParam("fileName") String fileName) throws Exception {
        // 防止路径穿越
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        java.nio.file.Path file = storage.resolve(fileName);
        if (!Files.isRegularFile(file)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        String type = Files.probeContentType(file);
        if (type == null) {
            type = "application/octet-stream";
        }
        return Response.ok(file.toFile(), type)
                .header("Cache-Control", "max-age=3600")
                .build();
    }
}

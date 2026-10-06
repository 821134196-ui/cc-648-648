package com.leak.api;

import com.leak.entity.Report;
import com.leak.model.RecheckResult;
import com.leak.service.ReportService;
import com.leak.service.UploadedPhoto;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.resteasy.reactive.RestForm;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Path("/api/reports")
@Produces(MediaType.APPLICATION_JSON)
public class ReportsResource {

    @Inject
    ReportService reports;

    @Inject
    DtoAssembler assembler;

    @GET
    public Object list() {
        return assembler.listReports();
    }

    @GET
    @Path("/{id}")
    public Object detail(@PathParam("id") Long id) {
        return assembler.detail(id);
    }

    // ---- 住户提交报修 ----

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Object submit(@RestForm("building") String building,
                         @RestForm("facade") String facade,
                         @RestForm("location") String location,
                         @RestForm("room") String room,
                         @RestForm("residentName") String residentName,
                         @RestForm("contact") String contact,
                         @RestForm("description") String description,
                         @RestForm("photos") List<FileUpload> photos) {
        Report r = reports.submit(building, facade, location, room, residentName,
                contact, description, toUploads(photos));
        return assembler.detail(r.id);
    }

    // ---- 维修员记录施工 ----

    @POST
    @Path("/{id}/repairs")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Object addRepair(@PathParam("id") Long id,
                            @RestForm("workerName") String workerName,
                            @RestForm("summary") String summary,
                            @RestForm("happenedAt") String happenedAt,
                            @RestForm("photos") List<FileUpload> photos) {
        reports.addRepair(id, workerName, summary, parseTime(happenedAt),
                toUploads(photos), null);
        return assembler.detail(id);
    }

    // ---- 复查人员雨后复查 ----

    @POST
    @Path("/{id}/rechecks")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Object addRecheck(@PathParam("id") Long id,
                             @RestForm("inspectorName") String inspectorName,
                             @RestForm("result") String result,
                             @RestForm("conclusion") String conclusion,
                             @RestForm("photos") List<FileUpload> photos) {
        RecheckResult parsed;
        try {
            parsed = result == null ? null : RecheckResult.valueOf(result);
        } catch (IllegalArgumentException e) {
            throw new com.leak.service.BusinessException("复查结果取值非法: " + result);
        }
        reports.addRecheck(id, inspectorName, parsed, conclusion,
                toUploads(photos), null);
        return assembler.detail(id);
    }

    // ---- 住户提出异议 ----

    @POST
    @Path("/{id}/objections")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Object object(@PathParam("id") Long id,
                         @RestForm("reason") String reason,
                         @RestForm("photos") List<FileUpload> photos) {
        reports.object(id, reason, toUploads(photos));
        return assembler.detail(id);
    }

    static List<UploadedPhoto> toUploads(List<FileUpload> files) {
        List<UploadedPhoto> out = new ArrayList<>();
        if (files != null) {
            for (FileUpload f : files) {
                if (f == null || f.uploadedFile() == null) {
                    continue;
                }
                out.add(new UploadedPhoto(f.uploadedFile(), f.fileName(), f.contentType()));
            }
        }
        return out;
    }

    /** datetime-local 提交 "2026-10-03T09:00"；仅日期时补默认上午 9 点。 */
    static LocalDateTime parseTime(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String t = s.trim();
        if (t.length() == 10) {
            return LocalDateTime.parse(t + "T09:00");
        }
        return LocalDateTime.parse(t);
    }
}

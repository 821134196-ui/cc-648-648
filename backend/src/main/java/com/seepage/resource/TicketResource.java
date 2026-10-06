package com.seepage.resource;

import com.seepage.dto.DtoAssembler;
import com.seepage.dto.Requests;
import com.seepage.dto.Views;
import com.seepage.model.Photo;
import com.seepage.model.Phase;
import com.seepage.model.RepairTicket;
import com.seepage.model.TicketStatus;
import com.seepage.service.TicketService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@Path("/api/tickets")
@Produces(MediaType.APPLICATION_JSON)
public class TicketResource {

    @Inject
    TicketService service;

    @Inject
    DtoAssembler dto;

    @ConfigProperty(name = "app.photo-dir")
    String photoDir;

    @GET
    public List<Views.TicketSummary> list(@QueryParam("status") TicketStatus status) {
        List<RepairTicket> tickets = status == null
                ? RepairTicket.list("order by id")
                : RepairTicket.list("status = ?1 order by id", status);
        return tickets.stream().map(dto::summary).toList();
    }

    @POST
    public Views.TicketDetail create(Requests.CreateTicket req) {
        return dto.detail(service.create(req));
    }

    @GET
    @Path("/{id}")
    public Views.TicketDetail detail(@PathParam("id") long id) {
        return dto.detail(service.mustFind(id));
    }

    @POST
    @Path("/{id}/start-repair")
    public Views.TicketDetail startRepair(@PathParam("id") long id) {
        return dto.detail(service.startRepair(id));
    }

    @POST
    @Path("/{id}/construction")
    public Views.TicketDetail construction(@PathParam("id") long id, Requests.Construction req) {
        service.completeConstruction(id, req);
        return dto.detail(service.mustFind(id));
    }

    @GET
    @Path("/{id}/recheck-eligibility")
    public Views.EligibilityView eligibility(@PathParam("id") long id) {
        return service.eligibility(service.mustFind(id));
    }

    @POST
    @Path("/{id}/recheck")
    public Views.TicketDetail recheck(@PathParam("id") long id, Requests.Recheck req) {
        service.recheck(id, req);
        return dto.detail(service.mustFind(id));
    }

    @POST
    @Path("/{id}/link-point/{pointId}")
    public Views.TicketDetail linkPoint(@PathParam("id") long id, @PathParam("pointId") long pointId) {
        return dto.detail(service.linkPoint(id, pointId));
    }

    @POST
    @Path("/{id}/recurrence")
    public Views.TicketDetail recurrence(@PathParam("id") long id, Requests.Recurrence req) {
        return dto.detail(service.createRecurrence(id, req));
    }

    @POST
    @Path("/{id}/disputes")
    public Views.TicketDetail dispute(@PathParam("id") long id, Requests.DisputeReq req) {
        service.addDispute(id, req);
        return dto.detail(service.mustFind(id));
    }

    @POST
    @Path("/{id}/disputes/{disputeId}/reply")
    public Views.TicketDetail replyDispute(@PathParam("id") long id,
                                           @PathParam("disputeId") long disputeId,
                                           Requests.ReplyReq req) {
        service.replyDispute(id, disputeId, req);
        return dto.detail(service.mustFind(id));
    }

    /** 照片上传：multipart，字段名 file，可多文件；按阶段归档 */
    @POST
    @Path("/{id}/photos")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public List<Views.PhotoView> upload(@PathParam("id") long id,
                                        @QueryParam("phase") Phase phase,
                                        @RestForm("file") List<FileUpload> files) throws IOException {
        RepairTicket t = service.mustFind(id);
        if (phase == null) {
            throw new BadRequestException("缺少 phase 参数（REPORT/CONSTRUCTION/RECHECK）");
        }
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("未收到文件");
        }
        java.nio.file.Path dir = java.nio.file.Path.of(photoDir);
        Files.createDirectories(dir);

        List<Views.PhotoView> out = new ArrayList<>();
        for (FileUpload f : files) {
            String original = f.fileName() == null ? "photo" : f.fileName();
            String ext = "";
            int dot = original.lastIndexOf('.');
            if (dot >= 0 && dot < original.length() - 1) {
                ext = original.substring(dot).toLowerCase();
                if (ext.length() > 8 || !ext.matches("\\.[a-z0-9]+")) {
                    ext = "";
                }
            }
            String name = UUID.randomUUID() + ext;
            Files.move(f.uploadedFile(), dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);

            Photo p = new Photo();
            p.ticket = t;
            p.phase = phase;
            p.fileName = name;
            p.label = original;
            p.persist();
            out.add(dto.photo(p));
        }
        return out;
    }
}

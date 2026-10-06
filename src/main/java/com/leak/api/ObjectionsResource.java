package com.leak.api;

import com.leak.entity.Objection;
import com.leak.entity.Report;
import com.leak.model.ObjectionOutcome;
import com.leak.service.BusinessException;
import com.leak.service.ReportService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api/objections")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ObjectionsResource {

    @Inject
    ReportService reports;

    @Inject
    DtoAssembler assembler;

    /** 物业回复住户异议：MAINTAIN 维持原结论 / REOPEN 重新安排处理。 */
    @POST
    @Path("/{id}/reply")
    public Object reply(@PathParam("id") Long id, ReplyRequest req) {
        ObjectionOutcome outcome;
        try {
            outcome = ObjectionOutcome.valueOf(req.outcome);
        } catch (Exception e) {
            throw new BusinessException("处理结果取值非法: " + req.outcome);
        }
        Objection o = reports.replyObjection(id, outcome, req.replyNote, req.repliedBy);
        return assembler.detail(o.report.id);
    }

    public static class ReplyRequest {
        public String outcome;
        public String replyNote;
        public String repliedBy;
    }
}

package com.seepage.dto;

import com.seepage.model.ConstructionRecord;
import com.seepage.model.Dispute;
import com.seepage.model.LeakPoint;
import com.seepage.model.Photo;
import com.seepage.model.Phase;
import com.seepage.model.RecheckRecord;
import com.seepage.model.RepairTicket;
import com.seepage.service.TicketService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

/** 实体 → 视图组装，避免 Jackson 直接序列化实体造成懒加载/循环引用问题 */
@ApplicationScoped
public class DtoAssembler {

    @Inject
    TicketService ticketService;

    public Views.TicketRef ref(RepairTicket t) {
        return new Views.TicketRef(t.id, t.room, t.residentName, t.status);
    }

    public Views.TicketSummary summary(RepairTicket t) {
        return new Views.TicketSummary(t.id, t.building, t.facade, t.room, t.residentName,
                t.status, t.createdAt, t.point.note,
                t.recurrenceOf != null,
                RepairTicket.count("recurrenceOf", t) > 0);
    }

    public Views.PointView point(LeakPoint p, Long excludeTicketId) {
        List<Views.TicketRef> tickets = RepairTicket.<RepairTicket>list("point", p).stream()
                .filter(t -> excludeTicketId == null || !t.id.equals(excludeTicketId))
                .map(this::ref)
                .toList();
        return new Views.PointView(p.id, p.building, p.facade, p.note, tickets);
    }

    public Views.PhotoView photo(Photo p) {
        return new Views.PhotoView(p.id, "/files/" + p.fileName, p.label, p.uploadedAt);
    }

    public Views.ConstructionView construction(ConstructionRecord r) {
        return new Views.ConstructionView(r.id, r.workerName, r.content, r.completedAt, r.createdAt);
    }

    public Views.RecheckView recheck(RecheckRecord r) {
        return new Views.RecheckView(r.id, r.inspectorName, r.result, r.content, r.checkedAt,
                r.rainEvent != null ? r.rainEvent.date : null,
                r.rainEvent != null ? r.rainEvent.rainfallMm : null);
    }

    public Views.DisputeView dispute(Dispute d) {
        return new Views.DisputeView(d.id, d.residentName, d.content, d.status,
                d.reply, d.resolution, d.createdAt, d.repliedAt);
    }

    public Views.TicketDetail detail(RepairTicket t) {
        List<Photo> photos = Photo.list("ticket", t);
        List<Views.PhotoView> report = photosOf(photos, Phase.REPORT);
        List<Views.PhotoView> construction = photosOf(photos, Phase.CONSTRUCTION);
        List<Views.PhotoView> recheck = photosOf(photos, Phase.RECHECK);

        List<Views.ConstructionView> constructions = ConstructionRecord
                .<ConstructionRecord>list("ticket = ?1 order by completedAt, id", t).stream()
                .map(this::construction).toList();
        List<Views.RecheckView> rechecks = RecheckRecord
                .<RecheckRecord>list("ticket = ?1 order by checkedAt, id", t).stream()
                .map(this::recheck).toList();
        List<Views.DisputeView> disputes = Dispute
                .<Dispute>list("ticket = ?1 order by createdAt, id", t).stream()
                .map(this::dispute).toList();
        List<Views.TicketRef> recurrences = RepairTicket
                .<RepairTicket>list("recurrenceOf = ?1 order by id", t).stream()
                .map(this::ref).toList();

        return new Views.TicketDetail(t.id, t.building, t.facade, t.room, t.residentName, t.phone,
                t.description, t.status, t.createdAt,
                point(t.point, t.id),
                RepairTicket.<RepairTicket>list("point = ?1 and id <> ?2 order by id", t.point, t.id)
                        .stream().map(this::ref).toList(),
                t.recurrenceOf != null ? ref(t.recurrenceOf) : null,
                recurrences,
                report, construction, recheck,
                constructions, rechecks, disputes,
                ticketService.eligibility(t));
    }

    private List<Views.PhotoView> photosOf(List<Photo> photos, Phase phase) {
        return photos.stream().filter(p -> p.phase == phase).map(this::photo).toList();
    }
}

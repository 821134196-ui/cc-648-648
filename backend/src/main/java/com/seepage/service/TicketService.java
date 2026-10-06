package com.seepage.service;

import com.seepage.dto.Requests;
import com.seepage.dto.Views;
import com.seepage.model.ConstructionRecord;
import com.seepage.model.Dispute;
import com.seepage.model.DisputeStatus;
import com.seepage.model.LeakPoint;
import com.seepage.model.RainEvent;
import com.seepage.model.RecheckRecord;
import com.seepage.model.RecheckResult;
import com.seepage.model.RepairTicket;
import com.seepage.model.Resolution;
import com.seepage.model.TicketStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** 工单状态机与业务规则 */
@ApplicationScoped
public class TicketService {

    @Inject
    WeatherService weather;

    public RepairTicket mustFind(long id) {
        RepairTicket t = RepairTicket.findById(id);
        if (t == null) {
            throw new NotFoundException("工单不存在: " + id);
        }
        return t;
    }

    private static void require(boolean ok, String message) {
        if (!ok) {
            throw new BadRequestException(message);
        }
    }

    @Transactional
    public RepairTicket create(Requests.CreateTicket req) {
        require(req.building() != null && !req.building().isBlank(), "请填写楼栋");
        require(req.facade() != null && !req.facade().isBlank(), "请填写立面");
        require(req.room() != null && !req.room().isBlank(), "请填写房间号");
        require(req.residentName() != null && !req.residentName().isBlank(), "请填写报修人");

        LeakPoint point;
        if (req.pointId() != null) {
            // 住户/物业选择了已有点位：同点位关联
            point = LeakPoint.findById(req.pointId());
            require(point != null, "所选点位不存在");
        } else {
            point = new LeakPoint();
            point.building = req.building();
            point.facade = req.facade();
            point.note = (req.locationNote() == null || req.locationNote().isBlank())
                    ? req.building() + " " + req.facade() : req.locationNote();
            point.persist();
        }

        RepairTicket t = new RepairTicket();
        t.point = point;
        t.building = req.building();
        t.facade = req.facade();
        t.room = req.room();
        t.residentName = req.residentName();
        t.phone = req.phone();
        t.description = req.description();
        t.persist();
        return t;
    }

    @Transactional
    public RepairTicket startRepair(long id) {
        RepairTicket t = mustFind(id);
        require(t.status == TicketStatus.REPORTED, "仅“待受理”工单可开始维修");
        t.status = TicketStatus.IN_REPAIR;
        return t;
    }

    /** 登记施工记录并完工：进入待复查，等待有效降雨 */
    @Transactional
    public ConstructionRecord completeConstruction(long id, Requests.Construction req) {
        RepairTicket t = mustFind(id);
        require(t.status == TicketStatus.IN_REPAIR, "工单不在“维修中”状态，不能登记完工");
        require(req.workerName() != null && !req.workerName().isBlank(), "请填写施工人员");

        ConstructionRecord r = new ConstructionRecord();
        r.ticket = t;
        r.workerName = req.workerName();
        r.content = req.content();
        r.completedAt = weather.currentDate();
        r.persist();

        t.status = TicketStatus.PENDING_RECHECK;
        return r;
    }

    /** 复查资格：最近一次完工之后是否已发生有效降雨 */
    public Views.EligibilityView eligibility(RepairTicket t) {
        ConstructionRecord latest = ConstructionRecord
                .find("ticket = ?1 order by completedAt desc, id desc", t)
                .firstResult();
        double threshold = weather.threshold();
        if (latest == null) {
            return new Views.EligibilityView(false, "尚未完成施工", null,
                    weather.currentDate(), threshold, List.of());
        }
        List<RainEvent> rains = weather.effectiveRainsAfter(latest.completedAt);
        List<Views.RainEventView> rainViews = rains.stream()
                .map(e -> new Views.RainEventView(e.id, e.date, e.rainfallMm, true, e.note))
                .toList();
        if (rains.isEmpty()) {
            return new Views.EligibilityView(false,
                    "完工（" + latest.completedAt + "）后暂无有效降雨（≥" + fmt(threshold)
                            + "mm），不满足雨后复查条件，不能标记验证通过",
                    latest.completedAt, weather.currentDate(), threshold, rainViews);
        }
        RainEvent first = rains.get(0);
        return new Views.EligibilityView(true,
                "已满足雨后复查条件：" + first.date + " 降雨 " + fmt(first.rainfallMm) + "mm",
                latest.completedAt, weather.currentDate(), threshold, rainViews);
    }

    /** 提交复查：未满足雨后条件一律拒绝（包括完工当天直接验证） */
    @Transactional
    public RecheckRecord recheck(long id, Requests.Recheck req) {
        RepairTicket t = mustFind(id);
        require(t.status == TicketStatus.PENDING_RECHECK, "工单不在“待复查”状态");
        require(req.inspectorName() != null && !req.inspectorName().isBlank(), "请填写复查人员");
        require(req.result() != null, "请选择复查结论");

        Views.EligibilityView el = eligibility(t);
        if (!el.eligible()) {
            throw new BadRequestException("未达到雨后复查条件：" + el.reason());
        }

        RecheckRecord r = new RecheckRecord();
        r.ticket = t;
        r.inspectorName = req.inspectorName();
        r.result = req.result();
        r.content = req.content();
        r.checkedAt = weather.currentDate();
        r.rainEvent = RainEvent.findById(el.effectiveRains().get(0).id());
        r.persist();

        t.status = req.result() == RecheckResult.PASS ? TicketStatus.VERIFIED : TicketStatus.IN_REPAIR;
        return r;
    }

    /** 同点位关联：把工单挂到已有点位；原点位若无其他工单则删除 */
    @Transactional
    public RepairTicket linkPoint(long ticketId, long pointId) {
        RepairTicket t = mustFind(ticketId);
        LeakPoint p = LeakPoint.findById(pointId);
        require(p != null, "点位不存在: " + pointId);
        LeakPoint old = t.point;
        if (Objects.equals(old.id, p.id)) {
            return t;
        }
        t.point = p;
        if (RepairTicket.count("point", old) == 0) {
            old.delete();
        }
        return t;
    }

    /** 复发报修：原点位再次漏水，新工单关联原工单，保留完整历史 */
    @Transactional
    public RepairTicket createRecurrence(long id, Requests.Recurrence req) {
        RepairTicket orig = mustFind(id);
        require(orig.status == TicketStatus.VERIFIED, "仅“复查通过”的工单可登记复发");
        require(req.residentName() != null && !req.residentName().isBlank(), "请填写报修人");
        require(req.room() != null && !req.room().isBlank(), "请填写房间号");

        RepairTicket t = new RepairTicket();
        t.point = orig.point;
        t.building = orig.building;
        t.facade = orig.facade;
        t.room = req.room();
        t.residentName = req.residentName();
        t.phone = req.phone();
        t.description = req.description();
        t.recurrenceOf = orig;
        t.persist();
        return t;
    }

    @Transactional
    public Dispute addDispute(long id, Requests.DisputeReq req) {
        RepairTicket t = mustFind(id);
        require(t.status == TicketStatus.VERIFIED, "仅“复查通过”的工单可提出异议");
        require(req.content() != null && !req.content().isBlank(), "请填写异议内容");

        Dispute d = new Dispute();
        d.ticket = t;
        d.residentName = (req.residentName() == null || req.residentName().isBlank())
                ? t.residentName : req.residentName();
        d.content = req.content();
        d.persist();

        t.status = TicketStatus.DISPUTED;
        return d;
    }

    @Transactional
    public Dispute replyDispute(long ticketId, long disputeId, Requests.ReplyReq req) {
        RepairTicket t = mustFind(ticketId);
        Dispute d = Dispute.findById(disputeId);
        require(d != null && d.ticket.id.equals(t.id), "异议不存在");
        require(d.status == DisputeStatus.OPEN, "该异议已回复");
        require(req.reply() != null && !req.reply().isBlank(), "请填写回复内容");
        require(req.resolution() != null, "请选择处理方式");

        d.reply = req.reply();
        d.resolution = req.resolution();
        d.status = DisputeStatus.REPLIED;
        d.repliedAt = LocalDateTime.now();

        t.status = switch (req.resolution()) {
            case KEEP_VERIFIED -> TicketStatus.VERIFIED;
            case REOPEN_REPAIR -> TicketStatus.IN_REPAIR;
            case RECHECK_AGAIN -> TicketStatus.PENDING_RECHECK;
        };
        return d;
    }

    private static String fmt(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}

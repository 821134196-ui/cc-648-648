package com.leak.service;

import com.leak.entity.*;
import com.leak.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 报修与状态流转核心规则：
 * - 同楼栋+立面+位置的报修自动关联到同一物理点位，各住户报修单独立保留；
 * - 同一户在该点位再次报修记为复发（recurrenceNo 递增），点位曾解决则重新打开；
 * - 施工完成后进入“待复查”，必须出现晚于施工日的有效降雨才允许回原址复查通过；
 * - 复查不通过回到待维修；住户可对已验证结论提异议，物业回复后维持或重开。
 */
@ApplicationScoped
public class ReportService {

    @Inject
    SpotService spots;

    @Inject
    WeatherService weather;

    @Inject
    PhotoStorage storage;

    @Transactional
    public Report submit(String building, String facade, String location, String room,
                         String residentName, String contact, String description,
                         List<UploadedPhoto> uploadedPhotos) {
        require(building, "楼栋");
        require(facade, "立面");
        require(location, "位置");
        require(room, "房间");
        require(residentName, "住户姓名");

        LeakSpot spot = spots.getOrCreate(building, facade, location);
        String normRoom = norm(room);

        Report prior = Report.<Report>list("spot = ?1 order by createdAt desc", spot).stream()
                .filter(r -> norm(r.room).equals(normRoom))
                .findFirst().orElse(null);

        Report report = new Report();
        report.spot = spot;
        report.room = room.trim();
        report.residentName = residentName.trim();
        report.contact = contact;
        report.description = description;
        report.createdAt = LocalDateTime.now();

        if (prior != null) {
            // 同一户同一位置再次报修 = 原问题复发，挂到原报修链上，历史处理过程保留可查
            report.recurrenceNo = prior.recurrenceNo + 1;
            report.recurrenceOf = prior;
            report.reopenedAfterResolved = spot.status == SpotStatus.RESOLVED;
        } else {
            report.recurrenceNo = 1;
        }
        report.status = ReportStatus.PENDING_REPAIR;
        report.persist();

        for (UploadedPhoto up : nullSafe(uploadedPhotos)) {
            attachReportPhoto(report, up, null);
        }

        spots.refreshStatus(spot);
        return report;
    }

    @Transactional
    public RepairRecord addRepair(Long reportId, String workerName, String summary,
                                  LocalDateTime happenedAt, List<UploadedPhoto> photos,
                                  List<String> captions) {
        Report report = getReport(reportId);
        require(workerName, "维修员姓名");
        LocalDateTime doneAt = happenedAt != null ? happenedAt : LocalDateTime.now();

        RepairRecord repair = new RepairRecord();
        repair.spot = report.spot;
        repair.workerName = workerName.trim();
        repair.summary = summary;
        repair.happenedAt = doneAt;
        repair.createdAt = LocalDateTime.now();
        repair.persist();

        for (int i = 0; i < nullSafe(photos).size(); i++) {
            String caption = captions != null && i < captions.size() ? captions.get(i) : null;
            attachRepairPhoto(repair, photos.get(i), caption);
        }

        // 同一点位关联的所有住户报修单一起进入待复查，等待同一场有效降雨
        for (Report r : report.spot.reports) {
            if (r.status == ReportStatus.PENDING_REPAIR
                    || r.status == ReportStatus.PENDING_RECHECK) {
                r.status = ReportStatus.PENDING_RECHECK;
                r.awaitingSince = doneAt;
            }
        }
        spots.refreshStatus(report.spot);
        return repair;
    }

    @Transactional
    public RecheckRecord addRecheck(Long reportId, String inspectorName, RecheckResult result,
                                    String conclusion, List<UploadedPhoto> photos,
                                    List<String> captions) {
        Report report = getReport(reportId);
        require(inspectorName, "复查人员姓名");
        if (result == null) {
            throw new BusinessException("请选择复查结论（未见渗水 / 仍有渗水）");
        }
        if (report.status != ReportStatus.PENDING_RECHECK) {
            throw new BusinessException("当前报修单状态为「" + report.status.label
                    + "」，不能提交复查记录");
        }

        // 关键规则：未满足有效雨后条件，禁止复查通过，任务保持待复查
        WeatherService.RainEligibility eligibility = weather.evaluate(report);
        if (!eligibility.eligible()) {
            throw new BusinessException("不满足雨后复查条件：" + eligibility.message());
        }
        RainRecord rain = eligibility.qualifyingRain();

        RecheckRecord recheck = new RecheckRecord();
        recheck.report = report;
        recheck.inspectorName = inspectorName.trim();
        recheck.result = result;
        recheck.conclusion = conclusion;
        recheck.qualifyingRainDate = rain.rainDate;
        recheck.qualifyingRainMm = rain.mm;
        recheck.createdAt = LocalDateTime.now();
        recheck.persist();

        for (int i = 0; i < nullSafe(photos).size(); i++) {
            String caption = captions != null && i < captions.size() ? captions.get(i) : null;
            attachRecheckPhoto(recheck, photos.get(i), caption);
        }

        if (result == RecheckResult.DRY) {
            report.status = ReportStatus.VERIFIED;
        } else {
            // 雨后复查仍有渗水：回到待维修，等待重新施工与下一场有效降雨
            report.status = ReportStatus.PENDING_REPAIR;
            report.awaitingSince = null;
        }
        spots.refreshStatus(report.spot);
        return recheck;
    }

    @Transactional
    public Objection object(Long reportId, String reason, List<UploadedPhoto> photos) {
        Report report = getReport(reportId);
        require(reason, "异议理由");
        if (report.status != ReportStatus.VERIFIED) {
            throw new BusinessException("只有「已验证修复」的报修单可以提出异议");
        }
        Objection objection = new Objection();
        objection.report = report;
        objection.reason = reason.trim();
        objection.outcome = ObjectionOutcome.PENDING;
        objection.createdAt = LocalDateTime.now();
        objection.persist();

        for (UploadedPhoto up : nullSafe(photos)) {
            attachObjectionPhoto(objection, up, null);
        }

        report.status = ReportStatus.OBJECTIONED;
        spots.refreshStatus(report.spot);
        return objection;
    }

    @Transactional
    public Objection replyObjection(Long objectionId, ObjectionOutcome outcome,
                                    String replyNote, String repliedBy) {
        Objection objection = Objection.findById(objectionId);
        if (objection == null) {
            throw new BusinessException("异议不存在: " + objectionId);
        }
        if (objection.outcome != ObjectionOutcome.PENDING) {
            throw new BusinessException("该异议已回复");
        }
        if (outcome == null || outcome == ObjectionOutcome.PENDING) {
            throw new BusinessException("请选择物业处理结果（维持原结论 / 重新安排处理）");
        }
        require(replyNote, "物业回复内容");
        objection.outcome = outcome;
        objection.replyNote = replyNote.trim();
        objection.repliedBy = repliedBy;
        objection.repliedAt = LocalDateTime.now();

        Report report = objection.report;
        if (outcome == ObjectionOutcome.MAINTAIN) {
            report.status = ReportStatus.VERIFIED;
        } else {
            // 物业认可住户异议，重新安排处理：回到待维修，需重新施工并再次等待有效降雨
            report.status = ReportStatus.PENDING_REPAIR;
            report.awaitingSince = null;
        }
        spots.refreshStatus(report.spot);
        return objection;
    }

    public Report getReport(Long id) {
        Report r = Report.findById(id);
        if (r == null) {
            throw new BusinessException("报修单不存在: " + id);
        }
        return r;
    }

    // ---- 照片挂载 ----

    private void attachReportPhoto(Report report, UploadedPhoto up, String caption) {
        String fileName = storage.store(up.path(), up.originalName(), up.contentType());
        Photo p = basePhoto(PhotoStage.REPORT, fileName, up.contentType(), caption);
        p.report = report;
        p.persist();
    }

    private void attachRepairPhoto(RepairRecord repair, UploadedPhoto up, String caption) {
        String fileName = storage.store(up.path(), up.originalName(), up.contentType());
        Photo p = basePhoto(PhotoStage.REPAIR, fileName, up.contentType(), caption);
        p.repair = repair;
        p.persist();
    }

    private void attachRecheckPhoto(RecheckRecord recheck, UploadedPhoto up, String caption) {
        String fileName = storage.store(up.path(), up.originalName(), up.contentType());
        Photo p = basePhoto(PhotoStage.RECHECK, fileName, up.contentType(), caption);
        p.recheck = recheck;
        p.persist();
    }

    private void attachObjectionPhoto(Objection objection, UploadedPhoto up, String caption) {
        String fileName = storage.store(up.path(), up.originalName(), up.contentType());
        Photo p = basePhoto(PhotoStage.OBJECTION, fileName, up.contentType(), caption);
        p.objection = objection;
        p.persist();
    }

    private Photo basePhoto(PhotoStage stage, String fileName, String contentType, String caption) {
        Photo p = new Photo();
        p.stage = stage;
        p.fileName = fileName;
        p.contentType = contentType != null ? contentType : "image/jpeg";
        p.caption = caption;
        p.createdAt = LocalDateTime.now();
        return p;
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase().replaceAll("\\s+", "");
    }

    private static void require(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(field + "不能为空");
        }
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? List.of() : list;
    }
}

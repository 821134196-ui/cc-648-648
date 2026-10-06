package com.leak.api;

import com.leak.entity.*;
import com.leak.model.ObjectionOutcome;
import com.leak.model.ReportStatus;
import com.leak.model.SpotStatus;
import com.leak.service.WeatherService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

/** 组装接口返回结构（在事务内遍历懒加载关联）。 */
@ApplicationScoped
public class DtoAssembler {

    @Inject
    WeatherService weather;

    public record PhotoDto(Long id, String url, String stage, String stageLabel,
                           String caption, String createdAt) {
    }

    public record SpotSummary(Long id, String building, String facade, String location,
                              String status, String statusLabel, String createdAt,
                              int reportCount) {
    }

    public record LinkedReport(Long id, String room, String residentName,
                               String status, String statusLabel, int recurrenceNo,
                               boolean reopenedAfterResolved, String createdAt) {
    }

    public record RepairDto(Long id, String workerName, String summary,
                            String happenedAt, List<PhotoDto> photos) {
    }

    public record RecheckDto(Long id, String inspectorName, String result, String resultLabel,
                             String conclusion, String qualifyingRainDate, double qualifyingRainMm,
                             String createdAt, List<PhotoDto> photos) {
    }

    public record ObjectionDto(Long id, String reason, String outcome, String outcomeLabel,
                               String replyNote, String repliedBy,
                               String createdAt, String repliedAt, List<PhotoDto> photos) {
    }

    public record ReportSummary(Long id, Long spotId, String building, String facade,
                                String location, String room, String residentName,
                                String contact, String description,
                                String status, String statusLabel,
                                int recurrenceNo, boolean recurrence,
                                boolean reopenedAfterResolved,
                                String spotStatus, String createdAt, String awaitingSince,
                                boolean rainEligible, String rainMessage,
                                String qualifyingRainDate, Double qualifyingRainMm) {
    }

    public record ReportDetail(ReportSummary report, SpotSummary spot,
                               List<LinkedReport> linkedReports,
                               List<LinkedReport> recurrenceChain,
                               List<PhotoDto> reportPhotos,
                               List<RepairDto> repairs,
                               List<RecheckDto> rechecks,
                               List<ObjectionDto> objections) {
    }

    @Transactional
    public List<ReportSummary> listReports() {
        return Report.<Report>list("order by createdAt desc").stream()
                .map(this::reportSummary).toList();
    }

    @Transactional
    public List<SpotSummary> listSpots() {
        return LeakSpot.<LeakSpot>list("order by createdAt desc").stream()
                .map(s -> spotSummary(s, s.reports.size())).toList();
    }

    @Transactional
    public ReportDetail detail(Long reportId) {
        Report report = Report.findById(reportId);
        if (report == null) {
            throw new com.leak.service.BusinessException("报修单不存在: " + reportId);
        }
        List<PhotoDto> reportPhotos = Photo.<Photo>list("report = ?1 order by createdAt asc", report)
                .stream().map(this::photo).toList();

        List<RepairDto> repairs = report.spot.repairs.stream().map(r -> new RepairDto(
                r.id, r.workerName, r.summary, r.happenedAt.toString(),
                r.photos.stream().map(this::photo).toList())).toList();

        List<RecheckDto> rechecks = RecheckRecord
                .<RecheckRecord>list("report = ?1 order by createdAt asc", report)
                .stream().map(rc -> new RecheckDto(rc.id, rc.inspectorName,
                        rc.result.name(), rc.result.label, rc.conclusion,
                        rc.qualifyingRainDate == null ? null : rc.qualifyingRainDate.toString(),
                        rc.qualifyingRainMm, rc.createdAt.toString(),
                        rc.photos.stream().map(this::photo).toList())).toList();

        List<ObjectionDto> objections = Objection
                .<Objection>list("report = ?1 order by createdAt asc", report)
                .stream().map(o -> new ObjectionDto(o.id, o.reason, o.outcome.name(),
                        o.outcome.label, o.replyNote, o.repliedBy,
                        o.createdAt.toString(),
                        o.repliedAt == null ? null : o.repliedAt.toString(),
                        o.photos.stream().map(this::photo).toList())).toList();

        List<LinkedReport> linked = report.spot.reports.stream()
                .filter(r -> !r.id.equals(report.id))
                .map(r -> new LinkedReport(r.id, r.room, r.residentName,
                        r.status.name(), r.status.label, r.recurrenceNo,
                        r.reopenedAfterResolved, r.createdAt.toString())).toList();

        // 本户在同一点位的历史报修链（复发时可查看此前处理过程）
        List<LinkedReport> chain = report.spot.reports.stream()
                .filter(r -> norm(r.room).equals(norm(report.room)))
                .map(r -> new LinkedReport(r.id, r.room, r.residentName,
                        r.status.name(), r.status.label, r.recurrenceNo,
                        r.reopenedAfterResolved, r.createdAt.toString())).toList();

        return new ReportDetail(reportSummary(report),
                spotSummary(report.spot, report.spot.reports.size()),
                linked, chain, reportPhotos, repairs, rechecks, objections);
    }

    public ReportSummary reportSummary(Report r) {
        boolean eligible = false;
        String message = null;
        String rainDate = null;
        Double rainMm = null;
        if (r.status == ReportStatus.PENDING_RECHECK) {
            WeatherService.RainEligibility e = weather.evaluate(r);
            eligible = e.eligible();
            message = e.message();
            if (e.qualifyingRain() != null) {
                rainDate = e.qualifyingRain().rainDate.toString();
                rainMm = e.qualifyingRain().mm;
            }
        }
        return new ReportSummary(r.id, r.spot.id, r.spot.building, r.spot.facade,
                r.spot.location, r.room, r.residentName, r.contact, r.description,
                r.status.name(), r.status.label, r.recurrenceNo, r.recurrenceNo > 1,
                r.reopenedAfterResolved, r.spot.status.name(),
                r.createdAt.toString(),
                r.awaitingSince == null ? null : r.awaitingSince.toString(),
                eligible, message, rainDate, rainMm);
    }

    private SpotSummary spotSummary(LeakSpot s, int reportCount) {
        return new SpotSummary(s.id, s.building, s.facade, s.location,
                s.status.name(), s.status.label, s.createdAt.toString(), reportCount);
    }

    private PhotoDto photo(Photo p) {
        return new PhotoDto(p.id, "/photos/" + p.fileName, p.stage.name(),
                p.stage.label, p.caption, p.createdAt.toString());
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase().replaceAll("\\s+", "");
    }
}

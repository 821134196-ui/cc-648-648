package com.seepage.dto;

import com.seepage.model.DisputeStatus;
import com.seepage.model.RecheckResult;
import com.seepage.model.Resolution;
import com.seepage.model.TicketStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 各接口的响应视图 */
public class Views {

    public record TicketRef(Long id, String room, String residentName, TicketStatus status) {}

    public record TicketSummary(Long id, String building, String facade, String room,
                                String residentName, TicketStatus status, LocalDateTime createdAt,
                                String pointNote, boolean recurrence, boolean hasRecurrences) {}

    public record PointView(Long id, String building, String facade, String note,
                            List<TicketRef> tickets) {}

    public record PhotoView(Long id, String url, String label, LocalDateTime uploadedAt) {}

    public record ConstructionView(Long id, String workerName, String content,
                                   LocalDate completedAt, LocalDateTime createdAt) {}

    public record RecheckView(Long id, String inspectorName, RecheckResult result, String content,
                              LocalDate checkedAt, LocalDate rainDate, Double rainMm) {}

    public record DisputeView(Long id, String residentName, String content, DisputeStatus status,
                              String reply, Resolution resolution,
                              LocalDateTime createdAt, LocalDateTime repliedAt) {}

    public record RainEventView(Long id, LocalDate date, double rainfallMm, boolean effective, String note) {}

    /** 复查资格：是否已有完工后有效降雨；reason 给住户/物业看的说明 */
    public record EligibilityView(boolean eligible, String reason, LocalDate completedAt,
                                  LocalDate currentDate, double thresholdMm,
                                  List<RainEventView> effectiveRains) {}

    public record TicketDetail(Long id, String building, String facade, String room,
                               String residentName, String phone, String description,
                               TicketStatus status, LocalDateTime createdAt,
                               PointView point, List<TicketRef> pointTickets,
                               TicketRef recurrenceOf, List<TicketRef> recurrences,
                               List<PhotoView> reportPhotos, List<PhotoView> constructionPhotos,
                               List<PhotoView> recheckPhotos,
                               List<ConstructionView> constructions, List<RecheckView> rechecks,
                               List<DisputeView> disputes, EligibilityView eligibility) {}

    public record WeatherView(LocalDate currentDate, double thresholdMm, List<RainEventView> events) {}

    /** 天气提醒：每个待复查工单一行 */
    public record ReminderView(Long ticketId, String building, String facade, String room,
                               String residentName, LocalDate completedAt,
                               boolean eligible, String reason) {}
}

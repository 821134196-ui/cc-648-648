package com.seepage.dto;

import com.seepage.model.RecheckResult;
import com.seepage.model.Resolution;
import java.time.LocalDate;

/** 各接口的请求体 */
public class Requests {

    public record CreateTicket(String building, String facade, String room,
                               String residentName, String phone, String description,
                               String locationNote, Long pointId) {}

    public record Construction(String workerName, String content) {}

    public record Recheck(String inspectorName, RecheckResult result, String content) {}

    public record DisputeReq(String residentName, String content) {}

    public record ReplyReq(String reply, Resolution resolution) {}

    public record Recurrence(String residentName, String phone, String room, String description) {}

    public record RainEventReq(LocalDate date, double rainfallMm, String note) {}
}

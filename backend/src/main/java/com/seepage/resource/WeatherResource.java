package com.seepage.resource;

import com.seepage.dto.Requests;
import com.seepage.dto.Views;
import com.seepage.model.ConstructionRecord;
import com.seepage.model.RainEvent;
import com.seepage.model.RepairTicket;
import com.seepage.model.SimState;
import com.seepage.model.TicketStatus;
import com.seepage.service.TicketService;
import com.seepage.service.WeatherService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/** 本地天气模拟：降雨事件录入、模拟日期推进、待复查提醒 */
@Path("/api/weather")
@Produces(MediaType.APPLICATION_JSON)
public class WeatherResource {

    @Inject
    WeatherService weather;

    @Inject
    TicketService ticketService;

    @GET
    public Views.WeatherView view() {
        List<Views.RainEventView> events = RainEvent.<RainEvent>list("order by date").stream()
                .map(e -> new Views.RainEventView(e.id, e.date, e.rainfallMm, weather.isEffective(e), e.note))
                .toList();
        return new Views.WeatherView(weather.currentDate(), weather.threshold(), events);
    }

    /** 录入一场模拟降雨；若日期晚于当前模拟日期，则推进模拟时钟 */
    @POST
    @Path("/events")
    @Transactional
    public Views.WeatherView addEvent(Requests.RainEventReq req) {
        if (req.date() == null) {
            throw new BadRequestException("请填写降雨日期");
        }
        if (req.rainfallMm() < 0) {
            throw new BadRequestException("降雨量不能为负");
        }
        RainEvent e = RainEvent.find("date", req.date()).firstResult();
        if (e == null) {
            e = new RainEvent();
            e.date = req.date();
            e.persist();
        }
        e.rainfallMm = req.rainfallMm();
        e.note = req.note();

        SimState sim = SimState.findById(1L);
        if (sim == null) {
            sim = new SimState();
            sim.currentDate = req.date();
            sim.persist();
        } else if (req.date().isAfter(sim.currentDate)) {
            sim.currentDate = req.date();
        }
        return view();
    }

    /** 天气提醒：所有待复查工单及其雨后复查资格 */
    @GET
    @Path("/reminders")
    public List<Views.ReminderView> reminders() {
        return RepairTicket.<RepairTicket>list("status", TicketStatus.PENDING_RECHECK).stream()
                .map(t -> {
                    Views.EligibilityView el = ticketService.eligibility(t);
                    return new Views.ReminderView(t.id, t.building, t.facade, t.room,
                            t.residentName, el.completedAt(), el.eligible(), el.reason());
                })
                .toList();
    }
}

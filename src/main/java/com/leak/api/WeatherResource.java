package com.leak.api;

import com.leak.entity.RainRecord;
import com.leak.service.WeatherService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 本地模拟天气：登记/删除降雨，查询有效降雨阈值。 */
@Path("/api/weather")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WeatherResource {

    @Inject
    WeatherService weather;

    @GET
    public Map<String, Object> status() {
        return Map.of(
                "thresholdMm", weather.thresholdMm(),
                "rainRecords", weather.listRain().stream()
                        .map(r -> Map.of(
                                "id", r.id,
                                "rainDate", r.rainDate.toString(),
                                "mm", r.mm,
                                "note", r.note == null ? "" : r.note))
                        .toList());
    }

    @POST
    @Path("/rain")
    public Map<String, Object> addRain(RainRequest req) {
        LocalDate date = req.rainDate == null || req.rainDate.isBlank()
                ? LocalDate.now() : LocalDate.parse(req.rainDate.trim());
        RainRecord r = weather.addRain(date, req.mm == null ? 0 : req.mm, req.note);
        return Map.of("id", r.id, "rainDate", r.rainDate.toString(),
                "mm", r.mm, "note", r.note == null ? "" : r.note);
    }

    @DELETE
    @Path("/rain/{id}")
    public Map<String, Object> delete(@PathParam("id") Long id) {
        weather.deleteRain(id);
        return Map.of("ok", true);
    }

    public static class RainRequest {
        public String rainDate;
        public Double mm;
        public String note;
    }
}

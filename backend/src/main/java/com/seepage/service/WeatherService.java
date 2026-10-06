package com.seepage.service;

import com.seepage.model.RainEvent;
import com.seepage.model.SimState;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** 本地天气模拟：维护模拟当前日期与降雨事件，判定“有效降雨” */
@ApplicationScoped
public class WeatherService {

    @ConfigProperty(name = "app.min-rainfall-mm", defaultValue = "10.0")
    double minRainfallMm;

    /** 模拟当前日期（天气模拟会推进它） */
    public LocalDate currentDate() {
        SimState s = SimState.findById(1L);
        return s != null && s.currentDate != null ? s.currentDate : LocalDate.now();
    }

    public double threshold() {
        return minRainfallMm;
    }

    public boolean isEffective(RainEvent e) {
        return e.rainfallMm >= minRainfallMm;
    }

    /**
     * 完工日之后（不含当天）且已经发生（不晚于模拟当前日期）的有效降雨。
     * 复查必须晚于完工当天，因此降雨日必须严格晚于完工日。
     */
    public List<RainEvent> effectiveRainsAfter(LocalDate completedAt) {
        return RainEvent.list("date > ?1 and rainfallMm >= ?2 and date <= ?3 order by date",
                completedAt, minRainfallMm, currentDate());
    }
}

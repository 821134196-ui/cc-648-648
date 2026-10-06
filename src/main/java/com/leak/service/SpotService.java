package com.leak.service;

import com.leak.entity.LeakSpot;
import com.leak.model.SpotStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;

/** 渗水点位：同楼栋+同立面+同位置归一化为同一点位，供多户报修自动关联。 */
@ApplicationScoped
public class SpotService {

    public static String buildKey(String building, String facade, String location) {
        return normalize(building) + "|" + normalize(facade) + "|" + normalize(location);
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase().replaceAll("\\s+", "");
    }

    public LeakSpot findByKey(String key) {
        return LeakSpot.<LeakSpot>find("spotKey", key).firstResultOptional().orElse(null);
    }

    @Transactional
    public LeakSpot getOrCreate(String building, String facade, String location) {
        String key = buildKey(building, facade, location);
        LeakSpot spot = findByKey(key);
        if (spot == null) {
            spot = new LeakSpot();
            spot.building = building.trim();
            spot.facade = facade.trim();
            spot.location = location.trim();
            spot.spotKey = key;
            spot.status = SpotStatus.OPEN;
            spot.createdAt = LocalDateTime.now();
            spot.persist();
        }
        return spot;
    }

    /** 根据点位下各报修单状态重算点位整体状态。 */
    @Transactional
    public void refreshStatus(LeakSpot spot) {
        boolean everRepaired = !spot.repairs.isEmpty();
        boolean allVerified = !spot.reports.isEmpty()
                && spot.reports.stream().allMatch(r -> r.status == com.leak.model.ReportStatus.VERIFIED);
        if (allVerified) {
            spot.status = SpotStatus.RESOLVED;
        } else if (everRepaired) {
            spot.status = SpotStatus.IN_PROGRESS;
        } else {
            spot.status = SpotStatus.OPEN;
        }
    }
}

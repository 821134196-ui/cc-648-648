package com.leak.service;

import com.leak.entity.RainRecord;
import com.leak.entity.RepairRecord;
import com.leak.entity.Report;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 模拟天气与“有效降雨”判定。
 * 有效降雨条件（同时满足，二者缺一不可，防止施工当天直接验证）：
 *   1. 降雨量 >= leak.rain.min-mm；
 *   2. 降雨日期严格晚于施工完成日期（当天降雨不算，雨后才能复查）。
 */
@ApplicationScoped
public class WeatherService {

    @ConfigProperty(name = "leak.rain.min-mm")
    double minMm;

    public double thresholdMm() {
        return minMm;
    }

    public List<RainRecord> listRain() {
        return RainRecord.list("order by rainDate desc");
    }

    @Transactional
    public RainRecord addRain(LocalDate date, double mm, String note) {
        if (date == null) {
            throw new BusinessException("降雨日期不能为空");
        }
        if (mm <= 0) {
            throw new BusinessException("降雨量必须大于 0");
        }
        if (RainRecord.find("rainDate", date).firstResultOptional().isPresent()) {
            throw new BusinessException(date + " 已存在降雨记录，请直接修改或选择其他日期");
        }
        RainRecord r = new RainRecord();
        r.rainDate = date;
        r.mm = mm;
        r.note = note;
        r.createdAt = LocalDateTime.now();
        r.persist();
        return r;
    }

    @Transactional
    public void deleteRain(Long id) {
        RainRecord.deleteById(id);
    }

    /** 找到施工完成后第一场达到阈值的有效降雨。 */
    public Optional<RainRecord> qualifyingRainAfter(LocalDateTime repairCompletedAt) {
        if (repairCompletedAt == null) {
            return Optional.empty();
        }
        LocalDate repairDay = repairCompletedAt.toLocalDate();
        return RainRecord
                .<RainRecord>list("rainDate > ?1 and mm >= ?2 order by rainDate asc", repairDay, minMm)
                .stream().findFirst();
    }

    /** 点位最近一次施工完成时间。 */
    public LocalDateTime latestRepairAt(com.leak.entity.LeakSpot spot) {
        return spot.repairs.stream()
                .map(r -> r.happenedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    /**
     * 评估报修单当前雨后复查条件。
     * @param report 报修单（awaitingSince = 使其进入待复查的最近一次施工完成时间）
     */
    public RainEligibility evaluate(Report report) {
        if (report.awaitingSince == null) {
            RepairRecord latest = report.spot.repairs.stream()
                    .max((a, b) -> a.happenedAt.compareTo(b.happenedAt)).orElse(null);
            if (latest == null) {
                return new RainEligibility(false, null, null, "尚未完成施工");
            }
            return new RainEligibility(false, latest.happenedAt, null, "状态异常：缺少施工完成时间");
        }
        LocalDate repairDay = report.awaitingSince.toLocalDate();
        Optional<RainRecord> heavyEnough = RainRecord
                .<RainRecord>list("mm >= ?1 order by rainDate asc", minMm).stream()
                .filter(r -> r.rainDate.isAfter(repairDay))
                .findFirst();
        if (heavyEnough.isPresent()) {
            RainRecord rain = heavyEnough.get();
            return new RainEligibility(true, report.awaitingSince, rain,
                    "施工完成于 " + repairDay + "，" + rain.rainDate + " 出现 " + trim(rain.mm)
                            + "mm 有效降雨（阈值 " + trim(minMm) + "mm），可以回原址复查");
        }
        // 给出不满足的具体原因，便于演示
        Optional<RainRecord> anyRainAfter = RainRecord
                .<RainRecord>list("rainDate > ?1 order by rainDate asc", repairDay).stream().findFirst();
        String reason;
        if (anyRainAfter.isEmpty()) {
            String sameDay = RainRecord
                    .<RainRecord>list("rainDate = ?1 and mm >= ?2", repairDay, minMm).stream()
                    .findFirst().map(r -> "（" + repairDay + " 当天降雨 " + trim(r.mm)
                            + "mm，但施工当天的降雨不能作为雨后复查依据）").orElse("");
            reason = "施工完成于 " + repairDay + sameDay + "，此后尚无达到 "
                    + trim(minMm) + "mm 的有效降雨，任务保持待复查（不能在施工当天直接标记验证通过）";
        } else {
            RainRecord r = anyRainAfter.get();
            reason = "施工完成于 " + repairDay + "，" + r.rainDate + " 降雨仅 "
                    + trim(r.mm) + "mm，低于有效降雨阈值 " + trim(minMm)
                    + "mm，任务保持待复查";
        }
        return new RainEligibility(false, report.awaitingSince, null, reason);
    }

    private static String trim(double d) {
        return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    /** 雨后复查条件评估结果。 */
    public record RainEligibility(boolean eligible,
                                  LocalDateTime repairCompletedAt,
                                  RainRecord qualifyingRain,
                                  String message) {
    }
}

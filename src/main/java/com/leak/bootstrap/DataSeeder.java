package com.leak.bootstrap;

import com.leak.entity.*;
import com.leak.model.*;
import com.leak.service.PhotoStorage;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 首次启动时写入演示数据（日期相对今天生成，保证任意时候启动都可演示）：
 * 点位一 3栋东立面：两户关联报修、已施工，只有一场 4mm 小雨 —— 未达雨后条件，保持待复查；
 * 点位二 7栋南立面：本户曾验证修复，近日再次漏水 —— 复发报修，历史处理过程保留；
 * 点位三 5栋西立面：已验证修复，住户提出异议 —— 异议处理中，等待物业回复。
 */
@ApplicationScoped
public class DataSeeder {

    @Inject
    PhotoStorage storage;

    @Transactional
    public void seed(@Observes StartupEvent ev) {
        if (Report.count() > 0) {
            return;
        }

        LocalDate today = LocalDate.now();

        // ---------- 点位一：同点位两户关联 + 未达雨后复查条件 ----------
        LeakSpot s1 = spot("3栋", "东立面", "12层主卧窗上角", SpotStatus.IN_PROGRESS, today.minusDays(5).atTime(9, 0));
        LocalDateTime repair1At = today.minusDays(2).atTime(10, 0);
        Report r1 = report(s1, "2单元1202", "张伟", "13800000001",
                "东立面主卧窗上角墙面渗水，雨后有水痕，墙皮鼓起。",
                ReportStatus.PENDING_RECHECK, 1, null, false, today.minusDays(5).atTime(9, 0), repair1At);
        Report r2 = report(s1, "2单元1203", "王芳", "13800000002",
                "同位置相邻户，窗边顶面渗水，滴到地板上。",
                ReportStatus.PENDING_RECHECK, 1, null, false, today.minusDays(4).atTime(15, 0), repair1At);
        photo(r1, PhotoStage.REPORT, "报修：窗上角水痕", "#5b8dd9");
        photo(r2, PhotoStage.REPORT, "报修：顶面滴水", "#5b8dd9");
        RepairRecord rp1 = repair(s1, "赵师傅", "外墙裂缝注胶封堵，窗上口重做防水涂层并打耐候胶。",
                repair1At, today.minusDays(2).atTime(8, 30));
        repairPhoto(rp1, "施工：铲除鼓皮", "#c98a3d");
        repairPhoto(rp1, "施工：防水涂层完成", "#c98a3d");
        rain(today.minusDays(1), 4, "小雨（低于有效降雨阈值 10mm，两户任务均保持待复查）");

        // ---------- 点位二：修复后复发 ----------
        LeakSpot s2 = spot("7栋", "南立面", "次卧窗台下方", SpotStatus.IN_PROGRESS, today.minusDays(22).atTime(10, 0));
        LocalDateTime repair2At = today.minusDays(20).atTime(14, 0);
        Report r3 = report(s2, "1单元301", "陈强", "13800000003",
                "次卧南立面窗台下方渗水，墙面发霉。",
                ReportStatus.VERIFIED, 1, null, false,
                today.minusDays(22).atTime(10, 0), repair2At);
        photo(r3, PhotoStage.REPORT, "报修：窗台下发霉", "#5b8dd9");
        RepairRecord rp2 = repair(s2, "钱师傅", "窗台外侧重做排水坡，更换密封胶。",
                repair2At, today.minusDays(20).atTime(13, 30));
        repairPhoto(rp2, "施工：重做排水坡", "#c98a3d");
        rain(today.minusDays(16), 28, "大雨（有效降雨）");
        RecheckRecord rc3 = recheck(r3, "孙质检", RecheckResult.DRY,
                "大雨后次日到场复查，窗台及墙面干燥，未见渗水，验证修复。",
                today.minusDays(16), 28, today.minusDays(15).atTime(10, 0));
        recheckPhoto(rc3, "复查：墙面干燥", "#3f9d54");

        // 同一户同一位置再次报修 —— 原问题复发
        Report r4 = report(s2, "1单元301", "陈强", "13800000003",
                "上次修好后这两场雨又开始漏水，原窗台下方位置出现新水痕。",
                ReportStatus.PENDING_REPAIR, 2, r3, true, today.minusDays(4).atTime(19, 0), null);
        photo(r4, PhotoStage.REPORT, "复发报修：新水痕", "#d95b5b");

        // ---------- 点位三：已验证后住户异议 ----------
        LeakSpot s3 = spot("5栋", "西立面", "客厅顶板东南角", SpotStatus.IN_PROGRESS, today.minusDays(14).atTime(11, 0));
        LocalDateTime repair3At = today.minusDays(12).atTime(9, 0);
        Report r5 = report(s3, "3单元806", "刘洋", "13800000005",
                "客厅顶板东南角渗水掉皮。",
                ReportStatus.OBJECTIONED, 1, null, false,
                today.minusDays(14).atTime(11, 0), repair3At);
        photo(r5, PhotoStage.REPORT, "报修：顶板掉皮", "#5b8dd9");
        RepairRecord rp3 = repair(s3, "周师傅", "上层外墙面裂缝注浆，室内顶板修补刷漆。",
                repair3At, today.minusDays(12).atTime(8, 30));
        repairPhoto(rp3, "施工：顶板修补", "#c98a3d");
        rain(today.minusDays(10), 22, "中到大雨（有效降雨）");
        RecheckRecord rc5 = recheck(r5, "孙质检", RecheckResult.DRY,
                "雨后复查顶板干燥，初步判定修复。",
                today.minusDays(10), 22, today.minusDays(9).atTime(9, 30));
        recheckPhoto(rc5, "复查：当时干燥", "#3f9d54");
        objection(r5, "复查后又下了一场雨，顶板东南角重新出现水渍并发潮，物业验收时没看出来，请重新处理。",
                today.minusDays(8).atTime(20, 0), "#d95b5b");
    }

    // ---- helpers ----

    private LeakSpot spot(String building, String facade, String location, SpotStatus st, LocalDateTime created) {
        LeakSpot s = new LeakSpot();
        s.building = building;
        s.facade = facade;
        s.location = location;
        s.spotKey = com.leak.service.SpotService.buildKey(building, facade, location);
        s.status = st;
        s.createdAt = created;
        s.persist();
        return s;
    }

    private Report report(LeakSpot spot, String room, String name, String contact, String desc,
                          ReportStatus status, int recurrenceNo, Report recurrenceOf,
                          boolean reopened, LocalDateTime created, LocalDateTime awaitingSince) {
        Report r = new Report();
        r.spot = spot;
        r.room = room;
        r.residentName = name;
        r.contact = contact;
        r.description = desc;
        r.status = status;
        r.recurrenceNo = recurrenceNo;
        r.recurrenceOf = recurrenceOf;
        r.reopenedAfterResolved = reopened;
        r.createdAt = created;
        r.awaitingSince = awaitingSince;
        r.persist();
        return r;
    }

    private RepairRecord repair(LeakSpot spot, String worker, String summary,
                                LocalDateTime happenedAt, LocalDateTime createdAt) {
        RepairRecord r = new RepairRecord();
        r.spot = spot;
        r.workerName = worker;
        r.summary = summary;
        r.happenedAt = happenedAt;
        r.createdAt = createdAt;
        r.persist();
        return r;
    }

    private RecheckRecord recheck(Report report, String inspector, RecheckResult result,
                                  String conclusion, LocalDate rainDate, double rainMm,
                                  LocalDateTime createdAt) {
        RecheckRecord r = new RecheckRecord();
        r.report = report;
        r.inspectorName = inspector;
        r.result = result;
        r.conclusion = conclusion;
        r.qualifyingRainDate = rainDate;
        r.qualifyingRainMm = rainMm;
        r.createdAt = createdAt;
        r.persist();
        return r;
    }

    private Objection objection(Report report, String reason, LocalDateTime createdAt, String color) {
        Objection o = new Objection();
        o.report = report;
        o.reason = reason;
        o.outcome = ObjectionOutcome.PENDING;
        o.createdAt = createdAt;
        o.persist();
        objectionPhoto(o, "异议：再次出现水渍", color);
        return o;
    }

    private void rain(LocalDate date, double mm, String note) {
        RainRecord r = new RainRecord();
        r.rainDate = date;
        r.mm = mm;
        r.note = note;
        r.createdAt = date.atTime(8, 0);
        r.persist();
    }

    private void photo(Report r, PhotoStage stage, String caption, String color) {
        Photo p = base(stage, caption, color);
        p.report = r;
        p.persist();
    }

    private void repairPhoto(RepairRecord r, String caption, String color) {
        Photo p = base(PhotoStage.REPAIR, caption, color);
        p.repair = r;
        p.persist();
    }

    private void recheckPhoto(RecheckRecord r, String caption, String color) {
        Photo p = base(PhotoStage.RECHECK, caption, color);
        p.recheck = r;
        p.persist();
    }

    private void objectionPhoto(Objection o, String caption, String color) {
        Photo p = base(PhotoStage.OBJECTION, caption, color);
        p.objection = o;
        p.persist();
    }

    private Photo base(PhotoStage stage, String caption, String color) {
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='480' height='320'>"
                + "<rect width='480' height='320' fill='" + color + "' opacity='0.85'/>"
                + "<rect x='12' y='12' width='456' height='296' fill='none' stroke='white' stroke-width='3'/>"
                + "<text x='240' y='150' font-size='26' fill='white' text-anchor='middle' "
                + "font-family='sans-serif'>" + stage.label + "照片</text>"
                + "<text x='240' y='195' font-size='20' fill='white' text-anchor='middle' "
                + "font-family='sans-serif'>" + escape(caption) + "</text></svg>";
        String fileName = storage.storeBytes(svg.getBytes(java.nio.charset.StandardCharsets.UTF_8), ".svg");
        Photo p = new Photo();
        p.stage = stage;
        p.fileName = fileName;
        p.contentType = "image/svg+xml";
        p.caption = caption;
        p.createdAt = LocalDateTime.now();
        return p;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

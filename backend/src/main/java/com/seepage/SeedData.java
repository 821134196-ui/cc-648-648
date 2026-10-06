package com.seepage;

import com.seepage.model.ConstructionRecord;
import com.seepage.model.Dispute;
import com.seepage.model.DisputeStatus;
import com.seepage.model.LeakPoint;
import com.seepage.model.Photo;
import com.seepage.model.Phase;
import com.seepage.model.RainEvent;
import com.seepage.model.RecheckRecord;
import com.seepage.model.RecheckResult;
import com.seepage.model.RepairTicket;
import com.seepage.model.Resolution;
import com.seepage.model.SimState;
import com.seepage.model.TicketStatus;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * 演示种子数据（模拟当前日期 2026-10-06）：
 *  T1 3栋东立面501  —— 报修→施工→有效雨→复查通过（全流程）
 *  T2 3栋东立面502  —— 与 T1 同点位关联，昨日完工，之后无有效雨 → 待复查（未达条件）
 *  T3 5栋北立面1201 —— 维修中
 *  T4 3栋东立面501  —— T1 的复发报修（待受理）
 *  T5 2栋南立面303  —— 复查通过后住户异议，物业回复安排二次复查 → 待复查（仍无有效雨）
 */
@ApplicationScoped
public class SeedData {

    @ConfigProperty(name = "app.photo-dir")
    String photoDir;

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        if (SimState.count() > 0) {
            return;
        }
        cleanPhotoDir();

        SimState sim = new SimState();
        sim.currentDate = LocalDate.of(2026, 10, 6);
        sim.persist();

        RainEvent rain0914 = rain(LocalDate.of(2026, 9, 14), 18.0, "台风外围强降雨，持续约6小时");
        rain(LocalDate.of(2026, 9, 22), 6.0, "小雨，未达有效雨量");
        rain(LocalDate.of(2026, 10, 2), 4.0, "阵雨，未达有效雨量");

        LeakPoint p1 = point("3栋", "东立面", "5层窗台下沿渗水带");
        LeakPoint p2 = point("5栋", "北立面", "顶层伸缩缝");
        LeakPoint p3 = point("2栋", "南立面", "3层空调板根部");

        // T1：全流程已验证
        RepairTicket t1 = ticket(p1, "501室", "张女士", "13800002201",
                "东立面窗台下沿雨天渗水，窗台内侧墙面发霉起皮。", 2026, 9, 10);
        photo(t1, Phase.REPORT, "报修-窗台霉斑", "#7c5c3e", "501室 窗台霉斑", "报修照片 · 2026-09-10");
        photo(t1, Phase.REPORT, "报修-外墙裂缝", "#6b4f35", "3栋东立面 外墙裂缝", "报修照片 · 2026-09-10");
        construction(t1, "张工", "铲除空鼓砂浆层，裂缝注浆堵漏，外墙重新涂刷防水涂层两遍。", 2026, 9, 12);
        photo(t1, Phase.CONSTRUCTION, "施工-注浆堵漏", "#2f5d50", "注浆堵漏作业", "施工照片 · 2026-09-12");
        photo(t1, Phase.CONSTRUCTION, "施工-防水涂刷", "#35604f", "外墙防水涂层", "施工照片 · 2026-09-12");
        recheck(t1, "周复查", RecheckResult.PASS,
                "9月14日降雨18mm后复查：窗台下沿干燥，无新渗迹，墙面含水率正常。", 2026, 9, 15, rain0914);
        photo(t1, Phase.RECHECK, "复查-雨后干燥", "#3a6ea5", "雨后复查 窗台干燥", "复查照片 · 2026-09-15");
        t1.status = TicketStatus.VERIFIED;

        // T2：同点位（3栋东立面），昨日完工，之后无有效雨 → 待复查但不可复查
        RepairTicket t2 = ticket(p1, "502室", "李先生", "13800002202",
                "与501同一面外墙，雨天渗水，卧室墙皮起泡脱落。", 2026, 9, 20);
        photo(t2, Phase.REPORT, "报修-墙皮起泡", "#8a5a44", "502室 墙皮起泡", "报修照片 · 2026-09-20");
        construction(t2, "张工", "同点位一并处理：外墙裂缝注浆+防水涂刷，内墙铲除待干。", 2026, 10, 5);
        photo(t2, Phase.CONSTRUCTION, "施工-防水涂刷", "#2f5d50", "502外墙 防水涂刷", "施工照片 · 2026-10-05");
        t2.status = TicketStatus.PENDING_RECHECK;

        // T3：维修中
        RepairTicket t3 = ticket(p2, "1201室", "王先生", "13800002203",
                "顶层伸缩缝渗水，雨天吊顶角落有水渍。", 2026, 10, 3);
        photo(t3, Phase.REPORT, "报修-吊顶水渍", "#5d6b7a", "1201室 吊顶水渍", "报修照片 · 2026-10-03");
        t3.status = TicketStatus.IN_REPAIR;

        // T4：T1 修复后再次漏水 → 复发报修
        RepairTicket t4 = ticket(p1, "501室", "张女士", "13800002201",
                "复发：上次维修后再次漏水，9月28日降雨后窗台又出现水迹，请查看此前处理记录。", 2026, 9, 28);
        photo(t4, Phase.REPORT, "复发-窗台水迹", "#7c4a4a", "501室 窗台新水迹", "复发报修 · 2026-09-28");
        t4.recurrenceOf = t1;

        // T5：复查通过 → 住户异议 → 物业回复安排二次复查（之后仍无有效雨）
        RepairTicket t5 = ticket(p3, "303室", "陈女士", "13800002205",
                "空调板根部渗水，沿管道井洇湿墙面。", 2026, 9, 5);
        photo(t5, Phase.REPORT, "报修-墙面洇湿", "#6b5a7a", "303室 墙面洇湿", "报修照片 · 2026-09-05");
        construction(t5, "刘工", "空调板根部密封胶剔除重做，管根防水加强。", 2026, 9, 8);
        photo(t5, Phase.CONSTRUCTION, "施工-根部密封", "#2f5d50", "管根密封重做", "施工照片 · 2026-09-08");
        recheck(t5, "周复查", RecheckResult.PASS,
                "9月14日降雨18mm后复查：根部干燥无渗漏。", 2026, 9, 15, rain0914);
        photo(t5, Phase.RECHECK, "复查-根部干燥", "#3a6ea5", "雨后复查 根部干燥", "复查照片 · 2026-09-15");
        t5.status = TicketStatus.VERIFIED;

        Dispute d = new Dispute();
        d.ticket = t5;
        d.residentName = "陈女士";
        d.content = "复查那周我印象中没下过大雨，不认可“雨后复查通过”的结论，要求重新检查。";
        d.createdAt = LocalDateTime.of(2026, 9, 20, 10, 30);
        d.status = DisputeStatus.REPLIED;
        d.reply = "已与气象记录核对：9月14日降雨18mm，复查于9月15日进行，流程合规。"
                + "为消除疑虑，将在下一场有效降雨后安排二次复查并邀请您到场。";
        d.resolution = Resolution.RECHECK_AGAIN;
        d.repliedAt = LocalDateTime.of(2026, 9, 21, 14, 0);
        d.persist();
        t5.status = TicketStatus.PENDING_RECHECK;
    }

    private RainEvent rain(LocalDate date, double mm, String note) {
        RainEvent e = new RainEvent();
        e.date = date;
        e.rainfallMm = mm;
        e.note = note;
        e.persist();
        return e;
    }

    private LeakPoint point(String building, String facade, String note) {
        LeakPoint p = new LeakPoint();
        p.building = building;
        p.facade = facade;
        p.note = note;
        p.persist();
        return p;
    }

    private RepairTicket ticket(LeakPoint point, String room, String resident, String phone,
                                String description, int y, int m, int d) {
        RepairTicket t = new RepairTicket();
        t.point = point;
        t.building = point.building;
        t.facade = point.facade;
        t.room = room;
        t.residentName = resident;
        t.phone = phone;
        t.description = description;
        t.createdAt = LocalDateTime.of(y, m, d, 9, 30);
        t.persist();
        return t;
    }

    private void construction(RepairTicket t, String worker, String content, int y, int m, int d) {
        ConstructionRecord r = new ConstructionRecord();
        r.ticket = t;
        r.workerName = worker;
        r.content = content;
        r.completedAt = LocalDate.of(y, m, d);
        r.createdAt = r.completedAt.atTime(17, 0);
        r.persist();
    }

    private void recheck(RepairTicket t, String inspector, RecheckResult result, String content,
                         int y, int m, int d, RainEvent rain) {
        RecheckRecord r = new RecheckRecord();
        r.ticket = t;
        r.inspectorName = inspector;
        r.result = result;
        r.content = content;
        r.checkedAt = LocalDate.of(y, m, d);
        r.rainEvent = rain;
        r.createdAt = r.checkedAt.atTime(11, 0);
        r.persist();
    }

    /** 生成占位“照片”（SVG，浏览器可直接渲染） */
    private void photo(RepairTicket t, Phase phase, String label, String bg, String line1, String line2) {
        String name = UUID.randomUUID() + ".svg";
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="640" height="420">
                  <rect width="100%%" height="100%%" fill="%s"/>
                  <rect x="18" y="18" width="604" height="384" fill="none" stroke="rgba(255,255,255,.55)" stroke-width="2" stroke-dasharray="10 6"/>
                  <text x="44" y="200" font-size="34" fill="#ffffff" font-family="sans-serif">%s</text>
                  <text x="44" y="248" font-size="22" fill="rgba(255,255,255,.85)" font-family="sans-serif">%s</text>
                  <text x="44" y="380" font-size="16" fill="rgba(255,255,255,.6)" font-family="sans-serif">模拟照片 · 渗水复查系统</text>
                </svg>
                """.formatted(bg, escape(line1), escape(line2));
        try {
            Path dir = Path.of(photoDir);
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(name), svg);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Photo p = new Photo();
        p.ticket = t;
        p.phase = phase;
        p.fileName = name;
        p.label = label;
        p.persist();
    }

    /** 数据库重建时同步清空照片目录，避免孤儿文件 */
    private void cleanPhotoDir() {
        try {
            Path dir = Path.of(photoDir);
            Files.createDirectories(dir);
            try (var files = Files.list(dir)) {
                for (Path f : files.filter(Files::isRegularFile).toList()) {
                    Files.deleteIfExists(f);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

package com.sportmate.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportmate.entity.Event;
import com.sportmate.entity.Post;
import com.sportmate.entity.User;
import com.sportmate.service.EventService;
import com.sportmate.service.PostService;
import com.sportmate.service.UserService;

import jakarta.servlet.http.HttpSession;

/**
 * หน้าแรก (Home)
 *  - ซ้าย : โพสต์ล่าสุด + ทัวร์นาเมนต์ล่าสุด
 *  - ขวา  : ปฏิทินบุ๊กมาร์กวันที่มีกิจกรรมของผู้ใช้
 *           ส่งข้อมูลเป็น JSON ครั้งเดียว แล้วให้ JS วาดปฏิทิน/เปลี่ยนเดือนเอง (ไม่โหลดหน้าใหม่)
 */
@Controller
public class HomeController {

    private final PostService postService;
    private final EventService eventService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public HomeController(PostService postService, EventService eventService,
                          UserService userService, ObjectMapper objectMapper) {
        this.postService = postService;
        this.eventService = eventService;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/home")
    public String home(HttpSession session, Model model) {

        User me = userService.getById((Integer) session.getAttribute("uid"));

        // ---------- ฝั่งซ้าย: โพสต์ / ทัวร์นาเมนต์ ล่าสุด ----------
        List<Post> allPosts = postService.activePosts();
        List<Post> allTours = postService.tournaments();
        List<Post> latestPosts = allPosts.size() > 6 ? new ArrayList<>(allPosts.subList(0, 6)) : allPosts;
        List<Post> latestTournaments = allTours.size() > 4 ? new ArrayList<>(allTours.subList(0, 4)) : allTours;

        Map<Integer, Long> joinCounts = new HashMap<>();
        for (Post p : latestPosts) joinCounts.put(p.getId(), eventService.countJoins(p));
        for (Post p : latestTournaments) joinCounts.put(p.getId(), eventService.countJoins(p));

        model.addAttribute("latestPosts", latestPosts);
        model.addAttribute("latestTournaments", latestTournaments);
        model.addAttribute("joinCounts", joinCounts);

        // ---------- ฝั่งขวา: ข้อมูลปฏิทิน (JSON) ----------
        Map<String, List<Map<String, Object>>> byDate = new TreeMap<>();
        for (Event ev : eventService.joinedByUser(me)) {
            addAgenda(byDate, ev.getPost(), "เข้าร่วม", ev.getStatus());
        }
        for (Post p : postService.ownedBy(me)) {
            addAgenda(byDate, p, "ผู้จัด", "approved");
        }
        for (List<Map<String, Object>> list : byDate.values()) {
            list.sort((a, b) -> String.valueOf(a.get("time")).compareTo(String.valueOf(b.get("time"))));
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(byDate);
        } catch (Exception e) {
            json = "{}";
        }
        // กัน "</script>" ที่อาจหลุดมาจากชื่อโพสต์
        json = json.replace("<", "\\u003C");

        model.addAttribute("agendaJson", json);
        model.addAttribute("todayIso", LocalDate.now().toString());

        return "home";
    }

    /** ใส่กิจกรรมหนึ่งรายการลงในปฏิทิน (ข้ามรายการที่ถูกยกเลิก/ปฏิเสธ) */
    private void addAgenda(Map<String, List<Map<String, Object>>> map, Post p,
                           String role, String eventStatus) {
        if (p == null || p.getDatePlay() == null) return;
        if ("cancelled".equals(p.getStatus())) return;
        if ("rejected".equals(eventStatus) || "cancelled".equals(eventStatus)) return;

        Map<String, Object> item = new HashMap<>();
        item.put("id", p.getId());
        item.put("name", p.getPostName());
        item.put("time", String.format("%02d:%02d",
                p.getDatePlay().getHour(), p.getDatePlay().getMinute()));
        item.put("sport", p.getSport() != null ? p.getSport().getName() : "");
        item.put("place", p.getLocation() != null ? p.getLocation().getName() : "");
        item.put("role", role);
        item.put("pending", "pending".equals(eventStatus));
        item.put("tournament", p.isTournament());

        map.computeIfAbsent(p.getDatePlay().toLocalDate().toString(), k -> new ArrayList<>()).add(item);
    }
}
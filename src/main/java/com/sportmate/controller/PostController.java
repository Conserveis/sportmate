package com.sportmate.controller;

import com.sportmate.entity.Post;
import com.sportmate.entity.User;
import com.sportmate.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
public class PostController {

    private final PostService postService;
    private final EventService eventService;
    private final ChatService chatService;
    private final UserService userService;

    public PostController(PostService postService, EventService eventService,
                          ChatService chatService, UserService userService) {
        this.postService = postService;
        this.eventService = eventService;
        this.chatService = chatService;
        this.userService = userService;
    }

    private User me(HttpSession session) {
        return userService.getById((Integer) session.getAttribute("uid"));
    }

    // ---- หน้า Post (โพสต์ที่ยังไม่หมดเวลา) + ตัวกรองค้นหา ----
    @GetMapping("/posts")
    public String posts(@RequestParam(required = false) Integer sportId,
                        @RequestParam(required = false) Integer locationId,
                        @RequestParam(required = false) String date,
                        @RequestParam(required = false) String time,
                        Model model) {
        java.time.LocalDate d = (date == null || date.isBlank()) ? null : java.time.LocalDate.parse(date);
        java.time.LocalTime t = (time == null || time.isBlank()) ? null : java.time.LocalTime.parse(time);
        var list = postService.search(false, sportId, locationId, d, t);
        model.addAttribute("posts", list);
        model.addAttribute("joinCounts", buildJoinCounts(list));
        addFilterOptions(model, sportId, locationId, d, t);
        return "posts";
    }

    // ---- ฟอร์มสร้างโพสต์ ----
    @GetMapping("/posts/new")
    public String newPost(@RequestParam(defaultValue = "Post") String type, Model model) {
        model.addAttribute("type", type);
        model.addAttribute("sports", userService.allSports());
        model.addAttribute("locations", postLocations());
        return "post-create";
    }

    @PostMapping("/posts")
    public String create(@RequestParam String type,
                         @RequestParam Integer sportId,
                         @RequestParam Integer locationId,
                         @RequestParam String postName,
                         @RequestParam(required = false) String description,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime datePlay,
                         @RequestParam Integer maxPlayer,
                         @RequestParam Integer minPlayer,
                         @RequestParam(defaultValue = "true") boolean isPublic,
                         @RequestParam(required = false) String publishAt,
                         HttpSession session, RedirectAttributes ra, Model model) {
        try {
            LocalDateTime publishAtParsed = (publishAt == null || publishAt.isBlank())
                    ? null : LocalDateTime.parse(publishAt);
            postService.create(me(session), type, sportId, locationId, postName,
                    description, datePlay, maxPlayer, minPlayer, isPublic, publishAtParsed);
            ra.addFlashAttribute("msg", "สร้าง" + ("Tournament".equals(type) ? "ทัวร์นาเมนต์" : "โพสต์") + "สำเร็จ");
            return "redirect:/" + ("Tournament".equals(type) ? "tournaments" : "posts");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("type", type);
            model.addAttribute("sports", userService.allSports());
            model.addAttribute("locations", postLocations());
            return "post-create";
        }
    }

    // ---- รายละเอียดโพสต์ + คอมเมนต์ ----
    @GetMapping("/posts/{id}")
    public String detail(@PathVariable Integer id, HttpSession session, Model model) {
        Post p = postService.getById(id);
        User me = me(session);
        model.addAttribute("post", p);
        model.addAttribute("participants", eventService.participants(p));
        model.addAttribute("joinCount", eventService.countJoins(p));
        model.addAttribute("hasJoined", eventService.hasJoined(me, p));
        model.addAttribute("isOwner", p.getOwner().getId().equals(me.getId()));
        model.addAttribute("comments", chatService.comments(p));
        return "post-detail";
    }

    @PostMapping("/posts/{id}/join")
    public String join(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        try {
            eventService.join(me(session), postService.getById(id));
            ra.addFlashAttribute("msg", "เข้าร่วมกิจกรรมสำเร็จ");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/posts/" + id;
    }

    @PostMapping("/posts/{id}/cancel-join")
    public String cancelJoin(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        try {
            eventService.cancelJoin(me(session), postService.getById(id));
            ra.addFlashAttribute("msg", "ยกเลิกการเข้าร่วมสำเร็จ");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/posts/" + id;
    }

    @PostMapping("/posts/{id}/comment")
    public String comment(@PathVariable Integer id, @RequestParam String text,
                          HttpSession session, RedirectAttributes ra) {
        try {
            chatService.addComment(me(session), postService.getById(id), text);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/posts/" + id;
    }

    @PostMapping("/posts/{id}/cancel")
    public String cancel(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        try {
            postService.cancel(id, me(session));
            ra.addFlashAttribute("msg", "ยกเลิกกิจกรรมสำเร็จ");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    // helpers
    private java.util.Map<Integer, Long> buildJoinCounts(java.util.List<Post> list) {
        var map = new java.util.HashMap<Integer, Long>();
        for (Post p : list) map.put(p.getId(), eventService.countJoins(p));
        return map;
    }

    private java.util.List<com.sportmate.entity.Location> postLocations() {
        return locationRepo.findAll();
    }

    // เติมข้อมูลสำหรับ dropdown ตัวกรอง + ค่าที่เลือกไว้ (ใช้ทั้งหน้า posts และ tournaments)
    private void addFilterOptions(Model model, Integer sportId, Integer locationId,
                                  java.time.LocalDate date, java.time.LocalTime time) {
        model.addAttribute("sports", userService.allSports());
        model.addAttribute("locations", locationRepo.findAll());
        model.addAttribute("fSportId", sportId);
        model.addAttribute("fLocationId", locationId);
        model.addAttribute("fDate", date);
        model.addAttribute("fTime", time);
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.sportmate.repository.LocationRepository locationRepo;
}

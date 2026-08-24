package com.sportmate.controller;

import com.sportmate.entity.Post;
import com.sportmate.repository.LocationRepository;
import com.sportmate.service.EventService;
import com.sportmate.service.PostService;
import com.sportmate.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class TournamentController {

    private final PostService postService;
    private final EventService eventService;
    private final UserService userService;
    private final LocationRepository locationRepo;

    public TournamentController(PostService postService, EventService eventService,
                                UserService userService, LocationRepository locationRepo) {
        this.postService = postService;
        this.eventService = eventService;
        this.userService = userService;
        this.locationRepo = locationRepo;
    }

    @GetMapping("/tournaments")
    public String tournaments(@RequestParam(required = false) Integer sportId,
                              @RequestParam(required = false) Integer locationId,
                              @RequestParam(required = false) String date,
                              @RequestParam(required = false) String time,
                              Model model) {
        LocalDate d = (date == null || date.isBlank()) ? null : LocalDate.parse(date);
        LocalTime t = (time == null || time.isBlank()) ? null : LocalTime.parse(time);
        List<Post> list = postService.search(true, sportId, locationId, d, t);
        Map<Integer, Long> counts = new HashMap<>();
        for (Post p : list) counts.put(p.getId(), eventService.countJoins(p));
        model.addAttribute("tournaments", list);
        model.addAttribute("joinCounts", counts);
        // ตัวกรอง
        model.addAttribute("sports", userService.allSports());
        model.addAttribute("locations", locationRepo.findAll());
        model.addAttribute("fSportId", sportId);
        model.addAttribute("fLocationId", locationId);
        model.addAttribute("fDate", d);
        model.addAttribute("fTime", t);
        return "tournaments";
    }
}

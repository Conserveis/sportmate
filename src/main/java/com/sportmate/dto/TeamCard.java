package com.sportmate.dto;

import java.util.List;

import com.sportmate.entity.Team;
import com.sportmate.entity.TeamMember;

/** ข้อมูลทีม 1 ใบสำหรับแสดงในหน้าโพสต์ทัวร์นาเมนต์ */
public class TeamCard {

    private final Team team;
    private final long memberCount;      // นับรวมหัวหน้าทีมแล้ว
    private final int teamSize;
    private final List<TeamMember> members;
    /** สถานะของผู้ดูกับทีมนี้: "member" / "pending" / null */
    private final String viewerStatus;

    public TeamCard(Team team, long memberCount, int teamSize, List<TeamMember> members, String viewerStatus) {
        this.team = team;
        this.memberCount = memberCount;
        this.teamSize = teamSize;
        this.members = members;
        this.viewerStatus = viewerStatus;
    }

    public Team getTeam() { return team; }
    public long getMemberCount() { return memberCount; }
    public int getTeamSize() { return teamSize; }
    public List<TeamMember> getMembers() { return members; }
    public String getViewerStatus() { return viewerStatus; }

    public boolean isFull() { return memberCount >= teamSize; }
    public long getSlotsLeft() { return Math.max(0, teamSize - memberCount); }
    public int getPct() { return teamSize <= 0 ? 0 : (int) Math.min(100, memberCount * 100 / teamSize); }
    public boolean isViewerMember() { return "member".equals(viewerStatus); }
    public boolean isViewerPending() { return "pending".equals(viewerStatus); }
}

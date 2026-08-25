package com.sportmate.dto;

/**
 * ผลลัพธ์การนับจำนวนครั้งที่ผู้ใช้เข้าร่วมกิจกรรม แยกตามชนิดกีฬา
 * ใช้กับ constructor expression ใน JPQL (EventRepository.countJoinsBySport)
 */
public class SportCount {

    private final String sportName;
    private final long count;

    public SportCount(String sportName, Long count) {
        this.sportName = sportName;
        this.count = count == null ? 0L : count;
    }

    public String getSportName() { return sportName; }
    public long getCount() { return count; }
}

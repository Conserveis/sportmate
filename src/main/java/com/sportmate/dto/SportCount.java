package com.sportmate.dto;

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

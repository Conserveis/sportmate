package com.sportmate.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "PostType")
public class PostType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PostTypeID")
    private Integer id;

    @Column(name = "PtypeName", nullable = false, unique = true)
    private String name;   // 'Post', 'Tournament'

    public static final String POST = "Post";
    public static final String TOURNAMENT = "Tournament";

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

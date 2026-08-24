package com.sportmate.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "UserType")
public class UserType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserTypeID")
    private Integer id;

    @Column(name = "UTypeName", nullable = false, unique = true)
    private String name;   // 'Normal', 'Member'

    public static final String NORMAL = "Normal";
    public static final String MEMBER = "Member";

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

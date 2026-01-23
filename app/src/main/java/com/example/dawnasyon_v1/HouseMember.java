package com.example.dawnasyon_v1;

import java.io.Serializable;

public class HouseMember implements Serializable {
    private Long member_id;
    private String head_id;
    private String full_name;
    private String relation;
    private Integer age;
    private String gender;
    private Boolean is_registered_census;
    private Boolean is_authorized_proxy;

    public HouseMember() {
        // Required empty constructor
    }

    // --- GETTERS (Matching your error messages exactly) ---
    public Long getMember_id() {
        return member_id;
    }

    public Boolean getIs_authorized_proxy() {
        return is_authorized_proxy;
    }

    public String getFull_name() {
        return full_name;
    }

    public String getRelation() {
        return relation;
    }

    public String getHead_id() {
        return head_id;
    }

    public Integer getAge() {
        return age;
    }

    public String getGender() {
        return gender;
    }

    public Boolean getIs_registered_census() {
        return is_registered_census;
    }

    // --- SETTERS ---
    public void setMember_id(Long member_id) {
        this.member_id = member_id;
    }

    public void setIs_authorized_proxy(Boolean is_authorized_proxy) {
        this.is_authorized_proxy = is_authorized_proxy;
    }

    public void setFull_name(String full_name) {
        this.full_name = full_name;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public void setHead_id(String head_id) {
        this.head_id = head_id;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setIs_registered_census(Boolean is_registered_census) {
        this.is_registered_census = is_registered_census;
    }
}
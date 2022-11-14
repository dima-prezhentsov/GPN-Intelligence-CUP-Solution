package com.example.solution.model;

import lombok.Data;

@Data
public class RequestUserInfo {
    private final Long user_id;
    private final Long group_id;
}

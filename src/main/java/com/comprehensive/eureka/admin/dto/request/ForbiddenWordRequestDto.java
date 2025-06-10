package com.comprehensive.eureka.admin.dto.request;

import lombok.Data;

@Data
public class ForbiddenWordRequestDto {
    private String word;
    private boolean used;
}
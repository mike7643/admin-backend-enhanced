package com.comprehensive.eureka.admin.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ForbiddenWordRequestDto {
    private String word;
    private boolean used;
}
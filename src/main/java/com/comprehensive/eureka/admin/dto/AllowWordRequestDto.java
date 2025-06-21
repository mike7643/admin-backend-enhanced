package com.comprehensive.eureka.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AllowWordRequestDto {
    private String word;
    private boolean isUsed;
}

package com.comprehensive.eureka.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AllowWordResponseDto {
    private Long id;
    private String word;
    private boolean status;
}

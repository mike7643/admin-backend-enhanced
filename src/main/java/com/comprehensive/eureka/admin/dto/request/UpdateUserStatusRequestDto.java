package com.comprehensive.eureka.admin.dto.request;

import com.comprehensive.eureka.admin.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserStatusRequestDto {

    private Long userId;
    private Status status;
    private LocalDateTime unbanTime;
}
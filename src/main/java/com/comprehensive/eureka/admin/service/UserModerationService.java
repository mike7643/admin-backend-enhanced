package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.dto.request.UpdateUserStatusRequestDto;
import com.comprehensive.eureka.admin.enums.Status;
import com.comprehensive.eureka.admin.exception.AdminException;
import com.comprehensive.eureka.admin.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserModerationService {

    private final WebClient originClient;

    public void applyBanIfThresholdCrossed(Long userId, long beforeCount, long afterCount) {
        int[] thresholds = {1000, 500, 300, 100, 50, 30};
        int targetThreshold = 0;

        for (int t : thresholds) {
            if (beforeCount < t && afterCount >= t) {
                targetThreshold = t;
                break;
            }
        }

        if (targetThreshold == 0) return;

        LocalDateTime unbanTime = null;
        LocalDate banEndDate = null;

        switch (targetThreshold) {
            case 30:
                banEndDate = LocalDate.now();
                break;
            case 50:
                banEndDate = LocalDate.now().plusDays(1);
                break;
            case 100:
                banEndDate = LocalDate.now().plusWeeks(1);
                break;
            case 300:
                banEndDate = LocalDate.now().plusWeeks(2);
                break;
            case 500:
                banEndDate = LocalDate.now().plusMonths(1);
                break;
            default:
                unbanTime = LocalDateTime.of(9999, 12, 31, 23, 59, 59);
                break;
        }

        if (banEndDate != null) unbanTime = banEndDate.plusDays(1).atStartOfDay();

        UpdateUserStatusRequestDto req = UpdateUserStatusRequestDto.builder()
                .userId(userId)
                .status(Status.INACTIVE)
                .unbanTime(unbanTime)
                .build();

        try {
            originClient.put()
                    .uri("http://localhost:8085/user/status")
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (Exception ex) {
            log.error("사용자 차단 API 호출 실패, userId={}", userId, ex);
            throw new AdminException(ErrorCode.USER_STATUS_UPDATE_FAILED);
        }
    }

    public void applyUnbanIfThresholdDropped(Long userId, long beforeCount, long afterCount) {
        int[] thresholds = {30, 50, 100, 300, 500, 1000};
        boolean needsUnban = false;
        for (int t : thresholds) {
            if (beforeCount == t && afterCount < t) {
                needsUnban = true;
                break;
            }
        }
        if (!needsUnban) {
            return;
        }

        UpdateUserStatusRequestDto req = UpdateUserStatusRequestDto.builder()
                .userId(userId)
                .status(Status.ACTIVE)
                .unbanTime(null)
                .build();

        try {
            originClient.put()
                    .uri("http://localhost:8085/user/status")
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (Exception ex) {
            log.error("사용자 해제 API 호출 실패, userId={}", userId, ex);
            throw new AdminException(ErrorCode.USER_STATUS_UPDATE_FAILED);
        }
    }
}

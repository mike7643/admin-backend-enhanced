package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.dto.AllowWordRequestDto;
import com.comprehensive.eureka.admin.dto.request.ForbiddenWordRequestDto;
import com.comprehensive.eureka.admin.dto.response.AllowWordResponseDto;
import com.comprehensive.eureka.admin.dto.response.ForbiddenWordResponseDto;
import com.comprehensive.eureka.admin.entity.AllowWord;
import com.comprehensive.eureka.admin.entity.ForbiddenWord;
import com.comprehensive.eureka.admin.exception.AdminException;
import com.comprehensive.eureka.admin.exception.ErrorCode;
import com.comprehensive.eureka.admin.repository.AllowWordRepository;
import com.comprehensive.eureka.admin.repository.ForbiddenWordRepository;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AllowWordServiceImpl implements AllowWordService {

    private final AllowWordRepository allowWordRepository;
    private final AllowWordRedisService redisService;


    @Override
    public AllowWordResponseDto addAllowWord(AllowWordRequestDto requestDto) {
        String word = requestDto.getWord().trim();


        if (allowWordRepository.existsByWord(word)) {
            throw new AdminException(ErrorCode.ALLOW_WORD_ALREADY_EXISTS);
        }

        AllowWord saved;
        try {
            saved = allowWordRepository.save(
                    AllowWord.builder()
                            .word(word)
                            .status(requestDto.isUsed())
                            .build()
            );

            if(requestDto.isUsed()) {
                redisService.addAllowWord(word);
            }
/*            BadwordToChatbotDto dto = new BadwordToChatbotDto(word);
            chatbotClient.post()
                    .uri("/chatbot/api/badwords")
                    .bodyValue(dto)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();*/


        } catch (Exception ex) {
            throw new AdminException(ErrorCode.ALLOW_WORD_CREATE_FAILED);
        }

        return new AllowWordResponseDto(
                saved.getId(),
                saved.getWord(),
                saved.isStatus()
        );
    }
}

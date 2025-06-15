package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.dto.request.ForbiddenWordRequestDto;
import com.comprehensive.eureka.admin.dto.response.ForbiddenWordResponseDto;
import com.comprehensive.eureka.admin.entity.ForbiddenWord;
import com.comprehensive.eureka.admin.exception.AdminException;
import com.comprehensive.eureka.admin.exception.ErrorCode;
import com.comprehensive.eureka.admin.repository.ForbiddenWordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForbiddenWordServiceImpl implements ForbiddenWordService {

    private final ForbiddenWordRepository forbiddenWordRepository;
    private final ForbiddenWordRedisService redisService;

/*    @Qualifier("chatbotClient")
    private final WebClient chatbotClient;*/

    /**
     * 금칙어 목록 조회 (사용 여부·단어 필터 지원)
     */
    @Override
    @Transactional(readOnly = true)
    public List<ForbiddenWordResponseDto> getForbiddenWords(Boolean used, String value) {
        List<ForbiddenWord> entities;

        if (used != null && value != null) {
            // 상태 + 단어 필터
            entities = forbiddenWordRepository.findByStatusAndWord(used, value);
        }
        else if (used != null) {
            // 상태만 필터
            entities = forbiddenWordRepository.findByStatus(used);
        }
        else if (value != null) {
            // 단어만 필터
            entities = forbiddenWordRepository.findByWord(value)
                    .map(List::of)
                    .orElse(Collections.emptyList());
        }
        else {
            // 필터 없을 때
            entities = forbiddenWordRepository.findAll();
        }

        return entities.stream()
                .map(e -> new ForbiddenWordResponseDto(
                        e.getId(),
                        e.getWord(),
                        e.isStatus()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 금칙어 추가 및 챗봇 모듈에 등록
     */
    @Override
    @Transactional
    public ForbiddenWordResponseDto addForbiddenWord(ForbiddenWordRequestDto requestDto) {
        String word = requestDto.getWord().trim();


        if (forbiddenWordRepository.existsByWord(word)) {
            throw new AdminException(ErrorCode.FORBIDDEN_WORD_ALREADY_EXISTS);
        }

        ForbiddenWord saved;
        try {
            saved = forbiddenWordRepository.save(
                    ForbiddenWord.builder()
                            .word(word)
                            .status(requestDto.isUsed())
                            .build()
            );

            if(requestDto.isUsed()) {
                redisService.addForbiddenWord(word);
            }
/*            BadwordToChatbotDto dto = new BadwordToChatbotDto(word);
            chatbotClient.post()
                    .uri("/chatbot/api/badwords")
                    .bodyValue(dto)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();*/


        } catch (Exception ex) {
            throw new AdminException(ErrorCode.FORBIDDEN_WORD_CREATE_FAILED);
        }

        return new ForbiddenWordResponseDto(
                saved.getId(),
                saved.getWord(),
                saved.isStatus()
        );
    }


    /**
     * 금칙어 삭제 및 챗봇 모듈에서 제거
     */
    @Override
    @Transactional
    public void deleteForbiddenWord(Long id) {
        ForbiddenWord fw = forbiddenWordRepository.findById(id)
                .orElseThrow(() -> new AdminException(ErrorCode.FORBIDDEN_WORD_NOT_FOUND));

        String word = fw.getWord();

        try {
            forbiddenWordRepository.delete(fw);
            if( fw.isStatus()) {
                redisService.removeForbiddenWord(word);
            }
/*            chatbotClient.delete()
                    .uri("/chatbot/api/badwords/{word}", word)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();*/

        } catch (Exception ex) {
            throw new AdminException(ErrorCode.FORBIDDEN_WORD_DELETE_FAILED);
        }
    }


    /**
     * 금칙어 상태 토글 (DB 업데이트 + 챗봇 모듈 동기화)
     */
    @Override
    @Transactional
    public ForbiddenWordResponseDto toggleForbiddenWordStatus(Long id) {
        ForbiddenWord fw = forbiddenWordRepository.findById(id)
                .orElseThrow(() -> new AdminException(ErrorCode.FORBIDDEN_WORD_NOT_FOUND));

        boolean newStatus = !fw.isStatus();
        fw.setStatus(newStatus);
        ForbiddenWord updated = forbiddenWordRepository.save(fw);

        try {
            if (newStatus) {
                // 챗봇에 추가
                redisService.addForbiddenWord(updated.getWord());
 /*                BadwordToChatbotDto dto = new BadwordToChatbotDto(updated.getWord());
                    chatbotClient.post()
                        .uri("/chatbot/api/badwords")
                        .bodyValue(dto)
                        .retrieve()
                        .bodyToMono(Void.class)
                        .block();*/
            } else {
                // 챗봇에서 삭제
                redisService.removeForbiddenWord(updated.getWord());
/*                chatbotClient.delete()
                        .uri("/chatbot/api/badwords/{word}", updated.getWord())
                        .retrieve()
                        .bodyToMono(Void.class)
                        .block();*/
            }
        } catch (Exception ex) {
            if (newStatus) {
                throw new AdminException(ErrorCode.FORBIDDEN_WORD_CHATBOT_ADD_FAILED);
            } else {
                throw new AdminException(ErrorCode.FORBIDDEN_WORD_CHATBOT_DELETE_FAILED);
            }
        }

        return new ForbiddenWordResponseDto(
                updated.getId(),
                updated.getWord(),
                updated.isStatus()
        );
    }
}

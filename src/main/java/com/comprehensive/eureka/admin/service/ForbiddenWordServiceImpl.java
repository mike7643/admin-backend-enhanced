package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.dto.request.ForbiddenWordRequestDto;
import com.comprehensive.eureka.admin.dto.response.ForbiddenWordResponseDto;
import com.comprehensive.eureka.admin.entity.ForbiddenWord;
import com.comprehensive.eureka.admin.exception.AdminException;
import com.comprehensive.eureka.admin.exception.ErrorCode;
import com.comprehensive.eureka.admin.repository.ForbiddenWordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForbiddenWordServiceImpl implements ForbiddenWordService {

    private final ForbiddenWordRepository forbiddenWordRepository;
    private final WebClient webClient;


    @Override
    @Transactional(readOnly = true)
    public List<ForbiddenWordResponseDto> getForbiddenWords(Boolean used, String value) {
        List<ForbiddenWord> entities;

        if (used != null && value != null) {
            // 상태 + 단어 필터
            entities = forbiddenWordRepository.findByStatusAndWord(used, value);
        } else if (used != null) {
            // 상태만 필터
            entities = forbiddenWordRepository.findByStatus(used);
        } else if (value != null) {
            // 단어만 필터
            entities = forbiddenWordRepository.findByWord(value)
                    .map(List::of)
                    .orElseThrow(() -> new AdminException(ErrorCode.FORBIDDEN_WORD_NOT_FOUND));
        } else {
            // 필터 없을 때
            entities = forbiddenWordRepository.findAll();
        }

        if (entities.isEmpty()) {
            throw new AdminException(ErrorCode.FORBIDDEN_WORD_NOT_FOUND);
        }

        return entities.stream()
                .map(e -> new ForbiddenWordResponseDto(
                        e.getId(),
                        e.getWord(),
                        e.isStatus()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ForbiddenWordResponseDto addForbiddenWord(ForbiddenWordRequestDto requestDto) {
        log.info("Adding forbidden word: {}", requestDto);
        String word = requestDto.getWord().trim();
        log.info("Word: {}", word);

        if (forbiddenWordRepository.existsByWord(word)) {
            log.info("AdminException");
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
/*

            webClient.post()
                    .uri("/api/badwords")
                    .bodyValue(requestDto)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

*/
            log.info("saved: {}", saved);
        } catch (Exception ex) {
            log.info("AdminException");
            throw new AdminException(ErrorCode.FORBIDDEN_WORD_CREATE_FAILED);
        }

        ForbiddenWordResponseDto forbiddenWordResponseDto = new ForbiddenWordResponseDto(
                saved.getId(),
                saved.getWord(),
                saved.isStatus());
        log.info("forbiddenWordResponseDto: {}", forbiddenWordResponseDto);
        return forbiddenWordResponseDto;
    }

    @Override
    @Transactional
    public void deleteForbiddenWord(Long id) {
        ForbiddenWord fw = forbiddenWordRepository.findById(id)
                .orElseThrow(() -> new AdminException(ErrorCode.FORBIDDEN_WORD_NOT_FOUND));

        String word = fw.getWord();

        try {
            forbiddenWordRepository.delete(fw);

            webClient.delete()
                    .uri("/api/badwords/{word}", word)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (Exception ex) {
            throw new AdminException(ErrorCode.FORBIDDEN_WORD_DELETE_FAILED);
        }
    }

    @Override
    @Transactional
    public ForbiddenWordResponseDto toggleForbiddenWordStatus(Long id) {
        ForbiddenWord fw = forbiddenWordRepository.findById(id)
                .orElseThrow(() -> new AdminException(ErrorCode.FORBIDDEN_WORD_NOT_FOUND));

        fw.setStatus(!fw.isStatus());
        ForbiddenWord updated = forbiddenWordRepository.save(fw);

        return new ForbiddenWordResponseDto(
                updated.getId(),
                updated.getWord(),
                updated.isStatus()
        );
    }
}

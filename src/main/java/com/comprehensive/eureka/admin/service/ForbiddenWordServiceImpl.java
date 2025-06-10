package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.dto.request.ForbiddenWordRequestDto;
import com.comprehensive.eureka.admin.dto.response.ForbiddenWordResponseDto;
import com.comprehensive.eureka.admin.entity.ForbiddenWord;
import com.comprehensive.eureka.admin.exception.AdminException;
import com.comprehensive.eureka.admin.exception.ErrorCode;
import com.comprehensive.eureka.admin.repository.ForbiddenWordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ForbiddenWordServiceImpl implements ForbiddenWordService {

    private final ForbiddenWordRepository forbiddenWordRepository;

    public ForbiddenWordServiceImpl(ForbiddenWordRepository forbiddenWordRepository) {
        this.forbiddenWordRepository = forbiddenWordRepository;
    }

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
                    .orElseThrow(() -> new AdminException(ErrorCode.FORBIDDEN_WORD_NOT_FOUND));
        }
        else {
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
        } catch (Exception ex) {
            throw new AdminException(ErrorCode.FORBIDDEN_WORD_CREATE_FAILED);
        }

        return new ForbiddenWordResponseDto(
                saved.getId(),
                saved.getWord(),
                saved.isStatus()
        );
    }
}

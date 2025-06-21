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
import java.net.CacheRequest;
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

        } catch (Exception ex) {
            throw new AdminException(ErrorCode.ALLOW_WORD_CREATE_FAILED);
        }

        return new AllowWordResponseDto(
                saved.getId(),
                saved.getWord(),
                saved.isStatus()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AllowWordResponseDto> getAllowWords(Boolean used, String value) {
        List<AllowWord> entities;

        if (used != null && value != null) {
            // 사용 여부와 단어로 필터
            entities = allowWordRepository.findByStatusAndWord(used, value)
                    .orElse(Collections.emptyList());

        } else if (used != null) {
            // 사용 여부로 필터
            entities = allowWordRepository.findByStatus(used);

        } else if (value != null) {
            // 단어만 필터
            entities = allowWordRepository.findByWord(value)
                    .map(List::of)
                    .orElse(Collections.emptyList());
        } else {
            // 필터 없을 때
            entities = allowWordRepository.findAll();
        }

        return entities.stream()
                .map(e -> new AllowWordResponseDto(
                        e.getId(),
                        e.getWord(),
                        e.isStatus()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteAllowWord(Long id) {
        AllowWord aw = allowWordRepository.findById(id)
                .orElseThrow(() -> new AdminException(ErrorCode.ALLOW_WORD_NOT_FOUND));

        String word = aw.getWord();

        try {
            allowWordRepository.delete(aw);
            if (aw.isStatus()) {
                redisService.removeAllowWord(word);
            }
        } catch (Exception ex) {
            throw new AdminException(ErrorCode.ALLOW_WORD_DELETE_FAILED);
        }

    }

    @Override
    @Transactional
    public AllowWordResponseDto toggleAllowWordStatus(Long id) {
        AllowWord aw = allowWordRepository.findById(id)
                .orElseThrow(() -> new AdminException(ErrorCode.ALLOW_WORD_NOT_FOUND));

        boolean newStatus = !aw.isStatus();
        aw.setStatus(newStatus);
        AllowWord updated = allowWordRepository.save(aw);

        try {
            if (newStatus) {
                // 챗봇에 추가
                redisService.addAllowWord(updated.getWord());

            } else {
                // 챗봇에서 삭제
                redisService.removeAllowWord(updated.getWord());

            }

        } catch (Exception ex) {
            throw new AdminException(ErrorCode.ALLOW_WORD_UPDATE_FAILED);
        }

        return new AllowWordResponseDto(
                updated.getId(),
                updated.getWord(),
                updated.isStatus()
        );
    }
}

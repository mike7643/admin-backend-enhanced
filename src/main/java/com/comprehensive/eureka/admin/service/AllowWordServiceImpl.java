package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.dto.AllowWordRequestDto;
import com.comprehensive.eureka.admin.dto.response.AllowWordResponseDto;
import com.comprehensive.eureka.admin.entity.AllowWord;
import com.comprehensive.eureka.admin.exception.AdminException;
import com.comprehensive.eureka.admin.exception.ErrorCode;
import com.comprehensive.eureka.admin.repository.AllowWordRepository;
import com.comprehensive.eureka.admin.service.wordevent.SyncAction;
import com.comprehensive.eureka.admin.service.wordevent.WordCacheSyncEvent;
import com.comprehensive.eureka.admin.service.wordevent.WordType;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AllowWordServiceImpl implements AllowWordService {

    private final AllowWordRepository allowWordRepository;
    private final ApplicationEventPublisher eventPublisher;


    @Override
    @Transactional
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
        } catch (Exception ex) {
            throw new AdminException(ErrorCode.ALLOW_WORD_CREATE_FAILED);
        }
        if (requestDto.isUsed()) {
            publishAllowEvent(SyncAction.ADD, word);
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
        } catch (Exception ex) {
            throw new AdminException(ErrorCode.ALLOW_WORD_DELETE_FAILED);
        }
        if (aw.isStatus()) {
            publishAllowEvent(SyncAction.REMOVE, word);
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

        publishAllowEvent(newStatus ? SyncAction.ADD : SyncAction.REMOVE, updated.getWord());

        return new AllowWordResponseDto(
                updated.getId(),
                updated.getWord(),
                updated.isStatus()
        );
    }

    /**
     * Redis 반영은 커밋 이후 리스너에서 처리되므로, 반드시 트랜잭션 안에서 발행해야 한다.
     * 트랜잭션 밖에서 발행하면 AFTER_COMMIT 리스너가 실행되지 않고 이벤트가 버려진다.
     */
    private void publishAllowEvent(SyncAction action, String word) {
        eventPublisher.publishEvent(new WordCacheSyncEvent(WordType.ALLOW, action, word));
    }
}

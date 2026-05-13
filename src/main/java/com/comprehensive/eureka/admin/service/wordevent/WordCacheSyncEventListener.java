package com.comprehensive.eureka.admin.service.wordevent;

import com.comprehensive.eureka.admin.service.AllowWordRedisService;
import com.comprehensive.eureka.admin.service.ForbiddenWordRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class WordCacheSyncEventListener {

    private final ForbiddenWordRedisService forbiddenWordRedisService;
    private final AllowWordRedisService allowWordRedisService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(WordCacheSyncEvent event) {
        try {
            switch (event.wordType()) {
                case FORBIDDEN -> syncForbidden(event.action(), event.word());
                case ALLOW -> syncAllow(event.action(), event.word());
            }
        } catch (Exception ex) {
            log.error("레디스 동기화 이벤트 처리 실패: wordType={}, action={}, word={}",
                    event.wordType(), event.action(), event.word(), ex);
        }
    }

    private void syncForbidden(SyncAction action, String word) {
        switch (action) {
            case ADD -> forbiddenWordRedisService.addForbiddenWord(word);
            case REMOVE -> forbiddenWordRedisService.removeForbiddenWord(word);
        }
    }

    private void syncAllow(SyncAction action, String word) {
        switch (action) {
            case ADD -> allowWordRedisService.addAllowWord(word);
            case REMOVE -> allowWordRedisService.removeAllowWord(word);
        }
    }
}

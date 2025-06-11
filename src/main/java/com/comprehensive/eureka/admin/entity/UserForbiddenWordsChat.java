package com.comprehensive.eureka.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_forbidden_words_chat",
        indexes = {
                @Index(name = "idx_ufwc_user",      columnList = "user_id"),
                @Index(name = "idx_ufwc_chat",      columnList = "chat_message_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserForbiddenWordsChat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_forbidden_words_chat_id")
    private Long id;

    /** User-Service PK (ID 참조만) */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Chat-Service PK (ID 참조만) */
    @Column(name = "chat_message_id", nullable = false)
    private Long chatMessageId;

    /** ForbiddenWord 엔티티와 연관관계 설정 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "forbidden_word_id", nullable = false)
    private ForbiddenWord forbiddenWord;
}

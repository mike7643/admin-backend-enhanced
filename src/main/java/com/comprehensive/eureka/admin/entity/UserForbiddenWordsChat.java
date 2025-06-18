package com.comprehensive.eureka.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_forbidden_words_chat",
        indexes = {
                @Index(name = "idx_ufwc_user",      columnList = "user_id"),
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserForbiddenWordsChat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_forbidden_words_chat_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "forbidden_word_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ForbiddenWord forbiddenWord;

    @Column(name = "chat_message_text", nullable = false, length = 1000)
    private String chatMessageText;

    @Column(name = "chat_sent_at", nullable = false)
    private Long chatSentAt;
}

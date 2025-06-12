package com.comprehensive.eureka.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "chat_message_id", nullable = false)
    private Long chatMessageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "forbidden_word_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ForbiddenWord forbiddenWord;
}

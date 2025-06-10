package com.comprehensive.eureka.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "forbidden_words",
        uniqueConstraints = @UniqueConstraint(columnNames = "word")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForbiddenWord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 금칙어 단어 (중복 방지)
     */
    @Column(nullable = false, unique = true)
    private String word;

    /**
     * 금칙어 사용 여부 (true: 사용중, false: 비활성)
     */
    @Column(nullable = false)
    private boolean status;
}

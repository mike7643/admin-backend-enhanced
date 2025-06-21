package com.comprehensive.eureka.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "allow_words",
        uniqueConstraints = @UniqueConstraint(columnNames = "word")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllowWord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String word;

    @Column(nullable = false)
    private boolean status;
}

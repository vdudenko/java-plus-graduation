package ru.yandex.practicum.comment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1000, nullable = false)
    private String text;

    @Column(name = "event_id", nullable = false)
    private Long event;

    @Column(name = "owner_id", nullable = false)
    private Long owner;

    private LocalDateTime created;
}

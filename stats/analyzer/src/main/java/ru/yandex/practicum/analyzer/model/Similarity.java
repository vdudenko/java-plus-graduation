package ru.yandex.practicum.analyzer.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "similarities", uniqueConstraints = @UniqueConstraint(columnNames = {"event1", "event2"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Similarity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "event1", nullable = false) private Long event1;
    @Column(name = "event2", nullable = false) private Long event2;
    @Column(name = "similarity", nullable = false) private Double similarity;
    @Column(name = "ts", nullable = false) private Instant timestamp;
}

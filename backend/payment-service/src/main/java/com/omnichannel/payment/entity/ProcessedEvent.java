package com.omnichannel.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "processed_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEvent {

    @Id
    @Column(length = 128)
    private String eventId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant processedAt;
}

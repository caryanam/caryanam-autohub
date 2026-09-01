package com.autohub.entity;

import com.autohub.enums.TrafficSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "social_visit_logs", indexes = {
        @Index(name = "idx_svl_vehicle_id", columnList = "vehicle_id"),
        @Index(name = "idx_svl_dealer_id", columnList = "dealer_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialVisitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false, foreignKey = @ForeignKey(name = "fk_svl_vehicle"))
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_svl_dealer"))
    private Dealer dealer;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private TrafficSource source;

    @Column(name = "vehicle_name", length = 200)
    private String vehicleName;

    @Column(name = "post_id", length = 100)
    private String postId;

    @Column(name = "post_url", length = 500)
    private String postUrl;

    @CreationTimestamp
    @Column(name = "visited_at", nullable = false, updatable = false)
    private LocalDateTime visitedAt;
}

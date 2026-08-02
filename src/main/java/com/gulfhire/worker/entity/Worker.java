package com.gulfhire.worker.entity;

import com.gulfhire.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Worker {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String profession;

    @Column(nullable = false)
    private Integer experienceYears;

    @Column(nullable = false)
    private String currentCountry;

    @Column(nullable = false)
    private Double expectedSalary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String about;

    private String profilePictureUrl;

    private String cvUrl;

    private String videoCvUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

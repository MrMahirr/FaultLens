package com.faultlens.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notification_settings")
public class NotificationSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Builder.Default
    @Column(name = "email_enabled")
    private boolean emailEnabled = true;

    @Builder.Default
    @Column(name = "slack_enabled")
    private boolean slackEnabled = false;

    @Column(name = "slack_webhook")
    private String slackWebhook;

    @Builder.Default
    @Column(name = "push_enabled")
    private boolean pushEnabled = false;

    @Builder.Default
    @Column(name = "mobile_enabled")
    private boolean mobileEnabled = false;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

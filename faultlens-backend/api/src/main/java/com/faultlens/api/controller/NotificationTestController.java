package com.faultlens.api.controller;

import com.faultlens.api.notification.EmailChannel;
import com.faultlens.api.notification.Notification;
import com.faultlens.api.notification.WebhookChannel;
import com.faultlens.api.service.InAppNotificationService;
import com.faultlens.api.service.NotificationSettingsService;
import com.faultlens.api.service.SlackNotificationService;
import com.faultlens.api.service.SlackWebhookUrls;
import com.faultlens.common.dto.ApiResponse;
import java.security.Principal;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings/notifications/test")
@RequiredArgsConstructor
public class NotificationTestController {

    private final SlackNotificationService slackNotificationService;
    private final EmailChannel emailChannel;
    private final WebhookChannel webhookChannel;
    private final InAppNotificationService inAppNotificationService;
    private final NotificationSettingsService notificationSettingsService;

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SlackTestRequest(String webhookUrl) {}
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record WebhookTestRequest(String webhookUrl) {}

    @PostMapping("/slack")
    public ResponseEntity<ApiResponse<String>> testSlack(
            Principal principal,
            @RequestBody SlackTestRequest request) {
        
        if (request.webhookUrl() == null || request.webhookUrl().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Webhook URL boş olamaz.", 400));
        }
        if (!SlackWebhookUrls.isAllowed(request.webhookUrl())) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Geçersiz Slack webhook URL'si. https://hooks.slack.com/services/... biçiminde olmalı.", 400));
        }

        String testMessage = "🚀 *FaultLens Test:* " + principal.getName() + " kullanıcısı Slack entegrasyonunu test ediyor. Bağlantı başarılı!";
        boolean success = slackNotificationService.sendNotification(request.webhookUrl(), testMessage);

        if (success) {
            return ResponseEntity.ok(ApiResponse.ok("Test mesajı başarıyla gönderildi."));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Test mesajı gönderilemedi. Webhook URL'sini kontrol edin.", 400));
        }
    }

    /**
     * Sends a test e-mail to the caller's own profile address. Arbitrary recipients are not
     * accepted: that would let any user send mail through the server's SMTP account.
     */
    @PostMapping("/email")
    public ResponseEntity<ApiResponse<String>> testEmail(Principal principal) {
        var settings = notificationSettingsService.getByUsername(principal.getName());
        if (!settings.emailAvailable()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Sunucuda SMTP yapılandırılmamış (SMTP_HOST). Yöneticinize başvurun.", 400));
        }
        if (settings.email() == null || settings.email().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Profilinizde e-posta adresi yok. Ayarlar → Hesap bölümünden ekleyin.", 400));
        }
        boolean success = emailChannel.sendTo(settings.email(), Notification.of(
                "Test e-postası",
                principal.getName() + " kullanıcısı FaultLens e-posta bildirimlerini test ediyor. "
                        + "Bu e-postayı aldıysanız SMTP yapılandırması çalışıyor."));
        if (success) {
            return ResponseEntity.ok(ApiResponse.ok("Test e-postası " + settings.email() + " adresine gönderildi."));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error(
                "E-posta gönderilemedi. Sunucu SMTP ayarlarını kontrol edin.", 400));
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<String>> testWebhook(Principal principal, @RequestBody WebhookTestRequest request) {
        if (request.webhookUrl() == null || !webhookChannel.isAllowed(request.webhookUrl())) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Webhook URL'si herkese açık bir sunucuda https:// adresi olmalı.", 400));
        }
        boolean success = webhookChannel.post(request.webhookUrl(), Notification.of(
                "Test bildirimi", principal.getName() + " kullanıcısı FaultLens webhook entegrasyonunu test ediyor."));
        if (success) {
            return ResponseEntity.ok(ApiResponse.ok("Webhook test isteği başarıyla gönderildi."));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error(
                "Webhook isteği başarısız oldu (2xx yanıt alınamadı).", 400));
    }

    @PostMapping("/push")
    public ResponseEntity<ApiResponse<String>> testPush(Principal principal) {
        String title = "FaultLens Test Bildirimi";
        String message = principal.getName() + " kullanıcısı tarayıcı/uygulama içi bildirimleri test ediyor.";
        
        inAppNotificationService.broadcastAlarm(title, message);
        
        return ResponseEntity.ok(ApiResponse.ok("Push bildirimi WebSocket üzerinden başarıyla yayınlandı."));
    }
}

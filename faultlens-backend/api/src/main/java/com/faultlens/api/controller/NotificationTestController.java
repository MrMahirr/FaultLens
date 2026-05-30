package com.faultlens.api.controller;

import com.faultlens.api.dto.SettingsDtos.UpdateNotificationRequest;
import com.faultlens.api.service.EmailNotificationService;
import com.faultlens.api.service.InAppNotificationService;
import com.faultlens.api.service.NotificationSettingsService;
import com.faultlens.api.service.SlackNotificationService;
import com.faultlens.common.dto.ApiResponse;
import java.security.Principal;
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
    private final EmailNotificationService emailNotificationService;
    private final InAppNotificationService inAppNotificationService;
    private final NotificationSettingsService notificationSettingsService;

    public record SlackTestRequest(String webhookUrl) {}
    public record EmailTestRequest(String emailAddress) {}

    @PostMapping("/slack")
    public ResponseEntity<ApiResponse<String>> testSlack(
            Principal principal,
            @RequestBody SlackTestRequest request) {
        
        if (request.webhookUrl() == null || request.webhookUrl().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Webhook URL boş olamaz.", 400));
        }

        String testMessage = "🚀 *FaultLens Test:* " + principal.getName() + " kullanıcısı Slack entegrasyonunu test ediyor. Bağlantı başarılı!";
        boolean success = slackNotificationService.sendNotification(request.webhookUrl(), testMessage);

        if (success) {
            return ResponseEntity.ok(ApiResponse.ok("Test mesajı başarıyla gönderildi."));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Test mesajı gönderilemedi. Webhook URL'sini kontrol edin.", 400));
        }
    }

    @PostMapping("/email")
    public ResponseEntity<ApiResponse<String>> testEmail(
            Principal principal,
            @RequestBody EmailTestRequest request) {
        
        if (request.emailAddress() == null || request.emailAddress().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("E-posta adresi boş olamaz.", 400));
        }

        var settings = notificationSettingsService.getByUsername(principal.getName());
        
        if (settings.emailjsServiceId() == null || settings.emailjsTemplateId() == null || settings.emailjsPublicKey() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lütfen önce EmailJS ayarlarını (Service ID, Template ID, Public Key) kaydedin.", 400));
        }

        String subject = "FaultLens - Test E-postası";
        String body = "Merhaba,\n\n" + principal.getName() + " kullanıcısı FaultLens e-posta entegrasyonunu test ediyor.\nEğer bu maili aldıysanız Email.js yapılandırmanız doğru çalışıyor demektir!\n\nİyi çalışmalar,\nFaultLens Sistemi";

        boolean success = emailNotificationService.sendEmail(
            request.emailAddress(), 
            subject, 
            body,
            settings.emailjsServiceId(),
            settings.emailjsTemplateId(),
            settings.emailjsPublicKey()
        );

        if (success) {
            return ResponseEntity.ok(ApiResponse.ok("Test e-postası başarıyla gönderildi. Gelen kutunuzu kontrol edin."));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("E-posta gönderilemedi. Email.js ayarlarını kontrol edin.", 400));
        }
    }

    @PostMapping("/push")
    public ResponseEntity<ApiResponse<String>> testPush(Principal principal) {
        String title = "FaultLens Test Bildirimi";
        String message = principal.getName() + " kullanıcısı tarayıcı/uygulama içi bildirimleri test ediyor.";
        
        inAppNotificationService.broadcastAlarm(title, message);
        
        return ResponseEntity.ok(ApiResponse.ok("Push bildirimi WebSocket üzerinden başarıyla yayınlandı."));
    }
}

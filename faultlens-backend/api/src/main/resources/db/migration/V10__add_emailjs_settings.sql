ALTER TABLE notification_settings
ADD COLUMN emailjs_service_id VARCHAR(255),
ADD COLUMN emailjs_template_id VARCHAR(255),
ADD COLUMN emailjs_public_key VARCHAR(255);

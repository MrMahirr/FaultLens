# FaultLens Backend ⚙️

FaultLens platformunun kalbi olan veri işleme, log toplama ve yapay zeka analiz süreçlerinin yönetildiği backend servisidir. Yüksek ölçeklenebilirlik için modüler bir mimariyle tasarlanmıştır.

## 🛠️ Teknolojiler
- **Dil:** Java 21
- **Framework:** Spring Boot 3.2.x
- **Veritabanı:** PostgreSQL, Flyway (Veritabanı Göçleri / Migrations)
- **Mesaj Kuyruğu (Message Broker):** Apache Kafka
- **Build Aracı:** Maven

## 🧩 Modüler Mimari (Multi-Module)
Backend projesi, SOLID prensiplerine ve Separation of Concerns (Sorumlulukların Ayrılığı) mantığına uygun olarak alt modüllere ayrılmıştır:

1. **`common`:** Tüm modüllerin ortak kullandığı DTO'lar, Enum'lar (LogSourceType vb.) ve Custom Exception'ları barındırır.
2. **`collector`:** Dış kaynaklardan (Docker, K8s, SSH, Local File) logları toplayıp standart bir `LogEventDto` formatına çevirerek Kafka'ya (raw-logs topic) yazar.
3. **`processor`:** Kafka'dan ham logları alır, filtreler, etiketler ve işlenmiş haliyle PostgreSQL veritabanına kaydeder.
4. **`analyzer`:** Yapay zeka entegrasyonlarının bulunduğu modüldür. Belirli hata kalıplarını veya kullanıcı isteklerini işleyerek Kök Neden Analizi (Root Cause Analysis) üretir.
5. **`api`:** Frontend ile iletişim kuran REST API Controller'larını, WebSocket handler'larını ve konfigürasyonları barındırır. Uygulamanın ana giriş noktasıdır.

## ⚙️ Kurulum ve Çalıştırma

### 1. Veritabanı ve Kafka
Projenin ana dizinindeki `docker-compose.yml` ile PostgreSQL ve Kafka'yı ayağa kaldırdığınızdan emin olun.

### 2. Uygulama Ayarları
`api/src/main/resources/application.yml` dosyasındaki veritabanı şifresi (`faultlens`) ve Kafka ayarlarının kendi ortamınızla uyuştuğunu kontrol edin.

### 3. Derleme ve Çalıştırma
Projeyi derlemek ve tüm bağımlılıkları indirmek için:
```bash
./mvnw clean compile
```

Uygulamayı başlatmak için API modülü üzerinden Spring Boot'u çalıştırın:
```bash
./mvnw spring-boot:run -pl api
```

Backend varsayılan olarak **http://localhost:3500** portunda çalışacaktır.

## 📝 Kurallar (SOLID & IoC)
Bu projeye katkıda bulunurken veya kod yazarken şu kurallara dikkat edilmelidir:
- Tüm servisler arayüzler (interface) üzerinden tasarlanmalı ve Inversion of Control (IoC) prensibine uyulmalıdır.
- Nesneler arası bağımlılıklar Constructor Injection ile sağlanmalıdır (`@RequiredArgsConstructor` kullanımı önerilir).
- ORM/Veritabanı işlemleri için Type-ORM veya karmaşık Hibernate özelliklerinden ziyade temiz ve anlaşılır Spring Data JPA repository'leri kullanılmalıdır.

# FaultLens 🔍

FaultLens, yapay zeka destekli, modern ve yüksek performanslı bir log analizi ve izleme (monitoring) platformudur. Geliştiricilerin ve sistem yöneticilerinin logları merkezi bir noktada toplamalarını, hataların kök nedenlerini (Root Cause Analysis) yapay zeka ile saniyeler içinde bulmalarını sağlar.

## 🚀 Temel Özellikler
- **Gerçek Zamanlı Log Akışı (Live Tail):** Logları anlık olarak süzülmüş ve formatlanmış bir şekilde izleyin.
- **Yapay Zeka ile Kök Neden Analizi:** Anlaşılmaz stacktrace'leri ve hataları saniyeler içinde analiz edin, çözüm önerileri alın.
- **Çoklu Kaynak Desteği:** Docker, Kubernetes, SSH ve Local File üzerinden kolayca log toplayın.
- **Gelişmiş Filtreleme ve Arama:** Milyonlarca satır log içerisinde saniyeler içinde arama yapın.
- **Modern Arayüz:** Kullanıcı dostu, animasyonlu ve "dark mode" destekli harika tasarım.

## 🏗️ Mimari ve Teknolojiler
Proje iki ana bileşenden oluşmaktadır:
1. **Frontend (Kullanıcı Arayüzü):** Next.js, React, TailwindCSS ve TypeScript kullanılarak geliştirilmiştir.
2. **Backend (Sunucu ve Veri İşleme):** Java 21, Spring Boot, Apache Kafka ve PostgreSQL kullanılarak yüksek veri hacimlerini işleyebilecek şekilde tasarlanmıştır.

## 🏃‍♂️ Kurulum ve Çalıştırma

Projeyi lokal ortamınızda çalıştırmak için aşağıdaki adımları izleyebilirsiniz. Veritabanı ve Kafka gibi bağımlılıklar için projenin kök dizinindeki `docker-compose.yml` dosyasını kullanabilirsiniz.

### 1. Altyapıyı Ayağa Kaldırma (Docker)
```bash
docker-compose up -d
```
Bu komut PostgreSQL veritabanını, Apache Kafka'yı ve Zookeeper'ı başlatacaktır.

### 2. Backend'i Başlatma
Detaylı bilgi için [Backend README](./faultlens-backend/README.md) dosyasına göz atın.
```bash
cd faultlens-backend
./mvnw clean compile spring-boot:run
```

### 3. Frontend'i Başlatma
Detaylı bilgi için [Frontend README](./faultlens-frontend/README.md) dosyasına göz atın.
```bash
cd faultlens-frontend
pnpm install
pnpm dev
```

Uygulama başarıyla başlatıldığında **http://localhost:3000** adresinden arayüze erişebilirsiniz.

### Portlar (tek kaynak)
| Servis | Adres (host) | Not |
|---|---|---|
| Frontend (Next.js) | `http://localhost:3000` | `pnpm dev` / compose |
| Backend API | `http://localhost:3500/api/v1` | `PORT`, Swagger: `/swagger-ui/index.html` |
| Backend WebSocket | `ws://localhost:3500/ws/logs` | `NEXT_PUBLIC_WS_URL=ws://localhost:3500/ws` (`/logs` frontend ekler) |
| PostgreSQL | `localhost:5433` | compose `5433:5432`; container içinden `postgres:5432` |
| Redis | `localhost:6379` | |
| Kafka | `localhost:9092` | container içinden `kafka:29092` |
| Kafka UI | `http://localhost:8090` | |

Nginx örneği (`deploy/nginx.conf`) aynı portlara yönlendirir: `/api/v1` ve `/ws` → 3500, geri kalan → 3000.

## 📚 Entegrasyon Rehberi
Kendi projelerinizi FaultLens'e nasıl bağlayacağınızı öğrenmek için [INTEGRATION_GUIDE.md](./INTEGRATION_GUIDE.md) dosyasını inceleyebilirsiniz.

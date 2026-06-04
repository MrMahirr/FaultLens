# FaultLens Entegrasyon Rehberi

Bu doküman, geliştirdiğiniz herhangi bir projeyi (Java, Node.js, Python, vb.) **FaultLens** log analiz ve izleme platformuna nasıl bağlayacağınızı detaylı bir şekilde anlatmaktadır.

FaultLens mimarisi gereği uygulamalarınızın kodlarına doğrudan bir SDK kurmanızı gerektirmez. Uygulamalarınızın standart log çıktılarını (dosya, konsol, docker vb.) dinleyerek verileri toplar. Bu sayede uygulamanızın performansına etki etmez (Zero-Overhead).

---

## 1. Loglama Standartları (Önerilen)

FaultLens'in logları daha verimli analiz edebilmesi (örneğin yapay zeka ile kök neden analizi yapabilmesi) için uygulamanızın log formatının düzenli olması tavsiye edilir. İdeal bir log satırı şu bilgileri içermelidir:
- **Tarih ve Saat (Timestamp):** `YYYY-MM-DD HH:mm:ss.SSS` formatında.
- **Severity/Seviye:** `INFO`, `ERROR`, `WARN`, `DEBUG`
- **Sınıf/Modül Adı:** Hatanın nerede yaşandığı.
- **Hata Mesajı ve Stacktrace:** Özellikle hataların detaylı dökümü (stacktrace) yeni satırlar halinde bulunmalıdır.

**Örnek İdeal Log Satırı:**
```text
2026-05-30 14:30:15.123 ERROR com.myapp.service.UserService - Kullanıcı bulunamadı: ID 5
java.lang.NullPointerException: user cannot be null
    at com.myapp.service.UserService.getUser(UserService.java:45)
    ...
```

---

## 2. Entegrasyon Yöntemleri

Geliştirdiğiniz projeyi FaultLens'e bağlamak için 3 temel yöntemi kullanabilirsiniz:

### Yöntem A: Local File (Fiziksel Dosya Üzerinden)
En kolay ve en yaygın yöntemdir. Uygulamanızı loglarını konsol yerine (veya konsolla birlikte) bir `.log` dosyasına yazacak şekilde yapılandırırsınız. FaultLens bu dosyayı gerçek zamanlı olarak (tail -f) okur.

**Nasıl Yapılır?**
1. Uygulamanızın loglarını `C:\logs\myapp.log` (veya Linux için `/var/log/myapp.log`) dizinine yazmasını sağlayın.
2. FaultLens arayüzünden **Kaynaklar > Yeni Kaynak Ekle** butonuna tıklayın.
3. Tür olarak **Local File** seçin.
4. Dosya yoluna log dosyanızın "Absolute Path" (Tam yolunu) yazın.

### Yöntem B: Docker (Container Üzerinden)
Eğer uygulamanız bir Docker Container içerisinde çalışıyorsa hiçbir dosya ayarı yapmanıza gerek yoktur. Docker logları varsayılan olarak stdout/stderr üzerinden yönetir.

**Nasıl Yapılır?**
1. Uygulamanızın loglarını sadece standart konsola (Console/Terminal) basmasını sağlayın. (Docker bunu otomatik yakalar).
2. FaultLens arayüzünden **Docker** kaynak türünü seçin.
3. **Container ID veya İsmi** alanına izlemek istediğiniz container'ın adını veya ID'sini yazın.
4. **Docker Host (Opsiyonel)** alanını eğer uygulamanız aynı bilgisayardaysa **boş bırakın** (Sistem yerel Docker servisine otomatik bağlanır). Eğer uzak bir sunucudaki Docker ise API adresini yazın (Örn: `tcp://192.168.1.100:2375`).

### Yöntem C: SSH (Uzak Sunucu Üzerinden)
Uygulamanız uzak bir sunucuda (VDS/VPS) çalışıyorsa kullanılır.

**Nasıl Yapılır?**
1. Uzak sunucuda logların bir dosyaya (Örn: `/var/log/nginx/error.log`) yazılmasını sağlayın.
2. FaultLens arayüzünden **SSH** kaynak türünü seçin.
3. Sunucu IP, Port, Kullanıcı Adı, Şifre ve uzak sunucudaki log dosyasının yolunu girin.

### Yöntem D: Kubernetes (K8s Cluster Üzerinden)
Uygulamanız bir Kubernetes cluster'ı (örneğin Minikube, EKS, GKE, AKS) üzerinde pod olarak çalışıyorsa, Kubernetes API'si üzerinden loglar otomatik olarak okunabilir.

**Nasıl Yapılır?**
1. Pod'larınızın loglarını standart konsola (stdout/stderr) yazmasını sağlayın. Kubernetes bu logları otomatik olarak toplar.
2. FaultLens arayüzünden **Kubernetes** kaynak türünü seçin.
3. Uygulamanızın çalıştığı **Namespace** bilgisini girin (Örn: `default`, `production`, `monitoring`).
4. (Opsiyonel) Eğer FaultLens cluster dışında çalışıyorsa, Kubeconfig veya yetkilendirilmiş Service Account token ayarlarının FaultLens sunucusunda doğru yapıldığından emin olun. FaultLens Kubernetes API'sine bağlanarak ilgili namespace altındaki podların loglarını canlı olarak aktaracaktır.

---

## 3. Akıllı Doğrulama ve Kaynak Yönetimi

FaultLens, log kaynaklarını eklerken ve yönetirken işinizi kolaylaştıracak iki benzersiz özelliğe sahiptir:

### A) Test Edip Kaydetme (Pre-Save Validation)
Arayüzden (Add Source Modal) yeni bir kaynak eklerken doğrudan kaydedemezsiniz. Önce **"Bağlantıyı Test Et"** butonuna basmanız gerekir.
- FaultLens, veritabanına hiçbir şey kaydetmeden önce doğrudan belirttiğiniz dosyaya, Docker container'ına veya SSH sunucusuna erişmeye çalışır.
- Bağlantı başarılı olursa yeşil onay işareti çıkar ve **"Kaydet"** butonu aktifleşir.
- Başarısız olursa kırmızı hata uyarısı verilir ve hatalı yapılandırmanın sisteme eklenmesi engellenir.

### B) Proje Bazlı Log Görüntüleme ve Temizleme
- **Filtreleme:** `Logs` sayfasının sağ üst köşesindeki açılır menüden (Dropdown) istediğiniz projeyi seçerek sadece o projeye ait logları listeleyebilirsiniz.
- **Tek Tuşla Temizleme:** Eğer bir projeyi seçtiyseniz, hemen yanında kırmızı bir **"Logları Temizle"** butonu (Çöp Kutusu ikonu) belirir. Test yaparken biriken eski logları ve bunlara bağlı yapay zeka analizlerini veritabanından kalıcı olarak silmek için bu butonu kullanabilirsiniz.

---

## 3. Farklı Diller İçin Log Konfigürasyon Örnekleri

Uygulamalarınızın loglarını **Yöntem A (Local File)** için uygun hale getirecek kod örnekleri aşağıdadır:

### ☕ Spring Boot (Java)
Spring Boot projelerinde logları bir dosyaya yönlendirmek için `src/main/resources/logback-spring.xml` isimli bir dosya oluşturmanız yeterlidir.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Konsol Çıktısı -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Dosya Çıktısı (FaultLens'in okuyacağı dosya) -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/myapp.log</file> <!-- FAULTLENS'E VERİLECEK YOL -->
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/myapp.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="FILE" />
    </root>
</configuration>
```

### 🟩 Node.js (Winston Kütüphanesi ile)
Node.js projelerinde en popüler loglama kütüphanesi Winston'dır. Logları bir dosyaya yazdırmak için şu yapılandırmayı kullanabilirsiniz:

```javascript
const winston = require('winston');

// Logger'ı oluştur
const logger = winston.createLogger({
  level: 'info',
  format: winston.format.combine(
    winston.format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss.SSS' }),
    winston.format.printf(({ timestamp, level, message, stack }) => {
      return `${timestamp} ${level.toUpperCase()} - ${stack || message}`;
    })
  ),
  transports: [
    new winston.transports.Console(),
    // FaultLens'in okuyacağı dosya:
    new winston.transports.File({ filename: 'logs/myapp.log' }) 
  ],
});

// Kullanımı
logger.info("Uygulama başlatıldı");
try {
  throw new Error("Veritabanı bağlantısı koptu!");
} catch (error) {
  logger.error(error.message, { stack: error.stack });
}
```

### 🐍 Python (Standart Logging Modülü)
Python projelerinde harici bir kütüphaneye ihtiyaç duymadan logları dosyaya yazabilirsiniz.

```python
import logging
import os

# Log klasörünü oluştur
if not os.path.exists('logs'):
    os.makedirs('logs')

# Log formatını belirle
log_format = '%(asctime)s.%(msecs)03d %(levelname)-5s %(name)s - %(message)s'
date_format = '%Y-%m-%d %H:%M:%S'

logging.basicConfig(
    level=logging.INFO,
    format=log_format,
    datefmt=date_format,
    handlers=[
        logging.StreamHandler(), # Konsola yaz
        logging.FileHandler('logs/myapp.log', encoding='utf-8') # FaultLens'in okuyacağı dosya
    ]
)

logger = logging.getLogger("MyPythonApp")

# Kullanımı
logger.info("Uygulama başarıyla başlatıldı.")
try:
    1 / 0
except Exception as e:
    logger.exception("Matematiksel bir hata oluştu!")
```

### 🐘 PHP (Monolog Kütüphanesi ile)
Laravel veya Symfony gibi modern PHP framework'leri varsayılan olarak `storage/logs/laravel.log` dosyasına yazar. Eğer saf PHP kullanıyorsanız `Monolog` kütüphanesini kullanabilirsiniz.

```php
<?php
require __DIR__ . '/vendor/autoload.php';

use Monolog\Logger;
use Monolog\Handler\StreamHandler;
use Monolog\Formatter\LineFormatter;

$log = new Logger('MyApp');

// Log Formatı
$output = "%datetime% %level_name% %channel% - %message% %context% %extra%\n";
$formatter = new LineFormatter($output, "Y-m-d H:i:s.v");

// FaultLens'in okuyacağı dosya
$streamHandler = new StreamHandler(__DIR__ . '/logs/myapp.log', Logger::DEBUG);
$streamHandler->setFormatter($formatter);
$log->pushHandler($streamHandler);

// Kullanımı
$log->info('Kullanıcı sisteme giriş yaptı.', ['user_id' => 12]);
$log->error('Veritabanı sunucusuna ulaşılamıyor!');
```

---

## 4. Sorun Giderme ve İpuçları

- **Dosya İzinleri:** FaultLens uygulamasının log dosyasını okuyabilmesi için dosyanın bulunduğu dizine okuma yetkisi (read permission) olması gerekir. (Docker veya Linux ortamında CHMOD ayarlarını kontrol edin).
- **Log Rotasyonu (Log Rotation):** Uygulamalarınızın log dosyası zamanla gigabaytlarca boyuta ulaşabilir. Yukarıdaki Java örneğindeki gibi (`TimeBasedRollingPolicy`) loglarınızı günlük olarak arşivleyecek şekilde (Örn: `myapp.2026-05-30.log`) ayarlamayı unutmayın. FaultLens her zaman güncel isimli ana dosyayı takip eder.
- **Yapay Zeka Analizi:** Hataların yapay zeka tarafından (Kök Neden Analizi) doğru yorumlanabilmesi için Exception/Error yakalandığında sadece hata mesajını (`e.message`) değil, tam stacktrace'i (`e.stack`) loglara basmaya özen gösterin.

---

## 5. Desteklenen Çalışma Ortamları (Deployment Environments)

FaultLens, platform bağımsız bir mimariye sahip olduğu için projelerinizin çalıştığı neredeyse tüm modern ve geleneksel altyapılarla uyumludur. Projenizi barındırabileceğiniz ve FaultLens'e bağlayabileceğiniz ortamlar şunlardır:

1. **Lokal Geliştirme Ortamları (Local/Dev Environments)**
   - Kendi bilgisayarınız (Windows, macOS, Linux)
   - WSL (Windows Subsystem for Linux)
   - *Bağlantı Yöntemi: Local File veya Docker*

2. **Konteyner Mimarileri (Containerized Environments)**
   - Docker (Docker Desktop, Docker Engine)
   - Docker Compose
   - Docker Swarm
   - *Bağlantı Yöntemi: Docker API (TCP/Socket)*

3. **Kubernetes (K8s) Cluster'ları**
   - Lokal K8s (Minikube, k3s, Docker Desktop K8s, Kind)
   - Managed K8s (AWS EKS, Google GKE, Azure AKS, DigitalOcean DOKS)
   - RedHat OpenShift
   - *Bağlantı Yöntemi: Kubernetes*

4. **Geleneksel Sunucular (Bare-Metal & Virtual Machines)**
   - Fiziksel Sunucular (Bare-Metal)
   - Sanal Sunucular (VDS, VPS, EC2, Compute Engine, Droplet vb.)
   - *Bağlantı Yöntemi: SSH üzerinden log dosyası izleme*

5. **CI/CD ve Otomasyon Süreçleri**
   - Jenkins, GitLab CI, GitHub Actions (Devreye alma logları ve Deployment takibi)
   - *Bağlantı Yöntemi: FaultLens REST API (Webhook) üzerinden. Detaylar için Bölüm 6'ya bakınız.*

> **Not:** Serverless ortamlar (AWS Lambda, Google Cloud Functions) doğrudan bir dosya veya container erişimi sunmadığı için bu ortamlardaki logların FaultLens'e aktarılması için aracı bir CloudWatch/PubSub -> Kafka tetikleyicisi yazılması gerekir.

---

## 6. Otomatik Deployment (CI/CD) Entegrasyonu

FaultLens, yeni kodunuzun canlıya (veya test ortamına) alındığı anı tespit edip oluşan logları/hataları bu deployment ile ilişkilendirebilir (Korelasyon). Bunu sağlamak için CI/CD pipeline'ınıza ufak bir webhook (API çağrısı) eklemeniz yeterlidir.

### 🐙 GitHub Actions Örneği
Mevcut `deploy.yml` veya `ci.yml` dosyanızın en son adımına (başarılı deploy sonrası) şu curl komutunu ekleyin:

```yaml
    - name: Notify FaultLens Deployment
      run: |
        curl -X POST https://api.faultlens.com/api/v1/deployments \
          -H "Content-Type: application/json" \
          -d '{"serviceName":"my-backend-service", "version":"${{ github.sha }}", "environment":"PRODUCTION"}'
```

### 🦊 GitLab CI Örneği
`.gitlab-ci.yml` dosyanızdaki deploy aşamasının `script` bloğuna ekleyin:

```yaml
deploy_production:
  stage: deploy
  script:
    - echo "Uygulama sunucuya yükleniyor..."
    - # Kendi deploy komutlarınız...
    - curl -X POST https://api.faultlens.com/api/v1/deployments -H "Content-Type: application/json" -d '{"serviceName":"my-frontend", "version":"'$CI_COMMIT_SHORT_SHA'", "environment":"PRODUCTION"}'
```

### ⚙️ Genel cURL Kullanımı (Jenkins, Bitbucket vb.)
Herhangi bir bash/shell script içinden FaultLens'i tetiklemek için:
```bash
curl -X POST https://api.faultlens.com/api/v1/deployments \
  -H "Content-Type: application/json" \
  -d '{"serviceName":"servis-adiniz", "version":"v1.2.3", "environment":"PRODUCTION"}'
```

*(Not: `environment` alanı için `PRODUCTION`, `STAGING` veya `DEVELOPMENT` değerlerinden birini kullanmalısınız.)*

Bu entegrasyon sayesinde Deployments arayüzünde "Kırmızı Hata Parlaması" özelliğinden faydalanabilir ve hataların hangi versiyondan sonra patlak verdiğini saniyeler içinde görebilirsiniz.

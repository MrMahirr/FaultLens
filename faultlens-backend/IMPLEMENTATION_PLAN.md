# LogLens Backend Uygulama Plani

Bu dosya mevcut `faultlens-backend` reposuna gore hazirlanmis ilerleme planidir. Repo sifirdan bos degildir: Maven multi-module yapi, temel paketler, entity/service/controller siniflari, Flyway migration'lari ve calisan bir `-DskipTests package` derlemesi vardir. Bu nedenle plan, mevcut kodu koruyarak eksikleri kapatma, sozlesmeleri netlestirme ve uretime hazir hale getirme akisi olarak tasarlanmistir.

## Durum Takibi

- [X] Mevcut modul yapisi ve derleme durumu incelendi
- [X] Adim 1: Proje envanteri, sozlesme farklari ve kabul kriterleri
- [X] Adim 2: Parent POM, modul bagimliliklari ve build standardizasyonu
- [X] Adim 3: Common modul sozlesmelerinin tamamlanmasi
- [X] Adim 4: Flyway, application.yml ve docker-compose uyumu
- [X] Adim 5: Collector modulunun kaynak yonetimi ve adapter stabilizasyonu
- [X] Adim 6: Processor parser, grouping, batch ve realtime pipeline
- [X] Adim 7: Analyzer kurallari, korelasyon ve AI fallback tasarimi
- [X] Adim 8: API DTO, mapper ve query servis ayrimi
- [X] Adim 9: Security, JWT, auth ve hata sozlesmesi
- [X] Adim 10: WebSocket ve Redis realtime yayin akisi
- [ ] Adim 11: Test altyapisi ve modul testleri
- [ ] Adim 12: Uctan uca lokal dogrulama
- [ ] Adim 13: Operasyonel kalite, dokumantasyon ve production hardening

## UYGULAMA PLANI

### Adim 1: Proje Envanteri ve Sozlesme Farklari

**Modul:** Tum repo

**Bagimlilik:** Yok

**Ne yapilacak:**

- Mevcut kaynak kod ile verilen LogLens spesifikasyonu karsilastirilacak.
- Eksik DTO, exception, enum, controller endpoint, config, migration, test ve operasyonel ayarlar listelenecek.
- SOLID ve katmanli mimari acisindan duzeltilmesi gereken yerler belirlenecek.
- Bu plan dosyasinda ilgili adim checkbox'i tamamlandi olarak isaretlenecek.

**Dosyalar:**

- `IMPLEMENTATION_PLAN.md`
- Gerekirse `README.md`

**Dogrulama:**

- `.\mvnw.cmd -q -DskipTests package`
- Plan dosyasinda mevcut durum ve kabul kriterleri gorulebilir olmali.

**Tahmini sure:** 20 dakika

### Adim 2: Parent POM, Modul Bagimliliklari ve Build Standardizasyonu

**Modul:** root, common, collector, processor, analyzer, api

**Bagimlilik:** Adim 1

**Ne yapilacak:**

- Maven versiyonlari ve dependencyManagement spesifikasyona gore sabitlenecek.
- Sadece `api` modulunde `spring-boot-maven-plugin` kalacak.
- Test dependency'leri eklenecek: `spring-boot-starter-test`, `spring-kafka-test`, Testcontainers ihtiyaci degerlendirilecek.
- Compiler, annotation processor ve Java 21 ayarlari netlestirilecek.
- Build sirasinda modul bagimlilik zinciri korunacak: `common -> collector -> processor -> analyzer -> api`.

**Dosyalar:**

- `pom.xml`
- `common/pom.xml`
- `collector/pom.xml`
- `processor/pom.xml`
- `analyzer/pom.xml`
- `api/pom.xml`

**Dogrulama:**

- `.\mvnw.cmd -q clean package -DskipTests`
- `.\mvnw.cmd -q -pl common,collector,processor,analyzer,api -am test`

**Tahmini sure:** 30 dakika

### Adim 3: Common Modul Sozlesmelerinin Tamamlanmasi

**Modul:** common

**Bagimlilik:** Adim 2

**Ne yapilacak:**

- DTO alanlari spesifikasyonla birebir uyumlu hale getirilecek.
- `ApiResponse<T>` common modulune alinacak ve API tarafindaki tekrar kaldirilacak.
- `LogGroupDto`, `LogSourceDto`, `DeploymentDto`, auth/request DTO'lari icin ortak veya API'ye ozel sinirlar netlestirilecek.
- `Severity` seviyeleri `TRACE(0), DEBUG(1), INFO(2), WARN(3), ERROR(4), CRITICAL(5)` olacak ve `fromString(String)` eklenecek.
- `AnalyzerException` ve `ResourceNotFoundException` eklenecek.
- `LogLensException` constructor sirasi spesifikasyonla uyumlu hale getirilecek veya cagri noktalarinda tek sozlesme uygulanacak.
- Tumu icin Javadoc ve unit test eklenecek.

**Dosyalar:**

- `common/src/main/java/com/loglens/common/dto/LogEventDto.java`
- `common/src/main/java/com/loglens/common/dto/LogEntryDto.java`
- `common/src/main/java/com/loglens/common/dto/LogGroupDto.java`
- `common/src/main/java/com/loglens/common/dto/AnalysisResultDto.java`
- `common/src/main/java/com/loglens/common/dto/PagedResponse.java`
- `common/src/main/java/com/loglens/common/dto/ApiResponse.java`
- `common/src/main/java/com/loglens/common/enums/Severity.java`
- `common/src/main/java/com/loglens/common/enums/LogSourceType.java`
- `common/src/main/java/com/loglens/common/exception/*.java`
- `common/src/test/java/com/loglens/common/**`

**Dogrulama:**

- `.\mvnw.cmd -q -pl common test`
- `Severity.fromString("warn") == Severity.WARN` unit testi gecmeli.

**Tahmini sure:** 45 dakika

### Adim 4: Flyway, application.yml ve docker-compose Uyumu

**Modul:** api, root

**Bagimlilik:** Adim 3

**Ne yapilacak:**

- `application.yml` spesifikasyondaki port, DB, Redis, Kafka, actuator, Flyway ve LogLens property'leri ile uyumlu hale getirilecek.
- `docker-compose.yml` portlari ve healthcheck'leri spesifikasyona gore duzeltilecek.
- Flyway dosya adlari ve SQL icerikleri kontrol edilecek; `V6__create_users.sql` isim/icerik uyumu saglanacak.
- Hibernate `ddl-auto=validate` ile migration/entity uyumu saglanacak.
- Local dev icin gerekirse `.env.example` eklenecek.

**Dosyalar:**

- `api/src/main/resources/application.yml`
- `api/src/main/resources/db/migration/V1__create_log_sources.sql`
- `api/src/main/resources/db/migration/V2__create_log_groups.sql`
- `api/src/main/resources/db/migration/V3__create_log_entries.sql`
- `api/src/main/resources/db/migration/V4__create_deployments.sql`
- `api/src/main/resources/db/migration/V5__create_analyses.sql`
- `api/src/main/resources/db/migration/V6__create_users.sql`
- `docker-compose.yml`
- `.env.example`

**Dogrulama:**

- `docker-compose up -d`
- `.\mvnw.cmd -q -pl api -am package -DskipTests`
- `java -jar api/target/api-1.0.0-SNAPSHOT.jar`
- `curl http://localhost:8080/actuator/health`

**Tahmini sure:** 45 dakika

### Adim 5: Collector Modulunun Kaynak Yonetimi ve Adapter Stabilizasyonu

**Modul:** collector

**Bagimlilik:** Adim 4

**Ne yapilacak:**

- `LogSource` entity, repository ve servis metodlari transaction sinirlariyla netlestirilecek.
- Create/update request DTO'lari ve mapper ayrimi kurulacak; controller is mantigindan arindirilmaya hazir hale getirilecek.
- `KubernetesAdapter`, `SshAdapter`, `DockerAdapter` config parsing, lifecycle, reconnect ve `stopStreaming` davranislari icin net kaynak kapatma sozlesmesine cekilecek.
- Adapter secimi icin `Map<LogSourceType, LogSourceAdapter>` kurulumu test edilecek.
- `CollectorOrchestrator` kaynak basina thread, graceful shutdown ve enable/disable akislarini idempotent hale getirecek.
- Kafka producer ayarlari `acks=all`, `retries=3`, `enable.idempotence=true` olacak.

**Dosyalar:**

- `collector/src/main/java/com/loglens/collector/model/LogSource.java`
- `collector/src/main/java/com/loglens/collector/adapter/*.java`
- `collector/src/main/java/com/loglens/collector/config/CollectorProperties.java`
- `collector/src/main/java/com/loglens/collector/kafka/*.java`
- `collector/src/main/java/com/loglens/collector/repository/LogSourceRepository.java`
- `collector/src/main/java/com/loglens/collector/service/*.java`
- `collector/src/test/java/com/loglens/collector/**`

**Dogrulama:**

- `.\mvnw.cmd -q -pl collector -am test`
- Adapter unit testlerinde `stopStreaming` cagrisi acik kaynaklari kapatmali.
- `LogEventProducer` Kafka producer factory property testleri gecmeli.

**Tahmini sure:** 90 dakika

### Adim 6: Processor Parser, Grouping, Batch ve Realtime Pipeline

**Modul:** processor

**Bagimlilik:** Adim 5

**Ne yapilacak:**

- Parser zinciri `PatternRegistry` ile priority sirasina gore deterministik hale getirilecek.
- `SpringBootLogParser` eklenecek.
- `SeverityClassifier` STDERR metadata/label davranisini destekleyecek.
- `LogGroupingService` SHA-256 fingerprint ve eszamanli yazim icin kilitleme/upsert stratejisiyle guclendirilecek.
- `LogProcessorService` 100 mesaj veya 500ms batch politikasina gore ayrilacak; tekil process ve batch flush sorumluluklari ayrilacak.
- ERROR/CRITICAL loglar `log-errors` topic'ine, tum kayitlar Redis `log-realtime` kanalina yayinlanacak.
- Kafka DLQ error handler `log-raw-dlq` topic'ine yonlendirecek.

**Dosyalar:**

- `processor/src/main/java/com/loglens/processor/parser/*.java`
- `processor/src/main/java/com/loglens/processor/classifier/SeverityClassifier.java`
- `processor/src/main/java/com/loglens/processor/service/LogGroupingService.java`
- `processor/src/main/java/com/loglens/processor/service/LogProcessorService.java`
- `processor/src/main/java/com/loglens/processor/kafka/*.java`
- `processor/src/main/java/com/loglens/processor/config/KafkaConsumerConfig.java`
- `processor/src/main/java/com/loglens/processor/repository/*.java`
- `processor/src/test/java/com/loglens/processor/**`

**Dogrulama:**

- `.\mvnw.cmd -q -pl processor -am test`
- Parser unit testleri Java stack trace, Spring Boot, Nginx ve generic formatlari ayristirmali.
- Batch testinde 100 kayit veya 500ms flush tetiklenmeli.

**Tahmini sure:** 2 saat

### Adim 7: Analyzer Kurallari, Korelasyon ve AI Fallback Tasarimi

**Modul:** analyzer

**Bagimlilik:** Adim 6

**Ne yapilacak:**

- Eksik kurallar tamamlanacak: `StackOverflowRule`, `ClassNotFoundRule`; `DatabaseRule` adi spesifikasyondaki `DatabaseConnectionRule` ile uyumlu hale getirilecek.
- `RuleBasedEngine` priority ve fallback sozlesmesiyle test edilecek.
- `AiAnalysisEngine` config, timeout, JSON parse ve rule-based fallback ile izole edilecek.
- OpenAI entegrasyonu icin HTTP client sorumlulugu ayri bir gateway sinifina alinacak; API key yoksa engine unavailable olacak.
- `DeploymentCorrelator` 30 dakika ve 2 saat confidence kurallarini uygulayacak.
- `AnalyzerService` sonucu persist edip `log-analysis` topic'ine publish edecek.

**Dosyalar:**

- `analyzer/src/main/java/com/loglens/analyzer/engine/*.java`
- `analyzer/src/main/java/com/loglens/analyzer/rule/*.java`
- `analyzer/src/main/java/com/loglens/analyzer/correlation/DeploymentCorrelator.java`
- `analyzer/src/main/java/com/loglens/analyzer/service/AnalyzerService.java`
- `analyzer/src/main/java/com/loglens/analyzer/kafka/*.java`
- `analyzer/src/main/java/com/loglens/analyzer/model/*.java`
- `analyzer/src/main/java/com/loglens/analyzer/repository/*.java`
- `analyzer/src/test/java/com/loglens/analyzer/**`

**Dogrulama:**

- `.\mvnw.cmd -q -pl analyzer -am test`
- NullPointer, OOM, DB, timeout, stack overflow ve classpath kural testleri gecmeli.
- AI disabled iken uygulama OpenAI dependency'sine ihtiyac duymadan ayaga kalkmali.

**Tahmini sure:** 90 dakika

### Adim 8: API DTO, Mapper ve Query Servis Ayrimi

**Modul:** api

**Bagimlilik:** Adim 7

**Ne yapilacak:**

- Controller'lar sadece request/response donusumu yapacak sekilde inceltilecek.
- DTO mapper sorumluluklari servislerden ayrilacak.
- `LogController`, `SourceController`, `AnalysisController`, `DeploymentController` endpointleri spesifikasyonla birebir uyumlu olacak.
- `PagedResponse<T>` tum paged REST cevaplarinda kullanilacak.
- `GET /api/v1/logs/stats?windowMinutes=60` cache destekli hale getirilecek.
- `GET /api/v1/logs/{id}` analiz bilgisini kontrollu bir detay DTO'su ile dondurecek.

**Dosyalar:**

- `api/src/main/java/com/loglens/api/controller/*.java`
- `api/src/main/java/com/loglens/api/service/*.java`
- `api/src/main/java/com/loglens/api/dto/*.java`
- `api/src/main/java/com/loglens/api/mapper/*.java`
- `api/src/test/java/com/loglens/api/controller/**`

**Dogrulama:**

- `.\mvnw.cmd -q -pl api -am test`
- MockMvc testleri auth haric public/private endpoint sozlesmesini dogrulamali.

**Tahmini sure:** 2 saat

### Adim 9: Security, JWT, Auth ve Hata Sozlesmesi

**Modul:** api, common

**Bagimlilik:** Adim 8

**Ne yapilacak:**

- `JwtService` HS256 secret validation, token TTL, roles claim ve parse hatalarini guvenli sekilde ele alacak.
- `JwtAuthFilter` her istekte stateless authentication kuracak.
- `SecurityConfig` endpoint izinlerini spesifikasyona gore uygulayacak.
- `UserDetailsServiceImpl` veya mevcut repository tabanli servis netlestirilecek.
- `GlobalExceptionHandler` `LogLensException`, `ResourceNotFoundException`, validation, access denied ve generic hatalari standart envelope ile dondurecek.
- Password encoder BCrypt olacak ve seed admin kullanicisi migration ile gelecek.

**Dosyalar:**

- `api/src/main/java/com/loglens/api/security/*.java`
- `api/src/main/java/com/loglens/api/exception/GlobalExceptionHandler.java`
- `api/src/main/java/com/loglens/api/controller/AuthController.java`
- `api/src/main/java/com/loglens/api/model/UserAccount.java`
- `api/src/main/java/com/loglens/api/repository/UserAccountRepository.java`
- `api/src/test/java/com/loglens/api/security/**`

**Dogrulama:**

- `.\mvnw.cmd -q -pl api -am test`
- `curl -X POST http://localhost:8080/api/v1/auth/login ...` token dondurmeli.
- Token olmadan `/api/v1/logs` 401, token ile 200 donmeli.

**Tahmini sure:** 75 dakika

### Adim 10: WebSocket ve Redis Realtime Yayin Akisi

**Modul:** api, processor

**Bagimlilik:** Adim 9

**Ne yapilacak:**

- Processor tarafindan parse edilen loglar Redis `log-realtime` kanalina yayinlanacak.
- API tarafinda Redis listener lifecycle'i netlestirilecek.
- `LogWebSocketHandler` session yonetimi, kapali session temizligi ve broadcast hata izolasyonuyla guclendirilecek.
- Raw WebSocket `/ws/logs` endpointi auth'suz ama kaynak tuketimi acisindan kontrollu hale getirilecek.

**Dosyalar:**

- `api/src/main/java/com/loglens/api/ws/*.java`
- `api/src/main/java/com/loglens/api/redis/CacheService.java`
- `api/src/main/java/com/loglens/api/service/RealtimeLogConsumer.java`
- `api/src/main/java/com/loglens/api/config/RedisConfig.java`
- `processor/src/main/java/com/loglens/processor/service/LogProcessorService.java`

**Dogrulama:**

- `.\mvnw.cmd -q -pl api,processor -am test`
- `wscat -c ws://localhost:8080/ws/logs`
- Kafka'ya yeni log atildiginda WebSocket mesaj almali.

**Tahmini sure:** 60 dakika

### Adim 11: Test Altyapisi ve Modul Testleri

**Modul:** Tum moduller

**Bagimlilik:** Adim 10

**Ne yapilacak:**

- Unit testler: DTO/enums, parsers, rules, JWT, services.
- Slice testler: repository, controller, Kafka config.
- Integration test stratejisi: PostgreSQL, Kafka, Redis icin Testcontainers veya docker-compose tabanli profil.
- CI dostu test profili eklenecek.
- Test verisi ve object mother/helper siniflari tekrar etmeyen sekilde kurulacak.

**Dosyalar:**

- `common/src/test/java/**`
- `collector/src/test/java/**`
- `processor/src/test/java/**`
- `analyzer/src/test/java/**`
- `api/src/test/java/**`
- `api/src/test/resources/application-test.yml`
- Gerekirse `src/testFixtures` benzeri helper paketleri

**Dogrulama:**

- `.\mvnw.cmd test`
- Test coverage raporu icin gerekirse `.\mvnw.cmd verify`

**Tahmini sure:** 3 saat

### Adim 12: Uctan Uca Lokal Dogrulama

**Modul:** Tum sistem

**Bagimlilik:** Adim 11

**Ne yapilacak:**

- Docker servisleri temiz state ile baslatilacak.
- Uygulama package edilip `api` executable jar olarak calistirilacak.
- Health, login, source create, Kafka raw log, logs API, analyses API ve WebSocket akisinin tamamlanmasi saglanacak.
- E2E komutlari README'ye guncel sekilde yazilacak.

**Dosyalar:**

- `README.md`
- Gerekirse `scripts/e2e-smoke.ps1`
- Gerekirse `scripts/e2e-smoke.sh`

**Dogrulama:**

- `docker-compose up -d`
- `.\mvnw.cmd clean package -DskipTests`
- `java -jar api/target/api-1.0.0-SNAPSHOT.jar`
- `curl http://localhost:8080/actuator/health`
- Login ve authenticated endpoint curl'leri basarili olmali.
- Kafka test mesaji sonrasi `/api/v1/logs?severity=ERROR` ve `/api/v1/analyses` beklenen sonucu dondurmeli.

**Tahmini sure:** 90 dakika

### Adim 13: Operasyonel Kalite, Dokumantasyon ve Production Hardening

**Modul:** Tum sistem

**Bagimlilik:** Adim 12

**Ne yapilacak:**

- Structured logging, hassas veri maskeleme ve hata loglama politikalari gozden gecirilecek.
- Actuator `health`, `info`, `metrics` endpointleri dogrulanacak.
- CORS, JWT secret, AI key, DB/Kafka/Redis env degiskenleri dokumante edilecek.
- Graceful shutdown ve lifecycle davranislari dokumante edilecek.
- API endpoint sozlesmesi README'ye veya OpenAPI dokumanina eklenecek.
- Production icin eksik kalan bilincli trade-off'lar kaydedilecek.

**Dosyalar:**

- `README.md`
- `.env.example`
- Gerekirse `docs/architecture.md`
- Gerekirse `docs/api.md`
- Gerekirse `api/src/main/resources/application.yml`

**Dogrulama:**

- `.\mvnw.cmd clean verify`
- E2E smoke testi basarili olmali.
- README'deki komutlar temiz checkout uzerinde calisabilir olmali.

**Tahmini sure:** 60 dakika

## Mimari ve SOLID Ilkeleri

- Controller katmani yalnizca HTTP sozlesmesini yonetir; is kurallari service katmaninda kalir.
- Adapter, parser, analysis rule ve engine yapilari Open/Closed ilkesine uygun genisletilebilir olarak korunur.
- DTO, entity ve request/response modelleri birbirine karistirilmaz; mapper siniflari donusum sorumlulugunu tasir.
- Config degerleri hardcode edilmez; `@ConfigurationProperties` veya environment kaynakli property kullanilir.
- Public metodlara kisa ve amaca donuk Javadoc eklenir.
- Transaction sinirlari service katmaninda kurulur; read-only query metotlari `@Transactional(readOnly = true)` olur.
- Exception'lar yutulmaz; anlamli error code ile rethrow edilir veya yeterli context ile loglanir.
- Batch, Kafka, Redis ve WebSocket sorumluluklari ayri siniflarda tutulur; tek sinifta pipeline yogunlugu olusturulmaz.

## Mevcut Ilk Bulgular

- `.\mvnw.cmd -q -DskipTests package` basarili.
- Repo dizini Git repository olarak init edilmemis; `git status` calismiyor.
- `application.yml` ve `docker-compose.yml` portlari spesifikasyonla uyumsuz: uygulama `3000`, PostgreSQL `5435`, Redis `6385` kullanacak sekilde ayarlanmis.
- `common` modulunde `ApiResponse`, `LogGroupDto`, `AnalyzerException`, `ResourceNotFoundException` eksik gorunuyor.
- `Severity` seviyeleri spesifikasyondaki 0-5 yerine 0-50 araliginda ve `fromString(String)` yok.
- `LogEntryDto` alanlari spesifikasyondan sapmis: `sourceId` yok, `source/rawLine/labels/nodeName` gibi ek alanlar var.
- Processor tarafinda `SpringBootLogParser`, Redis publish ve gercek batch flush davranisi eksik gorunuyor.
- Analyzer tarafinda `StackOverflowRule` ve `ClassNotFoundRule` eksik; `DatabaseRule` adi sozlesmeyle uyumsuz.
- API katmaninda `ApiResponse` common yerine API modulunde duruyor; bu paylasimli sozlesme icin tekrar duzenlenmeli.

## Adim 1 Ciktisi: Envanter ve Kabul Kriterleri

### Mevcut Modul Envanteri

| Modul         | Durum        | Ana bulgu                                                                                                                                             |
| ------------- | ------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| `common`    | Kismen tamam | Temel DTO, enum ve exception'lar var; ortak API sozlesmesi ve bazi exception'lar eksik.                                                               |
| `collector` | Kismen tamam | Entity, adapter, producer ve orchestrator var; request DTO/mapper ayrimi, lifecycle ve adapter kaynak kapatma testleri eksik.                         |
| `processor` | Kismen tamam | Generic, Java stack trace ve Nginx parser var; Spring Boot parser, gercek batch flush, DLQ publish ve Redis realtime publish eksik.                   |
| `analyzer`  | Kismen tamam | Rule based engine ve bazi kurallar var; AI JSON sozlesmesi, fallback karari ve eksik kurallar tamamlanmali.                                           |
| `api`       | Kismen tamam | REST, JWT, Redis ve WebSocket iskeleti var; entity expose eden controller'lar, ortak response sozlesmesi ve endpoint parametre farklari duzeltilmeli. |
| `infra`     | Kismen tamam | Flyway ve docker-compose var; portlar, healthcheck'ler ve migration dosya adlari spesifikasyonla eslesmiyor.                                          |
| `tests`     | Eksik        | `src/test` klasoru bulunmuyor; test altyapisi sifirdan kurulacak.                                                                                   |

### Kritik Sozlesme Farklari

- `common/src/main/java/com/loglens/common/enums/Severity.java`: seviye degerleri `0,10,20,30,40,50`; hedef sozlesme `0,1,2,3,4,5`. `fromString(String)` eksik.
- `common/src/main/java/com/loglens/common/dto/LogEntryDto.java`: hedefte `sourceId` var; mevcut DTO'da `source`, `rawLine`, `labels`, `nodeName` gibi ek alanlar var ve sozlesme netlestirilmeli.
- `common/src/main/java/com/loglens/common/dto/AnalysisResultDto.java`: hedefte `logEntryId` ve `engineType` var; mevcut DTO'da eksik.
- `common/src/main/java/com/loglens/common/dto/LogGroupDto.java`: dosya yok; API su an `LogGroup` entity dondurebiliyor.
- `common/src/main/java/com/loglens/common/dto/ApiResponse.java`: common modulde yok; API modulunde local kopya var.
- `common/src/main/java/com/loglens/common/exception/AnalyzerException.java` ve `ResourceNotFoundException.java`: yok.
- `LogLensException` constructor sirasi mevcut kodda `(errorCode, message)`; spesifikasyon `(message, errorCode)`. Cagri noktalarinda kirilmadan tek sozlesmeye alinmali.
- `api/src/main/resources/application.yml`: `server.port=3000`, DB port `5435`, Redis port `6385`; hedef lokal E2E `8080`, `5432`, `6379`.
- `docker-compose.yml`: PostgreSQL ve Redis host portlari hedef sozlesmeden farkli; Kafka UI ve healthcheck'ler eksik.
- `api/src/main/resources/db/migration/V6__seed_admin_user.sql`: hedef dosya adi `V6__create_users.sql`; icerik kontrol edilip migration adlandirmasi kararli hale getirilmeli.
- `processor/src/main/java/com/loglens/processor/parser/SpringBootLogParser.java`: yok.
- `processor/src/main/java/com/loglens/processor/config/KafkaConsumerConfig.java`: DLQ publish eden `DeadLetterPublishingRecoverer` yerine sadece retry yapan `DefaultErrorHandler` var.
- `processor/src/main/java/com/loglens/processor/service/LogProcessorService.java`: `saveAll(List.of(entry))` tekil kayit yapiyor; 100 mesaj veya 500ms batch sozlesmesi yok.
- `processor` tarafinda Redis `log-realtime` publish akisi gorunmuyor.
- `processor/src/main/java/com/loglens/processor/repository/LogEntryRepository.java`: spesifikasyondaki pageable severity list/time range, source pageable ve native stats sorgulari eksik.
- `processor/src/main/java/com/loglens/processor/repository/LogGroupRepository.java`: `findTopBySeverityOrderByLastSeenAtDesc` imzasi hedef `findBySeverityOrderByLastSeenAtDesc` ile uyumsuz; `findAllByOrderByLastSeenAtDesc` eksik.
- `analyzer/src/main/java/com/loglens/analyzer/rule/StackOverflowRule.java` ve `ClassNotFoundRule.java`: yok.
- `analyzer/src/main/java/com/loglens/analyzer/rule/DatabaseRule.java`: hedef ad `DatabaseConnectionRule`; davranis da hedef metinlerle kontrol edilmeli.
- `analyzer/src/main/java/com/loglens/analyzer/engine/AiAnalysisEngine.java`: OpenAI cevabini hedef JSON schema'ya parse etmiyor; timeout ve confidence bazli rule fallback karari eksik.
- `analyzer/src/main/java/com/loglens/analyzer/service/AnalyzerService.java`: AI engine varsa dogrudan AI sonucunu seciyor; hedef rule ve AI analizinden confidence'i yuksek olani secmek.
- `api/src/main/java/com/loglens/api/controller/SourceController.java`: request/response DTO yerine `LogSource` entity kabul edip donduruyor.
- `api/src/main/java/com/loglens/api/controller/DeploymentController.java`: request/response DTO yerine `Deployment` entity kabul edip donduruyor.
- `api/src/main/java/com/loglens/api/controller/LogController.java`: `/groups` severity filtresi yok; `/stats` hedef `windowMinutes` yerine `sourceId` aliyor.
- `api/src/main/java/com/loglens/api/exception/GlobalExceptionHandler.java`: `AccessDeniedException -> 403` handler yok; `ResourceNotFoundException` hedef kodu uygulanmiyor.
- `api/src/main/java/com/loglens/api/config/KafkaTopicConfig.java`: topic bean'leri mevcut olsa da partition/replication sozlesmesi ayrica dogrulanmali.
- Test klasorleri yok; unit, slice ve integration testler eklenmeden production-ready kabul edilmeyecek.

### Uygulama Oncelikleri

1. Once sozlesme kiriklari giderilecek: common DTO/enum/exception, config ve migration uyumu.
2. Sonra pipeline davranisi tamamlanacak: collector lifecycle, processor batch/DLQ/Redis, analyzer rule/AI/fallback.
3. API katmaninda entity exposure kaldirilacak, mapper ve response DTO'lari kurulacak.
4. En son test, E2E smoke ve operasyonel dokumantasyon tamamlanacak.

### Adim 1 Kabul Kriterleri

- [X] Mevcut modul ve dosya envanteri cikarildi.
- [X] Spesifikasyona gore kritik farklar plan dosyasina islendi.
- [X] Sonraki adimlar icin uygulanabilir oncelik sirasi belirlendi.
- [X] Baseline derleme dogrulandi: `.\mvnw.cmd -q -DskipTests package`.

## Adim 2 Ciktisi: Build Standardizasyonu

### Yapilan Degisiklikler

- Parent `pom.xml` icinde Java 21 release ve UTF-8 encoding property'leri netlestirildi:
  - `maven.compiler.release`
  - `project.build.sourceEncoding`
  - `project.reporting.outputEncoding`
- Test altyapisi icin modullere `spring-boot-starter-test` eklendi.
- Kafka kullanan modullere `spring-kafka-test` eklendi:
  - `collector`
  - `processor`
  - `analyzer`
  - `api`
- Security testleri icin `api` modulune `spring-security-test` eklendi.
- `spring-boot-maven-plugin` yalnizca `api/pom.xml` icinde kalacak sekilde dogrulandi.
- Modul packaging yapisi korundu: root `pom`, diger moduller `jar`.

### Degisen Dosyalar

- `pom.xml`
- `common/pom.xml`
- `collector/pom.xml`
- `processor/pom.xml`
- `analyzer/pom.xml`
- `api/pom.xml`

### Dogrulama Sonuclari

- [X] `.\mvnw.cmd -q clean package -DskipTests` basarili.
- [X] `.\mvnw.cmd -q -pl common,collector,processor,analyzer,api -am test` basarili.
- [X] `spring-boot-maven-plugin` sadece `api/pom.xml` icinde bulundu.

### Kalan Notlar

- Test dosyalari henuz yok; test dependency'leri Adim 11'de yazilacak unit, slice ve integration testler icin hazirlandi.
- Testcontainers bu adimda eklenmedi; DB/Kafka/Redis integration stratejisi Adim 11'de netlestirilip yalnizca ihtiyac duyulan modullere eklenecek.

## Adim 3 Ciktisi: Common Sozlesme Tamamlama

### Yapilan Degisiklikler

- `Severity` seviyeleri hedef sozlesmeye cekildi: `TRACE(0), DEBUG(1), INFO(2), WARN(3), ERROR(4), CRITICAL(5)`.
- `Severity.fromString(String)` eklendi; case-insensitive parse yapiyor, blank/null degerleri `INFO` olarak ele aliyor.
- `LogEntryDto` hedef API/Kafka sonrasi cikis sozlesmesine gore duzenlendi:
  - `sourceId` eklendi.
  - `source`, `rawLine`, `labels`, `nodeName` gibi hedef disi response alanlari kaldirildi.
  - `parsedMessage`, `stackTrace`, pod/container/service/cluster/timestamp alanlari korundu.
- `AnalysisResultDto` hedef sozlesmeye tamamlandi:
  - `logEntryId` eklendi.
  - `engineType` eklendi.
- `ApiResponse<T>` common modulune tasindi.
- `LogGroupDto` common modulune eklendi.
- `AnalyzerException` ve `ResourceNotFoundException` eklendi.
- `LogLensException`, `CollectorException` ve `ProcessorException` constructor sozlesmesi `(message, errorCode)` sirasina alindi.
- API controller importlari common `ApiResponse` kullanacak sekilde guncellendi.
- Collector exception cagri noktalari yeni constructor sirasina gore duzeltildi.
- Processor/API/Analyzer DTO builder kullanimlari yeni DTO alanlariyla uyumlu hale getirildi.
- `LogEntry` entity'sine migration'da zaten bulunan `parsedMessage` alani eklendi ve processor mapping'e baglandi.
- Common modul icin hedefli unit testler eklendi.

### Degisen Dosyalar

- `common/src/main/java/com/loglens/common/enums/Severity.java`
- `common/src/main/java/com/loglens/common/dto/ApiResponse.java`
- `common/src/main/java/com/loglens/common/dto/LogEntryDto.java`
- `common/src/main/java/com/loglens/common/dto/LogGroupDto.java`
- `common/src/main/java/com/loglens/common/dto/AnalysisResultDto.java`
- `common/src/main/java/com/loglens/common/exception/LogLensException.java`
- `common/src/main/java/com/loglens/common/exception/CollectorException.java`
- `common/src/main/java/com/loglens/common/exception/ProcessorException.java`
- `common/src/main/java/com/loglens/common/exception/AnalyzerException.java`
- `common/src/main/java/com/loglens/common/exception/ResourceNotFoundException.java`
- `common/src/test/java/com/loglens/common/enums/SeverityTest.java`
- `common/src/test/java/com/loglens/common/exception/LogLensExceptionTest.java`
- `common/src/test/java/com/loglens/common/dto/CommonDtoContractTest.java`
- `api/src/main/java/com/loglens/api/controller/AnalysisController.java`
- `api/src/main/java/com/loglens/api/controller/AuthController.java`
- `api/src/main/java/com/loglens/api/controller/DeploymentController.java`
- `api/src/main/java/com/loglens/api/controller/LogController.java`
- `api/src/main/java/com/loglens/api/controller/SourceController.java`
- `api/src/main/java/com/loglens/api/dto/ApiResponse.java` silindi.
- `collector/src/main/java/com/loglens/collector/adapter/DockerAdapter.java`
- `collector/src/main/java/com/loglens/collector/adapter/KubernetesAdapter.java`
- `collector/src/main/java/com/loglens/collector/adapter/SshAdapter.java`
- `processor/src/main/java/com/loglens/processor/model/LogEntry.java`
- `processor/src/main/java/com/loglens/processor/service/LogProcessorService.java`
- `api/src/main/java/com/loglens/api/service/LogQueryService.java`
- `analyzer/src/main/java/com/loglens/analyzer/service/AnalyzerService.java`

### Dogrulama Sonuclari

- [X] `.\mvnw.cmd -q -pl common test` basarili.
- [X] `.\mvnw.cmd -q -DskipTests package` basarili.
- [X] `.\mvnw.cmd -q test` basarili.

### Kalan Notlar

- API ve service katmanlari hala bazi yerlerde entity donduruyor; bu Adim 8 kapsaminda DTO/mapper ayrimi ile temizlenecek.
- `ResourceNotFoundException` eklendi ancak tum `EntityNotFoundException` kullanimi henuz bu domain exception'a tasinmadi; bu Adim 8-9 kapsaminda ele alinacak.

## Adim 4 Ciktisi: Infra, Config ve Migration Uyumu

### Yapilan Degisiklikler

- `application.yml` hedef lokal E2E sozlesmesine cekildi:
  - API portu `8080`.
  - PostgreSQL default portu `5432`.
  - Redis default portu `6379`.
  - Hikari `connection-timeout: 30000`.
  - Flyway `baseline-on-migrate: true`.
  - Actuator exposure `health,info,metrics`.
  - JWT default secret hedef metinle uyumlu hale getirildi.
  - Collector config'inden hedefte olmayan `kubeconfig-path` kaldirildi.
- `docker-compose.yml` hedef servis setine uyarlandi:
  - PostgreSQL `5432:5432`.
  - Redis `6379:6379`.
  - ZooKeeper `2181:2181`.
  - Kafka `9092:9092`.
  - Kafka UI `8090:8080`.
  - PostgreSQL, Redis ve Kafka healthcheck'leri eklendi.
  - Kafka log retention ayari eklendi.
- Flyway migration'lari hedef SQL sozlesmesine cekildi:
  - `V2` source index ve `ON DELETE SET NULL`.
  - `V3` `parsed_message` kolonu ve FK delete davranislari.
  - `V4` `notes` kolonu ve service index.
  - `V5` cascade FK ve analysis indexleri.
  - `V6__seed_admin_user.sql` yerine `V6__create_users.sql`.
- `Deployment` entity'sine `notes` alani eklendi.
- `.env.example` eklendi.

### Degisen Dosyalar

- `api/src/main/resources/application.yml`
- `docker-compose.yml`
- `api/src/main/resources/db/migration/V1__create_log_sources.sql`
- `api/src/main/resources/db/migration/V2__create_log_groups.sql`
- `api/src/main/resources/db/migration/V3__create_log_entries.sql`
- `api/src/main/resources/db/migration/V4__create_deployments.sql`
- `api/src/main/resources/db/migration/V5__create_analyses.sql`
- `api/src/main/resources/db/migration/V6__create_users.sql`
- `api/src/main/resources/db/migration/V6__seed_admin_user.sql` silindi.
- `analyzer/src/main/java/com/loglens/analyzer/model/Deployment.java`
- `.env.example`

### Dogrulama Sonuclari

- [X] `.\mvnw.cmd -q clean package -DskipTests` basarili.
- [X] `docker compose config` basarili.
- [X] `docker compose up -d` servisleri baslatti; PostgreSQL, Redis ve Kafka healthcheck'leri basarili hale geldi.
- [X] Temiz PostgreSQL uzerinde API baslatildi ve `GET /actuator/health` sonucu `{"status":"UP"}` olarak dogrulandi.
- [X] `.\mvnw.cmd -q test` basarili.

### Ortam Notlari

- Makinede native bir PostgreSQL process'i de `5432` dinledigi icin default `localhost:5432` health check'i once native servise gidip parola hatasi verdi.
- Docker Postgres dogrulamasi icin ayrica temiz test portu `15432` kullanildi ve API datasource URL'i command-line arg ile bu porta yonlendirildi.
- Silinen `V6__seed_admin_user.sql` eski `target/classes` icinde kaldigi icin ilk jar denemesinde duplicate Flyway version goruldu; `clean package` bunu duzeltti.
- Docker Compose modern CLI `version` alanini obsolete olarak uyariyor. Spesifikasyon `version: '3.9'` istedigi icin dosyada korundu.

## Adim 5 Ciktisi: Collector Stabilizasyonu

### Yapilan Degisiklikler

- `LogSourceRepository` hedef sozlesmeye tamamlandi:
  - `findByEnabledTrue()`
  - `findByType(LogSourceType)`
- Collector create/update request sinirlari eklendi:
  - `LogSourceCreateRequest`
  - `LogSourceUpdateRequest`
  - `LogSourceMapper`
- `LogSourceService` sozlesmesi genisletildi:
  - `findAll()`
  - `findById(Long)`
  - `findByType(LogSourceType)`
  - request tabanli `create(...)` ve `update(...)`
  - mevcut entity tabanli metotlar API uyumlulugu icin korundu.
- `LogSource` entity default `enabled=true` olacak sekilde guclendirildi.
- `KubernetesAdapter` config parsing hedef anahtari `kubeconfigPath` destekleyecek hale getirildi; eski `kubeconfig` anahtari geriye uyumluluk icin korunuyor.
- Kubernetes event label'larina `sourceId` eklendi; `containerName` config'ten tasinabilir hale geldi.
- `SshAdapter` hedef config anahtarlarini destekleyecek hale getirildi:
  - `username`
  - `privateKeyPath`
  - `logFilePath`
  - eski `user`, `privateKey`, `path` anahtarlari geriye uyumluluk icin korunuyor.
- `SshAdapter.stopStreaming` artik aktif channel/session kaynaklarini disconnect ediyor.
- `DockerAdapter` hedef `dockerHost` config anahtarini destekleyecek hale getirildi.
- `DockerAdapter.stopStreaming` artik aktif log stream callback'ini kapatiyor.
- `LogEventProducer.send(LogEventDto)` hedefe uygun olarak `CompletableFuture<SendResult<String, LogEventDto>>` donduruyor.
- Kafka producer key secimi hedefe yaklastirildi:
  - once `podName`
  - sonra `labels.sourceId`
  - en son `event.id`
- `CollectorOrchestrator` tamamlanmis/cancel edilmis task'lari temizleyip kaynagi tekrar baslatabilecek sekilde idempotent hale getirildi.
- Collector thread isimleri ayirt edilebilir hale getirildi.
- Kafka producer reliable/idempotent config'i unit test ile sabitlendi.

### Degisen Dosyalar

- `collector/src/main/java/com/loglens/collector/repository/LogSourceRepository.java`
- `collector/src/main/java/com/loglens/collector/dto/LogSourceCreateRequest.java`
- `collector/src/main/java/com/loglens/collector/dto/LogSourceUpdateRequest.java`
- `collector/src/main/java/com/loglens/collector/mapper/LogSourceMapper.java`
- `collector/src/main/java/com/loglens/collector/model/LogSource.java`
- `collector/src/main/java/com/loglens/collector/service/LogSourceService.java`
- `collector/src/main/java/com/loglens/collector/service/CollectorOrchestrator.java`
- `collector/src/main/java/com/loglens/collector/adapter/KubernetesAdapter.java`
- `collector/src/main/java/com/loglens/collector/adapter/SshAdapter.java`
- `collector/src/main/java/com/loglens/collector/adapter/DockerAdapter.java`
- `collector/src/main/java/com/loglens/collector/kafka/LogEventProducer.java`
- `collector/src/test/java/com/loglens/collector/kafka/KafkaProducerConfigTest.java`
- `collector/src/test/java/com/loglens/collector/kafka/LogEventProducerTest.java`
- `collector/src/test/java/com/loglens/collector/mapper/LogSourceMapperTest.java`

### Dogrulama Sonuclari

- [X] `.\mvnw.cmd -q -pl collector -am test` basarili.
- [X] `.\mvnw.cmd -q clean package -DskipTests` basarili.
- [X] `.\mvnw.cmd -q test` basarili.

### Kalan Notlar

- `KubernetesAdapter` halen mevcut implementation davranisini koruyarak periyodik pod log okuma yapiyor; gercek `watchLog` tabanli streaming daha derin runtime testi gerektirdigi icin bu adimda lifecycle/config stabilizasyonu ile sinirli tutuldu.
- API controller'lari request DTO'larina henuz tasinmadi; bu Adim 8'de entity exposure temizlenirken yapilacak.

## Adim 6 Ciktisi: Processor Pipeline Tamamlama

### Yapilan Degisiklikler

- `LogParser` sozlesmesine `getPriority()` eklendi.
- `PatternRegistry` parser bean'lerini priority sirasina gore deterministik sekilde siralayacak hale getirildi.
- `SpringBootLogParser` eklendi:
  - Spring Boot timestamp/severity/thread/logger/message formatini parse ediyor.
  - priority `15`.
- Mevcut parser priority'leri hedef sozlesmeye uyarlandi:
  - `JavaStackTraceParser`: `10`
  - `NginxLogParser`: `20`
  - `GenericLogParser`: `999`
- `NginxLogParser` metadata kapsamı genisletildi:
  - `ip`
  - `method`
  - `path`
  - `status`
- `SeverityClassifier` STDERR stream icin minimum `WARN` kuralini destekliyor.
- `LogGroupingService` fingerprint sozlesmesi guncellendi:
  - stack trace varsa `SHA-256(stackTrace.trim())`
  - stack trace yoksa `SHA-256(severity + ":" + first 200 chars)`
- `LogGroupRepository` pessimistic write lock kullanan fingerprint sorgusu ile eszamanli update riskini azaltiyor.
- `LogEntryRepository` hedef sorgularla tamamlandi:
  - `findBySeverityInAndTimestampBetween(...)`
  - `findBySourceId(...)`
  - `countBySeverityGroupSince(...)`
- Processor Redis realtime publisher eklendi:
  - `RealtimeLogPublisher`
  - channel: `log-realtime`
- `LogProcessorService` batch akisa cekildi:
  - `processBatch(List<LogEventDto>)`
  - batch icindeki entry'ler `saveAll()` ile persist ediliyor.
  - ERROR/CRITICAL kayitlar `log-errors` topic'ine gonderiliyor.
  - Tum kayitlar Redis `log-realtime` kanalina publish ediliyor.
- Kafka consumer batch listener olarak ayarlandi:
  - `max.poll.records=100`
  - `fetch.max.wait.ms=500`
  - concurrency `3`
- DLQ davranisi listener icindeki manuel try/catch'ten Spring Kafka `DefaultErrorHandler + DeadLetterPublishingRecoverer` yapisina tasindi.
- Failed raw log record'lari `log-raw-dlq` topic partition `0` hedefiyle publish ediliyor.
- Processor icin parser, classifier ve fingerprint unit testleri eklendi.

### Degisen Dosyalar

- `processor/pom.xml`
- `processor/src/main/java/com/loglens/processor/parser/LogParser.java`
- `processor/src/main/java/com/loglens/processor/parser/PatternRegistry.java`
- `processor/src/main/java/com/loglens/processor/parser/JavaStackTraceParser.java`
- `processor/src/main/java/com/loglens/processor/parser/SpringBootLogParser.java`
- `processor/src/main/java/com/loglens/processor/parser/NginxLogParser.java`
- `processor/src/main/java/com/loglens/processor/parser/GenericLogParser.java`
- `processor/src/main/java/com/loglens/processor/classifier/SeverityClassifier.java`
- `processor/src/main/java/com/loglens/processor/repository/LogEntryRepository.java`
- `processor/src/main/java/com/loglens/processor/repository/LogGroupRepository.java`
- `processor/src/main/java/com/loglens/processor/service/LogGroupingService.java`
- `processor/src/main/java/com/loglens/processor/service/LogProcessorService.java`
- `processor/src/main/java/com/loglens/processor/realtime/RealtimeLogPublisher.java`
- `processor/src/main/java/com/loglens/processor/config/KafkaConsumerConfig.java`
- `processor/src/main/java/com/loglens/processor/kafka/LogEventConsumer.java`
- `collector/src/main/java/com/loglens/collector/kafka/LogEventProducer.java`
- `processor/src/test/java/com/loglens/processor/parser/ParserContractTest.java`
- `processor/src/test/java/com/loglens/processor/classifier/SeverityClassifierTest.java`
- `processor/src/test/java/com/loglens/processor/service/LogGroupingServiceTest.java`

### Dogrulama Sonuclari

- [X] `.\mvnw.cmd -q -pl processor -am test` basarili.
- [X] `.\mvnw.cmd -q clean package -DskipTests` basarili.
- [X] `.\mvnw.cmd -q test` basarili.

### Kalan Notlar

- Batch flush davranisi uygulama icinde ayrica timer buffer ile degil, Kafka consumer batch ayarlariyla saglandi: broker/consumer `100` kayit veya `500ms` bekleme sinirlarina gore batch teslim ediyor, servis de gelen batch'i `saveAll()` ile yaziyor.
- Redis publish processor modulune alindi; API tarafindaki Redis subscriber/WebSocket akisi Adim 10'da ayrica guclendirilecek.

## Adim 7 Ciktisi: Analyzer Kurallari ve Korelasyon

### Yapilan Degisiklikler

- `AnalysisEngine` sozlesmesine `isAvailable()` eklendi.
- `AnalysisResult` hedef alanlari destekleyecek sekilde genisletildi:
  - `matchedRule`
  - correlation sonrasi confidence/deployment kopyalama metodu
  - unknown fallback confidence `0.1`
- `AnalysisRule` sozlesmesine `getRuleName()` eklendi.
- `RuleBasedEngine` `isAvailable()` implementasyonu ile tamamlandi.
- Rule mesajlari ve confidence degerleri hedef sozlesmeye cekildi.
- Eksik rule'lar eklendi:
  - `StackOverflowRule` priority `15`
  - `ClassNotFoundRule` priority `35`
- `DatabaseRule` hedef ad ve davranisa cekildi:
  - `DatabaseConnectionRule` priority `25`
- Mevcut rule'lar guncellendi:
  - `NullPointerRule` priority `10`, confidence `0.9`
  - `OutOfMemoryRule` priority `20`, confidence `0.95`
  - `ConnectionTimeoutRule` priority `30`, confidence `0.85`
- `AiAnalysisEngine` guncellendi:
  - `@ConditionalOnProperty` davranisi korundu.
  - API key yoksa unavailable.
  - HTTP client/request timeout `10s`.
  - OpenAI response content'i JSON olarak parse ediliyor: `rootCause`, `suggestion`, `confidenceScore`.
  - Hata durumunda unknown result dondurerek service tarafinda rule-based sonuca fallback sagliyor.
- `AnalyzerService` hedef akisa cekildi:
  - once rule-based analiz calisir.
  - AI enabled ve available ise AI analizi de calisir.
  - confidence'i yuksek olan sonuc secilir.
  - deployment korelasyonu secilen sonuca uygulanir.
  - sonuc DB'ye persist edilir ve `log-analysis` topic'ine publish edilir.
- `DeploymentCorrelator` hedef korelasyon kurallarini uygular:
  - 30 dakika veya daha az: deployment set edilir, confidence penalty yok.
  - 30 dakika ile 2 saat arasi: deployment set edilir, confidence `0.3` azaltilir.
  - 2 saatten fazla: korelasyon yok.
- Analyzer repository sozlesmeleri tamamlandi:
  - `AnalysisRepository.findByLogGroupId(...)`
  - `AnalysisRepository.findByAnalyzedAtAfter(...)`
  - `DeploymentRepository.findFirstByDeployedAtBeforeOrderByDeployedAtDesc(...)`
  - `DeploymentRepository.findAllByOrderByDeployedAtDesc(...)`
- Analyzer unit testleri eklendi.

### Degisen Dosyalar

- `analyzer/src/main/java/com/loglens/analyzer/engine/AnalysisEngine.java`
- `analyzer/src/main/java/com/loglens/analyzer/engine/AnalysisResult.java`
- `analyzer/src/main/java/com/loglens/analyzer/engine/RuleBasedEngine.java`
- `analyzer/src/main/java/com/loglens/analyzer/engine/AiAnalysisEngine.java`
- `analyzer/src/main/java/com/loglens/analyzer/rule/AnalysisRule.java`
- `analyzer/src/main/java/com/loglens/analyzer/rule/NullPointerRule.java`
- `analyzer/src/main/java/com/loglens/analyzer/rule/OutOfMemoryRule.java`
- `analyzer/src/main/java/com/loglens/analyzer/rule/ConnectionTimeoutRule.java`
- `analyzer/src/main/java/com/loglens/analyzer/rule/DatabaseConnectionRule.java`
- `analyzer/src/main/java/com/loglens/analyzer/rule/StackOverflowRule.java`
- `analyzer/src/main/java/com/loglens/analyzer/rule/ClassNotFoundRule.java`
- `analyzer/src/main/java/com/loglens/analyzer/rule/DatabaseRule.java` silindi.
- `analyzer/src/main/java/com/loglens/analyzer/correlation/DeploymentCorrelator.java`
- `analyzer/src/main/java/com/loglens/analyzer/service/AnalyzerService.java`
- `analyzer/src/main/java/com/loglens/analyzer/repository/AnalysisRepository.java`
- `analyzer/src/main/java/com/loglens/analyzer/repository/DeploymentRepository.java`
- `analyzer/src/test/java/com/loglens/analyzer/engine/RuleBasedEngineTest.java`
- `analyzer/src/test/java/com/loglens/analyzer/engine/AiAnalysisEngineTest.java`
- `analyzer/src/test/java/com/loglens/analyzer/correlation/DeploymentCorrelatorTest.java`

### Dogrulama Sonuclari

- [X] `.\mvnw.cmd -q -pl analyzer -am test` basarili.
- [X] `.\mvnw.cmd -q clean package -DskipTests` basarili.
- [X] `.\mvnw.cmd -q test` basarili.

### Kalan Notlar

- AI entegrasyonu network cagrisini unit testte tetiklemiyor; `isAvailable()` ve JSON parse yolu kod seviyesinde hazir. Gercek OpenAI cagri testi environment/API key gerektirdigi icin E2E/ops kapsaminda ele alinacak.
- API tarafinda analysis/deployment response'lari hala entity dondugu yerler iceriyor; bu Adim 8 DTO/mapper ayriminda temizlenecek.

## Adim 8 Ciktisi: API DTO ve Mapper Ayrimi

### Yapilan Degisiklikler

- API response entity exposure azaltildi; controller'lar artik DTO/request sinirlariyla calisiyor.
- Yeni API DTO'lari eklendi:
  - `LogDetailDto`
  - `LogSourceDto`
  - `DeploymentDto`
- `ApiDtoMapper` eklendi:
  - `LogEntry -> LogEntryDto`
  - `LogGroup -> LogGroupDto`
  - `Analysis -> AnalysisResultDto`
  - `LogSource -> LogSourceDto`
  - `Deployment <-> DeploymentDto`
- `LogQueryService` mapper kullanacak sekilde guncellendi:
  - `GET /api/v1/logs` DTO page donduruyor.
  - `GET /api/v1/logs/{id}` `LogDetailDto` donduruyor.
  - `GET /api/v1/logs/groups` `PagedResponse<LogGroupDto>` donduruyor ve severity filtresini destekliyor.
  - `GET /api/v1/logs/groups/{groupId}/entries` DTO page donduruyor.
  - `GET /api/v1/logs/stats?windowMinutes=60` window bazli stats donduruyor ve `total` hesapliyor.
- `CacheService` hedef key pattern'i icin window bazli stats cache metodu destekliyor:
  - `loglens:stats:{windowMinutes}`
- `AnalysisQueryService` artik `Analysis` entity yerine `AnalysisResultDto` donduruyor.
- `DeploymentService` artik `DeploymentDto` ve `PagedResponse<DeploymentDto>` ile calisiyor.
- `SourceController` artik entity request kabul etmiyor:
  - create: `LogSourceCreateRequest`
  - update: `LogSourceUpdateRequest`
  - response: `LogSourceDto`
- `SourceController.DELETE /api/v1/sources/{id}` artik `204 No Content` donduruyor.
- `POST /api/v1/sources/{id}/test` response data alani `{ "connected": true/false }` seklinde.
- `DeploymentController` artik `DeploymentDto` kabul edip donduruyor.
- API mapper ve stats davranisi icin unit testler eklendi.

### Degisen Dosyalar

- `api/src/main/java/com/loglens/api/dto/LogDetailDto.java`
- `api/src/main/java/com/loglens/api/dto/LogSourceDto.java`
- `api/src/main/java/com/loglens/api/dto/DeploymentDto.java`
- `api/src/main/java/com/loglens/api/mapper/ApiDtoMapper.java`
- `api/src/main/java/com/loglens/api/service/LogQueryService.java`
- `api/src/main/java/com/loglens/api/service/AnalysisQueryService.java`
- `api/src/main/java/com/loglens/api/service/DeploymentService.java`
- `api/src/main/java/com/loglens/api/redis/CacheService.java`
- `api/src/main/java/com/loglens/api/controller/LogController.java`
- `api/src/main/java/com/loglens/api/controller/SourceController.java`
- `api/src/main/java/com/loglens/api/controller/DeploymentController.java`
- `api/src/test/java/com/loglens/api/mapper/ApiDtoMapperTest.java`
- `api/src/test/java/com/loglens/api/service/LogQueryServiceTest.java`

### Dogrulama Sonuclari

- [X] `.\mvnw.cmd -q -pl api -am test` basarili.
- [X] `.\mvnw.cmd -q clean package -DskipTests` basarili.
- [X] `.\mvnw.cmd -q test` basarili.

### Kalan Notlar

- Auth/security hata sozlesmesi ve `ResourceNotFoundException` kullaniminin tum service katmanina yayilmasi Adim 9 kapsaminda ele alinacak.
- WebSocket/Redis subscriber lifecycle guclendirmesi Adim 10 kapsaminda kalmaya devam ediyor.

## Adim 9 Ciktisi: Security, JWT ve Hata Sozlesmesi

### Yapilan Degisiklikler

- `JwtService` hedef public API ile tamamlandi:
  - `generateToken(String username, List<String> roles)`
  - `validateToken(String token)`
  - `extractUsername(String token)`
  - `parse(String token)` mevcut ic kullanim icin korundu.
- `JwtAuthFilter` token validation akisini guclendirdi:
  - invalid token durumunda context temizleniyor.
  - role claim'leri `ROLE_` prefix'i tekrar eklenmeden authority'ye cevriliyor.
  - exception yutulsa bile debug seviyesinde iz birakiliyor.
- `UserDetailsServiceImpl` eklendi:
  - `users` tablosundan username ile user yukluyor.
  - role bilgisini Spring Security authority'ye ceviriyor.
- Auth is mantigi controller'dan `AuthService` sinifina tasindi:
  - login
  - refresh
  - seeded admin fallback davranisi korundu.
- `AuthController` sadece request/response donusumu yapacak sekilde inceltildi.
- `SecurityConfig` stateless JWT sozlesmesiyle guclendirildi:
  - `/api/v1/auth/**`, `/ws/**`, `/actuator/health` permitAll.
  - diger tum endpointler authenticated.
  - CSRF disabled.
  - session stateless.
  - `JwtAuthFilter`, `UsernamePasswordAuthenticationFilter` oncesinde.
  - 401/403 durumlari JSON error envelope ile donuyor.
- `GlobalExceptionHandler` hedef mapping'lere yaklastirildi:
  - `ResourceNotFoundException -> 404`
  - `LogLensException -> 400`
  - validation -> 422
  - `AccessDeniedException -> 403`
  - `BadCredentialsException -> 401`
  - fallback exception -> 500
- Service katmaninda domain not-found kullanimi yayginlastirildi:
  - `LogSourceService`
  - `AnalyzerService`
  - `AnalysisQueryService`
  - `LogQueryService`
- JWT/Auth/Error handler davranisi icin unit testler eklendi.

### Degisen Dosyalar

- `api/src/main/java/com/loglens/api/security/JwtService.java`
- `api/src/main/java/com/loglens/api/security/JwtAuthFilter.java`
- `api/src/main/java/com/loglens/api/security/SecurityConfig.java`
- `api/src/main/java/com/loglens/api/security/UserDetailsServiceImpl.java`
- `api/src/main/java/com/loglens/api/service/AuthService.java`
- `api/src/main/java/com/loglens/api/controller/AuthController.java`
- `api/src/main/java/com/loglens/api/exception/GlobalExceptionHandler.java`
- `collector/src/main/java/com/loglens/collector/service/LogSourceService.java`
- `analyzer/src/main/java/com/loglens/analyzer/service/AnalyzerService.java`
- `api/src/main/java/com/loglens/api/service/AnalysisQueryService.java`
- `api/src/main/java/com/loglens/api/service/LogQueryService.java`
- `api/src/test/java/com/loglens/api/security/JwtServiceTest.java`
- `api/src/test/java/com/loglens/api/service/AuthServiceTest.java`
- `api/src/test/java/com/loglens/api/exception/GlobalExceptionHandlerTest.java`

### Dogrulama Sonuclari

- [X] `.\mvnw.cmd -q -pl api -am test` basarili.
- [X] `.\mvnw.cmd -q clean package -DskipTests` basarili.
- [X] `.\mvnw.cmd -q test` basarili.

### Kalan Notlar

- Gercek HTTP seviyesinde 401/403 ve login smoke testleri Adim 12 E2E dogrulamasinda tekrar calistirilacak.
- WebSocket endpointi spesifikasyon geregi auth'suz kalmaya devam ediyor; session/resource kontrolleri Adim 10 kapsaminda guclendirilecek.

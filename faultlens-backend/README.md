# LogLens Backend

Java 21, Spring Boot 3.3, Kafka, PostgreSQL, Redis ve Flyway kullanan multi-module Maven backend.

## Local

```bash
docker-compose up -d
./mvnw clean package -DskipTests
java -jar api/target/api-1.0.0-SNAPSHOT.jar
```

Varsayılan kullanıcı: `admin` / `admin123`.

# FaultLens Frontend 🎨

FaultLens platformunun kullanıcı arayüzü (UI) projesidir. Kullanıcıların log kaynaklarını yönettiği, canlı log akışlarını izlediği ve yapay zeka analizlerini görüntülediği web uygulamasıdır.

## 🛠️ Teknolojiler

- **Framework:** Next.js (App Router)
- **Kütüphane:** React
- **Dil:** TypeScript
- **Stil & Tasarım:** TailwindCSS, Framer Motion (Animasyonlar), Lucide Icons
- **Veri Yönetimi:** React Query (@tanstack/react-query), Zustand (Global State)
- **HTTP İstemcisi:** Axios
- **WebSocket:** Socket.io-client (veya yerleşik WebSocket API)

## 📂 Proje Yapısı

Proje, **Feature-Sliced Design (FSD)** mimarisinden esinlenerek modüler bir yapıda kurgulanmıştır:

- `app/`: Next.js yönlendirmeleri (Routing) ve ana sayfa düzenleri.
- `features/`: Uygulamanın temel iş özellikleri (`logs`, `sources`, `analyses`). Her özelliğin kendi bileşenleri ve API servisleri vardır.
- `shared/`: Tüm uygulama genelinde paylaşılan bileşenler (UI elemanları, Button, Modal, Sidebar), hook'lar, tipler ve yardımcı fonksiyonlar.

## ⚙️ Kurulum ve Çalıştırma

### Bağımlılıkları Yükleme

```bash
pnpm install
```

`pnpm install` ayrıca git pre-commit hook'unu kurar (husky): commit'lenen dosyalarda `eslint --fix` + `prettier --write` çalışır, ESLint hatası commit'i durdurur.

### Ortam Değişkenleri (Environment Variables)

Proje kök dizininde bir `.env.local` dosyası oluşturun ve backend API adresinizi belirtin:

```env
NEXT_PUBLIC_API_URL=http://localhost:3500/api/v1
NEXT_PUBLIC_WS_URL=ws://localhost:3500/ws
```

### Geliştirme Sunucusunu Başlatma

```bash
pnpm dev
```

Uygulama **http://localhost:3000** adresinde çalışmaya başlayacaktır.

### Test ve Kod Kalitesi

| Komut                               | Ne yapar                                                |
| ----------------------------------- | ------------------------------------------------------- |
| `pnpm test`                         | Vitest testleri (jsdom, API çağrıları MSW ile)          |
| `pnpm test:watch`                   | İzleme modunda test                                     |
| `pnpm test:coverage`                | Kapsam raporu → `coverage/index.html`                   |
| `pnpm lint` / `pnpm typecheck`      | ESLint / TypeScript                                     |
| `pnpm format` / `pnpm format:check` | Prettier ile biçimlendir / kontrol et (CI kontrol eder) |
| `pnpm gen:api`                      | `../contracts/openapi.json` → `shared/api/schema.d.ts`  |

### API sözleşmesi (OpenAPI → TypeScript)

Backend'in OpenAPI dokümanı repoda `contracts/openapi.json` olarak tutulur; `pnpm gen:api` ondan
`shared/api/schema.d.ts`'i üretir (elle düzenlenmez, CI güncel olduğunu kontrol eder).

- `shared/api/contract.ts`: `ApiSchema<"LogEntryDto">` — şema, axios interceptor'ının snake→camel
  dönüşümünden sonraki haliyle (`source_id` → `sourceId`).
- `shared/api/contract.check.ts`: elle yazılmış DTO'ları (`features/*/types`) derleme zamanında
  sözleşmeyle karşılaştırır. Backend'de bir alan adı değişir/eklenir/silinirse ya da bir enum'a değer
  eklenirse `pnpm typecheck` hangi alanın farklı olduğunu yazarak kırılır.
- DTO'lar elle yazılmaya devam eder: UI'ın kullandığı TS enum'larını ve hangi alanların `null`
  olabileceğini (OpenAPI dokümanında bu bilgi yok) onlar taşır.

## 🎨 Tasarım Prensipleri

- **Canlı ve Akıcı:** Kullanıcı etkileşimleri Framer Motion kullanılarak akıcı hale getirilmiştir.
- **Karanlık Tema (Dark Mode):** Log analizi yapan geliştiriciler için göz yormayan, kontrastı yüksek dark theme varsayılan olarak uygulanmıştır.
- **Modüler Bileşenler:** UI elemanları (`shared/components/ui/`) tamamen yeniden kullanılabilir (reusable) şekilde kodlanmıştır.

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
npm install
```

### Ortam Değişkenleri (Environment Variables)
Proje kök dizininde bir `.env.local` dosyası oluşturun ve backend API adresinizi belirtin:
```env
NEXT_PUBLIC_API_URL=http://localhost:3500/api/v1
NEXT_PUBLIC_WS_URL=ws://localhost:3500/ws/logs
```

### Geliştirme Sunucusunu Başlatma
```bash
npm run dev
```
Uygulama **http://localhost:3000** adresinde çalışmaya başlayacaktır.

## 🎨 Tasarım Prensipleri
- **Canlı ve Akıcı:** Kullanıcı etkileşimleri Framer Motion kullanılarak akıcı hale getirilmiştir.
- **Karanlık Tema (Dark Mode):** Log analizi yapan geliştiriciler için göz yormayan, kontrastı yüksek dark theme varsayılan olarak uygulanmıştır.
- **Modüler Bileşenler:** UI elemanları (`shared/components/ui/`) tamamen yeniden kullanılabilir (reusable) şekilde kodlanmıştır.

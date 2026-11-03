# FaultLens · PRISM — Design Patterns

> Tasarım dili: **Glassmorphism × Bento grid**, imza öğe **Radar (sonar) kapsamı**.
> Kaynak: ui-ux-pro-max design-system sorgusu (`observability log monitoring developer dashboard realtime`, variance 7 · motion 6 · density 7) → `design-system/faultlens-prism/MASTER.md`.
> Bu dosya **nasıl** kullanılacağını anlatır; değerlerin kendisi `prism.css` (tema/token dosyası) içindedir.

---

## 0. Dosya haritası

| Dosya | Rol |
|---|---|
| `prism.css` | Tek kaynak: tema token'ları (dark/light), ölçek token'ları, bileşen sınıfları, motion |
| `DESIGN_PATTERNS.md` | Bu dosya — desenler, kurallar, yapılmaması gerekenler |
| `design-system/faultlens-prism/MASTER.md` | Skill çıktısı: stil, renk, tipografi kararları |
| `design-system/faultlens-prism/pages/dashboard.md` | Dashboard'a özel override |

Next.js'e taşırken: `prism.css` → `app/globals.css` (Tailwind v4 `@theme inline` ile `--color-*` eşlemesi), bileşen sınıfları → `shared/components/ui/*` içinde `cva` varyantları.

---

## 1. Token desenleri

### 1.1 Üç katman
1. **Ham palet** yok — bileşen asla hex kullanmaz.
2. **Semantik token** (`--bg`, `--card`, `--fg-2`, `--accent`, `--danger`, `--sev-*`) → temaya göre değişir.
3. **Ölçek token'ı** (`--r-*`, `--sp-*`, `--dur-*`, `--ease-*`) → temadan bağımsız.

### 1.2 Tema değiştirme
- Kök: `<div class="px" data-theme="dark|light">`. Tema = yalnızca token setinin değişmesi; bileşen CSS'i aynı kalır.
- Light temada vurgu `#22C55E` → `#15803D` olur: beyaz zemin üzerinde metin/ikon kontrastı 4.5:1 olsun diye. **Dark değerleri light'a taşınmaz**, her tema ayrı kontrol edilir.
- Seçim `localStorage` (`fl-theme`, `fl-lang`, `fl-motion`) içinde tutulur; okunamazsa varsayılan `dark` / `tr`.

### 1.3 Severity spektrumu
| Seviye | Token | Dark | Light | Kullanım |
|---|---|---|---|---|
| TRACE | `--sev-trace` | #64748B | #64748B | gürültü, gri |
| DEBUG | `--sev-debug` | #94A3B8 | #475569 | |
| INFO | `--sev-info` | #38BDF8 | #0369A1 | |
| WARN | `--sev-warn` | #F59E0B | #B45309 | |
| ERROR | `--sev-error` | #FB923C | #C2410C | |
| CRITICAL | `--sev-crit` | #F87171 | #DC2626 | radar blip, alarm |

Renk **tek başına** anlam taşımaz: her badge'de seviye adı yazılıdır (`CRIT`, `WARN`…), grafiklerde legend vardır.

---

## 2. Yüzey desenleri

| Desen | Sınıf | Ne zaman |
|---|---|---|
| Glass panel | `.glass` | Sidebar, kartlar, bento tile, toolbar — zemindeki orb'ların üstünde |
| Solid panel | `.solid` | Uzun okunacak metin, tablo gövdesi, modal içi form — blur maliyeti/okunabilirlik |
| Well | `.well` | Panel içinde ikincil alan (config özeti, kod) |
| Code | `.code` | Stack trace, sorgu, JSON |

Kurallar:
- Glass yalnızca **renkli/dokulu zemin üstünde** anlamlıdır; `.px-bg` her sayfada bir kez bulunur.
- **Sonar field (zemin deseni)**: sağ üstteki ekran dışı bir kaynaktan dışa doğru yayılan ince dalga halkaları (`--field-line`, 7 sn'de bir halka genişliği), aynı merkezden 26 sn'de dönen yumuşak sonar ışını (`--sweep`), iki ışık kaynağı (`--orb-1` sinyal, `--orb-2` karşı ışık) ve film greni (`--grain`). Halkalar `@property --wave` ile animasyonludur; desteklemeyen tarayıcıda statik kalır. Reduced-motion / `data-motion="off"` → tamamen durur.
- Zemin desenine içerik **asla** bağlanmaz; yalnızca atmosferdir. Kontrast her zaman `--bg` üzerinden hesaplanır.
- Glass üstünde metin kontrastı ≥ 4.5:1 — `--card` opaklığı dark'ta .62, light'ta .68'in altına inmez.
- İç içe glass **2 seviyeyi** geçmez (panel → well). Modal `--card-hi` kullanır.

---

## 3. Layout desenleri

### 3.1 App shell
```
.app (flex-wrap)
 ├─ nav.side.glass   flex: 1 1 232px   (telefonda üstte tam genişlik)
 └─ main.main        flex: 999 1 560px; min-width: 0
      ├─ header.topbar  (crumbs · ⌘K arama · dil · tema · bildirim)
      └─ section.bento
```
Sidebar asla `100vh` + `sticky` değildir; dar ekranda içeriğin üstüne akar.

### 3.2 Bento grid
- 12 kolon, `gap: 16px`. Span sınıfları: `.c-3 .c-4 .c-5 .c-6 .c-7 .c-8 .c-9 .c-12`, `.r-2`.
- Kırılımlar: ≤1180px → 3/4/5 → 6, 7+ → 12; ≤720px → hepsi 12.
- **Hiyerarşi = alan**: en önemli içerik (Radar, AI özeti) en büyük tile'dır. Aynı satırda en fazla 4 tile.

### 3.3 Liste + detay (master/detail)
Logs, Deployments: sol liste `flex: 999 1 520px`, sağ detay `flex: 1 1 380px`. Seçim değişince detay `swapA/swapB` ile içeri kayar (aşağıda 6.4).

### 3.4 Breakpoint kontrol listesi
375 · 768 · 1024 · 1440. Yatay kaydırma yalnızca tabloların kendi `.scroll-x` kutusunda.

---

## 4. Bileşen desenleri

Her bileşen: **varsayılan · hover · focus-visible · active · disabled · loading · error** durumlarını tanımlar.

| Bileşen | Sınıf / varyant | Notlar |
|---|---|---|
| Button | `.btn` + `.btn-primary .btn-ai .btn-danger .btn-ghost` · boyut `.btn-sm .btn-lg .btn-icon` | Min 44×44. Sayfa başına **tek** `.btn-primary`. İkon-only → `aria-label`. Loading → spinner + metin değişir, genişlik sabit |
| Input | `.field > .label + .input + .help/.err` | Label her zaman görünür (placeholder label değildir). Hata alanın **altında**, `.is-error` |
| Search / ⌘K | `.search` + `.kbd` | Global; `⌘K` command palette'i açar |
| Switch | `button.switch[role=switch][aria-checked]` | Anında etki eden ayarlar. Form gönderimi gerekiyorsa checkbox |
| Checkbox / Radio | `input.check` / `input.radio` | Native input, özel görünüm |
| Segmented | `.seg > button[aria-pressed]` | 2–4 karşılıklı seçenek (zaman aralığı, dil, motor filtresi) |
| Tabs | `.tabs > .tab[role=tab][aria-selected]` | Aynı bağlamda görünüm değişimi; alt çizgi `scaleX` ile kayar |
| Badge | `.badge.b-crit|b-error|b-warn|b-info|b-debug|b-trace|b-ok|b-ai` | Metin + nokta; asla yalnız renk |
| Chip (filtre) | `button.chip[aria-pressed]` | Kapalıyken kesikli kenar + .55 opaklık |
| Live dot | `.dot.dot-live` | Yalnızca **gerçekten canlı** veri varsa; bayatlarsa (>60 sn) gri + "son güncelleme" |
| Progress / Confidence | `.progress > i` | Güven skoru; < eşik ise `--warn` |
| Spectrum | `.spectrum > i` | Seviye dağılımı tek çubukta |
| Table row | `.trow` (+ `.thead`) | Tüm satır `button`; seçili `aria-pressed` |
| Skeleton | `.skel` | İçerik geç gelirse ≥300ms sonra göster; alan rezerve (CLS) |
| Empty state | `.empty` | Başlık + açıklama + tek eylem |
| Modal | `.scrim > .modal[role=dialog][aria-modal]` | Odak içeri taşınır, Esc kapatır, arkaplan scroll kilitli |
| Toast | `.toast-dock > .toast[role=status]` | 4 sn; hata toast'u kalıcı + kapatma butonu |
| Tooltip | `.tip > .tip-b` | Hover **ve** focus ile açılır; yalnız ek bilgi |
| Menu / Command | `.menu > .menu-item` | Ok tuşlarıyla gezinme, `aria-selected` |
| Stepper | `.stepper > i[style=--k]` | Sihirbaz ilerlemesi |
| Nav item | `a.nav-item[aria-current=page]` + `.count` | Sol aktif çubuk; sayaç kırmızı |
| Radar | `.radar > .radar-sweep + .blip` | İmza bileşen — bkz. 5 |

---

## 5. İmza: Radar deseni
- **Anlam**: merkez = sistem; halka = gecikme bandı (iç → hızlı); açı = servis; blip = aktif hata grubu; renk = en yüksek seviye; boyut = olay sayısı.
- Sweep 6 sn'de bir döner (dekoratif) — `prefers-reduced-motion` ve "Animasyonlar: kapalı" ayarında durur, blip'ler statik kalır.
- Her blip bir `<button>`; `aria-label="payment-svc · CRITICAL · 412 olay"`. Radar'ın yanında **aynı verinin listesi** her zaman bulunur (radar tek bilgi kaynağı değildir).

---

## 6. Motion desenleri

| Token | Değer | Kullanım |
|---|---|---|
| `--dur-1` | 150ms | press, renk değişimi, exit |
| `--dur-2` | 250ms | hover, focus, segment |
| `--dur-3` | 400ms | switch, tab çizgisi, modal giriş |
| `--dur-4` | 700ms | sayfa girişi, tile stagger, tema geçişi |
| `--ease-out` | cubic-bezier(.16,1,.3,1) | varsayılan giriş |
| `--ease-spring` | cubic-bezier(.34,1.56,.64,1) | küçük öğeler (switch topu, ok, blip) — **tablolarda kullanılmaz** |

1. **Giriş stagger**: `.in .s1…s10` (50ms adım). `animation-fill-mode: backwards` — giriş bittikten sonra hover transform'ları çalışır.
2. **Exit girişten hızlı**: modal/toast kapanışı `--dur-1`.
3. **Yalnızca transform/opacity** animasyonu; width/height animasyonu yok (progress `scaleX` ile).
4. **Yeniden tetikleme**: aynı öğede animasyonu tekrar oynatmak için `swapA/swapB` (veya `slideA/slideB`) sınıfları dönüşümlü verilir — anahtar (key) değiştirmeden restart.
5. **Canlı veri**: yeni satır `slideA/B` ile üstten kayar; ticker/stream için **Duraklat** kontrolü zorunlu.
6. **Erişilebilirlik**: `prefers-reduced-motion` → tüm süreler ~0, sweep/orb durur, çizimler son halinde. Uygulama içi `data-motion="off"` aynı etkiyi verir.
7. Sayaçlar (count-up) 1.2 sn, `ease-out-cubic`; reduced-motion'da direkt son değer.

---

## 7. Etkileşim & form desenleri
- **Optimistic UI**: Onayla/Çöz/Toggle anında yansır; hata gelirse geri al + hata toast'u.
- **Loading feedback**: >300ms süren her işlemde buton içi spinner; sayfa verisinde skeleton.
- **Kirli form**: değişiklik olunca alt ortada "Kaydedilmemiş değişiklikler" barı (Vazgeç / Kaydet).
- **Yıkıcı eylem** (rollback, kaynak silme): ikinci onay — buton metni "Emin misin?"e döner veya modal.
- **Sihirbaz**: en fazla 3 adım, `stepper`, geri her zaman mümkün; test adımı kontrol listesini tek tek işaretler.
- **Klavye**: `⌘K` komut paleti · `G` + harf ile navigasyon (G D, G L…) · `Esc` modal kapat · `/` aramaya odak.

---

## 8. Veri görselleştirme desenleri
| Veri | Grafik | Not |
|---|---|---|
| Zaman içinde hata | Area/line + deploy dikey işaretleri | Deploy = yeşil kesikli çizgi + etiket |
| Seviye dağılımı | Spectrum bar + yatay bar listesi | Sayılar sağda tabular-nums |
| Güven skoru | Progress / ring | Eşik çizgisi |
| Kaynak sağlığı | Sparkline | Durum metni ayrıca yazılı |
| Öncesi/Sonrası | İki yatay bar | Fark `+412` metni |

Her grafik: legend, `role="img"` + `aria-label` özeti, eksen/zaman etiketi.

---

## 9. i18n deseni
- Tüm UI metni sözlükten (`tr`, `en`); log mesajları kaynak dilinde kalır.
- Sayı/tarih `Intl` ile (`tr-TR` → `1.284`, `en-US` → `1,284`).
- Metin uzunluğu EN/TR arasında %30 değişebilir — butonlar `white-space: nowrap`, kartlar esnek yükseklik.

---

## 10. Kod mimarisi desenleri (Next.js 16 · React 19)
| Desen | Uygulama |
|---|---|
| **Feature-sliced** | `features/<alan>/{api,components,hooks,store,types}` — mevcut yapı korunur |
| **Provider** | `ThemeProvider` (`data-theme`, `data-motion`), `I18nProvider`, `QueryProvider` |
| **Variant (cva)** | `Button({ variant: 'primary'|'ai'|'danger'|'ghost', size: 'sm'|'md'|'lg'|'icon' })` |
| **Compound component** | `<Card><Card.Header/><Card.Body/></Card>`, `<Tabs><Tabs.List/><Tabs.Panel/></Tabs>` |
| **Headless + skin** | Modal/Menu davranışı (odak, Esc, ok tuşları) hook'ta; görünüm `prism.css` sınıflarında |
| **Container / Presentational** | `useLogsQuery` veri getirir, `LogTable` yalnız çizer |
| **Observer (stream)** | `useLogStream` WebSocket → Zustand store → bileşenler seçici (selector) ile abone |
| **Strategy** | Kaynak sihirbazında tür başına form şeması (`KUBERNETES | SSH | DOCKER | CICD | LOCAL_FILE`) |
| **Optimistic mutation** | TanStack Query `onMutate` / `onError` rollback |

---

## 11. Yapılmayacaklar (anti-patterns)
- Emoji ikon (yalnız SVG, 1.5–2px stroke, tek aile)
- Placeholder'ı label yerine kullanmak
- Renkle tek başına durum anlatmak
- Focus halkasını kaldırmak
- Tablolarda spring/overshoot animasyonu
- Bayat veriyi "canlı" göstermek
- Glass üstünde glass üstünde glass
- Sayfa başına birden fazla birincil buton
- 0ms durum değişimi (hover/press geri bildirimi her zaman var)

---

## 12. Teslim öncesi kontrol listesi
- [ ] Emoji ikon yok, ikon ailesi tek
- [ ] Tıklanabilir her şey `button`/`a`, ≥44px
- [ ] Hover 150–300ms geçişli, focus görünür
- [ ] Dark ve light kontrastı **ayrı ayrı** 4.5:1
- [ ] `prefers-reduced-motion` + uygulama içi motion kapatma
- [ ] 375 / 768 / 1024 / 1440 genişliklerinde yatay scroll yok
- [ ] Semantik token dışında renk yok
- [ ] Canlı veri için duraklat + son güncelleme zamanı

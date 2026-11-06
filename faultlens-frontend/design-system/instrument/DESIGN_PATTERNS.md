# FaultLens · Instrument — Tasarım Desenleri

> Tema dosyası: `instrument.css` · Tasarım tuvali: Claude Design "FaultLens — Instrument" (16 board)
> Esin: Refero — Linear ("midnight precision instrument") + Vercel ("typeset terminal on white paper"); ui-ux-pro-max — Dark/OLED developer tool, yoğun dashboard.

## 1. İlkeler

1. **Tek sinyal.** Koyu temada asit lime (`--signal #e4f222`), açık temada mürekkep siyahı tek birincil eylemdir. Bir görünümde en fazla bir `btn-primary`.
2. **Gölge değil çizgi.** Yüzeyler 1px hairline (`--line*`) ve `well` (sunken) ile ayrılır; gölge yalnızca katmanlarda (modal, menu, toast).
3. **Önem asla yalnızca renkle anlatılmaz.** Renk + metin etiketi + nokta (`badge .d`).
4. **Yoğun ama okunur.** 13.5–15px gövde, mono rakamlar (`.num`, `tabular-nums`), 36px kontrol (≤760px'de 44px).
5. **Sakin hareket.** Yalnızca `transform`/`opacity`; 120/200/320ms, `cubic-bezier(.2,0,0,1)`.

## 2. Token'lar ve temalar

Kök: `.fl[data-theme="dark|light"][data-motion="on|off"]`.

| Rol                                              | Midnight (koyu)                                           | Paper (açık)                                              |
| ------------------------------------------------ | --------------------------------------------------------- | --------------------------------------------------------- |
| `--bg / --surface / --raised`                    | #08090a / #0f1011 / #161718                               | #fafafa / #fff / #fff                                     |
| `--line / --line-2 / --line-3`                   | #1d1f23 / #2a2c31 / #3a3d43                               | #ebebeb / …                                               |
| `--fg / --fg-2 / --fg-3`                         | #f7f8f8 / #d0d6e0 / #8a8f98                               | #171717 / #4d4d4d / #666                                  |
| `--primary-bg / --primary-fg`                    | #e4f222 / #08090a                                         | #171717 / #fff                                            |
| `--signal-ink` (çizgi/ikon vurgusu)              | #e4f222                                                   | #5c6600                                                   |
| `--ok / --warn / --err / --crit / --info / --ai` | #4cb782 / #f2c94c / #f2994a / #eb5757 / #4ea7fc / #a78bfa | #1a7f37 / #9a6700 / #bc4c00 / #cf222e / #0969da / #8250df |

Diğer: `--sev-*` (TRACE→CRITICAL), `--heat-0..4`, yarıçap 4/6/10/14/999, `--font: Inter`, `--mono: JetBrains Mono`, `--t-1/2/3`, `--ease`.

**Kural:** Bileşenlerde ham hex yok; her zaman semantik token. Açık temadaki durum renkleri 4.5:1 kontrast için ayrı seçildi.

### Tema çözümleme (Sistem / Koyu / Açık)

```
pref = localStorage['in-theme'] ?? 'system'
theme = pref === 'system' ? (matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark') : pref
→ <html data-theme={theme}>
```

- `matchMedia` `change` dinleyicisi ile işletim sistemi değişikliği canlı uygulanır.
- Menüde Sistem seçeneği o anki çözümü gösterir: "Sistem · Koyu".
- Next.js: FOUC olmaması için `<head>` içinde senkron inline script tema özniteliğini hydrate'ten önce yazar; `suppressHydrationWarning` kökte.

## 3. Çoklu dil (TR/EN)

- `localStorage['in-lang']`, varsayılan `tr`. `<html lang>` güncellenir.
- İki katman: **kabuk sözlüğü** (nav, topbar, palet) + **sayfa sözlüğü** (`T`). Veri içi metinler `[tr, en]` ikilisi.
- **Çevrilmez:** enum değerleri ve API sözcükleri — `ERROR`, `SUCCESS`, `THRESHOLD`, `IN_APP`, `ADMIN`, endpoint yolları.
- Tarih/sayı: `Intl.DateTimeFormat(lang)`, `Intl.NumberFormat(lang)`; göreli zaman `Intl.RelativeTimeFormat`.
- Next.js: `next-intl` önerilir; mesajlar `messages/tr.json`, `messages/en.json`. Dil seçimi çerez yerine localStorage + `router.refresh()` ya da `[locale]` segmenti.

## 4. Düzen

- **Uygulama kabuğu:** `.app` = `.sidebar` (240px) + `.content` (`.topbar` + `.page`).
- Sidebar: marka → ⌘K arama → nav grupları (Gözlem: Genel bakış, Loglar, Analizler · Altyapı: Kaynaklar, Deployments · Yanıt: Alarmlar) → Ayarlar → kullanıcı kartı (rol rozeti).
- Topbar: breadcrumb · dil menüsü · tema menüsü · alarm zili (aktif varsa kırmızı nokta).
- Sayfa: `.page-head` (h1 + alt satır + sağda filtre/birincil eylem) → `.grid` 12 kolon (`s-3…s-12`, gap 16).
- Liste + detay: `flex-wrap` ile liste (`flex: 999 1 560px`) ve detay paneli (`flex: 1 1 360px`); dar ekranda alt alta.
- Açık sayfalar (Landing, Login, Legal) `.fl` kökünü kabuksuz kullanır; dil ve tema seçicisi yine sağ üstte.
- Kimlik sayfaları: sol form (max 380px) + sağ marka paneli (≤ tablet'te gizlenebilir).
- Klavye: `⌘K` palet, `G O/L/A/S/D/M` sayfa geçişi, `/` arama, `A` üstlen, `R` çöz, `Esc` kapat.

## 5. Bileşenler (instrument.css)

| Grup          | Sınıflar                                                                                                                               | Durumlar                                                           |
| ------------- | -------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------ |
| Buton         | `btn` `btn-primary` `btn-ghost` `btn-danger` `.solid` `btn-sm` `btn-icon` `.k`                                                         | hover, `:focus-visible`, `aria-disabled`, `aria-busy` + `.spinner` |
| Form          | `field` `label` `help` `err` `input` `select.input` `textarea` `search` `kbd`                                                          | `aria-invalid`, hata alanın altında                                |
| Seçim         | `switch[role=switch]` `check` `radio` `seg` `chip[aria-pressed]` `tabs/.tab[aria-selected]`                                            | —                                                                  |
| Gösterim      | `badge` (`.d`, `.mono`, `.outline`), `sev-*`, `c-*`, `dot` `.live`, `avatar`, `bar>i`, `stat`, `delta`, `heat i[data-l]`, `skel`, `kv` | —                                                                  |
| Tablo         | `tr` `.th` (CSS grid satır)                                                                                                            | `aria-selected`, satır `in` + 30ms stagger                         |
| Geri bildirim | `banner[--c]`, `toast-dock/.toast`, `empty`, `tip>.tip-b`                                                                              | toast 2.6s, `aria-live=polite`                                     |
| Katman        | `scrim(.center)`, `modal(-h/-b/-f)`, `menu/menu-item/menu-sep/menu-label`, `pop-anchor`                                                | `role=dialog                                                       | alertdialog`, `aria-modal`, odak tuzağı |
| Kimlik        | `strength i.on`, `checklist li.ok`, `steps i[--k]`, `otp`                                                                              | —                                                                  |
| Metin         | `display h1 h2 h3 lead small xs mono num eyebrow t2–t4 mark link sr prose(.ph)`                                                        | —                                                                  |
| Yasal         | `consent`, `prose .ph` (doldurulacak yer)                                                                                              | —                                                                  |

Canlı katalog: **Components** board'u (tema/dil değiştirerek iki temada kontrol).

## 6. Veri görselleştirme

- **Hata hacmi:** saatlik yığılmış sütunlar; deploy anları dikey `--signal-ink` iğne + sürüm etiketi (Dashboard, Deployments).
- **Isı haritası:** servis × saat, `--heat-0..4`; her hücrede `title` ile sayı.
- **Sparkline:** KPI kartında, eksensiz; trend rengi anlamla (artan hata = `--crit`).
- **Güven çubuğu:** `bar` — AI `--ai`, kural `--fg-2`, < 0.70 `--warn` + "İnceleme" filtresi.
- Renk tek başına anlam taşımaz; lejant ve etiket her zaman var. `growY` animasyonu 18ms stagger.

## 7. Durumlar

| Durum         | Desen                                                                                  |
| ------------- | -------------------------------------------------------------------------------------- |
| Yükleniyor    | `skel` ile alan ayrılır (CLS 0); spinner yalnızca buton içinde                         |
| Boş           | `empty`: neden boş + sonraki adım ("Filtreyi genişlet", "Kaynak ekle")                 |
| Hata          | Alanın yanında `err`; sayfa seviyesinde `banner --crit` + yeniden dene                 |
| Canlı         | Yeni satır `inA/inB` alternasyonu ile; Duraklat butonu; `aria-live` yalnızca özet için |
| Kaydedilmemiş | Alt ortada savebar: Vazgeç / Kaydet (⌘S)                                               |

## 8. Hareket

| Süre          | Kullanım                                   |
| ------------- | ------------------------------------------ |
| 120ms `--t-1` | hover, focus, switch, check                |
| 200ms `--t-2` | tab çizgisi, menü, panel `swA/swB`         |
| 320ms `--t-3` | modal girişi, sayfa `in` + `d1…d8`, `draw` |
| 30ms stagger  | liste satırları                            |

- `.in` `animation-fill-mode: backwards` — giriş bittikten sonra hover transform'ları çalışır.
- İçerik değişimi için yeniden-key yerine sınıf alternasyonu (`swA/swB`, `inA/inB`).
- `[data-motion="off"]` (Ayarlar → Görünüm) ve `prefers-reduced-motion` tüm animasyonu kapatır.

## 9. Güvenlik ve gizlilik desenleri

**Yıkıcı işlem merdiveni**

1. Geri alınabilir → toast (gerekirse "Geri al").
2. Kalıcı → `alertdialog`, sonucu düz cümleyle anlatır, `btn-danger.solid`, varsayılan odak "Vazgeç".
3. Kaynak / toplu silme → adı yazdırarak onay.
4. Hesap silme → ne silinir listesi, önce dışa aktar, onay kutusu, kullanıcı adı + şifre, 30 gün bekleme ve iptal.

**Kimlik**

- Giriş: genel hata ("kullanıcı adı veya şifre hatalı"), 3 denemede 30 sn kilit, Caps Lock uyarısı, şifre göster/gizle, `autocomplete` doğru.
- Şifremi unuttum: hesabın varlığı açıklanmaz; FaultLens modeline uygun olarak **yönetici sıfırlar** → geçici şifre → zorunlu değişim.
- Şifre değiştir (`mustChangePassword`): güç ölçer + kural listesi (12+, büyük/küçük, rakam, sembol, geçiciden farklı), eşleşme kontrolü; başarıda diğer oturumlar kapanır.
- Oturumlar: liste + tek tek / toplu kapatma. Erişim belirteci 15 dk ve yalnızca bellekte; yenileme belirteci HttpOnly çerez, her kullanımda döner.
- Sırlar (SSH anahtarı, Slack/webhook URL) şifreli saklanır, `type=password`, tekrar gösterilmez. Webhook yalnızca https + HMAC imza.

**Gizlilik**

- AI motoru seçildiğinde açık uyarı: mesaj + stack trace OpenAI'ye (ABD) gider; KVKK md. 9 açık rıza onay kutusu.
- Çerez onayı: "Yalnızca zorunlu" ve "Tümünü kabul et" eşit ağırlıkta; analitik varsayılan kapalı; footer'da "Çerez tercihleri" ile geri alınır.
- Legal board: Gizlilik, KVKK aydınlatma, Çerez, Koşullar. `.ph` alanları doldurulmalı ve **hukukçu onayı gerekir**.

## 10. Rol tabanlı arayüz

| İşlem                             | ADMIN | EDITOR | VIEWER |
| --------------------------------- | ----- | ------ | ------ |
| Log, grup, analiz görüntüleme     | ✓     | ✓      | ✓      |
| Kaynak / kural / deploy düzenleme | ✓     | ✓      | —      |
| Alarm üstlenme / çözme            | ✓     | ✓      | —      |
| Analiz geçmişini silme            | ✓     | —      | —      |
| Sistem ayarları (saklama, motor)  | ✓     | —      | —      |
| Kullanıcı yönetimi                | ✓     | —      | —      |

Yetkisiz eylem gizlenmez; `aria-disabled` + ipucunda neden. Yönetici bölümleri `ADMIN` rozetiyle işaretlenir. API her durumda 403 döner — arayüz güvenlik sınırı değildir.

## 11. Sayfa haritası → Next.js

| Board          | Route                 | Ana endpoint'ler                                                                           |
| -------------- | --------------------- | ------------------------------------------------------------------------------------------ |
| Landing        | `/`                   | —                                                                                          |
| Login          | `/login`              | `POST /auth/login`                                                                         |
| ForgotPassword | `/forgot-password`    | _(backend'de yok — tasarım)_                                                               |
| ChangePassword | `/change-password`    | `PUT /auth/password`                                                                       |
| Legal          | `/legal/[doc]`        | —                                                                                          |
| Dashboard      | `/dashboard`          | `GET /logs/stats`, `/logs/groups`, `/alarms/stats`, WS `/ws/logs`                          |
| Logs           | `/logs`               | `GET /logs`, `/logs/groups`                                                                |
| LogDetail      | `/logs/groups/[id]`   | `GET /logs/groups/{id}/entries`, `POST /analyses/trigger`                                  |
| Analyses       | `/analyses`           | `GET /analyses`, `DELETE /analyses/history`                                                |
| Sources        | `/sources`            | `GET/POST /sources`, `POST /sources/test-config`, `/sources/{id}/test`, enable/disable     |
| Deployments    | `/deployments`        | `GET/POST /deployments`                                                                    |
| Alarms         | `/alarms`             | `GET /alarms`, ack/resolve/delete, `/alarms/rules` (+ enable/disable)                      |
| Settings       | `/settings/[section]` | `/auth/profile`, `/auth/password`, `/settings/notifications`, `/settings/system`, `/users` |

Uygulama:

- `instrument.css` → `app/globals.css` içine; `.fl` kökü `<body>` üzerinde.
- Tema/dil için `ThemeProvider` + `LocaleProvider` (client), topbar'daki `LangMenu` ve `ThemeMenu`.
- Bileşenler `components/ui/*` (Button, Field, Switch, Seg, Badge, Modal, AlertDialog, Menu, Toast, Table). Sınıf adları CSS ile birebir.
- Menü/modal erişilebilirliği için Radix primitive'leri + bu sınıflar.

## 12. Backend boşlukları (tasarımda var, API'de yok)

- Şifremi unuttum talebi (yöneticiye bildirim)
- Aktif oturum listesi ve oturum kapatma
- Hesap silme (30 gün bekleme) ve kişisel veri dışa aktarma
- Rollback işaretleme mevcut `POST /deployments` ile `ROLLED_BACK` kaydı olarak yapılabilir

## 13. Yapma / yap

| Yapma                            | Yap                          |
| -------------------------------- | ---------------------------- |
| Her yere lime                    | Görünümde tek birincil eylem |
| Önemi yalnızca renkle            | Renk + metin + nokta         |
| Placeholder'ı etiket olarak      | Görünür label + help         |
| Gölge yığını                     | Hairline + well              |
| Emoji ikon                       | SVG (1.8 stroke)             |
| width/height animasyonu          | transform/opacity            |
| Hata mesajını sayfanın tepesinde | Alanın hemen altında         |
| Yıkıcı işlemi tek tıkla          | Merdivendeki uygun onay      |

## 14. Teslim öncesi kontrol listesi

- [ ] Her iki temada metin kontrastı ≥ 4.5:1 (büyük metin 3:1)
- [ ] Tüm ikon butonlarında `aria-label`; `:focus-visible` halkası görünür
- [ ] Kontroller 36px, dokunmatikte 44px; aralar ≥ 8px
- [ ] Sistem teması işletim sistemi değişince anında güncelleniyor
- [ ] Tüm metinler TR/EN sözlükte; enum'lar çevrilmemiş
- [ ] Yükleniyor / boş / hata durumları tasarlanmış
- [ ] Yıkıcı işlemler `alertdialog`; varsayılan odak "Vazgeç"
- [ ] `prefers-reduced-motion` ve `data-motion="off"` test edildi
- [ ] Yetkisiz eylemler pasif + nedenli; API 403 ile korunuyor
- [ ] Yasal metinlerdeki `[...]` alanları dolduruldu ve hukukçu onayı alındı

# Flame in Freefall — Günlük Geliştirme Özeti

**Tarih:** 29 Eylül 2026  
**Proje:** Flame in Freefall (NASA Mikro-yerçekimi Yanma Deneyleri Dashboard & Analitik Platformu)  
**Hedef Platformlar:** Web (Compose Multiplatform / Wasm — Birincil), Android (İkincil), Ingestion CLI (Kotlin/JVM)  
**Backend & Veritabanı:** Supabase (PostgreSQL 15+ ile pgvector eklentisi, Storage, Functions)  

---

## 1. Mimari Altyapı ve Gradle Yapılandırması (Adım 0.1)

- **Root & Modül Mimarisi:**
  - Proje kök adı `FlameInFreefall` olarak ayarlandı.
  - `:shared`: Domain, data, repository, network, models, usecase, di, utils katmanlarından oluşan Temiz Mimari (Clean Architecture).
  - `:webApp`: Compose Multiplatform / Wasm web hedefi (birincil halka açık gösterge paneli).
  - `:androidApp`: Jetpack Compose tabanlı mobil hedef.
  - `:ingestion`: NASA kaynaklarından veri toplayan bağımsız çalıştırılabilir Kotlin/JVM komut satırı aracı (CLI).
- **Bağımlılıklar & Sürümler:**
  - Kotlin 2.4 / Compose Multiplatform 1.12.1 / Material 3
  - Koin 4.0.2 (Multiplatform DI)
  - Supabase-kt 3.1.1 (Postgrest, Storage, Functions)
  - Ktor 3.1.1 (CIO engine ingestion için, OkHttp Android için, JS Wasm/Web için)
  - Kotlinx Serialization 1.8.0 & Coroutines 1.11.0
- **Tasarım Dili & Material 3 Adaptive Breakpoints:**
  - NASA uzay ve mikro-yerçekimi küresel alev teması:
    - *Space Void* (`#080C14`, `#0F1726`)
    - *Plasma Cyan* (`#00E5FF`, `#38BDF8` — kemilüminesans/radikal dış alev)
    - *Solar Amber* (`#FF8C00`, `#FFA033` — iç yanma çekirdeği)
    - *Thermal Crimson* (`#EF4444` — risk göstergesi)
  - Adaptif Yerleşim: Mobil (Kompakt <600dp alt bar), Tablet (Orta 600-840dp NavigationRail), Masaüstü/Web (Genişletilmiş >=840dp sol kalıcı çekmece).
  - Gezinilebilir Sekmeler: *Keşfet*, *Risk Sıralaması*, *Hakkında*.
  - Güvenli Ortam Değişkenleri: `SUPABASE_URL` ve `SUPABASE_ANON_KEY` koda gömülmeden derleme zamanında Gradle task'ı ile enjekte edilir.

---

## 2. Supabase Veritabanı Şeması & Vektör Arama (Adım 0.2 & Kurulum)

- **Veritabanı Canlı Şeması Kuruldu ve Doğrulandı:**
  1. `vector` (pgvector) eklentisi etkinleştirildi.
  2. **`experiments` Tablosu:** NASA deney kayıtları (`id`, `title`, `series`, `material`, `gravity_level`, `oxygen_concentration`, `year`, `source_url` [ZORUNLU], `source_type` ['psi'|'ntrs'], `raw_text`, `needs_manual_review`, `created_at`).
  3. **`experiment_chunks` Tablosu:** 1536 boyutlu embedding vektörleri ile metin parçaları.
  4. **`experiment_summaries` Tablosu:** AI özeti, 0-100 risk skoru, risk etiketi, anahtar bulgular, durum.
  5. **HNSW Benzerlik İndeksi:** `vector_cosine_ops` ile yüksek başarımlı vektör arama indeksi.
  6. **Filtre İndeksleri:** `series`, `material`, `gravity_level`, `oxygen_concentration`, `year` filtreleme indeksleri.
  7. **Row Level Security (RLS):** Halka açık salt-okunur (SELECT herkese açık, yazma sadece `service_role` yetkisiyle).
  8. **RPC Fonksiyonu:** `match_experiment_chunks` vektör benzerlik arama prosedürü.
  9. **`experiment_details_view`:** Deneyler ile özetleri birleştiren, dinamik filtrelenebilir görünüm.
  10. Tek tıkla çalıştırılabilir konsolide SQL dosyası `supabase/setup_all.sql` olarak oluşturuldu.

---

## 3. Gerçek Veri Toplama & İşleme — FlameDataIngestor (Faz 1)

- **1.1 — CLI İskeleti:**
  - `:ingestion` modülünde bağımsız CLI kuruldu (`fetch`, `extract`, `embed`, `summarize`, `status`).
- **1.2 — NTRS Gerçek Veri Çekme Motoru (`fetch`):**
  - NASA Technical Reports Server (NTRS) resmi genel API'si (`https://ntrs.nasa.gov/api/citations/search`) sorgulandı ("microgravity combustion", "FLEX experiment", "SAFFIRE", "BASS combustion").
  - **40 adet gerçek, yayınlanmış NASA mikro-yerçekimi deney kaydı Supabase `experiments` tablosuna başarıyla aktarıldı.**
- **1.3 — PDF/Metin Çıkarımı (`extract`):**
  - Apache PDFBox 3.0.4 entegrasyonu ile NASA sunucularından gerçek teknik rapor PDF'leri indirildi.
  - **29 adet deneyin tam metni başarıyla çıkarılarak Supabase `raw_text` alanına yazıldı.**
  - Taranmış / OCR gereken veya metin çıkarılamayan 8 kayıt, proje kuralı gereği uydurma veri üretilmeksizin `needs_manual_review = true` olarak etiketlendi.
- **1.4 & 1.5 — Embedding & AI Özet Motorları (`embed`, `summarize`):**
  - Gerçek metinleri 500 token'lık parçalara bölen chunking ve OpenAI `text-embedding-3-small` (1536 boyut) istemcisi hazırlandı.
  - Gerçek metne dayalı 0-100 risk skoru ve güvenlik analizi üreten OpenAI `gpt-4o-mini` özet istemcisi kodlandı (halüsinasyonu yasaklayan sistem promptu ile).

---

## 4. AI Katmanı (RAG) & Supabase Entegrasyonu (Faz 2)

- **2.1 — Arama/Filtreleme Repository:**
  - `ExperimentRepositoryImpl` geliştirildi: `experiment_details_view` üzerinden malzeme, seri, yerçekimi seviyesi, risk skoru ve yıl filtreleriyle gerçek zamanlı sorgulama.
  - `getFilterOptions()`: Malzeme, seri ve yerçekimi listelerini veritabanındaki gerçek kayıtlardan tamamen dinamik çeker (sıfır hardcoded/mock veri).
- **2.2 — RAG Soru-Cevap Edge Function'ı ("ask") & RagRepository:**
  - Supabase Edge Function (`supabase/functions/ask/index.ts`) Deno/TypeScript ile oluşturuldu:
    - Gelen sorunun 1536 boyutlu embedding vektörünü OpenAI'dan alır.
    - PostgreSQL RPC (`match_experiment_chunks_with_metadata`) ile pgvector kosinüs benzerliği araması yapar.
    - Eşleşen gerçek deneyleri (ID, Başlık, Seri, `source_url`) bağlam olarak `gpt-4o-mini` modeline verir.
    - Sistem promptunda "sadece bağlamdaki gerçek metni kullan, bağlamda yoksa bilmediğini söyle" kuralı işletilir.
    - Kaynak gösterilemeyen durumlarda kesin olarak `"bu soruya cevap verecek yeterli veri bulunamadı"` yanıtı ve `insufficient_data` durumu döndürülür.
  - **Shared Network Katmanı (`RagRepository`):**
    - `RagRepository` domain arayüzü ve `RagRepositoryImpl` (Ktor client ile Edge Function'ı çağıran) sınıfı oluşturuldu.
    - `RagQuestionRequest`, `RagAnswer`, `RagSourceReference` modelleri tanımlandı.
    - `AskRagQuestionUseCase` yazıldı ve Koin DI konteynerine eklendi.
  - **SQL Migration:**
    - `supabase/migrations/20260929000002_match_chunks_with_metadata.sql` ve `setup_all.sql` güncellendi.
  - **Derleme Doğrulaması:**
    - `:shared:assemble` (Wasm, Js, Android, JVM) %100 başarıyla derlendi.

---

## 5. Dashboard Açık Tema Tasarım Sistemi (Faz 3.1)

- **Açık Tema Renk Paleti (Laboratuvar & Güvenlik Paneli):**
  - Arka plan: Kırık beyaz (`#F7F8FA` - `LabBackground`)
  - Kart ve Yüzeyler: Beyaz (`#FFFFFF` - `LabSurface`), İnce gri kenarlık (`#E2E5EA` - `LabBorder`)
  - Birincil vurgu: Turuncu-kırmızı (`#E8572E` - `AccentOrangeRed`, sadece ikon ve buton vurgularında)
  - İkincil vurgu: Koyu camgöbeği (`#1E88C7` - `AccentTeal`)
  - Risk seviyeleri: Düşük yeşil (`#2E9E6C`), Orta amber (`#C98A1A`), Yüksek kırmızı (`#D64545`)
  - Tipografi: Koyu antrasit (`#1B1F27` - `TextDarkAnthracite`), İkincil orta gri (`#6B7280` - `TextMediumGray`)
- **Material 3 ColorScheme:**
  - `FlameLabLightColorScheme` kuruldu; proje kuralına uyularak koyu tema eklenmedi.
- **Yeniden Kullanılabilir Panel Bileşenleri (`ui/components`):**
  - `FlameCard` & `FlameSectionCard`: Beyaz yüzey, 1dp ince gri kenarlık, 12dp yuvarlatılmış köşe.
  - `FlameBadge`: `FlameRiskBadge` (düşük/orta/yüksek açık zemin rozetleri), `FlameSeriesBadge`, `FlameMaterialBadge`, `FlameStatusBadge`.
  - `FlameButton`: `FlamePrimaryButton` (turuncu-kırmızı), `FlameSecondaryButton` (camgöbeği), `FlameOutlinedButton` (beyaz zemin).
  - `FlameSearchField`: İnce kenarlıklı, camgöbeği odaklı, temizleme butonlu laboratuvar arama alanı.
  - `FlameTabs`: Alt çizgi ve sayaç rozeti destekli açık tema sekme çubuğu.
- **Ekran Uyarlaması:**
  - `MainAdaptiveScreen` kompakt, orta ve genişletilmiş (desktop/web) yerleşimleriyle açık tema sistemine uyarlandı.

---

---

## 6. Ana Ekran — Arama ve Filtreleme (Faz 3.2 - ExperimentListScreen)

- **Adaptive Yerleşim:**
  - **Geniş Ekran (Web / Masaüstü):** Sol yan panelde dinamik filtreler + sağda çok sütunlu adaptive kart grid'i (`GridCells.Adaptive(minSize = 340.dp)`).
  - **Dar Ekran (Mobil):** Üstte açılır animasyonlu filtre paneli + tek sütun kart listesi.
- **Arama ve Canlı Durum:**
  - Üstte geniş `FlameSearchField` arama alanı (debounce destekli).
  - Canlı Supabase kayıt sayacı rozeti ("40 Kayıt") ve anlık yenileme butonu.
- **Gerçek Veri Kartları (`ExperimentItemCard`):**
  - Supabase `experiment_details_view` görünümünden çekilen gerçek kayıtlar:
    - Başlık (Gerçek NASA NTRS makale başlığı)
    - Seri rozeti (`FlameSeriesBadge` — FLEX, SAFFIRE, BASS vb.)
    - Malzeme rozeti (`FlameMaterialBadge` — PMMA, Heptane, Cotton vb.)
    - Yayın yılı ve NASA NTRS bağlantı göstergesi
    - AI özeti (varsa turuncu-kırmızı etiketle gösterim, yoksa "metin çıkarıldı / özet bekleniyor" durumu)
    - Risk rozeti (varsa `FlameRiskBadge`, yoksa "risk bekleniyor" rozeti)
- **State Management & DI:**
  - `ExperimentListViewModel` oluşturuldu, Koin DI konteynerine kaydedildi.
  - `GetFilterOptionsUseCase` ve `GetExperimentsUseCase` ile Supabase'den %100 dinamik besleme.
- **Derleme Doğrulaması:**
  - `:shared:compileKotlinWasmJs` ve `:shared:compileAndroidMain` başarıyla derlendi.

---

---

## 7. Deney Detay Ekranı (Faz 3.3 - ExperimentDetailScreen)

- **Detay Ekranı Bileşeni (`ExperimentDetailScreen`):**
  - **Başlık ve Meta Şerit:** Deney başlığı, UUID, seri rozeti (`FlameSeriesBadge`), malzeme rozeti (`FlameMaterialBadge`), oksijen konsantrasyonu (`% O2`), yerçekimi ortamı ve yayın yılı.
  - **AI Analiz Kartı (`AiSummaryCard`):** 
    - GERÇEK AI özeti (`aiSummary`).
    - Vurgulu Kutu: **"Ana Bulgu"** (`AccentTealMuted` zemin, 3dp camgöbeği sol kenarlık, `keyFinding`).
    - Vurgulu Kutu: **"Güvenlik Çıkarımı & Yanma Davranışı"** (`AccentOrangeRedMuted` zemin, 3dp turuncu-kırmızı sol kenarlık, risk skoru ve etiketi).
    - Özet bulunmadığında: *"bu deney için özet henüz üretilemedi"* veya *"veri yetersiz"* laboratuvar uyarısı.
  - **Çıkarılan Doküman Metni (`RawDocumentCard`):** NASA NTRS PDF'inden Apache PDFBox ile çıkarılmış gerçek ham metni görüntüleyen genişletilebilir (`isExpanded`) alan.
  - **Resmi Kaynak Bağlantısı (`SourceLinkCard`):** `source_url` bağlantısı ve platforma göre harici tarayıcıda açılmasını sağlayan Compose Multiplatform `LocalUriHandler` entegrasyonu.
  - **Benzer Deneyler Paneli (`SimilarExperimentsList`):**
    - Geniş ekranda (web/masaüstü): Sağ yan panelde (`width = 340.dp`).
    - Dar ekranda (mobil): Sayfanın altında dikey liste.
    - `GetSimilarExperimentsUseCase` ile Supabase'den aynı serideki diğer GERÇEK kayıtları dinamik sorgular ve tıklandığında o deneyin detayına geçiş sağlar.
- **Ekranlar Arası Navigasyon:**
  - `ExperimentListScreen` içindeki her karta (`ExperimentItemCard`) tıklama bağlandı; tıklandığında doğrudan `ExperimentDetailScreen` açılır, "← Geri" butonuyla listeye dönülür.
- **Derleme Doğrulaması:**
  - `:shared:compileKotlinWasmJs` ve `:shared:compileAndroidMain` hatasız derlendi.

---

---

## 8. Risk Sıralama Ekranı (Faz 3.4 - RiskRankingScreen)

- **Veri ve Modelleme (`RiskRankingViewModel` & `MaterialRiskSummary`):**
  - Supabase veritabanındaki GERÇEK deney kayıtlarını malzemelerine (`material`) göre dinamik gruplar.
  - Her malzeme için: Ortalama risk skoru (0-100), toplam gerçek deney sayısı, en yüksek riskli deney senaryosu ve veri yeterlilik durumu hesaplanır.
  - Hiç risk skoru üretilmemiş malzemeler *"veri yetersiz"* rozetiyle listenin sonuna alınır.
- **Dinamik Sıralama ve Arama Kontrolleri:**
  - 4 farklı sıralama ölçütü: **En Yüksek Risk (Varsayılan)**, **En Düşük Risk**, **Deney Sayısı (Çoktan Aza)**, **Malzeme Adı (A-Z)**.
  - `FlameSearchField` ile malzeme adına göre anlık filtreleme.
  - Hızlı istatistik göstergeleri: Toplam Test Edilen Malzeme, Risk Analizi Yapılan ve Veri Bekleyen sayıları.
- **Adaptive Yerleşim:**
  - **Geniş Ekran (Web / Masaüstü):** Tam laboratuvar tablosu (`WideRiskTableView`):
    - *Kolonlar:* Malzeme Adı, Ortalama Risk Skoru (renkli progress bar + sayı + seviye etiketi), Gerçek Deney Sayısı rozeti, En Yüksek Riskli Senaryo / Başlık.
  - **Dar Ekran (Mobil):** Zengin kart listesi (`MobileRiskCardListView`):
    - Malzeme etiketi, deney sayısı rozeti, renkli risk skoru çubuğu ve riskli senaryo açıklaması içeren `FlameCard` tasarımı.
- **Derleme Doğrulaması:**
  - `:shared:compileKotlinWasmJs` ve `:shared:compileAndroidMain` hatasız derlendi.

---

## 9. Soru-Cevap Paneli & Bilimsel AI Asistan (Faz 3.5 - AskPanel)

- **Bileşen Mimarisi (`AskPanel.kt` & `AskPanelViewModel.kt`):**
  - **Adaptive Yerleşim:**
    - **Geniş Ekran (Web / Masaüstü):** Sağdan animasyonlu açılan yan panel (`width = 380.dp`, `slideInHorizontally` + `fadeIn`). Hem üst başlıktaki hem de sol menüdeki "✦ NASA AI Asistanı" butonlarıyla kontrol edilir.
    - **Dar Ekran (Mobil):** Ekranın altından açılan `ModalBottomSheet` yapısı (`fillMaxHeight(0.85f)`). Üst bardaki "✦ AI Asistan" butonuyla tetiklenir.
  - **Mesaj Geçmişi ve Hizalama:**
    - Kullanıcı mesajları sağa hizalı, camgöbeği zeminli (`AccentTealMuted`).
    - AI yanıtları sola hizalı, beyaz yüzeyli (`LabSurface`), turuncu-kırmızı etiketli (`AccentOrangeRed`).
  - **Doğrulanmış Kaynak Referansları:**
    - AI cevabının altında GERÇEK kaynak deneyler listelenir (seri rozeti, NASA NTRS başlığı).
    - `source_url` bağlantısı platforma duyarlı `LocalUriHandler` ile tıklanarak doğrudan NASA NTRS makale sayfasına yönlendirir.
  - **Yetersiz Veri Güvenlik Kuralı:**
    - Modelden veya RAG'den kaynak bulunamazsa veya `insufficient_data` bayrağı gelirse sarı/amber renkli *"Yetersiz Veri Bildirimi"* kutusu gösterilir: *"Bu konuda mevcut NASA veri setinde doğrulanabilir yeterli bilgi bulunamadı. Kural gereği uydurma veya tahmini yanıt üretilmemiştir."*
  - **"Düşünüyor..." Göstergesi:**
    - Soru gönderildiğinde dönen dairesel yükleyici ve *"NASA deneyleri semantik olarak taranıyor"* durum bilgisi.
  - **Hızlı Soru Önerileri (Quick Chips):**
    - Sohbet başında kullanıcıya mikroyerçekimi deneylerine yönelik hızlı örnek sorular ("PMMA alev yayılımı", "FLEX damlacık yanması").
- **Koin DI Entegrasyonu:**
  - `AskPanelViewModel` `viewModelModule` içine tekil (`single`) olarak kaydedildi; böylece sekmeler arasında gezinildiğinde sohbet geçmişi korunur.
- **Tüm Form Faktörlerine Entegrasyon:**
  - `MainAdaptiveScreen` COMPACT, MEDIUM ve EXPANDED görünümlerine `AskPanel` tam olarak entegre edildi.
- **Derleme Doğrulaması:**
  - `:shared:compileKotlinWasmJs`, `:shared:compileAndroidMain` ve `:webApp:compileKotlinWasmJs` %100 başarıyla derlendi.

---

## 10. Navigasyon ve Birleştirme (Faz 3.6 - Adaptive Navigation, Global FAB, Live AboutScreen)

- **Adaptive Navigasyon İskeleti:**
  - **Geniş Ekran (Web / Masaüstü, >=840dp):** Kalıcı sol navigasyon çekmecesi (`ExpandedSidebar` / Drawer) ve tablet ekranında `MediumNavigationRail`. Her iki ekranda da sekmeler: *"Keşfet"*, *"Risk Sıralaması"*, *"Hakkında"*.
  - **Dar Ekran (Mobil, <600dp):** Açık tema uyumlu alt navigasyon çubuğu (`CompactBottomBar` / `NavigationBar`).
- **Global Floating Action Button (`GlobalAskFab`):**
  - Tüm ekranlarda (mobil Scaffold `floatingActionButton` ve geniş ekranda sağ alt köşe) her zaman erişilebilir.
  - Açık tema laboratuvar stili (hap formunda `RoundedCornerShape(24.dp)`, `AccentOrangeRed` zemin, beyaz metin).
  - Tıklandığında `AskPanel`'i geniş ekranda sağdan kaydırarak, mobilde alttan bottom sheet olarak açar/kapatır.
- **Canlı Verili "Hakkında" Ekranı (`AboutScreen` & `AboutViewModel`):**
  - **Açık Tema Dönüşümü:** Eski koyu tema renkleri tamamen temizlendi, Faz 3.1 laboratuvar açık temasına dönüştürüldü.
  - **Canlı Supabase Metrikleri:**
    - `ExperimentRepository` üzerinden anlık çekilen gerçek deney sayısı (**40 Gerçek Deney**).
    - PDF tam metni çıkarılan gerçek kayıt sayısı (**29 Tam Metin**).
    - AI analizi yapılan ve risk skoru üretilen kayıt sayısı.
    - Manuel inceleme gerektiren taranmış kayıt sayısı (**8 İnceleme Gereken**).
  - **Veri Kaynakları (Data Sources):**
    - **NASA NTRS (NASA Technical Reports Server):** Resmi API ve PDF raporları üzerinden çekilen yayınlanmış teknik raporlar ve makaleler (`https://ntrs.nasa.gov` bağlantısı).
    - **NASA PSI (Physical Sciences Informatics):** ISS ve mikroyerçekimi araçlarının ham sensör, video ve telemetri veri ambarı (`https://psi.nasa.gov` bağlantısı).
  - **Gerçek Deney Serileri:** FLEX, SAFFIRE, BASS programlarının gerçek kayıt sayıları ile gösterimi.
  - **Sıfır Mock Veri İlkesi:** Tüm veriler doğrudan Supabase `experiments` ve `experiment_details_view` üzerinden beslenir.
- **Derleme Doğrulaması:**
  - `:shared:compileKotlinWasmJs`, `:shared:compileAndroidMain` ve `:webApp:compileKotlinWasmJs` %100 başarıyla derlendi.

## 11. Karşılaştırmalı Grafik (Faz 4.1 - ComparativeChartCard)

- **Veritabanı & Şema Güncellemesi (`supabase/migrations/20260929000003_add_comparative_data.sql`):**
  - `experiment_summaries` tablosuna sayısal karşılaştırma alanları eklendi:
    - `one_g_value NUMERIC(10, 4)`: 1g Dünya ortamı ölçüm değeri (ör. alev yayılma hızı mm/s).
    - `micro_g_value NUMERIC(10, 4)`: Mikroyerçekimi ($\mu g$) ölçüm değeri.
    - `comparison_unit TEXT`: Ölçü birimi (ör. "mm/s", "cm/s").
    - `comparison_metric_name TEXT`: Ölçülen parametre (ör. "Alev Yayılma Hızı").
    - `comparison_notes TEXT`: NASA raporundan doğrulanmış bağlam alıntısı.
  - `experiment_details_view` görünümü bu alanları dışa aktaracak şekilde güncellendi.
- **Kurala Uygun Görsel Bileşen (`ComparativeChartCard.kt`):**
  - **Sayısal Değer Varsa:**
    - Parametre başlığı ve birim rozeti.
    - Fark değişim yüzdesi rozeti (ör. `▼ %64.2 Azalma` ya da `▲ +%18.5 Artış`).
    - 1g Dünya (`AccentTeal` #1E88C7) vs Mikroyerçekimi $\mu g$ (`AccentOrangeRed` #E8572E) sütun karşılaştırmalı animasyonlu bar grafiği.
    - NASA raporundan doğrulanmış sayısal alıntı kutusu.
  - **Sayısal Değer Yoksa (Kesin Kural):**
    - Asla sayı veya grafik uydurulmaz.
    - Şeffaf ve net laboratuvar bildirim kutusu gösterilir:
      *"Bu deney için karşılaştırmalı sayısal veri çıkarılamadı — NASA NTRS rapor metninde hem 1g hem de mikroyerçekimi ortamına ait doğrudan karşılaştırılabilir bir sayısal ölçüm değeri tespit edilememiştir. Bilimsel doğruluk ilkesi gereğince tahmin veya uydurma sayı üretilmemiştir."*
- **Ekran Entegrasyonu:**
  - `ExperimentDetailScreen.kt` içinde hem geniş ekran hem de mobil dikey kaydırma listesine entegre edildi.
- **Derleme Doğrulaması:**
  - `:shared:compileKotlinWasmJs`, `:shared:compileAndroidMain`, `:webApp:compileKotlinWasmJs`, `:shared:compileKotlinJvm` ve `:ingestion:compileKotlin` %100 başarıyla derlendi.

---

## 12. Supabase SQL Migration Komutları (Faz 4.1)

```sql
-- 1. 'experiment_summaries' Tablosuna 1g vs Mikroyerçekimi Sayısal Karşılaştırma Alanlarını Ekle
ALTER TABLE public.experiment_summaries
    ADD COLUMN IF NOT EXISTS one_g_value NUMERIC(10, 4),
    ADD COLUMN IF NOT EXISTS micro_g_value NUMERIC(10, 4),
    ADD COLUMN IF NOT EXISTS comparison_unit TEXT,
    ADD COLUMN IF NOT EXISTS comparison_metric_name TEXT,
    ADD COLUMN IF NOT EXISTS comparison_notes TEXT;

-- 2. 'experiment_details_view' Görünümünü Karşılaştırmalı Alanlarla Güncelle
CREATE OR REPLACE VIEW public.experiment_details_view
WITH (security_invoker = true) AS
SELECT
    e.id,
    e.title,
    e.series,
    e.material,
    e.gravity_level,
    e.oxygen_concentration,
    e.year,
    e.source_url,
    e.source_type,
    e.raw_text,
    e.needs_manual_review,
    e.created_at,
    s.id AS summary_id,
    s.ai_summary,
    s.risk_score,
    s.risk_label,
    s.key_finding,
    s.status AS summary_status,
    s.one_g_value,
    s.micro_g_value,
    s.comparison_unit,
    s.comparison_metric_name,
    s.comparison_notes
FROM public.experiments e
LEFT JOIN public.experiment_summaries s ON e.id = s.experiment_id;

GRANT SELECT ON public.experiment_details_view TO anon, authenticated, service_role;
```

## 13. Zaman Çizelgesi Ekranı (Faz 4.2 - TimelineScreen & TimelineViewModel)

- **Mimari & Veri Akışı (`TimelineViewModel.kt`):**
  - Supabase `experiments` ve `experiment_details_view` görünümündeki gerçek `year` bilgisine sahip deneyleri çeker ve kronolojik gruplara (`YearTimelineGroup`) ayırır.
  - Dinamik program/seri filtresi (FLEX, SAFFIRE, BASS vb. - tamamen veritabanından dinamik türetilir).
  - İki yönlü sıralama desteği: *Eskiden Yeniye (Artan)* ve *Yeniden Eskiye (Azalan)*.
  - Tarihsiz deneyler için ayrı `UndatedSection` bölümü.
- **Adaptive Zaman Çizelgesi Arayüzü (`TimelineScreen.kt`):**
  - **Üst Panel:** Filtreler, gerçek kayıt sayacı rozeti ("40 Gerçek Deney"), sıralama butonu ve yenileme.
  - **Yıl Atlama Şeridi (Jump Pills):** Sayfa üstünde yatay kayan yıl hapları (`[2021]`, `[2019]`, `[2016]`, `[2014]`, `[2011]`, `[2009]`), tıklandığında ilgili yıla animasyonlu kaydırma sağlar.
  - **Zaman Çizelgesi Omurgası (Timeline Spine):**
    - Sol tarafta parlayan dairesel nodlar (`CircleShape`, `AccentOrangeRedMuted` ve `AccentOrangeRed` iç nokta) ve dikey bağlayıcı çizgi (`LabBorder`).
    - Her yıl için belirgin başlık ("2016 • 8 Deney Kaydı").
  - **Gerçek Deney Kartları (`TimelineExperimentCard`):**
    - Deney başlığı, seri rozeti (`FlameSeriesBadge`), malzeme rozeti (`FlameMaterialBadge`), risk seviyesi rozeti (`FlameRiskBadge`).
    - Gerçek AI özeti veya NTRS ham metin özeti.
    - Yerçekimi seviyesi, oksijen konsantrasyonu ve harici NASA NTRS bağlantısı (`LocalUriHandler`).
    - Karta tıklandığında doğrudan `ExperimentDetailScreen` ekranına geçiş.
- **Navigasyon Entegrasyonu:**
  - `NavTab.TIMELINE` sekmesi (`"Zaman Çizelgesi"`, `"Yıllara Göre Deney Kronolojisi"`) eklendi ve `MainAdaptiveScreen` içerisinde tam entegre edildi.
- **Derleme Doğrulaması:**
  - `:shared:compileKotlinWasmJs`, `:shared:compileAndroidMain`, `:webApp:compileKotlinWasmJs`, `:shared:compileKotlinJvm` ve `:ingestion:compileKotlin` %100 başarıyla derlendi.

## 14. Rapor Oluşturucu (Faz 4.3 - Report Generator & Supabase Storage)

- **Mimari & Edge Function (`supabase/functions/generate-report/index.ts`):**
  - Deno / TypeScript tabanlı Supabase Edge Function (`generate-report`).
  - Gelen `experiment_ids` dizisini alır, `experiment_details_view` üzerinden gerçek NASA verilerini (başlık, seri, malzeme, AI özeti, risk skoru, ana bulgu, güvenlik çıkarımı, kaynak bağlantısı) sorgular.
  - Açık tema tasarım sistemimizle (`#F7F8FA`, `#FFFFFF`, `#1A1D20`, `#E8572E`, `#1E88C7`, `#2E9E6C`, `#D64545`) birebir uyumlu, yazdırılabilir (`@page { margin: 1.5cm; }` ve `window.print()` destekli) zengin bir HTML/PDF raporu derler.
  - Derlenen raporu Supabase Storage altındaki `reports` bucket'ına yükler (`reports/{report_id}.html`) ve public erişim/indirme URL'si üretir (Fallback olarak Base64 Data URI içerir).
- **Supabase Storage Kurulumu & SQL Göçü (`20260929000004_create_reports_bucket.sql`):**
  - `reports` isimli 10 MB limitli public bucket oluşturuldu.
  - Anon ve authenticated kullanıcılar için SELECT ve INSERT RLS güvenlik politikaları tanımlandı.
- **KMP Katmanı & Ağ İstekleri:**
  - `ReportResult.kt`: `ReportRequest` ve `ReportResult` veri modelleri (`report_id`, `report_url`, `filename`, `title`, `experiment_count`, `created_at`).
  - `ReportRepository.kt` & `ReportRepositoryImpl.kt`: Ktor HTTP client üzerinden Supabase Edge Function URL'ine (`/functions/v1/generate-report`) JSON POST isteği gönderir.
  - `GenerateReportUseCase.kt`: Seçilen deney ID'lerini doğrulayıp rapor üretim akışını yönetir.
  - Koin DI modülüne (`AppModule.kt`) eklendi.
- **Kullanıcı Arayüzü & İndirme Entegrasyonu (`ExperimentListScreen.kt` & `ExperimentListViewModel.kt`):**
  - **Çoklu Seçim Desteği:** Her deney kartına (`ExperimentItemCard`) şık bir seçim onay kutusu eklendi; seçilen kartlar `AccentOrangeRed` kenarlık ve hafif arka plan tonlamasıyla vurgulanır.
  - **Kayan Rapor Eylem Çubuğu:** Deney seçildiğinde ekranın altından animasyonla açılan bir floating bar:
    - `"X Deney Seçildi"` sayacı
    - `"Temizle"` vazgeçme butonu
    - `"Seçilenlerden Rapor Oluştur"` birincil eylem butonu (yükleme durumunda `"Rapor Derleniyor..."` göstergesi).
  - **Platforma Uygun Dosya İndirme / Görüntüleme:**
    - Rapor hazırlandığında kullanıcıya modal bilgi diyaloğu sunulur.
    - Compose Multiplatform'un `LocalUriHandler` mekanizması kullanılarak üretilen güvenli Supabase indirme linki Web (Wasm), Masaüstü (JVM) ve Mobil (Android) platformlarında doğrudan tarayıcı/indirme yöneticisinde açılır.
- **Derleme Doğrulaması:**
  - `:shared:compileKotlinWasmJs`, `:shared:compileAndroidMain`, `:webApp:compileKotlinWasmJs`, `:shared:compileKotlinJvm`, `:desktopApp:compileKotlin` ve `:ingestion:compileKotlin` sıfır hatayla başarıyla derlendi.

### Supabase Storage Kurulum SQL Komutları (Faz 4.3):

```sql
-- 1. 'reports' Depolama Kovasını (Storage Bucket) Oluştur
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'reports',
    'reports',
    true,
    10485760, -- 10 MB Maksimum Dosya Boyutu
    ARRAY['text/html', 'application/pdf', 'application/json']
)
ON CONFLICT (id) DO UPDATE SET
    public = true,
    file_size_limit = 10485760;

-- 2. Depolama Güvenlik Politikaları (RLS)
DROP POLICY IF EXISTS "Allow public read on reports" ON storage.objects;
CREATE POLICY "Allow public read on reports"
    ON storage.objects
    FOR SELECT
    TO anon, authenticated
    USING (bucket_id = 'reports');

DROP POLICY IF EXISTS "Allow insert on reports" ON storage.objects;
CREATE POLICY "Allow insert on reports"
    ON storage.objects
    FOR INSERT
    TO anon, authenticated, service_role
    WITH CHECK (bucket_id = 'reports');
```

---

## 15. Sıradaki Adımlar

1. **Web Dashboard'un Canlı Önizlemesi:**
   - `.\gradlew.bat :webApp:wasmJsBrowserDevelopmentRun` ile Compose Wasm web dashboard'un tarayıcıda canlı açılması ve test edilmesi.
2. **Kalan Ham Metinlerin Embed & Summarize Edilmesi:**
   - Kullanıcı `OPENAI_API_KEY` sağladığında `:ingestion` CLI'si üzerinden kalan 29 NASA deneyinin chunk embedding'lerini ve karşılaştırmalı sayısal alanları içeren AI özetlerini Supabase'e işlemek.
3. **Supabase Edge Function Dağıtımı:**
   - `supabase functions deploy generate-report --no-verify-jwt` ve `supabase functions deploy ask --no-verify-jwt` ile fonksiyonları canlı Supabase ortamına dağıtmak.

# FlameFall – NASA %100 Uyum: Uygulama Promptları

> Her prompt `GEMINI.md` kurallarına tabidir: kapsam dışına çıkma, onaysız silme yok, **mock/hardcoded veri yok**, Web/Desktop/Android'de aynı tasarım sistemi (Faz 3.1 açık tema, `Flame*` bileşenleri).
> Promptları **sırayla**, birer birer ver. Her prompttan sonra `:shared:compileKotlinWasmJs`, `:shared:compileAndroidMain` ve `:webApp:compileKotlinWasmJs` derlenmeli.

---

## ✅ HAZIR – Veri katmanı (Claude tarafından yapıldı)

| Dosya | İçerik |
|---|---|
| `supabase/migrations/20261113000000_psi_investigations.sql` | Yeni şema: `psi_investigations`, `psi_test_conditions`, `psi_publications`, `psi_relations`, `habitat_profiles`, `fire_incidents`, `earth_applications`, `psi_investigation_overview` görünümü, `rank_investigations_for_habitat()` RPC, `experiments.psi_id` kolonu, RLS |
| `supabase/migrations/20261113000001_psi_seed.sql` | NASA PSI'dan çekilen **24 yanma deneyi**, **2.009 test satırı** (Experimental Table CSV'leri), **352 yayın**, ilişkiler, habitat ön ayarları, kazalar, Dünya uygulamaları. Ayrıca 24 PSI kaydını mevcut `experiments` tablosuna `source_type='psi'` olarak ekler. Tekrar çalıştırılabilir, NTRS kayıtlarına dokunmaz. |
| `data/psi_raw.json` | PSI API'sinden ham veri (yeniden üretmek için) |

**Kurulum (Supabase SQL Editor):** önce `20261113000000_psi_investigations.sql`, sonra `20261113000001_psi_seed.sql` çalıştır.

---

## PROMPT 1 – KMP veri modeli ve repository

```
Flame in Freefall projesinde Supabase'e yeni PSI tabloları eklendi (supabase/migrations/20261113000000_psi_investigations.sql). Bu şemaya göre shared modülünde veri katmanını kur:

1. models/ altına @Serializable modeller:
   - PsiInvestigation (psi_investigation_overview görünümündeki tüm alanlar: psi_id, acronym, title, project_type, sub_research_area, platform, hardware, hardware_group, start_date, end_date, start_year, doi, data_size_bytes, nasa_center, objective, approach, hypothesis, research_impacts, safety_category, fuel_state, fuels[], diluents[], sample_geometry, o2_min, o2_max, pressure_min_atm, pressure_max_atm, flow_min_cms, flow_max_cms, sensors[], relevance_spacecraft, relevance_station, relevance_lunar_base, relevance_mars_base, relevance_rover, relevance_note, earth_applications[], source_url, test_count, publication_count, related_to (json), related_from (json))
   - PsiTestCondition (id, psi_id, source_file, row_index, data: JsonObject)
   - PsiPublication (id, psi_id, title, doi)
   - HabitatProfile (id, name, o2_percent, pressure_psi, pressure_atm, gravity_g, note)
   - HabitatRankingItem (rank_investigations_for_habitat RPC dönüş alanları)
   - FireIncident, EarthApplication
   - enum SafetyCategory (prevention, detection, suppression, fundamental, modeling) ve FuelState (solid, liquid_droplet, gas, aerosol) – Türkçe görünen etiketleriyle.
2. domain/repository/PsiRepository arayüzü + data/repository/PsiRepositoryImpl (supabase-kt Postgrest):
   - getInvestigations(filter: PsiFilter)  -> psi_investigation_overview (safety_category, fuel_state, hardware_group, o2 aralığı, basınç aralığı, arama metni)
   - getInvestigation(psiId), getTestConditions(psiId), getPublications(psiId)
   - getHabitatProfiles(), rankForHabitat(habitatId, o2Percent?, pressureAtm?) -> rpc("rank_investigations_for_habitat")
   - getFireIncidents(), getEarthApplications()
   - getFilterOptions(): kategori, yakıt durumu, donanım grubu listeleri VERİTABANINDAN dinamik
3. Her biri için UseCase ve Koin kaydı (AppModule.kt).
Mock veri yazma. Sadece bu veri katmanını ekle, UI'a dokunma.
```

## PROMPT 2 – Keşfet ekranı: PSI kataloğu

```
ExperimentListScreen'e PSI desteği ekle (mevcut NTRS akışını silme):
1. Üstte "Kaynak" segmenti: "NASA PSI Deneyleri" (varsayılan) | "NTRS Makaleleri".
2. PSI modunda sol filtre paneli (geniş ekran) / açılır panel (mobil):
   - Güvenlik kategorisi (Önleme / Algılama / Söndürme / Temel / Modelleme) çoklu seçim
   - Yakıt durumu (Katı / Sıvı damla / Gaz / Aerosol)
   - Donanım grubu (ACME/CIR, MDCA/CIR, MSG akış kanalı, Saffire/Cygnus, SAME, Yer/Hesaplama)
   - O₂ % aralık kaydırıcısı (0–100) ve basınç atm aralık kaydırıcısı (0–5): deneyin test aralığı seçilen aralıkla kesişiyorsa göster
   - Platform: Uçuş (ISS/Cygnus) / Yer çalışması
3. PSI kartı (FlameCard): PSI ID + kısaltma rozeti, başlık, kategori rozeti (renkli), yakıt durumu, O₂ aralığı, basınç aralığı, akış hızı çipleri, test sayısı, yayın sayısı, DOI. Karta tıklayınca PsiDetailScreen.
4. Kartlarda çoklu seçim (en fazla 3) -> alttaki kayan çubuğa "Karşılaştır" butonu ekle (rapor butonu kalsın).
Tüm değerler PsiRepository'den gelsin.
```

## PROMPT 3 – PSI detay ekranı

```
Yeni PsiDetailScreen + ViewModel oluştur (ExperimentDetailScreen'in tasarım dilini kullan):
1. Başlık şeridi: PSI ID, kısaltma, başlık, kategori rozeti, platform, donanım, tarih aralığı, NASA merkezi, veri boyutu (GB/TB), DOI bağlantısı ve "Kaynağı aç" (source_url).
2. Kartlar: Hedefler, Yaklaşım, Hipotez, Araştırma Etkileri / Dünya Faydaları (genişletilebilir metin).
3. "Test Koşulları" kartı: yakıtlar, seyrelticiler, numune geometrisi, O₂ min–max, basınç min–max, akış min–max, sensör listesi (çip).
4. "Deney Matrisi" kartı: psi_test_conditions satırlarını tablo olarak göster. Kolonlar JSON anahtarlarından DİNAMİK oluşturulsun; birden fazla source_file varsa sekme. Sıralama, yatay kaydırma, sayfalama (50 satır). "CSV indir" ve "JSON indir" butonları (modelleme için veri dışa aktarma) – platforma uygun indirme/paylaşım.
5. "Habitat Uygunluğu" kartı: uzay aracı / istasyon / Ay üssü / Mars üssü / rover için 0–10 çubukları + relevance_note.
6. "İlişkili Deneyler": related_to / related_from listesi (ilişki türü Türkçe: "verisini analiz eder", "devam çalışması", "yeniden uçuş", "aynı donanım", "risk azaltma"), tıklanınca o deneyin detayı.
7. "Yayınlar": başlık + DOI bağlantısı (https://doi.org/...).
8. "Bu deney hakkında AI'a sor" butonu: AskPanel'i bu deneyin PSI ID ve başlığıyla önceden doldurulmuş soruyla açar.
```

## PROMPT 4 – Habitat Risk Paneli (NASA hedef 1)

```
RiskRankingScreen'i "Habitat Risk Paneli"ne dönüştür (malzeme bazlı eski görünümü ikinci sekme olarak koru):
1. Habitat ön ayar çipleri habitat_profiles tablosundan: Dünya, ISS, Uzay giysisi, Keşif 10.2 psia, Ay üssü/rover, Keşif 8.2 psia, Mars üssü. Seçili ön ayarın O₂ %, basınç (psi/atm) ve yerçekimi değerleri gösterilsin.
2. "Özel atmosfer": O₂ % kaydırıcısı (15–100) ve basınç kaydırıcısı (psi 3–15, atm karşılığı yanında).
3. rank_investigations_for_habitat RPC sonucu sıralı liste: sıra no, PSI ID, başlık, kategori rozeti, skor çubuğu, "O₂ aralığını kapsıyor" / "Basınç aralığını kapsıyor" yeşil-gri rozetleri, relevance_note.
4. O₂–Basınç grafiği (Compose Canvas, ortak kod): x = basınç (atm), y = O₂ %. Her deney, test aralığını gösteren yarı saydam dikdörtgen (aralık yoksa nokta); renk = güvenlik kategorisi. Seçili habitat noktası kırmızı artı işaretiyle; "Ay üssü bölgesi" (O₂ ≥ 30%, basınç ≤ 10.2 psi = 0.694 atm) taralı alan. Dikdörtgene dokununca detay.
5. "AI açıklaması" butonu: ask Edge Function'a "<habitat adı> (O₂ %X, Y psi) için yangın güvenliği açısından en önemli PSI deneyleri hangileri ve neden?" sorusunu, sıralamadaki ilk 5 deneyin PSI ID'leriyle gönder; cevabı kaynaklarıyla göster.
6. Uyarı kutusu: "PSI'da kısmi yerçekimi (Ay 0.16 g / Mars 0.38 g) yanma verisi henüz yok. Sonuçlar mikroyerçekimi ve 1 g verisiyle sınırlanmıştır; E-FIELD Flames elektrik alanla kısmi yerçekimini taklit eder. LUCI ve FM2 deneyleri gelecekte eklenecek."
```

## PROMPT 5 – Karşılaştırma ekranı (NASA: benzerlik/fark)

```
Yeni CompareScreen + ViewModel: Keşfet'ten seçilen 2–3 PSI deneyi yan yana.
1. Satırlar: PSI ID/kısaltma, kategori, yakıt durumu, yakıtlar, seyrelticiler, numune geometrisi, O₂ aralığı, basınç aralığı, akış aralığı, sensörler, donanım grubu, platform, tarih, test sayısı, yayın sayısı, habitat skorları.
2. Değerler farklıysa hücre turuncu çerçeve, aynıysa yeşil "ortak" işareti. Üstte "Ortak noktalar" ve "Farklar" özet çipleri (kod ile hesaplanır).
3. "AI karşılaştırma özeti" butonu: ask fonksiyonuna seçili deneylerin ID'leriyle soru gönder, kaynaklı cevabı göster.
4. Geniş ekranda tablo, mobilde deney başına sekmeli kart.
5. Navigasyona "Karşılaştır" sekmesi (NavTab) ekle.
```

## PROMPT 6 – Zaman çizelgesi, Dünya uygulamaları, Hakkında

```
1. TimelineScreen: ikinci iz olarak fire_incidents (Apollo 1 1967, Apollo 13 1970, Mir 1997, Artemis II) kırmızı nodlarla, PSI deneyleri ise start_date–end_date çubuklarıyla aynı eksende. Filtre: "Kazalar / PSI deneyleri / NTRS".
2. Yeni "Dünya Uygulamaları" sekmesi (EarthApplicationsScreen): earth_applications kartları (başlık, açıklama, ilgili PSI deneyleri rozetleri tıklanabilir, kaynak linki) + psi_investigations.earth_applications alanlarından deney bazlı liste.
3. AboutScreen: veri kaynağı istatistiklerine PSI sayıları (deney, test satırı, yayın), "Kapsam dışı: roket/itki yangınları" notu, kısmi yerçekimi notu, PSI atıf kuralı ("PSI verisi kullanılırken DOI ile atıf zorunludur").
4. generate-report Edge Function ve rapor ekranı: PSI deneyleri seçildiğinde rapora PSI ID, DOI, kategori, test koşulları ve habitat skorlarını ekle.
```

## PROMPT 7 – Canlıya alma ve doğrulama

```
1. Supabase'de iki migration dosyasını sırayla çalıştır (SQL Editor).
2. ingestion CLI: OPENAI_API_KEY ile `embed` ve `summarize` komutlarını çalıştır; source_type='psi' olan 24 kayıt dahil tüm raw_text kayıtları işlensin.
3. `supabase functions deploy ask --no-verify-jwt` ve `supabase functions deploy generate-report --no-verify-jwt`.
4. `.\gradlew.bat :webApp:wasmJsBrowserDistribution` ile web çıktısını al ve yayınla (Netlify/Vercel/Supabase Storage).
5. Kontrol listesi: Keşfet'te 24 PSI deneyi, Habitat panelinde Ay üssü seçince ilk sırada ACME BRE (PSI-20) ve FLEX (PSI-69), detayda deney matrisi + CSV indirme, Karşılaştır ekranı, AI cevaplarında PSI ID/DOI kaynakları.
```

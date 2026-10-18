# Telco CRM — Sprint Roadmap (v2, MVP-first)

**Repo:** https://github.com/Erenkta/telco-crm
**Referans repo:** https://github.com/orhangolcur/telco-crm (fork of arshmed/telco-crm). Sadece mimari kararlar ve klasör yapısı için bakılır, kod kopyalanmaz.
**Bu dokümanın tarihi:** 17 Ekim 2026
**Önceki sürüm:** 17 fazlık "bitir" odaklı roadmap. Bu sürüm onun yerini alır; eski fazların nereye taşındığı Bölüm 12'de.

---

## 0. Bu Doküman Nasıl Kullanılır?

Bu doküman **ne zaman, neyi** yapacağını söyler. **Nasıl** yapacağın sana ait. Bir sprintte bir görev muğlak geliyorsa önce bu dokümandaki "Karar" ve "YAPMA" satırlarına bak; cevap çoğunlukla orada. Hâlâ belirsizse kendi kararını ver, bir ADR'ye (Bölüm 3) yaz ve devam et. Belirsizlik yüzünden durmak, yanlış karar vermekten daha pahalı.

**Hedef değişti:** "17 fazı bitirmek" değil, **uçtan uca çalışan bir sürüm (MVP) çıkarmak**, sonra onu genişletmek. Neden:

| İlanlarda tekrar eden eksik      | MVP'de bunu kapatan iş                                                                                     |
| -------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| Profesyonel mikroservis deneyimi | 3 iş servisi + gateway + discovery + config, servis başına DB, senkron (Feign) + asenkron (Kafka) iletişim |
| Test                             | Mockito birim testleri + Testcontainers ile Postgres ve Kafka entegrasyon testleri                         |
| CI/CD                            | GitHub Actions: her push'ta build + test, main'e merge'de Docker image üretimi (GHCR)                      |

MVP bittiğinde CV'deki telco-crm satırı "devam ediyor" olmaktan çıkar ve `v1.0.0` etiketli, `docker compose up` ile ayağa kalkan, pipeline'ı yeşil bir projeye dönüşür. Keycloak senkronizasyonu, Saga, Debezium, ArchUnit gibi konular MVP sonrası "v1.x / v2" genişlemeleridir.

### Tempo varsayımı
- Haftada **4-6 saat** (önceki roadmap'teki bütçen). Bir sprint = **2 hafta ≈ 10 saat**.
- Tarihler bu tempoya göre hesaplandı. Haftada 10+ saat ayırabilirsen sprintleri 1 haftaya sıkıştır; **sıra değişmez**, sadece takvim kısalır.
- Sprint 1 başlangıcı: **Pazartesi 19 Ekim 2026**.

### Üç altın kural
1. **Kapsam büyümez, sadece kayar.** Sprint biterken iş bitmediyse sprint uzatılmaz; kalan iş bir sonraki sprintin başına ya da tampon sprinte gider. Yeni fikirler Bölüm 11'deki "Park Alanı"na yazılır, MVP'ye eklenmez.
2. **Main her zaman yeşil.** Sprint 2'den itibaren CI kırmızıyken yeni özellik yazılmaz; önce CI düzeltilir.
3. **"Bu neden böyle?" sorusuna cevap veremediğin görevi bitmiş sayma.** Her sprintin "Kazanımlar" bölümündeki mülakat cümlesini kendi ağzınla kurabiliyor olmalısın.

---

## 1. Mevcut Durum (17 Ekim 2026 itibarıyla repo incelemesi)

**Tamamlanan:**
- Faz 0: Maven multi-module parent POM (Spring Boot 4.1.0, Spring Cloud 2025.1.2, Java 21), Docker Compose iskeleti (Postgres 16 + Redis 7.2).
- Faz 1: `naming-server` (Eureka), `config-server`, Dockerfile'lar, hello-service ile doğrulama (sonra silindi), Faz 1 notların (`md-infos/.../FAZ 1 Notlar.md`).
- Faz 2 (kısmen): `identity-service` entity'leri (User, Role, Permission, UserRole, RolePermission, IdentityAuditLog, Outbox, ProcessedEvent, BaseEntity), `V1__init.sql`, DTO'lar, MapStruct mapper'lar, repository/service/controller katmanı.
- `gateway` modülü: sadece boş iskelet (Application sınıfı + properties).

**İncelemede bulunan teknik borçlar (Sprint 1'in ilk işi bunlar):**
1. `identity-service-impl` branch'inde main'e merge edilmemiş 2 commit var (service/repository/DTO'lar ve `.gitignore` güncellemesi). Main'de identity-service'in sadece entity'leri görünüyor.
2. Parent `pom.xml` içindeki `<modules>` listesinde `identity-service` yok. Kökten `mvn clean install` identity-service'i derlemiyor.
3. `docker-compose.yml` Postgres'e `./scripts/init.sql` mount ediyor ama repoda `scripts/` klasörü yok.
4. Compose'da config-server `SPRING_PROFILES_ACTIVE=git` ile çalışıyor, eski roadmap "native" diyor; identity-service'te ise `spring.cloud.config.enabled=false` var ve aynı property'ler hem servis içinde hem `config-files/` altında kopyalanmış. Yani config-server şu an fiilen kullanılmıyor.
5. `.idea/` klasörü repoya commit'lenmiş, `.gitignore`'da yok.
6. `outbox` tablosunda yayınlanma durumunu tutan bir alan (örn. `published_at`) ve event tipi alanı yok. Sprint 5'te gerekecek.
7. `uuid-ossp` extension'ı oluşturuluyor ama kullanılmıyor (`gen_random_uuid()` Postgres 13+ ile dahili).
8. Hiç test yok (sadece varsayılan `contextLoads`).

---

## 2. MVP Tanımı (v1.0.0)

### İş akışı (MVP'nin hikâyesi)
> Bir çağrı merkezi temsilcisi müşteri kaydı açar ve KYC'sini onaylar. Müşteri kaydı oluştuğunda identity-service bu müşteri için otomatik olarak bir `ROLE_CUSTOMER` kullanıcı hesabı açar (asenkron). Temsilci müşteri adına bir tarife siparişi verir; order-service siparişi kabul etmeden önce customer-service'e müşterinin KYC durumunu sorar (senkron). Onaylı bir müşterinin KYC'si sonradan geri çekilirse (örn. sahte belge tespiti), order-service o müşterinin açık siparişlerini otomatik iptal eder (asenkron).

### Hedef mimari (MVP)
```
Postman / curl
     │  Bearer JWT (Keycloak'tan)
     ▼
gateway (JWT doğrulama + routing + Redis rate limit)  ──► Keycloak (realm import)
     │  lb:// (Eureka)
     ├──► identity-service ◄──────────────┐
     ├──► customer-service ──outbox──► Kafka ──► identity-service (CustomerRegistered → user aç)
     │          ▲                          └──► order-service    (CustomerKycRejected → siparişi iptal)
     └──► order-service ──Feign (senkron)──┘
                └──outbox──► Kafka (OrderCreated: MVP'de tüketicisi yok, v2'de payment dinleyecek)

Altyapı: naming-server (Eureka), config-server (native), Postgres (servis başına ayrı DB + kullanıcı),
         Kafka (KRaft), Kafka UI, Redis, Keycloak, Prometheus + Grafana
```

### MVP'de VAR
- Servisler: `naming-server`, `config-server`, `gateway`, `identity-service`, `customer-service`, `order-service`
- Senkron: OpenFeign (order → customer), timeout'lu
- Asenkron: Kafka + Transactional Outbox (polling relay) + idempotent consumer + dead letter topic
- Güvenlik: Keycloak (minimal realm), gateway'de ve servislerde JWT doğrulama, rol bazlı yetki
- Test: Mockito birim testleri, Testcontainers (Postgres + Kafka) entegrasyon testleri
- CI: GitHub Actions (build + test her push'ta, image push main'de)
- Çalıştırma: tek komut `docker compose up -d`
- Gözlem: Actuator + Prometheus + Grafana (1 dashboard)
- README: mimari diyagram, çalıştırma talimatı, ADR linkleri

### MVP'de YOK (bilinçli olarak sonraya)
product-catalog-service (tarifeler order-service'te sabit tablo), Debezium CDC, Saga orkestrasyonu, subscription/usage/billing/payment/notification/ticket, Resilience4j circuit breaker, distributed tracing, Loki, BFF, frontend, Kubernetes, Keycloak Admin API senkronizasyonu, ArchUnit/Pact/Pitest.

---

## 3. Sabitlenmiş Mimari Kararlar (ADR Listesi)

Bunlar kararlaştırıldı, tekrar tartışılmaz. Her birini `docs/adr/ADR-00X-*.md` olarak **kendi cümlelerinle** yaz (Bağlam / Karar / Sonuçlar / Değerlendirilen alternatifler). ADR yazmak sprintin bir parçası; mülakatta "neden" sorusunun cevabı bu dosyalar.

| No | Karar | Hangi sprintte yazılır |
|---|---|---|
| ADR-001 | Monorepo + Maven multi-module (Faz 0 cevabın zaten hazır) | S1 |
| ADR-002 | Config server **native** profil, `config-files/` aynı repoda. Gerekçe ADR-001 ile aynı: config ve kod değişikliği tek commit'te atomik. Git backend, config ayrı repoya taşınırsa düşünülür. | S2 |
| ADR-003 | Servis başına ayrı veritabanı ve ayrı DB kullanıcısı; MVP'de **tek Postgres instance** içinde. Bir servis başka servisin DB'sine bağlanamaz (kullanıcı yetkisiyle zorla). | S1 |
| ADR-004 | Mesajlaşma: **Kafka** (KRaft, Zookeeper yok). RabbitMQ değil. Gerekçeler: referans repo uyumu, log/replay yeteneği, partition bazlı sıralama, Debezium'a geçiş yolu. | S5 |
| ADR-005 | Outbox: MVP'de **polling relay** (zamanlanmış görev), v1.x'te Debezium CDC'ye geçiş. Outbox tablosu Debezium Outbox Event Router'ın beklediği kolonlarla uyumlu tasarlanır, böylece geçiş sadece relay'i değiştirmek olur. | S5 |
| ADR-006 | Event teslim garantisi **at-least-once**; her consumer `processed_events` tablosuyla idempotent. İş değişikliği ve `processed_events` kaydı **aynı DB transaction'ında**. | S5 |
| ADR-007 | Senkron Feign sadece **sorgu/doğrulama** için (örn. "müşteri KYC onaylı mı?"). Başka servisin durumunu değiştiren her şey event ile. | S4 |
| ADR-008 | Hata formatı: **RFC 9457 / 7807 ProblemDetail**, tüm servislerde aynı yapı. | S1 |
| ADR-009 | Test stratejisi: birim (Mockito) + entegrasyon (Testcontainers, gerçek Postgres/Kafka). **H2 kullanılmaz** (Postgres'e özgü SQL ve Flyway migration'ları gerçek DB'de test edilmeli). | S2 |
| ADR-010 | Flyway: uygulanmış migration **asla düzenlenmez**, her değişiklik yeni `V{n}__*.sql`. `ddl-auto=validate` kalır. | S1 |
| ADR-011 | Kimlik: MVP'de minimal **Keycloak** (realm export/import). Kendi JWT üreticimizi yazmıyoruz. Gateway **ve** her iş servisi resource server (gateway atlatılırsa servis yine korunur). | S7 |
| ADR-012 | Gateway: Spring Cloud Gateway **reactive (WebFlux) sürümü** (referans repoyla aynı; rate limiter Redis reactive ile çalışır). | S7 |
| ADR-013 | CI: GitHub Actions, kökte `mvn -B verify`; Docker image'ları mevcut multi-stage Dockerfile'larla üretilip **GHCR**'a push edilir. | S2 / S8 |

---

## 4. Takvim Özeti

| Sprint | Tarih | Tema | Sürüm / Kilometre taşı |
|---|---|---|---|
| S1 | 19 Eki – 1 Kas 2026 | identity-service'i tamamla + teknik borç | |
| S2 | 2 – 15 Kas | Test temeli + config-server bağlantısı + CI v1 | ✅ CV checkpoint 1 |
| S3 | 16 – 29 Kas | customer-service | |
| S4 | 30 Kas – 13 Ara | order-service + Feign | |
| S5 | 14 – 27 Ara | Kafka + Outbox relay + ilk consumer | |
| S6 | 28 Ara – 10 Oca 2027 | İkinci event akışı + DLT + Kafka entegrasyon testleri | ✅ CV checkpoint 2 |
| S7 | 11 – 24 Oca | Keycloak + Gateway JWT + rate limit | |
| S8 | 25 Oca – 7 Şub | Tek komutla sistem + CI v2 + Prometheus/Grafana + README | 🚀 `v1.0.0-rc` |
| S8.5 | 8 – 21 Şub | **Tampon**: kayan işler, hata düzeltme, demo | 🚀 **`v1.0.0` MVP** — CV güncellenir |
| S9 | 22 Şub – 7 Mar | product-catalog-service + Redis cache | v1.1 |
| S10 | 8 – 21 Mar | Debezium CDC'ye geçiş | v1.2 |
| S11 | 22 Mar – 4 Nis | Resilience4j (CB, retry, bulkhead) + kaos denemesi | v1.3 |
| S12 | 5 – 18 Nis | Tracing + merkezi log + correlation-id | v1.4 |
| S13 | 19 Nis – 2 May | subscription-service + payment-service | |
| S14 | 3 – 16 May | Saga orkestrasyonu + kompanzasyon | 🚀 `v2.0.0` |
| S15 | 17 – 30 May | usage-service + billing-service | |
| S16 | 31 May – 13 Haz | notification + ticket + bir choreography akışı | |
| S17 | 14 – 27 Haz | Keycloak Admin senkronizasyonu + BFF | |
| S18 | 28 Haz – 11 Tem | Frontend (minimal) | |
| S19 | 12 – 25 Tem | Kubernetes (kind) | |
| S20 | 26 Tem – 8 Ağu | Kalite: ArchUnit, Pact, Pitest, yük testi | 🚀 `v3.0.0` |

**MVP sonrası öncelik kuralı:** S9-S12 sırası sabit (her biri mülakatta doğrudan sorulan konular). S13'ten sonrası, o anki iş başvurularına göre yeniden sıralanabilir; S17-S19 tamamen opsiyonel sayılır.

---

## 5. Her Sprintte Tekrarlanan Ritüel

**Sprint başında (15 dk):**
- GitHub'da sprint adıyla bir **Milestone** aç (örn. `S3 - customer-service`), sprintteki her görevi bir **Issue** yap.
- Branch: `sprint/S3-customer-service` (ya da issue başına `feat/...` branch, hangisini seçersen tüm projede aynı kal).

**Sprint boyunca:**
- Commit formatı: Conventional Commits (`feat(customer): ...`, `fix(identity): ...`, `test(order): ...`, `docs(adr): ...`). Zaten kısmen böyle yazıyorsun, standartlaştır.
- Her PR CI'dan geçmeden merge edilmez (S2'den itibaren).

**Sprint sonunda (30 dk):**
- PR'ı main'e merge et, milestone'u kapat.
- `md-infos/sprint-notes/S3.md` dosyası yaz: ne yaptım, nerede takıldım, ne öğrendim, bir dahakine neyi farklı yaparım. (Faz 1 notlarındaki formatın iyi, onu sürdür.)
- O sprintin **mülakat sorularını** sesli cevapla (Bölüm 10).
- Claude'a gel: "S3 tamam, PR linki şu" → birlikte review.

### Her sprint için ortak Definition of Done
- [ ] Kökten `mvn -B verify` yeşil, CI yeşil.
- [ ] Yeni servis/endpoint için en az birim test, DB veya Kafka'ya dokunan her şey için Testcontainers testi var.
- [ ] Yeni Flyway migration'ları temiz bir DB'de baştan sona çalışıyor.
- [ ] Servis `docker compose` ile ayağa kalkıyor (S8'e kadar sadece altyapı compose'da olabilir, servisler IDE'den çalışabilir).
- [ ] O sprintin ADR'si yazıldı.
- [ ] Sprint notu yazıldı.

---

## 6. MVP Sprintleri (v1.0.0)

---

### SPRINT 1 — identity-service'i Tamamla + Teknik Borç
**Tarih:** 19 Eki – 1 Kas 2026 · **Bütçe:** ~10 saat
**Eski roadmap karşılığı:** Faz 2.1 – 2.4, 2.5, 2.6

**Hedef:** identity-service'in iş mantığı tamam, temiz bir DB'de baştan ayağa kalkıyor, hatalar standart formatta dönüyor.

**Görevler:**
- [x] **Borç temizliği:** `identity-service-impl` branch'ini PR ile main'e merge et. Parent POM `<modules>` listesine `identity-service` ekle. `.idea/` klasörünü `.gitignore`'a ekleyip repodan kaldır (`git rm -r --cached`).
- [ ] **Postgres init script'i:** `scripts/init.sql` (compose zaten bunu bekliyor) oluştur. Servis başına ayrı DB + ayrı kullanıcı: şimdilik `identity`, `customer`, `orders` (`order` SQL'de rezerve kelime, DB adı `orders` olsun). Her kullanıcı sadece kendi DB'sinde yetkili (ADR-003).
- [x] **Faz 2.1 Flyway:** V1 temiz DB'de çalışıyor mu doğrula. Bundan sonraki her şema değişikliği V2+. `uuid-ossp` extension'ının gereksizliğini değerlendir (V1'i düzenleme! Gerekirse V2'de `DROP EXTENSION`). Seed rollerinin V1'de mi yoksa ayrı bir migration'da mı durması gerektiğine karar ver.
- [x] **Faz 2.2 Exception yapısı:** Kendi exception hiyerarşin (örn. `NotFound`, `Conflict`, `BusinessRuleViolation`) + `@RestControllerAdvice` + `ProblemDetail` dönüşü. Validation (`@Valid`) hataları da aynı formatta, alan bazlı detayla dönsün. Unique constraint ihlali (aynı email) 409 dönsün, 500 değil.
- [x] **Faz 2.3 User–Role ilişkisi:** `POST /api/v1/users/{id}/roles` (rol ata), `DELETE /api/v1/users/{id}/roles/{roleId}` (rol kaldır). `UserRole` join entity'si `assigned_by` ve `assigned_at` taşıyor; `assigned_by` şimdilik sabit `"system"`, S7'de JWT'den gelecek. Aynı rolü iki kez atamak 409.
- [ ] **Faz 2.4 Domain kuralları:** En az 3 iş kuralı belirle ve service katmanında uygula. Öneri: BLOCKED kullanıcıya rol atanamaz; son `ROLE_SYSTEM_ADMIN` kullanıcısından admin rolü alınamaz; silme yerine soft delete (status=INACTIVE) mı hard delete mi, karar ver ve ADR'ye yaz.
- [ ] **Faz 2.6 Sayfalama:** Kullanıcı listesi `Pageable` ile (offset). Varsayılan ve maksimum sayfa boyutu sınırı koy. Cursor pagination **bu sprintte YAPMA**, Park Alanı'na.
- [ ] **Faz 2.5 Audit (opsiyonel, zaman kalırsa):** User/Role değişikliklerinde `identity_audit_logs`'a kayıt. Kalmazsa S2'ye kayar, sorun değil.
- [ ] ADR-001, ADR-003, ADR-008, ADR-010 yaz.

**YAPMA:** Outbox'a event yazma (S5). Keycloak senkronu (S17). Test (S2'de toplu yazılacak, ama yazarken test edilebilir tasarla: constructor injection, statik çağrı yok).

**Definition of Done:** Temiz bir `docker compose down -v && up -d` sonrası identity-service IDE'den başlıyor, Flyway migration'ları uygulanıyor, Postman koleksiyonundaki tüm CRUD + rol atama istekleri çalışıyor; hatalı isteklerde ProblemDetail JSON dönüyor.

**Kazanımlar:**
- *Öğrendiğin kavramlar:* Versiyonlu şema yönetimi ve "migration immutability" ilkesi; Flyway checksum doğrulaması; ProblemDetail standardı; global exception handling; join entity'nin `@ManyToMany`'ye göre avantajı (ilişkiye özellik ekleyebilmek); soft vs hard delete trade-off'u; offset pagination'ın derin sayfalardaki maliyeti.
- *Kazandığın tecrübe:* Bir servisin hata sözleşmesini (error contract) tasarlamak; iş kurallarını controller'dan değil domain/service katmanından yönetmek; DB-per-service'i yetki seviyesinde zorlamak.
- *Mülakat cümlesi:* "Unique constraint ihlallerini DataIntegrityViolationException'dan yakalayıp 409 ProblemDetail'e çevirdim; client hiçbir zaman stack trace görmüyor ve tüm servisler aynı hata şemasını kullanıyor."

---

### SPRINT 2 — Test Temeli + Config Server Bağlantısı + CI v1
**Tarih:** 2 – 15 Kas 2026 · **Bütçe:** ~10 saat
**Eski roadmap karşılığı:** Faz 2 (test ve Eureka/config maddeleri), Faz 2.9, Faz 17 (öne çekildi)

**Hedef:** identity-service testli, config'ini gerçekten config-server'dan alıyor, her push'ta GitHub Actions çalışıyor.

**Görevler:**
- [ ] **Birim test:** identity-service service katmanı için JUnit 5 + Mockito. S1'deki her iş kuralı için en az bir pozitif ve bir negatif test.
- [ ] **Entegrasyon testi:** Testcontainers ile gerçek Postgres. Spring Boot'un `@ServiceConnection` desteğini kullan. Kapsam: Flyway migration'ları temiz DB'de çalışıyor, repository sorguları, en az bir controller'dan DB'ye uçtan uca akış (MockMvc ya da RestClient), 409/404/400 hata cevapları.
- [ ] Test container'ını testler arasında paylaşmayı (singleton container / reuse) araştır, test süresini ölç ve sprint notuna yaz.
- [ ] **Config server:** Profili `native` yap (ADR-002), `config-files/` klasörünü container'a mount et. identity-service'ten `spring.cloud.config.enabled=false` satırını kaldır, servis içindeki kopyalanmış property'leri sil; sadece `spring.application.name` ve `spring.config.import` kalsın. Testlerde config-server'a bağlanılmaması için test profili ayarla.
- [ ] identity-service'in Eureka'ya kayıt olduğunu dashboard'da doğrula.
- [ ] **CI v1:** `.github/workflows/ci.yml`: `push` ve `pull_request`'te çalışır, Java 21 (Temurin), Maven cache, kökte `mvn -B verify`. GitHub-hosted Ubuntu runner'da Docker hazır olduğu için Testcontainers ek ayar istemez.
- [ ] Test raporlarını (surefire/failsafe) artifact olarak yükle. README'ye CI badge'i ekle.
- [ ] **Branch koruma:** main'e doğrudan push'u kapat, PR + yeşil CI zorunlu olsun.
- [ ] ADR-002, ADR-009 yaz.

**YAPMA:** Docker image build/push (S8). Coverage hedefi koymak (sayıya değil anlamlı senaryolara odaklan; JaCoCo raporu eklemek serbest ama eşik koyma).

**Definition of Done:** PR açınca CI otomatik çalışıyor ve yeşil; bilinçli bozulan bir test PR'ı merge edilemiyor (bunu bir kez dene). identity-service property'lerinin tamamı config-server'dan geliyor.

**Kazanımlar:**
- *Öğrendiğin kavramlar:* Test piramidi; unit vs integration test sınırı; Testcontainers yaşam döngüsü; `@ServiceConnection`; Maven surefire vs failsafe fazları; externalized configuration; CI pipeline temel adımları, cache ve branch protection.
- *Kazandığın tecrübe:* Gerçek bağımlılıklarla tekrarlanabilir test ortamı kurmak; "bende çalışıyordu" problemini CI ile ortadan kaldırmak; bir projeye kalite kapısı (quality gate) eklemek.
- *Mülakat cümlesi:* "H2 yerine Testcontainers kullandım çünkü Flyway migration'larım Postgres'e özgü; H2'de yeşil, prod'da kırmızı bir test benim için sıfır değerde."

> **✅ CV checkpoint 1:** CV'de projeyi "Spring Boot microservices, Testcontainers, GitHub Actions CI" anahtar kelimeleriyle "in progress" olarak yazabilirsin; artık arkasında yeşil bir pipeline var.

---

### SPRINT 3 — customer-service
**Tarih:** 16 – 29 Kas 2026 · **Bütçe:** ~10 saat
**Eski roadmap karşılığı:** Faz 4 (Feign kısmı hariç)

**Hedef:** İkinci gerçek iş servisi, identity-service'te oturttuğun bütün kalıplarla (Flyway, ProblemDetail, test, config, CI).

**Görevler:**
- [ ] Modül oluştur, parent POM'a ekle, port **9002** (identity 9001, order 9003 olacak; referans repoda catalog 9003, burada sapıyoruz çünkü catalog MVP'de yok).
- [ ] Entity'ler: `Customer` (bireysel/kurumsal tipi, ad, email, telefon, durum: ACTIVE/SUSPENDED, KYC durumu: PENDING/VERIFIED/REJECTED, `@Version`), `Address` (1-N), `Document` (1-N, sadece dosya referansı/metadata; dosya yükleme YOK).
- [ ] Flyway V1 + kendi DB'si (`customer`) + kendi DB kullanıcısı.
- [ ] Endpoint'ler: müşteri CRUD (sayfalı liste, email/durum filtresi), adres ekle/sil, `POST /customers/{id}/kyc/approve`, `POST /customers/{id}/kyc/reject` (red sebebi zorunlu).
- [ ] KYC durum geçişleri: PENDING → VERIFIED, PENDING → REJECTED ve VERIFIED → REJECTED (onay geri çekme, örn. sahte belge tespiti; red sebebi zorunlu) geçerli; diğerleri (örn. REJECTED → VERIFIED) ProblemDetail ile 409/422. Bu geçişleri tek bir yerde topla (dağınık `if`'ler değil). VERIFIED → REJECTED geçişi S6'daki sipariş iptal akışının tetikleyicisi olacak.
- [ ] identity-service'teki ortak parçaları (ProblemDetail handler, BaseEntity) ikinci kez yazarken fark et: kopyala mı, ortak bir `common` modülü mü? **Karar:** MVP'de küçük bir `common-web` modülüne taşı (sadece hata formatı ve temel sınıflar; iş kodu ASLA). Bu kararın riskini (dağıtık monolit, ortak modülün herkesi bağlaması) ADR'ye yaz.
- [ ] Birim + Testcontainers testleri (S2 kalıbıyla).
- [ ] Config'i `config-files/customer-service/` altına koy, Eureka'ya kayıt.

**YAPMA:** Event yayınlama (S5), Feign (S4), Redis cache (S9), dosya depolama/MinIO (Park Alanı).

**Definition of Done:** Müşteri oluştur → adres ekle → KYC onayla → onayı geri çek akışı Postman'de çalışıyor; geçersiz geçiş (REJECTED → VERIFIED) reddediliyor; CI yeşil.

**Kazanımlar:**
- *Öğrendiğin kavramlar:* 1-N ilişki modelleme ve cascade/orphanRemoval; lazy loading ve N+1 problemi (liste endpoint'inde gerçekten yaşayıp çözersin: fetch join / entity graph / DTO projection); state machine'i basit enum geçiş tablosuyla modellemek; optimistic locking (`@Version`) ve eşzamanlı güncelleme; shared library trade-off'u.
- *Kazandığın tecrübe:* Bir kalıbı ikinci kez uygulayıp hızlanmak ve tekrar eden kodu bilinçli şekilde soyutlamak (ya da soyutlamamak).
- *Mülakat cümlesi:* "Müşteri listesinde adresler yüzünden N+1 oluştuğunu SQL loglarından gördüm ve listeleme için DTO projection'a geçtim; detay endpoint'inde ise fetch join kullandım."

---

### SPRINT 4 — order-service + Senkron İletişim (Feign)
**Tarih:** 30 Kas – 13 Ara 2026 · **Bütçe:** ~10 saat
**Eski roadmap karşılığı:** Faz 4'ün Feign kısmı + Faz 7'nin sadece Order entity'si (saga YOK)

**Hedef:** Üçüncü iş servisi; servisler arası ilk senkron çağrı ve idempotent sipariş oluşturma.

**Görevler:**
- [ ] Modül, port **9003**, DB `orders`, kendi kullanıcısı.
- [ ] Entity'ler: `Order` (customerId, planCode, tutar, durum, `@Version`, oluşturulma zamanı), `Plan` (MVP'de order-service'in kendi DB'sinde seed edilen 3-4 sabit tarife: kod, ad, aylık ücret). Plan tablosu S9'da product-catalog'a taşınacak; bunu sınıfa yorum olarak değil ADR'ye yaz.
- [ ] Sipariş durumları (MVP): `CREATED`, `CANCELLED`. (Saga durumları v2'de eklenecek, şimdi ekleme.)
- [ ] `POST /api/v1/orders`: OpenFeign ile `customer-service`'ten müşteriyi çek; müşteri yoksa 404'ü kendi 422'ne çevir, KYC VERIFIED değilse 422. `lb://` + Eureka ile çağır, URL hardcode etme.
- [ ] Feign için **connect/read timeout** ayarla ve customer-service'in kapalı olduğu durumu (bağlantı hatası, timeout) yakalayıp 503 ProblemDetail dön. Circuit breaker **bu sprintte YAPMA** (S11).
- [ ] Feign `ErrorDecoder` ile karşı servisin ProblemDetail cevabını anlamlı exception'a çevir.
- [ ] **Idempotency-Key:** `POST /orders` opsiyonel `Idempotency-Key` header'ı kabul etsin. Aynı key ile ikinci istek yeni sipariş açmasın, ilk siparişi dönsün. Aynı key farklı body ile gelirse 422. Key'i nerede ve ne kadar süre saklayacağına karar ver (DB tablosu + unique constraint önerilir; Redis alternatifi Park Alanı'nda). Bu, işteki idempotency çalışmanla aynı hikâye: mülakatta ikisini bağla.
- [ ] `GET /orders/{id}`, `GET /orders?customerId=` (sayfalı), `POST /orders/{id}/cancel`.
- [ ] Testler: Feign client'ı entegrasyon testinde **WireMock** ile taklit et (customer-service'i ayağa kaldırmadan 200/404/timeout senaryoları). Idempotency için eşzamanlı iki aynı istek testi.
- [ ] ADR-007 yaz.

**YAPMA:** Outbox (S5). Retry (Feign'de retry'ı kapalı tut: idempotent olmayan çağrıda retry tehlikeli, bunu sprint notuna yaz). Ürün kataloğu servisi (S9).

**Definition of Done:** KYC onaylı müşteri için sipariş açılıyor, onaysız için 422; customer-service durdurulunca order-service 503 dönüyor (500 değil, sonsuza kadar beklemiyor); aynı Idempotency-Key ile 2 istek tek sipariş üretiyor.

**Kazanımlar:**
- *Öğrendiğin kavramlar:* Client-side load balancing (Spring Cloud LoadBalancer + Eureka); declarative HTTP client; timeout'un neden zorunlu olduğu (thread tükenmesi, cascading failure); idempotency key tasarımı ve race condition; contract'ı WireMock ile test etmek; senkron bağımlılığın availability'yi nasıl çarptığı (iki servis %99 ise zincir ~%98).
- *Kazandığın tecrübe:* Bir servisin başka servise bağımlılığını kontrollü hale getirmek; dağıtık sistemde "aynı istek iki kez gelir" varsayımıyla tasarlamak.
- *Mülakat cümlesi:* "Sipariş oluşturma idempotent; Idempotency-Key'i unique constraint ile tutuyorum, böylece eşzamanlı iki aynı istekte bile DB seviyesinde tek kayıt garanti."

---

### SPRINT 5 — Kafka + Transactional Outbox + İlk Consumer
**Tarih:** 14 – 27 Ara 2026 · **Bütçe:** ~10-12 saat (yılbaşı haftası; gerekirse S6'ya taşma normal)
**Eski roadmap karşılığı:** Faz 6 (Debezium hariç) + Faz 2.7

**Hedef:** customer-service'te müşteri oluşunca, iş verisiyle aynı transaction'da outbox'a event yazılıyor; relay Kafka'ya taşıyor; identity-service tüketip kullanıcı açıyor. Aynı event iki kez gelse bile tek kullanıcı.

**Görevler:**
- [ ] **Altyapı:** Compose'a Kafka (KRaft modu, tek broker) ve Kafka UI (kafbat/kafka-ui) ekle. Topic'leri otomatik oluşturma yerine açıkça tanımla (partition sayısı ve neden o sayı: sprint notuna).
- [ ] **Outbox tablosu (customer-service):** V-migration ile oluştur. Debezium uyumlu kolonlar (ADR-005): `id` (event id, UUID), `aggregatetype`, `aggregateid`, `type` (event tipi, örn. `CustomerRegistered`), `payload` (JSON), `created_at`, ve polling relay için `published_at` (nullable). identity-service'teki mevcut outbox tablosunu da aynı şemaya getir (V2+).
- [ ] **Event yazma:** `createCustomer` içinde müşteri kaydı ve outbox satırı **aynı `@Transactional`** içinde. Event payload'unda: eventId, occurredAt, customerId, email, fullName. Event'i versiyonla (örn. `schemaVersion: 1`).
- [ ] **Polling relay:** Zamanlanmış görev; `published_at IS NULL` satırları sırayla al, Kafka'ya gönder, gönderim onaylanınca `published_at` set et. Birden fazla instance aynı satırı almasın: `SELECT ... FOR UPDATE SKIP LOCKED`. Kafka key = `aggregateid` (aynı müşterinin event'leri aynı partition'da, sıralı).
- [ ] **Producer ayarları:** `acks=all`, idempotent producer açık. Neden: sprint notuna.
- [ ] **Consumer (identity-service):** `CustomerRegistered` tüketir → o email ile kullanıcı yoksa `ROLE_CUSTOMER` rollü, `customer_id` dolu bir kullanıcı açar; varsa sadece `customer_id`'yi bağlar. Mevcut `processed_events` tablosuna `event_id` yazılır; iş değişikliği ile aynı transaction (ADR-006). Aynı event tekrar gelirse sessizce atlanır.
- [ ] Spring Boot 4 Jackson 3'e geçti: Kafka JSON serializer/deserializer seçerken paket ve sürüm uyumuna dikkat et (takılırsan buraya bak). Alternatif olarak payload'u String olarak gönderip consumer'da kendin parse etmek de geçerli bir MVP kararı.
- [ ] Testler: outbox satırının iş verisiyle birlikte yazıldığını ve iş hatasında **ikisinin de** rollback olduğunu test et. Consumer için aynı mesajı iki kez işleyip tek kullanıcı oluştuğunu doğrula (bu sprintte birim/DB seviyesinde; Kafka container'lı test S6'da).
- [ ] ADR-004, ADR-005, ADR-006 yaz.

**YAPMA:** Debezium (S10). Exactly-once / Kafka transactions (gerekmez; at-least-once + idempotent consumer bilinçli tercih, ADR'de neden olduğunu yaz). Schema Registry/Avro (Park Alanı).

**Definition of Done:** Postman'de müşteri oluştur → Kafka UI'da event görünür → birkaç saniye içinde identity-service'te kullanıcı oluşur. Relay'i durdurup müşteri oluştur, relay'i aç: event kaybolmadan gider. Kafka UI'dan aynı mesajı manuel tekrar gönder: ikinci kullanıcı oluşmaz.

**Kazanımlar:**
- *Öğrendiğin kavramlar:* Dual-write problemi ve neden tehlikeli olduğu; transactional outbox; at-least-once / at-most-once / exactly-once farkı; idempotent consumer; Kafka topic, partition, key, consumer group, offset commit; `acks` ve idempotent producer; `SKIP LOCKED` ile çok instance'lı iş dağıtımı; eventual consistency.
- *Kazandığın tecrübe:* DB ve message broker arasında veri tutarlılığını bozmayan bir köprü kurmak; kesinti senaryosunu bilerek üretip gözlemlemek.
- *Mülakat cümlesi:* "DB'ye yazıp ardından Kafka'ya göndermek yerine outbox kullandım; relay'i kapatıp açarak hiçbir event'in kaybolmadığını, consumer'ı aynı mesajla iki kez besleyerek de tekrar işlenmediğini gösterdim."

---

### SPRINT 6 — İkinci Event Akışı + Hata Yönetimi + Kafka Entegrasyon Testleri
**Tarih:** 28 Ara 2026 – 10 Oca 2027 · **Bütçe:** ~10 saat
**Eski roadmap karşılığı:** Faz 6'nın consumer kısmı, Faz 8'in "event consumer" kavramları

**Hedef:** Event-driven iletişim tek bir örnek olmaktan çıkıp kalıplaşıyor; zehirli mesajlar sistemi kilitlemiyor; Kafka akışları gerçek broker'la test ediliyor.

**Görevler:**
- [ ] S5 taşmasını bitir.
- [ ] customer-service KYC reddinde `CustomerKycRejected` (ve onayında `CustomerKycApproved`) event'ini outbox'a yaz.
- [ ] order-service `CustomerKycRejected` tüketir → o müşterinin `CREATED` durumdaki siparişlerini `CANCELLED` yapar (iptal sebebiyle). Pratikte bu, VERIFIED → REJECTED geri çekmesinde anlam kazanır (PENDING müşterinin zaten siparişi olamaz). Idempotent (kendi `processed_events` tablosu).
- [ ] order-service'e outbox + relay ekle; sipariş oluşunca `OrderCreated`, iptalde `OrderCancelled` yazsın. MVP'de tüketicisi yok, v2'de payment/subscription dinleyecek. Relay kodunu ikinci kez yazarken `common` modülüne alıp almama kararını ver (S3'teki ADR'nle tutarlı ol).
- [ ] **Hata yönetimi:** Consumer'da geçici hatalar için sınırlı retry + backoff, kalıcı hatalar (bozuk JSON, bilinmeyen event tipi) için **Dead Letter Topic** (`*.DLT`). Spring Kafka'nın error handler + dead letter recoverer yapısını kullan. Hangi exception'ların retry edilmeyeceğini açıkça belirle.
- [ ] Kafka UI'dan bozuk bir mesaj göndererek consumer'ın kilitlenmediğini ve mesajın DLT'ye düştüğünü gör.
- [ ] **Entegrasyon testi:** Testcontainers Kafka (+ Postgres) ile: event yayınlanır → consumer DB'yi günceller. Asenkron doğrulama için Awaitility kullan (`Thread.sleep` YASAK).
- [ ] Event sözleşmelerini `docs/events.md`'ye yaz: topic, key, payload alanları, üretici, tüketiciler.

**YAPMA:** Saga (S14). Consumer'dan Feign çağrısı (kullanıcı token'ı olmadığı için client_credentials gerekir, S13+).

**Definition of Done:** Onaylı müşterinin KYC'si geri çekilince açık siparişleri iptal oluyor; bozuk mesaj DLT'de, consumer çalışmaya devam ediyor; Kafka'lı entegrasyon testleri CI'da yeşil.

**Kazanımlar:**
- *Öğrendiğin kavramlar:* Poison message ve DLT; retryable vs non-retryable hata ayrımı; consumer offset'i ne zaman commit edilir; event sözleşmesi ve geriye uyumluluk; asenkron kodu test etmek (Awaitility); servisler arası "reaktif iş kuralı" (bir serviste olan şey diğerinde sonuç doğurur ama ikisi birbirini bilmez).
- *Kazandığın tecrübe:* Prod'da en sık yaşanan Kafka problemi olan "tek bozuk mesaj tüm partition'ı durdurdu" senaryosunu önceden çözmek.
- *Mülakat cümlesi:* "Bozuk JSON'u retry etmenin anlamı yok, onu doğrudan DLT'ye gönderiyorum; DB bağlantı hatası gibi geçici hataları ise backoff ile sınırlı sayıda deniyorum."

> **✅ CV checkpoint 2:** "Event-driven communication with Kafka, Transactional Outbox, idempotent consumers, DLT" artık kanıtlı. CV satırına ekle.

---

### SPRINT 7 — Keycloak + Gateway JWT + Rate Limit
**Tarih:** 11 – 24 Oca 2027 · **Bütçe:** ~10-12 saat (ilk zor eşik)
**Eski roadmap karşılığı:** Faz 3

**Hedef:** Tüm dış trafik gateway'den geçiyor, JWT'siz istek 401, yetkisiz rol 403, limit aşımı 429.

**Görevler:**
- [ ] **Önce kağıt:** Koda geçmeden OAuth2/OIDC akışını (token alma, JWT header/payload/signature, JWKS ile doğrulama) çiz ve sprint notuna koy. Bunu ayrı bir oturumda yap.
- [ ] Compose'a Keycloak ekle (dev modu, realm import). Realm: `telco-crm`. Realm export dosyası `infra/keycloak/realm-export.json`, repoda versiyonlanır. Not: import mevcut realm'i ezmez; dosyayı değiştirince container'ı yeniden oluşturman gerekir.
- [ ] Realm içinde: roller (`CALL_CENTER_AGENT`, `SYSTEM_ADMIN`, `CUSTOMER`), 3 test kullanıcısı, Postman için bir client. Test için password grant'i dev'de açabilirsin (prod'da kullanılmayacağını, OAuth 2.1'de kaldırıldığını sprint notuna yaz).
- [ ] **Gateway:** Reactive Spring Cloud Gateway (ADR-012). `/api/v1/users/**` → identity, `/api/v1/customers/**` → customer, `/api/v1/orders/**` → order; `lb://` ile Eureka üzerinden. Gateway OAuth2 resource server; `realm_access.roles` claim'ini Spring authority'lerine çevir.
- [ ] **Rate limit:** Redis tabanlı request rate limiter, anahtar = JWT'deki kullanıcı (`sub` veya `preferred_username`). Düşük bir limitle 429'u tetikle.
- [ ] **Servisler de resource server** (ADR-011): identity, customer, order JWT doğrular. Örnek yetki: KYC onay/red sadece `CALL_CENTER_AGENT`; rol atama sadece `SYSTEM_ADMIN`.
- [ ] `assigned_by` (S1) ve audit alanları artık JWT'deki kullanıcıdan gelsin.
- [ ] **Token relay:** order → customer Feign çağrısı gelen isteğin bearer token'ını iletsin (Feign request interceptor).
- [ ] Servislerin portlarını compose'da dışarı açma (sadece gateway, Keycloak ve UI'lar dışarıda). Böylece "gateway atlatılamaz" fiziksel olarak da doğru olur.
- [ ] Testler: güvenlikli endpoint'ler için mock JWT ile 401/403/200 testleri (Spring Security test desteği). Testcontainers ile Keycloak **zorunlu değil**.
- [ ] ADR-011, ADR-012 yaz.

**YAPMA:** BFF/session (S17). identity-service → Keycloak kullanıcı senkronu (S17). Kendi JWT imzalama kodu.

**Definition of Done:** Postman: Keycloak'tan token al → gateway üzerinden 3 servise istek at → 200. Token'sız 401, yanlış rol 403, limit aşımı 429. order → customer Feign çağrısı token ile çalışıyor.

**Kazanımlar:**
- *Öğrendiğin kavramlar:* OAuth2 rolleri (resource owner, client, authorization server, resource server); OIDC; JWT yapısı ve asimetrik imza; JWKS ve key rotation; access vs refresh token; neden JWT stateless ve iptali neden zor; edge'de vs servis içinde doğrulama (defense in depth); token relay; token bucket rate limiting; reactive vs servlet gateway farkı.
- *Kazandığın tecrübe:* Merkezi kimlik sağlayıcıya dayalı uçtan uca güvenlik zinciri kurmak ve rol bazlı yetkiyi servis sınırlarında uygulamak.
- *Mülakat cümlesi:* "Gateway JWT'yi doğruluyor ama servisler de doğruluyor; gateway bir gün yanlış konfigüre edilse ya da biri iç ağdan servise doğrudan gitse bile istek imzasız token'la geçemiyor."

---

### SPRINT 8 — Tek Komutla Sistem + CI v2 + Gözlem + README
**Tarih:** 25 Oca – 7 Şub 2027 · **Bütçe:** ~10 saat
**Eski roadmap karşılığı:** Faz 15, Faz 17'nin image kısmı, Faz 12'nin metrik kısmı

**Hedef:** Hiç görmemiş biri repoyu klonlayıp `docker compose up -d` ile tüm sistemi çalıştırabiliyor ve README'den ne yaptığını 2 dakikada anlıyor.

**Görevler:**
- [ ] Tüm servisler için Dockerfile (mevcut multi-stage kalıbınla aynı), non-root kullanıcı, JVM container ayarları (heap'in container limitine göre belirlenmesi).
- [ ] Compose: tüm servisler + altyapı; healthcheck'ler ve `depends_on: condition: service_healthy` ile doğru başlama sırası (config → naming → servisler → gateway). Ortam değişkenleri `.env.example` dosyasında; gerçek `.env` git'e girmez.
- [ ] Sıfırdan test: `docker compose down -v && docker compose up -d` → tüm healthcheck'ler yeşil → README'deki demo senaryosu çalışıyor.
- [ ] **CI v2:** main'e merge'de her servisin image'ı build edilip GHCR'a push edilsin (tag: commit SHA + `latest`; release tag'inde `v1.0.0`). PR'larda sadece build (push yok).
- [ ] **Prometheus + Grafana:** Actuator + Prometheus registry tüm servislerde; compose'a Prometheus + Grafana (provisioned datasource ve dashboard, dosyaları repoda). Dashboard'da en az: servis başına istek sayısı/hata oranı/latency (RED), JVM heap, ve **kendi custom metriğin**: yayınlanmayı bekleyen outbox satır sayısı (gauge) ve işlenen event sayısı (counter). Bu kısmı .NET'teki OpenTelemetry metrik tecrübenle kıyasla, sprint notuna yaz.
- [ ] **README:** proje amacı, Mermaid mimari diyagramı, MVP iş akışı, servis/port tablosu, "nasıl çalıştırılır" (tek komut + token alma + örnek istekler), Postman koleksiyonu (`docs/postman/`), ADR linkleri, event sözleşmeleri linki, "bilinçli olarak yapılmayanlar ve sonraki adımlar" bölümü.
- [ ] Bir **demo senaryosu** yaz (README'de adım adım): token al → müşteri oluştur → identity'de kullanıcının oluştuğunu gör → KYC onaylanmadan sipariş dene (422) → KYC onayla → iki sipariş aç → aynı Idempotency-Key ile tekrar dene (yeni sipariş yok) → KYC onayını geri çek → siparişlerin iptal olduğunu gör → Grafana'da event metriklerini gör.
- [ ] `v1.0.0-rc` tag'i at.

**YAPMA:** Kubernetes (S19). Tracing/Loki (S12). Yeni özellik.

**Definition of Done:** Temiz bir makinede (ya da tüm image/volume'ları silinmiş kendi makinende) README'yi takip ederek demo senaryosu baştan sona çalışıyor; GHCR'da image'lar var.

**Kazanımlar:**
- *Öğrendiğin kavramlar:* Container orkestrasyonunun temel problemi (başlama sırası, readiness vs liveness); 12-factor config; image tag stratejisi; container içinde JVM bellek davranışı; CD'nin ilk adımı (artifact üretimi); RED metrikleri; custom business metric.
- *Kazandığın tecrübe:* Bir sistemi "bende çalışıyor"dan "herkeste çalışıyor"a taşımak; teknik çalışmayı okuyana anlatan dokümantasyon yazmak.
- *Mülakat cümlesi:* "Outbox'ta bekleyen event sayısını gauge olarak Grafana'ya koydum; relay durduğunda bunu log'dan değil dashboard'daki yükselen çizgiden fark ediyorum."

---

### SPRINT 8.5 — Tampon + MVP Sürümü
**Tarih:** 8 – 21 Şub 2027 · **Bütçe:** ~10 saat

**Görevler:**
- [ ] Önceki sprintlerden kayan işleri bitir. Kayan iş yoksa: test eksiklerini kapat, Park Alanı'ndan **en fazla bir** küçük iş al.
- [ ] Bilinen hataları düzelt, README'yi bir kez daha baştan sona dene.
- [ ] `v1.0.0` tag'i ve GitHub Release (sürüm notu: neler var, neler bilinçli olarak yok).
- [ ] Sistemi tek servis kapatarak dene: customer-service'i durdur → hangi akışlar çalışmaya devam ediyor, hangileri 503 dönüyor, customer-service geri gelince outbox'taki event'ler gidiyor mu? Sonucu README'ye "Failure scenarios" başlığıyla yaz.
- [ ] **CV güncellemesi** (Bölüm 9).

**MVP Kazanımı (bütün olarak):** Artık "mikroservis biliyorum" değil, "üç servisli, senkron ve asenkron iletişimli, testli, CI'lı, tek komutla çalışan bir sistem kurdum ve hangi servis çökerse ne olduğunu gösterebiliyorum" diyebilirsin. İlanlardaki üç eksiğin üçü de somut bir repo ile kapandı.

---

## 7. MVP Sonrası Sprintler (v1.x → v3)

Aynı ritüel ve DoD geçerli. Detay seviyesi bilinçli olarak biraz daha düşük; her sprintin başında görevleri issue'lara bölerken kendi alt adımlarını çıkar.

---

### SPRINT 9 — product-catalog-service + Redis Cache (v1.1)
**Tarih:** 22 Şub – 7 Mar 2027 · **Eski:** Faz 5
- [ ] Yeni servis: `Tariff` ve `AddOn`, fiyat/kota alanları, **versiyonlama** (tarife fiyatı değişince yeni sürüm; eski siparişler eski sürümü referans alır).
- [ ] order-service'teki sabit `Plan` tablosunu kaldır, sipariş anında catalog'dan Feign ile tarife sürümünü al ve siparişe **fiyatın kopyasını** yaz (neden kopyası: ADR).
- [ ] Catalog okumalarında Redis cache (TTL + değişiklikte invalidation). Tarife değişikliğinde `TariffChanged` event'i (outbox).
- [ ] Gateway route'u, testler.
- **Kazanımlar:** Referans veri yönetimi, temporal/versiyonlu veri modelleme, cache-aside, TTL vs event ile invalidation, cache stampede, "veri sahipliği" ve neden siparişin fiyatı kopyalaması gerektiği. *Mülakat:* "Tarifeyi versiyonladım; fiyat değişse de geçmiş siparişin tutarı değişmiyor çünkü sipariş anlık sürümü ve fiyatı saklıyor."

---

### SPRINT 10 — Debezium CDC'ye Geçiş (v1.2)
**Tarih:** 8 – 21 Mar 2027 · **Eski:** Faz 6'nın Debezium kısmı
- [ ] Compose'a Debezium Connect; Postgres'te logical replication (`wal_level=logical`).
- [ ] Connector kayıt script'i (`infra/debezium/register-connectors.sh`), Outbox Event Router SMT ile topic yönlendirme.
- [ ] customer-service'te polling relay'i kapat, Debezium'a geçir; consumer'larda **hiçbir kod değişmemeli** (değişiyorsa S5'teki şema kararını gözden geçir).
- [ ] İki yaklaşımı ölç ve ADR-005'i güncelle: gecikme, DB yükü, operasyonel karmaşıklık, outbox tablo temizliği.
- [ ] Replication slot'un neden diski doldurabileceğini araştır, Grafana'ya ilgili metriği ekle.
- **Kazanımlar:** CDC kavramı, WAL ve logical replication, polling vs log-based CDC trade-off'u, Kafka Connect, SMT. *Mülakat:* "Önce polling relay yazdım, sonra Debezium'a geçtim; consumer'lar değişmedi çünkü outbox şemasını en baştan Outbox Event Router'a uyumlu kurmuştum."

---

### SPRINT 11 — Resilience4j + Kaos Denemesi (v1.3)
**Tarih:** 22 Mar – 4 Nis 2027 · **Eski:** Faz 11
- [ ] order → customer ve order → catalog Feign çağrılarına circuit breaker + fallback; sadece idempotent GET'lere retry (backoff + jitter).
- [ ] Bulkhead ile bir bağımlılığın tüm thread'leri tüketmesini engelle.
- [ ] CB durum metriklerini Grafana'ya ekle; bir servisi durdurup CLOSED → OPEN → HALF_OPEN → CLOSED geçişini dashboard'da izle.
- [ ] Retry + CB etkileşimini (sıralama, retry storm) bilinçli test et.
- **Kazanımlar:** Circuit breaker durumları, fallback tasarımı (sahte veri dönmek ne zaman tehlikeli), retry storm, jitter, bulkhead, timeout bütçesi. *Mülakat:* "Retry'ı sadece idempotent GET'lerde açtım ve circuit breaker'ın içine koydum; aksi halde çöken servise retry'larla yük bindiriyordum."

---

### SPRINT 12 — Observability: Tracing + Merkezi Log (v1.4)
**Tarih:** 5 – 18 Nis 2027 · **Eski:** Faz 12
- [ ] Micrometer Tracing (OpenTelemetry bridge) ile tüm servisler, Zipkin'e (ya da Grafana Tempo'ya) export.
- [ ] Trace context'in Feign **ve Kafka header'ları** üzerinden taşınması: gateway → order → customer → outbox → Kafka → identity tek trace'te görünsün (outbox'ta trace id'yi saklaman gerekecek).
- [ ] Yapılandırılmış log (Spring Boot structured logging, ECS formatı), Loki + Promtail/Alloy + Grafana; log'larda traceId ile tracing'e atlama.
- **Kazanımlar:** Observability'nin üç ayağı, trace/span, context propagation (senkron vs asenkron farkı), structured logging, korelasyon. *Mülakat:* "Asenkron akışta trace kopmasın diye trace context'i outbox satırına yazıp Kafka header'ına taşıdım."

---

### SPRINT 13 — subscription-service + payment-service
**Tarih:** 19 Nis – 2 May 2027 · **Eski:** Faz 8 (subscription), Faz 9 (payment)
- [ ] subscription-service: `OrderCreated`'da PENDING abonelik, aktivasyon/askı/sonlandırma durum makinesi, idempotent consumer.
- [ ] payment-service: mock PSP (yönteme göre farklı başarı oranı), `paymentRequestId` ile idempotency, `OrderCreated`'ı dinleyip ödeme başlatma, sonuç event'leri (`PaymentCompleted` / `PaymentFailed`).
- [ ] Kafka tetiklemeli işlerde Feign gerekiyorsa servis-servis kimlik: Keycloak `client_credentials`.
- **Kazanımlar:** Servis kimliği (machine-to-machine auth), ödeme idempotency'si, durum makinesi tekrarı. *Mülakat:* "Kullanıcı oturumu olmayan, Kafka tetiklemeli çağrılarda client_credentials ile servis token'ı alıyorum."

---

### SPRINT 14 — Saga Orkestrasyonu (v2.0.0)
**Tarih:** 3 – 16 May 2027 · **Eski:** Faz 7
- [ ] **Önce tablo:** Tüm durum geçişlerini ve her hata dalında hangi kompanzasyonun tetikleneceğini tablo olarak çıkar (`docs/saga.md`). Kod ondan sonra.
- [ ] order-service'te orkestratör: `CREATED → PENDING_PAYMENT → PAID → FULFILLED`, hata dallarında `CANCELLED` / `COMPENSATING` / `FAILED`. Saga durumu (`currentStep`, `retryCount`, `errorMessage`) sipariş cevabında görünsün.
- [ ] Kompanzasyon: abonelik aktivasyonu başarısızsa ödeme iadesi (`PaymentRefunded`).
- [ ] Takılı kalan saga'lar için zaman aşımı tarayıcısı.
- [ ] `v2.0.0` release.
- **Kazanımlar:** Distributed transaction problemi, 2PC'nin neden tercih edilmediği, orchestration vs choreography, compensating transaction ve her işlemin geri alınamayabileceği gerçeği, saga timeout. *Mülakat:* "Ödeme alındı ama abonelik aktive edilemedi senaryosunu iade kompanzasyonuyla çözdüm; saga'nın hangi adımda kaldığı sipariş üzerinde görülebiliyor."

---

### SPRINT 15 — usage-service + billing-service
**Tarih:** 17 – 30 May 2027 · **Eski:** Faz 8 (usage), Faz 9 (billing)
- [ ] usage: CDR event'lerini tüket, kota sayacı, eşzamanlı CDR'larda yarış koşulunu `SELECT ... FOR UPDATE` ile önle, %80 ve %100 eşik event'leri (her dönemde sadece **ilk** geçişte).
- [ ] billing: zamanlanmış fatura koşusu; çok instance'ta tek çalışması için ShedLock (ya da DB kilidi); `PaymentCompleted` ile faturayı PAID yap.
- **Kazanımlar:** Pessimistic vs optimistic locking, "exactly once" iş kuralını event'le sağlamak, dağıtık zamanlanmış iş. *Mülakat:* "Aynı aboneye ait eşzamanlı CDR'larda sayaç kaybını satır kilidiyle önledim ve eşik event'inin dönem başına bir kez çıkmasını garanti ettim."

---

### SPRINT 16 — notification + ticket + Choreography
**Tarih:** 31 May – 13 Haz 2027 · **Eski:** Faz 10, Bölüm 8 önerisi
- [ ] notification: platform event'lerini e-postaya çevir (Gmail app password ya da compose'da MailHog/Mailpit ile sahte SMTP; MVP sonrası için Mailpit önerilir, gerçek şifre gerektirmez).
- [ ] ticket: destek talebi CRUD + SLA ihlali tarayıcısı (çok instance'ta aynı ticket'ı iki kez yayınlamamak için `@Version`).
- [ ] Bir akışı bilinçli olarak **choreography** ile kur ve S14'teki orchestration ile karşılaştırıp ADR yaz.
- **Kazanımlar:** Fan-out tüketiciler, SLA tarama kalıbı, iki saga stilini elle deneyimleyip karşılaştırabilmek.

---

### SPRINT 17 — Keycloak Admin Senkronizasyonu + BFF (opsiyonel)
**Tarih:** 14 – 27 Haz 2027 · **Eski:** Faz 2.8, Faz 13
- [ ] identity-service → Keycloak Admin REST API ile kullanıcı/rol senkronu, best-effort (Keycloak kapalıysa kullanıcı DB'de kalır, `keycloak_user_id` sonradan dolar), circuit breaker.
- [ ] bff-server: authorization_code akışı, session + cookie, gateway'e TokenRelay, CSRF ve CORS ayarları.
- **Kazanımlar:** Kimlik verisinin iki sistemde tutarlılığı, BFF pattern, token'ın neden tarayıcıda tutulmaması gerektiği, session vs token auth.

---

### SPRINT 18 — Frontend (opsiyonel)
**Tarih:** 28 Haz – 11 Tem 2027 · **Eski:** Faz 14
- [ ] React + Vite + TypeScript + Tailwind; login, müşteri listesi/oluşturma, sipariş oluşturma, sipariş durumunu (saga adımı) gösteren sayfa. BFF üzerinden.
- **Kazanımlar:** Uçtan uca kullanıcı akışı; backend'in UX'e etkisi (eventual consistency'yi arayüzde göstermek).

---

### SPRINT 19 — Kubernetes (opsiyonel)
**Tarih:** 12 – 25 Tem 2027 · **Eski:** Faz 16
- [ ] kind ile lokal cluster; 3 servis + Postgres için Deployment, Service, ConfigMap, Secret, readiness/liveness probe, Ingress.
- [ ] Eureka ve config-server'ın K8s'te neden gereksizleşebileceğini (K8s Service discovery, ConfigMap) deneyerek ADR yaz.
- **Kazanımlar:** K8s temel objeleri, platform tarafından sağlanan discovery/config, probe tasarımı.

---

### SPRINT 20 — Kalite ve Farklılaştırıcılar (v3.0.0)
**Tarih:** 26 Tem – 8 Ağu 2027 · **Eski:** önceki Bölüm 11 ("referans reponun ötesi")
- [ ] ArchUnit: katman kuralları (controller repository'ye erişemez vb.) CI'da.
- [ ] Contract testing (Pact ya da Spring Cloud Contract): order ↔ customer sözleşmesi.
- [ ] Mutation testing (Pitest) bir serviste; test kalitesini ölç.
- [ ] Yük testi (k6 ya da Gatling): sipariş endpoint'i, rate limiter ve CB davranışı.
- [ ] Feature flag ve kaos testi (Park Alanı'ndan) zaman kalırsa.
- **Kazanımlar:** Mimari kuralları test olarak ifade etmek, servisler arası sözleşmeyi bağımsız deploy için güvenceye almak, coverage yerine test etkinliğini ölçmek, kapasite planlama.

---

## 8. Hızlı Başvuru: Port ve İsimlendirme

| Bileşen | Port | DB |
|---|---|---|
| config-server | 8888 | – |
| naming-server | 8761 | – |
| gateway | 8080 | – |
| identity-service | 9001 | `identity` |
| customer-service | 9002 | `customer` |
| order-service | 9003 | `orders` |
| product-catalog-service (S9) | 9004 | `catalog` |
| subscription / payment / usage / billing / notification / ticket | 9005 – 9010 | servis adıyla |
| Keycloak | 8085 | kendi DB'si (ayrı) |
| Kafka / Kafka UI | 9092 / 8090 | – |
| Prometheus / Grafana | 9090 / 3000 | – |

Topic isimlendirme: `<servis>.<aggregate>.<olay>` (örn. `customer.customer.registered`, `order.order.created`). Event tipleri PascalCase geçmiş zaman (`CustomerRegistered`). Bu kuralı `docs/events.md`'de sabitle.

Repo yerleşimi (hedef): servis modülleri kökte, `common-web/`, `config-files/`, `infra/` (keycloak, prometheus, grafana, debezium), `scripts/`, `docs/` (adr, events.md, saga.md, postman), `md-infos/` (roadmap + sprint notları), `.github/workflows/`.

---

## 9. CV ve README Kilometre Taşları

| Ne zaman | CV'de ne yazabilirsin |
|---|---|
| S2 sonu | "Building a Spring Boot microservices platform (in progress): Testcontainers integration tests, GitHub Actions CI" |
| S6 sonu | + "Kafka event-driven communication, Transactional Outbox, idempotent consumers, dead letter handling" |
| v1.0.0 (Şub 2027) | "devam ediyor" ibaresi kalkar. Örnek yapı: *Telco CRM — microservices platform (Java 21, Spring Boot 4, Spring Cloud). 3 business services + API gateway; sync (OpenFeign) and async (Kafka + Transactional Outbox) communication with idempotent consumers; Keycloak JWT at gateway and service level; Testcontainers integration tests; GitHub Actions CI publishing Docker images; one-command Docker Compose with Prometheus/Grafana.* Cümleleri kendin yaz, bunu kalıp olarak kullan. |
| v2.0.0 | + "Saga orchestration with compensating transactions, Debezium CDC, Resilience4j, distributed tracing" |

README bir işe alımcının okuduğu ilk ve çoğu zaman tek şey. Her release'te README'nin "Status" bölümünü güncelle.

---

## 10. Mülakat Soruları (Sprint Bazlı)

Her sprint sonunda ilgili soruları **sesli** cevapla. "Teoride" değil "projede şunu yaşadım" diye başla.

**S1-S2 (CRUD, Flyway, test, CI):**
- DTO ile Entity'yi neden ayırırız, Entity'yi doğrudan dönsek ne kaybederiz?
- Uygulanmış bir Flyway migration'ını düzenlersek ne olur?
- `@Transactional` ne zaman işe yaramaz (self-invocation, private metot, checked exception)?
- Unit ve integration test sınırını nereden çizersin? Neden H2 değil Testcontainers?
- Service discovery neden gerekli; client-side ve server-side load balancing farkı?

**S3-S4 (customer, order, Feign):**
- N+1 problemi nedir, nasıl fark edilir, hangi çözümler var ve trade-off'ları?
- Optimistic vs pessimistic locking, hangi durumda hangisi?
- Senkron servis çağrısında timeout koymazsan ne olur?
- Idempotency nedir, Idempotency-Key'i nasıl tasarlarsın, eşzamanlı aynı iki isteği nasıl ele alırsın?
- Servisler arası ortak kütüphane ne zaman faydalı, ne zaman dağıtık monolite götürür?

**S5-S6 (Kafka, outbox):**
- Dual-write problemi nedir? Outbox bunu nasıl çözer?
- At-least-once / at-most-once / exactly-once farkı; Kafka neyi garanti eder?
- Partition ve consumer group ilişkisi; aynı partition'ı aynı grupta iki consumer okuyabilir mi?
- Mesaj sıralaması nasıl garanti edilir (key seçimi)?
- Consumer mesajı işlerken çökerse ne olur? Offset ne zaman commit edilmeli?
- Poison message nedir, DLT nasıl kurgulanır?
- Eventual consistency kullanıcıya nasıl yansır?

**S7 (güvenlik):**
- JWT'nin yapısı, imzası, neden stateless olduğu ve iptalinin neden zor olduğu.
- Access vs refresh token; refresh token neden daha uzun ömürlü ve nasıl korunur?
- Sadece gateway'de doğrulamak yeterli mi?
- Rate limit neden gateway'de, hangi anahtarla?

**S8 (dağıtım, gözlem):**
- Liveness vs readiness farkı?
- Container içinde JVM belleği nasıl davranır?
- Image tag olarak `latest` kullanmanın riski?
- Log, metric, trace farkı ve hangisi ne zaman?

**v1.x - v2 (CDC, resilience, tracing, saga):**
- Polling relay vs CDC trade-off'u?
- Circuit breaker'ın 3 durumu; retry ile birlikte kullanırken retry storm riski; bulkhead farkı.
- Trace context senkron REST'te ve Kafka'da nasıl taşınır?
- Mikroservislerde 2PC neden tercih edilmez? Orchestration vs choreography?
- Her işlem geri alınabilir mi? Geri alınamayan adım saga'da nereye konur?

**Proje bittiğinde (genel):**
- Tek bir servis çökerse hangi akışlar etkilenir? (S8.5'te gerçekten test ettin, sonucu anlat.)
- Neden servis başına ayrı DB?
- Sıfırdan yapsan hangi kararı değiştirirdin ve neden?

---

## 11. Park Alanı (MVP'ye Girmeyecek Fikirler)

Yeni fikir gelince buraya yaz, sprinte ekleme. Sprint 8.5'te ve v1.x planlarken buradan seçilir.
- Cursor (keyset) pagination
- Idempotency key'lerin Redis'te TTL ile tutulması
- Dosya/belge depolama (MinIO)
- Schema Registry + Avro/Protobuf event şemaları
- Testcontainers ile Keycloak entegrasyon testi
- Spring Modulith / hexagonal mimari denemesi bir serviste
- SonarQube / statik analiz CI'da
- Feature flag
- Kaos testi (Toxiproxy ile ağ gecikmesi)
- Helm chart
- (kendi fikirlerin)

---

## 12. Eski Fazlar → Yeni Sprintler Eşleştirmesi

| Eski faz | Yeni yeri |
|---|---|
| Faz 0, Faz 1 | ✅ Tamamlandı |
| Faz 2 (2.1–2.6) | S1 |
| Faz 2 test + 2.9 + Eureka/config bağlantısı | S2 |
| Faz 2.7 outbox stub | S5 (stub değil, gerçek) |
| Faz 2.8 Keycloak sync stub | S17 |
| Faz 3 Keycloak + Gateway | S7 |
| Faz 4 customer | S3 (Feign kısmı S4) |
| Faz 5 catalog | S9 (MVP'de order-service içinde sabit tarife) |
| Faz 6 Kafka + outbox + Debezium | S5-S6 (Debezium S10) |
| Faz 7 order + saga | S4 (order) + S14 (saga) |
| Faz 8 subscription + usage | S13 + S15 |
| Faz 9 billing + payment | S13 + S15 |
| Faz 10 notification + ticket | S16 |
| Faz 11 Resilience4j | S11 (temel timeout S4'te) |
| Faz 12 Observability | Metrik S8, tracing/log S12 |
| Faz 13 BFF | S17 |
| Faz 14 Frontend | S18 |
| Faz 15 Docker Compose | S8 |
| Faz 16 Kubernetes | S19 |
| Faz 17 Test + CI | S2'ye öne çekildi, S8'de genişledi |
| Eski Bölüm 11 (ArchUnit vb.) | S20 |

---

## 13. Arşiv: Tamamlanan Fazlardan Notlar

**Faz 0 sorusu:** Neden monorepo?
**Cevabım:** CI/CD ve yönetim işlemlerinden ziyade 2 temel konu daha var:
1. Atomik değişiklik (version skew'in önüne geçmek): Farklı repolar 2 ayrı deploy işlemi demek. Aradaki süre boyunca tutarsızlıklar olabileceğinden dolayı monorepo tercih edilir.
2. Ortak bağımlılık versiyonlaması: `pom.xml` içerisinde `<dependencyManagement/>` bloğunu kullanarak versiyon yönetimini tek bir yerden yapıyoruz.

Trade-off: Proje büyüdükçe CI/CD build süresi artar. *(S20'de ya da CI yavaşladığında: sadece değişen modülleri build etmek, `mvn -pl <modül> -am`, ile bu trade-off'u yönet.)*

**Faz 1 notları:** `md-infos/telco-crm-roadmap/FAZ 1 Notlar.md` (Eureka self-preservation, eviction, AP tercihi, standalone Eureka).

---

## 14. Kaynaklar
- Spring Boot ve Spring Cloud resmi dokümantasyonu (Boot 4 / Cloud 2025.1 sürümleri; eski blog yazılarındaki `bootstrap.yml` ve eski artifact isimlerine dikkat).
- Spring for Apache Kafka referans dokümanı (error handling, DLT bölümleri).
- Testcontainers dokümantasyonu (Postgres, Kafka modülleri) ve Spring Boot'un Testcontainers desteği.
- Debezium dokümantasyonu, "Outbox Event Router" bölümü (S5'te şema tasarlarken bir kez, S10'da tamamen).
- Keycloak Server Administration Guide (realm import/export).
- Chris Richardson, *Microservices Patterns* (Outbox, Saga, API Gateway bölümleri).
- Martin Kleppmann, *Designing Data-Intensive Applications* (Bölüm 11: stream processing; S5-S6 için).
- Referans repo servis README'leri: **sadece ilgili sprinti bitirdikten sonra**, kendi çözümünle karşılaştırmak için.

---

**Şimdi başlangıç noktası:** Sprint 1, Pazartesi 19 Ekim 2026. İlk iş: `identity-service-impl` branch'ini merge etmek ve parent POM'a modülü eklemek. Sprint bitince "S1 tamam" + PR linkiyle gel.

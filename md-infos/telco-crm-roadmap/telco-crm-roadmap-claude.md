# Telco CRM Klonu — Kişisel Bootcamp Roadmap'i

**Referans repo:** https://github.com/orhangolcur/telco-crm (fork of arshmed/telco-crm)
**Senin profilin:** Java/Spring'de başlangıç sonrası deneyim, haftada 4-6 saat, hedef stack repoyla birebir aynı, öncelikli motivasyon **mid-level Java mülakatı** (sistem tasarımını zaten makale okuyarak çalışıyorsun — bu proje sana pratik/uygulamalı tarafı verecek).
**Tahmini toplam süre:** ~5-7 ay (17 faz) — bootcamp'teki sıfırdan başlayanlarla kıyaslamak yanıltıcı: onlarda muhtemelen tam zamanlı temel + mentor + hazır ödev/rubric + eş zamanlı grup baskısı var, bu dördü ilerlemeyi ciddi hızlandırır. Sen zaten temel biliyorsun, bu yüzden fazların çoğunu bootcamp'in "öğrenme" kısmından değil "uygulama" kısmından geçeceksin — süre tahminlerini ona göre kısalttım.
**Kural:** Referans repoyu ASLA kopyala-yapıştır yapma. Sadece mimari kararları ve klasör yapısını referans al, kodu kendin yaz. Kod yazarken "bu neden böyle?" sorusunu her seferinde kendine sor — cevap veremiyorsan o fazı bitirmiş sayma.
**Mülakat odağı:** Her faz sonunda o fazın konusuyla ilgili gerçek mülakat sorularını göreceksin (Bölüm 11). Bu projeyi bitirdiğinde bu sorulara "makaleden okudum" değil "kurdum, kırdım, düzelttim" diyerek cevap verebileceksin — mid-level mülakatlarda asıl fark burada ortaya çıkıyor.

---

## 0. Neden Bu Sırayla?

Repo 14 servisten oluşuyor ama bunları rastgele sırayla yapamazsın çünkü bazıları diğerlerine bağımlı (mesela her şey Eureka'ya kayıt olur, Config Server'dan config çeker). Roadmap şu mantıkla ilerliyor:

1. Önce **altyapı iskeleti** (discovery, config) — servisler bunsuz ayağa kalkamaz.
2. Sonra **tek bir basit servis** uçtan uca (identity) — CRUD + DB + Flyway + test döngüsünü otur.
3. Sonra **güvenlik katmanı** (Keycloak, JWT, Gateway) — çünkü sonraki her servis buna güvenecek.
4. Sonra **iş servislerini** tek tek, bağımlılık sırasına göre (customer → catalog → order → subscription → usage → billing → payment).
5. En son **asenkron/dağıtık zorluklar**: Kafka, Outbox, Debezium CDC, Saga — bunlar en riskli parça, temelin sağlam olmadan buraya girme.
6. En sonda **gözlemlenebilirlik, frontend, container/orkestrasyon** — sistem çalışıyorken üstüne eklenecek katmanlar.

---

## 1. Mimari Genel Bakış (Hedef Sistem)

```
FE (React/Vite) → BFF (OAuth2 login+session) → API Gateway (JWT+RateLimit+Routing)
                                                        ↓
        ┌───────────────────────────────────────────────────────────────┐
        │  identity · customer · product-catalog · order · subscription │
        │  usage · billing · payment · notification · ticket            │
        └───────────────────────────────────────────────────────────────┘
                    ↓ (senkron: OpenFeign+CircuitBreaker)   ↓ (asenkron: Outbox→Debezium→Kafka)
              servis-servis REST çağrıları                     event-driven iletişim

Her servisin kendi Postgres DB'si var. Discovery=Eureka, Config=Config Server, Auth=Keycloak.
```

**Kritik mimari prensip — tek yön kuralı:** Bir servis başka bir servisin veritabanına asla doğrudan erişmez. Her şey ya REST (senkron, gerçek zamanlı ihtiyaç için) ya da event (asenkron, "bir şey oldu, haberdar ol" için) üzerinden gider. Bu kararı her faza girerken hatırla.

---

## 2. Teknoloji Yığını ve Neden Öğrenmen Gerekiyor

| Katman | Teknoloji | Bu projede rolü |
|---|---|---|
| Servis discovery | Netflix Eureka | Servisler birbirini IP değil isimle bulur |
| Config | Spring Cloud Config Server | Ortak/servis-bazlı config tek yerden yönetilir |
| Gateway | Spring Cloud Gateway | Tek giriş noktası, JWT doğrulama, rate limit |
| Auth | Keycloak (OIDC) | Kullanıcı kimlik doğrulama, JWT üretimi |
| Senkron iletişim | OpenFeign + Resilience4j | Servisler arası REST çağrısı + circuit breaker |
| Asenkron iletişim | Kafka + Debezium CDC | Event-driven, transactional outbox pattern |
| DB | PostgreSQL + Flyway | Servis başına izole DB, versiyonlu migration |
| Cache | Redis | Cache, rate-limit sayaçları, session |
| Observability | Zipkin, Micrometer, Loki+Grafana | Distributed tracing, merkezi log |
| Test | JUnit5 + Mockito | Birim test |
| Container | Docker Compose → Kubernetes | Lokal çalıştırma → orkestrasyon |

---

## FAZ 0 — Ortam ve Proje İskeleti (2-3 gün)

**Öğrenilecek konular:** Maven multi-module proje yapısı, parent POM, Git branch stratejisi.

**Görevler:**
- [x] Yeni bir Git reposu aç (referans repoyu fork/clone ETME — sıfırdan başla).
- [x] Kök dizinde parent `pom.xml` oluştur (packaging: pom), ortak dependency versiyonlarını `<dependencyManagement>` altında topla.
- [x] JDK 21, Maven 3.x, Docker Desktop kurulumlarını doğrula.
- [x] Boş bir `docker-compose.yml` iskeleti oluştur (şimdilik sadece Postgres + Redis servisleri).

**Çıktı / Definition of Done:** `mvn clean install` kök dizinde hatasız çalışıyor, `docker compose up -d` ile Postgres ayağa kalkıyor.

**Kendine sorman gereken soru:** Neden tek repo (monorepo) tercih ediyoruz, her servisi ayrı repo yapsak ne kaybederdik?

**Cevap** : CICD ve yönetim işlemlerinden ziyade 2 temel konu daha var : 
1- Atomik değişiklik (version skew'in önüne geçmek) : Farklı repolar demek 2 ayrı deploy işlemi demek. Aradaki süre boyunca tutarsızlıklar olabileceğinden dolayı monorepo tercih edilir.
2-Ortak bağımlılık versiyonlaması : pom.xml içerisinde <dependencyManagement/> bloğunu kullanarak versiyon yönetimini tek bir yerden yapıyoruz.

Yazılımda hiçbir çözüm silver bullet olmadığı için trade-off'unu da bilmekte fayda var : 
Proje büyüdükçe CI/CD build süresi artar.

---

## FAZ 1 — Discovery Server + Config Server (1 hafta)

**Öğrenilecek konular:** Service discovery kavramı, client-side load balancing, externalized configuration.

**Görevler:**
- [ ] `discovery-server` modülü: `spring-cloud-starter-netflix-eureka-server`, `@EnableEurekaServer`.
- [ ] `config-server` modülü: `spring-cloud-config-server`, native profil ile `configs/` klasöründen dosya oku (Git backend değil, native — repoyla aynı).
- [ ] Her iki servisi de Docker Compose'a ekle (portlar: 8761, 8888).
- [ ] Basit bir "hello-service" yaz, Eureka'ya kayıt olduğunu ve config-server'dan bir property çektiğini doğrula, sonra bu servisi sil (öğrenme amaçlı).

**Çıktı:** Eureka dashboard'unda (localhost:8761) test servisin görünüyor, config-server bir `application.yml`'i native profille serve ediyor.

**Sık yapılan hata:** Config-server'ı `bootstrap.yml` yerine yeni Spring Cloud'da `spring.config.import` ile bağlamayı unutmak — versiyon farkına dikkat et.

---

## FAZ 2 — identity-service: İlk Gerçek Servis (1-2 hafta)

**Öğrenilecek konular:** Katmanlı mimari (controller/service/repository), Flyway migration, DTO/Entity ayrımı, MapStruct.

**Görevler:**
- [ ] `identity-service`: User/Role/Permission entity'leri, Postgres, Flyway ile ilk migration (`V1__init.sql`).
- [ ] CRUD endpoint'leri: kullanıcı oluştur/listele/güncelle/sil.
- [ ] MapStruct ile Entity↔DTO mapping (elle yazma, kod üretimini gözlemle).
- [ ] Birim testler: service katmanı için Mockito ile mock repository.
- [ ] Eureka'ya kayıt ol, config-server'dan config çek.

**Çıktı:** Postman/curl ile CRUD uçtan uca çalışıyor, `mvn test` yeşil.

**Kendine sorman gereken soru:** DTO ile Entity'yi neden ayırıyoruz — direkt Entity'yi API'de dönsek ne kaybederdik?

---

## FAZ 3 — Keycloak + JWT + API Gateway (2-3 hafta) — İLK ZOR EŞİK

**Öğrenilecek konular:** OIDC/OAuth2 temelleri, JWT yapısı (header/payload/signature), Resource Server pattern, API Gateway routing, rate limiting.

**Görevler:**
- [ ] Keycloak'ı Docker Compose'a ekle, bir realm oluştur (repodaki gibi export/import mekanizmasını kur).
- [ ] Realm içinde bir client ve birkaç test kullanıcısı/rolü tanımla.
- [ ] `identity-service`'i `spring-boot-starter-oauth2-resource-server` ile JWT doğrulayan hale getir.
- [ ] `api-gateway`: Spring Cloud Gateway, route'ları identity-service'e yönlendir, JWT'yi gateway seviyesinde doğrula, Redis ile rate-limit filtresi ekle.
- [ ] Postman'de: Keycloak'tan token al → gateway üzerinden identity-service'e istek at, token'sız isteğin 401 döndüğünü doğrula.

**Çıktı:** Gateway üzerinden JWT'li istek identity-service'e ulaşıyor, JWT'siz istek reddediliyor, rate limit tetiklenince 429 dönüyor.

**Bu fazda takılırsan normal:** OAuth2/OIDC ilk defa görülünce kafa karıştırır. "JWT nasıl imzalanır ve gateway nasıl doğrular" konusunu ayrı bir günde sadece bunu öğrenmeye ayır, koda geçmeden önce akışı kağıda çiz.

---

## FAZ 4 — customer-service (1 hafta)

**Öğrenilecek konular:** İlişkisel modelleme (Customer → Address, Document 1-N), dosya/belge referansı tutma (henüz MinIO'ya bağlamadan).

**Görevler:**
- [ ] Customer, Address, Document entity'leri + KYC durum alanı (enum: PENDING/VERIFIED/REJECTED).
- [ ] Flyway migration'ları, CRUD + KYC durum güncelleme endpoint'i.
- [ ] Gateway'e route ekle, JWT ile korunmasını sağla.
- [ ] identity-service'e Feign ile basit bir çağrı yap (örn. kullanıcı var mı kontrolü) — OpenFeign'i ilk kez burada dene.

**Çıktı:** Bir müşteri oluşturup KYC durumunu güncelleyebiliyorsun, işlem identity-service'i Feign üzerinden sorguluyor.

---

## FAZ 5 — product-catalog-service (1 hafta)

**Öğrenilecek konular:** Katalog/ürün modelleme, versiyonlama (tarife değişince eski siparişler ne olacak sorusu).

**Görevler:**
- [ ] Tarife (Plan) ve ek paket (AddOn) entity'leri, fiyat/kota alanları.
- [ ] CRUD + "aktif tarifeleri listele" endpoint'i.
- [ ] Bu servisin diğer servisler için "tek doğruluk kaynağı" olacağını unutma — order-service buradan fiyat/kota bilgisini Feign ile çekecek.

**Çıktı:** Katalog servisi bağımsız çalışıyor, testleri geçiyor.

---

## FAZ 6 — Kafka + Transactional Outbox + Debezium CDC (2-3 hafta) — EN RİSKLİ PARÇA (detay Bölüm 8'de)

**Öğrenilecek konular:** At-least-once delivery, dual-write problemi, outbox pattern, CDC (Change Data Capture), Debezium connector konfigürasyonu.

**Görevler:**
- [ ] Kafka + Zookeeper/KRaft + Debezium Connect'i Docker Compose'a ekle.
- [ ] `product-catalog-service`'te basit bir outbox tablosu kur: bir tarife güncellendiğinde aynı transaction içinde outbox tablosuna event yaz.
- [ ] Debezium connector'ı Postgres'in outbox tablosuna bağla, Kafka topic'ine event'in düştüğünü Kafka UI'dan doğrula.
- [ ] Başka bir "dinleyici" servis yaz (basit bir consumer), event'i tüket ve logla.

**Çıktı:** Postgres'e yazılan bir satır, kod tarafında Kafka'ya hiç dokunmadan Kafka topic'inde beliriyor.

---

## FAZ 7 — order-service ve Saga Orkestrasyonu (3-4 hafta) — EN ZOR SERVİS

**Öğrenilecek konular:** Distributed transaction problemi, saga pattern (orchestration vs choreography — Bölüm 8), compensating transaction, state machine ile sipariş durumu yönetimi.

**Görevler:**
- [ ] Order entity'si + durum makinesi (CREATED → PAYMENT_PENDING → PAID → SUBSCRIPTION_ACTIVE → COMPLETED, ve hata dallarında CANCELLED/FAILED).
- [ ] Saga orkestratörünü order-service içinde kur: sipariş oluşunca payment-service'e event gönder (henüz payment-service yoksa mock bir endpoint ile simüle et), cevaba göre sonraki adımı tetikle.
- [ ] Compensating action yaz: ödeme başarısız olursa siparişi CANCELLED yap ve rezerve edilen kotayı geri al.
- [ ] Outbox pattern'i order-service'e de uygula (Faz 6'da öğrendiğini tekrar kullan).

**Çıktı:** Mutlu senaryoda (happy path) sipariş baştan sona ilerliyor; ödeme başarısız simülasyonunda sipariş doğru şekilde geri sarılıyor (rollback/compensation çalışıyor).

**Bu faz gerçekten zor.** Acele etme, önce kağıt üzerinde tüm durum geçişlerini ve her hata durumunda hangi compensating action'ın tetikleneceğini bir tablo halinde çıkar, ondan sonra kodla.

---

## FAZ 8 — subscription-service ve usage-service (1-2 hafta)

**Öğrenilecek konular:** Event consumer yazma (order tamamlanınca subscription aktive olması), sayaç/kota mantığı, eşik (threshold) event'leri.

**Görevler:**
- [ ] `subscription-service`: order-service'in yaydığı "sipariş tamamlandı" event'ini dinleyip aboneliği aktive et. Askıya alma/sonlandırma endpoint'leri.
- [ ] `usage-service`: CDR (call detail record) benzeri kullanım kayıtları, kota takibi, %80/%100 eşiği aşıldığında event yayınlama.
- [ ] Gateway route'larını ekle.

**Çıktı:** Bir sipariş tamamlanınca abonelik otomatik aktive oluyor (event üzerinden, senkron çağrı YOK).

---

## FAZ 9 — billing-service ve payment-service (2 hafta)

**Öğrenilecek konular:** Fatura döngüsü (billing cycle) tasarımı, retry mekanizmaları, idempotency (aynı ödemenin iki kez işlenmemesi).

**Görevler:**
- [ ] `billing-service`: periyodik fatura üretimi (scheduled job), usage-service'ten kullanım verisini çek.
- [ ] `payment-service`: mock PSP (payment service provider) entegrasyonu, retry politikası (Resilience4j Retry), idempotency key kullanımı.
- [ ] order-service'in saga'sını gerçek payment-service'e bağla (Faz 7'deki mock'u değiştir).

**Çıktı:** Fatura oluşuyor, ödeme deneniyor, başarısızlıkta retry çalışıyor, aynı ödeme iki kez tahsil edilmiyor.

---

## FAZ 10 — notification-service ve ticket-service (1-2 hafta)

**Öğrenilecek konular:** Event-to-notification çevirimi, SMTP entegrasyonu, SLA takibi için scheduled scanner pattern.

**Görevler:**
- [ ] `notification-service`: platform genelindeki event'leri (sipariş tamam, fatura kesildi vb.) dinleyip e-posta gönder (Gmail app password ile).
- [ ] `ticket-service`: destek talebi CRUD + SLA ihlali tarayan zamanlanmış görev (örn. 24 saat cevapsız kalan ticket'ı flag'le).

**Çıktı:** Bir sipariş tamamlandığında gerçek bir e-posta alıyorsun.

---

## FAZ 11 — Resilience4j Circuit Breaker + Tüm Servis-Servis Çağrılarını Sağlamlaştırma (1 hafta)

**Öğrenilecek konular:** Circuit breaker durumları (CLOSED/OPEN/HALF_OPEN), fallback method, bulkhead.

**Görevler:**
- [ ] Faz 4'te yazdığın Feign çağrısına circuit breaker ekle, bilinçli olarak identity-service'i kapatıp fallback'in devreye girdiğini gözlemle.
- [ ] Tüm senkron servis-servis çağrılarına aynı pattern'i uygula.

**Çıktı:** Bağımlı servis çöktüğünde sistem 500 hatası vermek yerine kontrollü bir fallback dönüyor.

---

## FAZ 12 — Observability: Zipkin + Loki/Grafana + Correlation-Id (1 hafta)

**Öğrenilecek konular:** Distributed tracing kavramı, trace/span, ECS log formatı, correlation-id propagation.

**Görevler:**
- [ ] Micrometer/OpenTelemetry ile tüm servislere tracing ekle, Zipkin'e gönder.
- [ ] Bir isteğin gateway'den başlayıp 3 servisi geçtiği bir trace'i Zipkin UI'da bul.
- [ ] Loki+Promtail+Grafana kurulumu, ECS formatlı log + correlation-id ile bir isteğin tüm servislerdeki loglarını tek sorguda bulma.

**Çıktı:** Bir HTTP isteğinin gateway'den son servise kadar tüm yolculuğunu tracing'de görebiliyorsun.

---

## FAZ 13 — bff-server: OAuth2 Login + Session + TokenRelay (1 hafta)

**Öğrenilecek konular:** BFF (Backend-for-Frontend) pattern, session-based vs token-based auth farkı, TokenRelay filtresi.

**Görevler:**
- [ ] `bff-server`: Spring Session, OAuth2 login akışı (browser → Keycloak → callback), session'da token'ı tutup gateway'e TokenRelay ile ilet.
- [ ] Neden frontend'in JWT'yi doğrudan tutmadığını, bunun yerine BFF'in session kullandığını araştır ve kendi cümlelerinle özetle.

**Çıktı:** Tarayıcıdan login olup, session cookie ile gateway arkasındaki servislere erişebiliyorsun.

---

## FAZ 14 — Frontend: React + Vite + TS + Tailwind (2 hafta)

**Görevler:**
- [ ] Temel sayfalar: login, müşteri listesi, sipariş oluşturma, fatura görüntüleme.
- [ ] BFF ile session tabanlı auth entegrasyonu.
- [ ] En azından 3-4 ana akışı (customer create, order create, ticket create) uçtan uca UI'dan test et.

**Çıktı:** Tarayıcıdan gerçek bir kullanıcı akışını (müşteri oluştur → sipariş ver → faturayı gör) baştan sona yapabiliyorsun.

---

## FAZ 15 — Docker Compose ile Tüm Sistemi Ayağa Kaldırma (2-3 gün)

**Görevler:**
- [ ] Tüm servisleri, Postgres'leri (servis başına ayrı), Kafka, Debezium, Redis, Keycloak, Zipkin, Loki/Grafana'yı tek `docker-compose.yml`'de topla.
- [ ] Debezium connector kayıt scriptini yaz.
- [ ] Sıfırdan `docker compose up -d` ile tüm sistemin ayağa kalktığını doğrula.

---

## FAZ 16 — Kubernetes Manifestleri (2-3 hafta, opsiyonel ama hedefte var)

**Öğrenilecek konular:** Deployment, Service, ConfigMap, Secret, temel Ingress.

**Görevler:**
- [ ] Her servis için Deployment + Service manifesti yaz.
- [ ] ConfigMap/Secret ile config-server'ın yerini kısmen K8s native config'e taşımayı dene (repodaki yaklaşımı incele, birebir kopyalama).
- [ ] Minikube/kind ile lokal bir cluster'da 2-3 servisi ayağa kaldır (tüm sistemi K8s'de çalıştırmak zorunlu değil, öğrenme amaçlı).

---

## FAZ 17 — Test Kapsamını Genişletme + CI (1 hafta)

**Görevler:**
- [ ] Her serviste JUnit5+Mockito ile service-layer testleri (repo gibi).
- [ ] GitHub Actions ile `mvn test`'i her push'ta çalıştıran basit bir workflow kur.

---

## 8. En Riskli Parça: Saga + Outbox — İki Yaklaşımın Karşılaştırması

Bu proje için en riskli teknik karar, sipariş akışının (order → payment → subscription) dağıtık ortamda nasıl koordine edileceği. İki ana yaklaşım var:

### A) Orchestration (Merkezi Orkestratör) — repo bunu kullanıyor
Order-service bir "beyin" gibi davranır: her adımı sırayla tetikler, sonucu bekler, hata olursa compensating action'ı yine kendisi tetikler.

- **Artıları:** Akışı tek yerde görürsün (order-service kodunu okuyunca tüm saga anlaşılır), debug etmesi kolay, hangi adımda kaldığını izlemek basit.
- **Eksileri:** Order-service diğer tüm servisleri "bilmek" zorunda — bağımlılık merkezi bir servise yığılır, order-service büyüdükçe god-object riski var.

### B) Choreography (Koreografi / Event-Driven)
Merkezi orkestratör yok; her servis kendi işini yapar, bir event yayınlar, sıradaki servis o event'i dinleyip kendi işini yapar ve yeni bir event yayınlar (order.created → payment.completed → subscription.activated zinciri).

- **Artıları:** Servisler birbirinden tamamen bağımsız, yeni bir servis eklemek (örn. yeni bir bildirim türü) mevcut servisleri değiştirmeden yapılabilir.
- **Eksileri:** "Şu an sipariş hangi adımda?" sorusunun cevabı hiçbir yerde tek başına yok — event zincirini takip etmek gerekir, debug etmesi zor, döngüsel bağımlılık riski (kim kimi dinliyor karmaşası).

**Önerim:** Repo orchestration kullanıyor, sen de öyle yap (Faz 7) — 3-4 servisli bir akış için orchestration daha öğretici ve debug edilebilir. Ama Faz 8'i bitirdikten sonra, en az bir akışı (örn. notification tetikleme) bilinçli olarak choreography ile yeniden yaz — ikisini de elleriyle deneyimlemiş olman, "neden bu karar" sorusuna gerçek cevap verebilmen için kritik.

**Outbox + Debezium neden var, alternatifi ne olurdu?**
Alternatif "dual write" olurdu: aynı anda hem DB'ye yaz hem Kafka'ya event gönder. Sorun şu: iki ayrı sistem (DB ve Kafka) aynı transaction'a giremez — DB commit olur ama Kafka'ya gönderim başarısız olursa (network kesintisi vb.) sistemler senkronizasyonunu kaybeder. Outbox pattern bunu tek bir DB transaction'ına indirger (asıl veri + outbox satırı aynı transaction'da yazılır), Debezium ise bu outbox tablosunu WAL (write-ahead log) seviyesinde izleyip Kafka'ya güvenilir şekilde taşır. Bunu Faz 6'da mutlaka kendi ellerinle kur, sadece okuma yeterli değil.

---

## 9. Mülakat Hazırlığı: Faz Bazlı Olası Sorular

Sistem tasarım makalelerinden öğrendiğin kavramları burada ellerinle kurup kırdığın için, mülakatta bu sorulara "teoride biliyorum" değil "implementasyonunda şu problemle karşılaştım" diyerek cevap verebileceksin. Her fazı bitirince ilgili soruları kendine sor, cevabını sesli/yazılı pratik et.

**Faz 1-2 (Discovery/Config/CRUD temelleri):**
- Service discovery neden gerekli, hardcoded IP/port kullansak ne olurdu?
- Client-side vs server-side load balancing farkı nedir?
- N+1 query problemi nedir, JPA'da nasıl fark edilir/önlenir?
- `@Transactional` bir servis metodunda ne zaman işe yaramaz (self-invocation, private method, checked exception vb.)?

**Faz 3 (Keycloak/JWT/Gateway):**
- JWT'nin yapısı nedir (header/payload/signature), neden stateless kabul edilir?
- Access token ile refresh token farkı, refresh token neden daha uzun ömürlü?
- API Gateway olmadan da güvenlik sağlanabilir miydi — her servis kendi JWT'sini doğrulasa ne kaybederdik/kazanırdık?
- Rate limiting'i neden Gateway'de, servis içinde değil de merkezi yapıyoruz?

**Faz 6 (Kafka/Outbox/Debezium):**
- Dual-write problemi nedir, neden tehlikelidir?
- At-least-once vs at-most-once vs exactly-once delivery farkları — Kafka hangisini garanti eder?
- Kafka'da partition ve consumer group ilişkisi nasıl işler, aynı partition'ı iki consumer aynı anda okuyabilir mi?
- CDC (Change Data Capture) nedir, polling ile CDC arasındaki fark nedir?

**Faz 7 (Saga/order-service):**
- Distributed transaction'da 2PC (two-phase commit) neden mikroservislerde tercih edilmez?
- Saga pattern'de orchestration ve choreography arasındaki trade-off nedir? (Bölüm 8'i tekrar oku)
- Compensating transaction nedir, her işlemin "geri alınabilir" olması garanti mi?
- Idempotency nedir, neden dağıtık sistemde her endpoint idempotent olmalı (özellikle retry olan yerlerde)?

**Faz 8-9 (Subscription/Usage/Billing/Payment):**
- Event-driven mimaride bir consumer event'i işlerken çökerse ne olur, event kaybolur mu?
- Eventual consistency nedir, kullanıcıya UX açısından nasıl yansır (örn. sipariş verdi ama abonelik anlık aktive olmadı)?
- Retry + idempotency key kombinasyonu neden birlikte kullanılır?

**Faz 11 (Circuit Breaker):**
- Circuit breaker'ın 3 durumu (CLOSED/OPEN/HALF_OPEN) nedir, her birinde sistem nasıl davranır?
- Circuit breaker ile retry'ı aynı anda kullanmak neden dikkat gerektirir (retry storm riski)?
- Bulkhead pattern nedir, circuit breaker'dan farkı ne?

**Faz 12 (Observability):**
- Distributed tracing'de trace-id/span-id nasıl propagate edilir (senkron REST'te ve asenkron Kafka'da farkı ne)?
- Log, metric, trace (observability'nin 3 ayağı) arasındaki fark ve her biri ne zaman kullanılır?

**Genel mimari sorular (tüm proje bittiğinde cevaplayabilmen gereken):**
- Bu sistemde tek bir servis çökerse hangi akışlar etkilenir, hangileri etkilenmez? (Bunu gerçekten test et — bir servisi durdur, gör.)
- Neden servis başına ayrı veritabanı, tek bir ortak DB kullansak ne kaybederdik?
- Bu sistemi sıfırdan tasarlasan, hangi kararı değiştirirdin ve neden?

---

## 10. Sana Sorabileceğim / Senin Bana Soracağın Olası Sorular

Her fazı bitirdiğinde bana şu formatta gel, birlikte gözden geçirelim:
- "Bu fazda şunu yaptım, kodu şurada — mimari açıdan doğru mu?"
- "X kavramını anlamadım, örnekle anlatır mısın?"
- "Bu servisi Y yerine Z şekilde tasarlasam ne değişirdi?"

Kendine sorman gereken tekrarlayan kontrol soruları (her faz sonunda):
1. Bu servis başka bir servisin veritabanına dokunuyor mu? (Dokunmamalı.)
2. Bu iletişim neden senkron (Feign) değil de asenkron (Kafka) — ya da tam tersi?
3. Bu servis çökerse sistemin geri kalanı ne olur?
4. Test yazmadan "çalışıyor" diyebilir miyim?

---

## 11. Kaynaklar

- Spring Cloud resmi dokümantasyonu (Eureka, Config, Gateway modülleri).
- Debezium resmi dokümantasyonu — "Outbox Event Router" bölümü özellikle bu proje için kritik.
- Chris Richardson, *Microservices Patterns* (Saga, Outbox pattern'lerinin orijinal kaynağı).
- Referans repo servis README'leri — her servisin kendi klasöründeki README'yi SADECE o fazı bitirdikten sonra oku (kendi çözümünle karşılaştırmak için, kopyalamak için değil).

---

**Şimdi başlangıç noktası:** Faz 0'ı bitirip bana "Faz 0 tamam" dediğinde, birlikte kodunu gözden geçirir ve Faz 1'e geçeriz.

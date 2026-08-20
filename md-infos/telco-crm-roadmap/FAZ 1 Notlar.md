**Discovery client (naming-server modülü) kurulurken karşılaştığım önemli maddeler ve konular :** 
-------------------------------------------------------------------------

>1-Eureka ve self-preservation mantığı : Eureka çalışma mantığı olarak belirli aralıklarla servislerden heartbeat bekler. Bu heartbeat belirli bir süre gelmezse o kayıdı registry'den siler. Buna evication denir. Fakat bu davranışın prod ortamında kullanmak yerine "belki de network hatasıdır" senaryosunu da düşünüp buna göre hareket etmemiz gerekebilir. Burada devreye self-preservation girer. Eğer eureka beklediği heartbeat sayısının %85'inden daha azını alıyorsa bunu network hatası olarak değerlendirir ve registry'leri silmez.

Bu işlem az sayıda instance ve çok fazla servis restart olduğu durumlarda registry'de fazla ölü kayıt birikmesine sebep olur eski servislere istek atılmasına, doğal olarak da isteklerin kaybolmasına sebep olabilir. Bu davranış CAP teoremindeki AP ile ilişkilendirilebilir. Eureka sistemdeki trafiği cevapsız bırakmamak adına bayat veri göstermeyi göze alır.

>2- Standalone çalışmayan eurekalar : Şu an ki projemde tek bir eureka server çalıştıracak olsam da productionda **Single point of failure**'u engellemek adına birden fazla eureka çalıştırıp bunları birbirine register etmek önerilir. Burada 2 eureka server birbirini client olarak görür ve registry'lerini senkronize eder (peer replication denir). Servislerde :

**eureka.client.service-url.defaultZone**=http://eureka1:8761,http://eureka2:8762 şeklinde bir konfigurasyon ile beraber kayıt yapabiliriz. Bu sayede bir node cevap vermezse diğerini dinler

*************************************************************************
Mikroservis (Client) $\rightarrow$ Eureka Kaydı (Registration) 

Bir mikroservis (order-service) ayağa kalktığında şu adımlar gerçekleşir :

1- Registration (Kayıt) : Servis, kendi ip,port,uygulama adı (application.properties'de bulunur), health status gibi bilgileri içeren POST isteğini EurekaServer'ın endpoint'ine atar
2- Heartbeat : Servis kaydolduktan sonra ölmüş görünmemek ( ayakta olduğunu doğrulamak ) için varsayılan olarak her 30 saniyede bir Eurekaya bir HTTP Put isteği atar. Bu PUT isteği 90 saniye boyunca gitmezse bu servis registry'den silinir.

Eureka $\rightarrow$ Eureka (Peer Replication)

- **Asenkron Replikasyon:** `order-service` gidip `peer1`'e kaydolduğunda, `peer1` bu kayıt bilgisini alır almaz kendi içindeki **PeerReplicator** bileşeni üzerinden arka planda `peer2`'ye bir HTTP isteği fırlatır.
    
- **Registry State Transfer:** `peer1` sadece kendi aldığı kaydı değil, client'lardan gelen heartbeat (yenileme) ve cancel (kapanma) isteklerini de anlık olarak `peer2`'ye iletir.
    
- **Tüm Liste Değil, Deltalar:** Her seferinde tüm veritabanı baştan sona gönderilmez. Sadece değişen durumlar (yeni eklenen, silinen veya güncellenen servisler - _delta_) replike edilir. Böylece ağ trafiği minimumda tutulur.
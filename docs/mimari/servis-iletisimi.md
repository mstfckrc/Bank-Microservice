# Servis iletişimi ve veri sahipliği

Bu görünüm kaynak kodunda kullanım yeri bulunan başlıca çağrıları ve veri
sahipliğini gösterir; bütün endpoint'lerin veya her çağrının canlı kabulü değildir.

[PNG görüntüsü](servis-iletisimi.png)

```mermaid
---
config:
  theme: base
  flowchart:
    curve: basis
    nodeSpacing: 30
    rankSpacing: 65
    padding: 18
    diagramPadding: 24
    useMaxWidth: false
  themeVariables:
    darkMode: false
    background: "#ffffff"
    primaryColor: "#dcf5eb"
    primaryTextColor: "#0f172a"
    primaryBorderColor: "#5bbb9a"
    lineColor: "#64748b"
    textColor: "#26364c"
    edgeLabelBackground: "#ffffff"
    fontFamily: "Arial, sans-serif"
    fontSize: "15px"
  themeCSS: |
    .node rect { rx: 12px; ry: 12px; }
    .nodeLabel { font-weight: 600; }
    .edgeLabel { font-size: 13px; color: #475569; }
    .flowchart-link { stroke-width: 1.4px; }
---
flowchart LR
    auth["Auth"] -->|"Şirket kaydı / okuma / güncelleme / silme"| corporate["Corporate"]
    auth -->|"Müşteri hesaplarını kapat"| core["Core"]
    corporate -->|"Müşteri profili / erişim durumu"| auth
    corporate -->|"Hesap doğrulama / maaş"| core
    bill["Bill"] -->|"Fatura tahsilatı"| core
    bill -->|"Müşteri erişim durumu"| auth
    core -->|"Müşteri erişim durumu"| auth
    core -->|"Kur bilgisi"| currency["Currency"]
    currency -->|"Dış HTTP API"| provider["Kur sağlayıcısı"]

    auth --> adb[("Auth DB")]
    corporate --> cdb[("Corporate DB")]
    core --> bdb[("Core DB")]
    bill --> fdb[("Bill DB")]
    currency --> redis[("Redis cache")]

    auth & corporate & core & bill -->|"Bildirim yayını"| exchange["bank_exchange"]
    exchange -->|"notification_routing_key"| queue["notification_queue"]
    queue --> consumer["Notification"]

    classDef service fill:#dcf5eb,stroke:#5bbb9a,color:#0f172a,stroke-width:1.5px
    classDef data fill:#fff1d6,stroke:#e7b65a,color:#0f172a,stroke-width:1.5px
    classDef messaging fill:#ffebe5,stroke:#e5a58f,color:#0f172a,stroke-width:1.5px
    classDef external fill:#f1f5f9,stroke:#94a3b8,color:#0f172a,stroke-width:1.5px
    class auth,corporate,core,bill,currency,consumer service
    class adb,cdb,bdb,fdb,redis data
    class exchange,queue messaging
    class provider external
```

HTTP çağrıları ile RabbitMQ olayları farklı iletişim biçimleridir; bu görünüm
veritabanı işlemi ile mesaj yayınının tek bir dağıtık transaction olduğunu iddia
etmez. Notification gerçek e-posta/SMS göndermek yerine demo işlemi yapar.
Eureka servis keşfine katılır; her Gateway route'u için kullanıldığı anlamına gelmez.

Kaynaklar: [Corporate çalışan işlemleri](../../bank-corporate-micro/src/main/java/com/mustafa/service/impl/CompanyEmployeeServiceImpl.java),
[Auth müşteri yönetimi](../../bank-auth-micro/src/main/java/com/mustafa/service/impl/AdminUserServiceImpl.java),
[Bill tahsilatı](../../bill-service/src/main/java/com/mustafa/service/impl/BillPaymentServiceImpl.java),
[RabbitMQ consumer yapılandırması](../../notification-service/src/main/java/com/mustafa/config/RabbitMQConfig.java).

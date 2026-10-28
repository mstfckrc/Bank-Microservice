# Genel sistem mimarisi

Bu görünüm yerel Docker Compose ortamını özetler; bütün endpoint'leri ve servisler
arası çağrıları göstermez. Oklar ilgili iletişimin yönünü belirtir.

[PNG görüntüsü](genel-sistem.png)

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
    primaryColor: "#e0edff"
    primaryTextColor: "#0f172a"
    primaryBorderColor: "#60a5fa"
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
    user["Kullanıcı<br/>Tarayıcı"] -->|"HTTPS"| nginx["Nginx"]
    nginx -->|"app.bank.local"| frontend["Next.js<br/>Arayüz ve sunucu"]
    nginx -->|"auth.bank.local"| keycloak["Keycloak"]
    nginx -->|"api.bank.local / docs.bank.local"| gateway["API Gateway"]
    nginx -->|"eureka.bank.local"| eureka["Eureka"]
    nginx -->|"monitor.bank.local"| grafana["Grafana"]
    frontend -->|"API istekleri"| gateway
    frontend -->|"Giriş ve oturum"| keycloak

    gateway --> core["Core<br/>Hesap ve işlemler"]
    gateway --> auth["Auth<br/>Müşteri ve kimlik"]
    gateway --> corporate["Corporate<br/>Şirket ve çalışan"]
    gateway --> bill["Bill<br/>Fatura"]
    gateway --> currency["Currency<br/>Kur"]

    core --> coredb[("PostgreSQL<br/>Core")]
    auth --> authdb[("PostgreSQL<br/>Auth")]
    corporate --> corpdb[("PostgreSQL<br/>Corporate")]
    bill --> billdb[("PostgreSQL<br/>Bill")]
    keycloak --> kcdb[("PostgreSQL<br/>Keycloak")]
    currency --> redis[("Redis")]
    gateway -->|"Kayıt hız sınırı"| redis
    currency -->|"Kur bilgisi"| provider["Dış kur sağlayıcısı"]
    core & auth & corporate & bill -->|"Bildirim olayı"| rabbit["RabbitMQ"]
    rabbit --> notify["Notification<br/>Demo bildirim tüketicisi"]

    logs["Uygulama logları"] --> logstash["Logstash"]
    logstash --> elastic["Elasticsearch"]
    elastic --> kibana["Kibana"]
    metrics["Servis ve host metrikleri"] --> prometheus["Prometheus"]
    prometheus --> grafana

    classDef edge fill:#e0edff,stroke:#60a5fa,color:#0f172a,stroke-width:1.5px
    classDef security fill:#f0e7ff,stroke:#b49ae3,color:#0f172a,stroke-width:1.5px
    classDef gateway fill:#fff1d6,stroke:#e7b65a,color:#0f172a,stroke-width:1.5px
    classDef service fill:#dcf5eb,stroke:#5bbb9a,color:#0f172a,stroke-width:1.5px
    classDef data fill:#fff1d6,stroke:#e7b65a,color:#0f172a,stroke-width:1.5px
    classDef messaging fill:#ffebe5,stroke:#e5a58f,color:#0f172a,stroke-width:1.5px
    classDef observing fill:#e3f2fa,stroke:#80b5ce,color:#0f172a,stroke-width:1.5px
    classDef external fill:#f1f5f9,stroke:#94a3b8,color:#0f172a,stroke-width:1.5px
    class user,provider external
    class nginx,frontend edge
    class keycloak security
    class gateway gateway
    class core,auth,corporate,bill,currency,notify service
    class coredb,authdb,corpdb,billdb,kcdb,redis data
    class rabbit messaging
    class logs,logstash,elastic,kibana,metrics,prometheus,grafana,eureka observing
```

**Okuma notları:** RabbitMQ üzerinden tüketilen bildirimler mevcut uygulamada gerçek
e-posta/SMS teslimi değil, demo davranışıdır. Gözlemleme zinciri okunabilirlik için
özetlenmiştir; her servis bağlantısı tek tek çizilmemiştir. Eureka bütün Gateway
route'larının kullandığı bir yönlendirme mekanizması değildir.

Kaynaklar: [Compose](../../docker-compose.yml), [Nginx](../../nginx.conf),
[Gateway route'ları](../../api-gateway/src/main/java/com/mustafa/config/GatewayRoutingConfig.java),
[Prometheus](../../prometheus/prometheus.yml).

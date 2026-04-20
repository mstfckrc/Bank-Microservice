package com.mustafa.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

// 🚀 ÇÖZÜM: YML dosyasını es geçip, DOĞRUDAN Docker ortam değişkenini okuyoruz!
// Eğer Docker'da çalışıyorsa http://elasticsearch:9200 olacak, IntelliJ'de çalışıyorsa localhost olacak.
@FeignClient(name = "elasticsearch-client", url = "${ELASTICSEARCH_URL:http://localhost:9200}")
public interface ElasticsearchClient {

    @PostMapping("/bank-logs-*/_search")
    Map<String, Object> searchLogs(@RequestBody Map<String, Object> query);
}
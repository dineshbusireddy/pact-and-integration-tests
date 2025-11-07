package com.example.orders;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class OrdersClient {

  private final WebClient webClient;

  public OrdersClient(WebClient.Builder builder) {
    String baseUrl = System.getProperty("orders.baseUrl", "http://localhost:4010");
    this.webClient = builder.baseUrl(baseUrl).build();
  }

  public Mono<List<Map>> listOrders(String status) {
    return webClient.get()
        .uri(uri -> uri.path("/orders")
            .queryParamIfPresent("status", Optional.ofNullable(status))
            .build())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToFlux(Map.class)
        .collectList();
  }

  public Mono<Map> getOrder(long id) {
    return webClient.get()
        .uri("/orders/{id}", id)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(Map.class);
  }

  public Mono<Map> createOrder(Map body) {
    return webClient.post()
        .uri("/orders")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve()
        .bodyToMono(Map.class);
  }

  public Mono<Map> putOrder(long id, Map body) {
    return webClient.put()
        .uri("/orders/{id}", id)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve()
        .bodyToMono(Map.class);
  }

  public Mono<Map> patchOrder(long id, Map body) {
    return webClient.patch()
        .uri("/orders/{id}", id)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve()
        .bodyToMono(Map.class);
  }

  public Mono<Void> deleteOrder(long id) {
    return webClient.delete()
        .uri("/orders/{id}", id)
        .retrieve()
        .bodyToMono(Void.class);
  }
}

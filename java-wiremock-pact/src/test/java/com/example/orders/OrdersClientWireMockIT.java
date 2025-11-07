package com.example.orders;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class OrdersClientWireMockIT {

  @RegisterExtension
  static WireMockExtension wiremock = WireMockExtension.newInstance()
          .options(wireMockConfig().port(4010))
          .build();

  @Autowired
  private WebTestClient webTestClient;

  @BeforeEach
  void setup() {

    wiremock.stubFor(get(urlPathEqualTo("/orders"))
            .willReturn(okJson("""
                [
                  {"id":1,"customerName":"Alice","status":"NEW","totalAmount":120.5}
                ]
            """)));

    wiremock.stubFor(get(urlPathEqualTo("/orders/1"))
            .willReturn(okJson("""
                {"id":1,"customerName":"Alice","status":"PROCESSING","totalAmount":120.5}
            """)));

    wiremock.stubFor(get(urlPathEqualTo("/orders/9999"))
            .willReturn(notFound()));

    wiremock.stubFor(post(urlPathEqualTo("/orders"))
            .withRequestBody(matchingJsonPath("$.items"))
            .willReturn(aResponse()
                    .withStatus(201)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                    {"id":2,"customerName":"Bob","status":"NEW","totalAmount":100.0}
                """)));

    wiremock.stubFor(put(urlPathEqualTo("/orders/2"))
            .willReturn(aResponse()
                    .withStatus(409)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                    {"code":"CONFLICT","message":"Version conflict"}
                """)));

    wiremock.stubFor(patch(urlPathEqualTo("/orders/1"))
            .willReturn(okJson("""
                {"id":1,"customerName":"Alice","status":"COMPLETED","totalAmount":120.5}
            """)));

    wiremock.stubFor(delete(urlPathEqualTo("/orders/3"))
            .willReturn(noContent()));
  }

  @Test
  void listOrders_OK() {
    webTestClient.get()
        .uri("/api/orders")
        .exchange()
        .expectStatus().isOk()
        .expectBodyList(Map.class)
        .value(orders -> assertThat(orders).isNotEmpty());
  }

  @Test
  void getOrder_OK() {
    webTestClient.get()
        .uri("/api/orders/1")
        .exchange()
        .expectStatus().isOk()
        .expectBody(Map.class)
        .value(order -> assertThat(order.get("id")).isEqualTo(1));
  }

  @Test
  void getOrder_NotFound() {
    webTestClient.get()
        .uri("/api/orders/9999")
        .exchange()
        .expectStatus().isNotFound();
  }

  @Test
  void createOrder_Created() {
    Map body = Map.of(
            "customerName","Bob",
            "items", List.of(Map.of("sku","SKU1","qty",2,"unitPrice",50.0))
    );
    webTestClient.post()
        .uri("/api/orders")
        .bodyValue(body)
        .exchange()
        .expectStatus().isCreated()
        .expectBody(Map.class)
        .value(created -> assertThat(created.get("id")).isNotNull());
  }

  @Test
  void putOrder_Conflict() {
    Map body = Map.of("customerName","X", "status","PROCESSING");
    webTestClient.put()
        .uri("/api/orders/2")
        .bodyValue(body)
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void patchOrder_OK() {
    Map body = Map.of("status","COMPLETED");
    webTestClient.patch()
        .uri("/api/orders/1")
        .bodyValue(body)
        .exchange()
        .expectStatus().isOk()
        .expectBody(Map.class)
        .value(result -> assertThat(result.get("status")).isEqualTo("COMPLETED"));
  }

  @Test
  void deleteOrder_NoContent() {
    webTestClient.delete()
        .uri("/api/orders/3")
        .exchange()
        .expectStatus().isNoContent();
  }
}

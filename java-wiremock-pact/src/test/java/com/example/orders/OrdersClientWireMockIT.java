package com.example.orders;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OrdersClientWireMockIT {

  @RegisterExtension
  static WireMockExtension wiremock = WireMockExtension.newInstance()
          .options(wireMockConfig().port(4010))
          .build();

  private OrdersClient client;

  @BeforeEach
  void setup() {
    System.setProperty("orders.baseUrl", "http://localhost:4010");
    client = new OrdersClient(WebClientConfig.builder());

    // --- LIST ORDERS ---
    wiremock.stubFor(get(urlPathEqualTo("/orders"))
            .willReturn(okJson("""
        [
          {"id":1,"customerName":"Alice","status":"NEW","totalAmount":120.5}
        ]
        """)));

    // --- GET ORDER ---
    wiremock.stubFor(get(urlPathEqualTo("/orders/1"))
            .willReturn(okJson("""
        {"id":1,"customerName":"Alice","status":"PROCESSING","totalAmount":120.5}
        """)));

    wiremock.stubFor(get(urlPathEqualTo("/orders/9999"))
            .willReturn(notFound()));

    // --- POST CREATE ORDER (valid) ---
    wiremock.stubFor(post(urlPathEqualTo("/orders"))
            .withRequestBody(matchingJsonPath("$.items"))
            .willReturn(aResponse()
                    .withStatus(201)
                    .withHeader("Content-Type","application/json")
                    .withBody("""
            {
              "id": 10,
              "customerName": "Bob",
              "items": [
                { "sku": "SKU1", "qty": 2, "unitPrice": 50.0 }
              ],
              "status": "NEW",
              "totalAmount": 100.0
            }
            """)
            ));

    // --- POST CREATE ORDER (invalid -> 400) ---
    wiremock.stubFor(post(urlPathEqualTo("/orders"))
            .withRequestBody(notMatching(".*items.*"))
            .willReturn(badRequest()));

    // --- PUT UPDATE ORDER ---
    wiremock.stubFor(put(urlPathEqualTo("/orders/1"))
            .willReturn(okJson("""
        {"id":1,"customerName":"Alice","status":"PROCESSING","totalAmount":120.5}
        """)));

    wiremock.stubFor(put(urlPathEqualTo("/orders/2"))
            .willReturn(aResponse().withStatus(409)
                    .withHeader("Content-Type","application/json")
                    .withBody("""
            {"code":"CONFLICT","message":"Version conflict"}
            """)));

    // --- PATCH ---
    wiremock.stubFor(patch(urlPathEqualTo("/orders/1"))
            .willReturn(okJson("""
        {"id":1,"customerName":"Alice","status":"COMPLETED","totalAmount":120.5}
        """)));

    // --- DELETE ---
    wiremock.stubFor(delete(urlPathEqualTo("/orders/3"))
            .willReturn(noContent()));
  }

  @Test
  void listOrders_OK() {
    assertThat(client.listOrders(null).block()).isNotEmpty();
  }

  @Test
  void getOrder_OK() {
    assertThat(client.getOrder(1).block().get("id")).isEqualTo(1);
  }

  @Test
  void getOrder_NotFound() {
    assertThrows(Exception.class, () -> client.getOrder(9999).block());
  }

  @Test
  void createOrder_Created() {
    Map body = Map.of("customerName","Bob",
            "items", List.of(Map.of("sku","SKU1","qty",2,"unitPrice",50.0)));
    assertThat(client.createOrder(body).block().get("id")).isNotNull();
  }

  @Test
  void putOrder_Conflict() {
    assertThrows(Exception.class, () ->
            client.putOrder(2, Map.of("customerName","X", "status","PROCESSING")).block());
  }

  @Test
  void patchOrder_OK() {
    assertThat(client.patchOrder(1, Map.of("status","COMPLETED")).block().get("status"))
            .isEqualTo("COMPLETED");
  }

  @Test
  void deleteOrder_NoContent() {
    client.deleteOrder(3).block();
  }
}

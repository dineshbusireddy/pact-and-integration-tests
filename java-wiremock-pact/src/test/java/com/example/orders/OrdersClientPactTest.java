package com.example.orders;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "OrdersProvider")
public class OrdersClientPactTest {

  @Pact(consumer = "OrdersConsumer")
  public RequestResponsePact pactListOrders(PactDslWithProvider builder) {
    return builder
      .uponReceiving("list orders with optional status")
        .path("/orders").method("GET")
      .willRespondWith()
        .status(200)
        .headers(Map.of("Content-Type", "application/json"))
        .body(PactDslJsonArray.arrayMinLike(1)
          .numberType("id", 1)
          .stringMatcher("customerName", ".+", "Alice")
          .stringMatcher("status", "NEW|PROCESSING|COMPLETED|CANCELLED", "NEW")
          .decimalType("totalAmount", 120.5))
      .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "pactListOrders", pactVersion = PactSpecVersion.V3)
  void testListOrders(MockServer server) {
    System.setProperty("orders.baseUrl", server.getUrl());
    OrdersClient client = new OrdersClient(WebClientConfig.builder());
    List<Map> orders = client.listOrders(null).block();
    assertThat(orders).isNotEmpty();
  }

  @Pact(consumer = "OrdersConsumer")
  public RequestResponsePact pactGetOrderOk(PactDslWithProvider builder) {
    return builder
      .uponReceiving("get order ok")
        .path("/orders/1").method("GET")
      .willRespondWith()
        .status(200)
        .headers(Map.of("Content-Type", "application/json"))
        .body(new PactDslJsonBody()
          .numberType("id", 1)
          .stringType("customerName", "Alice")
          .stringType("status", "PROCESSING")
          .decimalType("totalAmount", 120.5))
      .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "pactGetOrderOk", pactVersion = PactSpecVersion.V3)
  void testGetOrderOk(MockServer server) {
    System.setProperty("orders.baseUrl", server.getUrl());
    OrdersClient client = new OrdersClient(WebClientConfig.builder());
    Map order = client.getOrder(1).block();
    assertThat(order.get("id")).isEqualTo(1);
  }

  @Pact(consumer = "OrdersConsumer")
  public RequestResponsePact pactGetOrderNotFound(PactDslWithProvider builder) {
    return builder
      .uponReceiving("get order 404")
        .path("/orders/9999").method("GET")
      .willRespondWith()
        .status(404)
        .headers(Map.of("Content-Type", "application/json"))
        .body(new PactDslJsonBody()
          .stringType("code", "NOT_FOUND")
          .stringType("message", "Order not found"))
      .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "pactGetOrderNotFound", pactVersion = PactSpecVersion.V3)
  void testGetOrderNotFound(MockServer server) {
    System.setProperty("orders.baseUrl", server.getUrl());
    OrdersClient client = new OrdersClient(WebClientConfig.builder());
    assertThrows(Exception.class, () -> client.getOrder(9999).block());
  }

  @Pact(consumer = "OrdersConsumer")
  public RequestResponsePact pactCreateOrder201(PactDslWithProvider builder) {
    return builder
      .uponReceiving("create order 201")
        .path("/orders").method("POST")
        .headers(Map.of("Content-Type","application/json"))
        .body(Objects.requireNonNull(Objects.requireNonNull(new PactDslJsonBody()
                .stringType("customerName", "Bob")
                .eachLike("items", 1)
                .stringType("sku", "SKU1")
                .integerType("qty", 2)
                .decimalType("unitPrice", 50.0)
                .closeObject()).closeArray()))
      .willRespondWith()
        .status(201)
        .headers(Map.of("Content-Type", "application/json", "Location", "/orders/2"))
        .body(new PactDslJsonBody()
          .numberType("id", 2)
          .stringType("customerName", "Bob")
          .stringMatcher("status", "NEW|PROCESSING|COMPLETED|CANCELLED", "NEW")
          .decimalType("totalAmount", 100.0))
      .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "pactCreateOrder201", pactVersion = PactSpecVersion.V3)
  void testCreateOrder201(MockServer server) {
    System.setProperty("orders.baseUrl", server.getUrl());
    OrdersClient client = new OrdersClient(WebClientConfig.builder());
    Map body = Map.of("customerName","Bob",
                      "items", List.of(Map.of("sku","SKU1","qty",2,"unitPrice",50.0)));
    Map created = client.createOrder(body).block();
    assertThat(created.get("id")).isNotNull();
  }

  @Pact(consumer = "OrdersConsumer")
  public RequestResponsePact pactPutOrder409(PactDslWithProvider builder) {
    return builder
      .uponReceiving("put order conflict")
        .path("/orders/2").method("PUT")
        .headers(Map.of("Content-Type","application/json"))
        .body(new PactDslJsonBody()
          .stringType("customerName", "X")
          .stringType("status", "PROCESSING"))
      .willRespondWith()
        .status(409)
        .headers(Map.of("Content-Type","application/json"))
        .body(new PactDslJsonBody()
          .stringType("code","CONFLICT")
          .stringType("message","Version conflict"))
      .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "pactPutOrder409", pactVersion = PactSpecVersion.V3)
  void testPutOrder409(MockServer server) {
    System.setProperty("orders.baseUrl", server.getUrl());
    OrdersClient client = new OrdersClient(WebClientConfig.builder());
    assertThrows(Exception.class, () -> client.putOrder(2, Map.of("customerName","X","status","PROCESSING")).block());
  }

  @Pact(consumer = "OrdersConsumer")
  public RequestResponsePact pactPatchOrder200(PactDslWithProvider builder) {
    return builder
      .uponReceiving("patch order ok")
        .path("/orders/1").method("PATCH")
        .headers(Map.of("Content-Type","application/json"))
        .body(new PactDslJsonBody().stringType("status","COMPLETED"))
      .willRespondWith()
        .status(200)
        .headers(Map.of("Content-Type","application/json"))
        .body(new PactDslJsonBody()
          .numberType("id",1)
          .stringType("customerName","Alice")
          .stringType("status","COMPLETED")
          .decimalType("totalAmount",120.5))
      .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "pactPatchOrder200", pactVersion = PactSpecVersion.V3)
  void testPatchOrder200(MockServer server) {
    System.setProperty("orders.baseUrl", server.getUrl());
    OrdersClient client = new OrdersClient(WebClientConfig.builder());
    Map resp = client.patchOrder(1, Map.of("status","COMPLETED")).block();
    assertThat(resp.get("status")).isEqualTo("COMPLETED");
  }

  @Pact(consumer = "OrdersConsumer")
  public RequestResponsePact pactDelete204(PactDslWithProvider builder) {
    return builder
      .uponReceiving("delete 204")
        .path("/orders/3").method("DELETE")
      .willRespondWith()
        .status(204)
      .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "pactDelete204", pactVersion = PactSpecVersion.V3)
  void testDelete204(MockServer server) {
    System.setProperty("orders.baseUrl", server.getUrl());
    OrdersClient client = new OrdersClient(WebClientConfig.builder());
    client.deleteOrder(3).block();
  }
}

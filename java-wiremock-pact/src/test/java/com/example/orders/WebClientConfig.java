package com.example.orders;

import org.springframework.web.reactive.function.client.WebClient;

public class WebClientConfig {
  public static WebClient.Builder builder() {
    return WebClient.builder();
  }
}

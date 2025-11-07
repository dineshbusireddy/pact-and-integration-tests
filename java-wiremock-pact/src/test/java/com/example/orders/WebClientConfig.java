package com.example.orders;

import com.example.orders.config.OrdersProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(OrdersProperties.class)
public class WebClientConfig {

  @Bean
  public OrdersClient ordersClient(WebClient.Builder builder,
                                   @Value("${orders.base-url}") String baseUrl) {
    return new OrdersClient(baseUrl);
  }

  @Bean
  public WebClient webClient() {
    return WebClient.builder().build();
  }
}


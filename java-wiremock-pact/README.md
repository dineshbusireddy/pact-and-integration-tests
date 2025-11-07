# Java-only Contract Testing POC (Pact + WireMock from OpenAPI)

- **No Node** required
- **Contracts** with Pact (JUnit 5)
- **Mocks** with WireMock **generated** from `openapi/orders.yaml` via OpenAPI Generator
- Spring Boot **3.3.x**, **Java 17**, client via **WebClient**

## Run
```bash
mvn -q -DskipTests=false test
```
This will:
1. Generate WireMock stubs from OpenAPI
2. Run Pact consumer tests (produces `target/pacts/OrdersConsumer-OrdersProvider.json`)
3. Run WireMock integration test against generated mappings

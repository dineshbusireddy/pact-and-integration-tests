# Prism + Pact (Spring Boot Consumer)

- **Prism** serves mocks from `openapi/orders.yaml` on port 4010
- **Pact** creates consumer contracts; tests run with Pact mock server, and you can hit Prism for integration tests.
- Spring Boot **3.3.x**, **Java 17**, client via **WebClient**

## Run Prism mock
```bash
npm i
npm run mock
# Prism listens on http://localhost:4010
```

## Run tests
```bash
mvn -q -DskipTests=false test
```

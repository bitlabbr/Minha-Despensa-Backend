# Minha Despensa - Backend API 🛒🚀

Backend RESTful e motor de sincronização offline-first para o ecossistema **Minha Despensa** (Kotlin Multiplatform / Compose Multiplatform).

---

## 🛠️ Stack Tecnológica

- **Linguagem:** Kotlin 2.3.x (JVM 17)
- **Framework:** Spring Boot 4.1.x
- **Persistência:** Spring Data JPA + Hibernate (com suporte a Kotlin `allOpen`)
- **Banco de Dados:** PostgreSQL (Domus / Local)
- **Documentação de API:** Springdoc OpenAPI 3 / Swagger UI (v2.8.x)
- **Validação:** Jakarta Bean Validation (Hibernate Validator)
- **Testes:** JUnit 5, Spring Boot Test, Mockito Kotlin

---

## 🏛️ Arquitetura e Organização de Pacotes

A aplicação adota Clean Architecture modularizada por contexto de domínio, com separação estrita de responsabilidades:

```text
com.bitlabbr.minhadespensa
├── common/                  # Componentes compartilhados e infraestrutura transversal
│   ├── config/              # Configurações de OpenAPI/Swagger e CORS (WebConfig)
│   ├── dto/                 # Envelopes ApiResponse<T> e ErrorResponse padronizado
│   ├── entity/              # HouseholdScopedSyncEntity (multi-tenant, updatedAt, serverVersion, isDeleted)
│   └── exception/           # Hierarquia de exceções de domínio e GlobalExceptionHandler
├── controller/              # Endpoints utilitários e diagnósticos (TestController)
└── modules/                 # Módulos funcionais de negócio
    ├── catalog/             # Catálogo de produtos (Entity, DTOs, Repository, Service, Controller)
    ├── pantry/              # Itens da despensa doméstica e datas de validade
    ├── shoppinglist/        # Listas de compras (PLANNED, SCRATCHPAD) e itens
    ├── price/               # Histórico de preços e estabelecimentos
    └── sync/                # Contratos e DTOs de sincronização offline-first (Push / Pull)
```

---

## 🔄 Princípios de Sincronização Offline-First (CRDT / LWW)

Para garantir integridade com o aplicativo cliente móvel (Android / iOS):
1. **Identificadores Imutáveis:** Chaves primárias UUID geradas no cliente ou no backend.
2. **Soft Deletes:** Exclusões não removem fisicamente o registro; o campo `isDeleted` é atualizado como `true`.
3. **Last-Write-Wins (LWW):** Cada registro possui `updatedAt` (epoch millis). No conflito de sincronização, o registro com timestamp mais recente prevalece.
4. **Ponteiro Incremental (`since`):** Requisições de Pull enviam `since` para recuperar somente registros alterados ou deletados após o último checkpoint.

---

## 🚀 Como Executar

### 1. Pré-requisitos
- JDK 17+
- Instância PostgreSQL acessível (ou container Docker)

### 2. Configuração de Banco
As configurações padrão residem em `src/main/resources/application.properties`:
```properties
server.port=8080
spring.application.name=minha-despensa

spring.datasource.url=jdbc:postgresql://<HOST>:5432/despensa
spring.datasource.username=<USER>
spring.datasource.password=<PASSWORD>
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### 3. Executando a Aplicação
```bash
./gradlew bootRun
```

### 4. Documentação Interativa (Swagger UI)
Após iniciar, acesse a documentação interativa e execute chamadas diretamente pelo navegador:
- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON Spec:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 📡 Endpoints Principais da API

| Módulo | Método | Endpoint | Descrição |
| :--- | :--- | :--- | :--- |
| **Sync** | `POST` | `/api/v1/sync/push` | Envio em lote de alterações locais (LWW) |
| **Sync** | `GET` | `/api/v1/sync/pull?since={ts}` | Download de alterações ocorridas desde `since` |
| **Catálogo** | `GET` | `/api/v1/products?q={query}` | Listar/buscar produtos ativos |
| **Catálogo** | `GET` | `/api/v1/products/{id}` | Buscar produto por ID |
| **Catálogo** | `GET` | `/api/v1/products/ean/{ean}` | Buscar produto por código de barras |
| **Catálogo** | `POST` | `/api/v1/products` | Cadastrar novo produto |
| **Catálogo** | `PUT` | `/api/v1/products/{id}` | Atualizar produto existente |
| **Catálogo** | `DELETE` | `/api/v1/products/{id}` | Soft delete de produto |
| **Despensa** | `GET` | `/api/v1/pantry?productId={id}` | Listar itens da despensa |
| **Despensa** | `POST` | `/api/v1/pantry` | Adicionar produto ao estoque da despensa |
| **Despensa** | `DELETE` | `/api/v1/pantry/{id}` | Soft delete de item da despensa |
| **Listas** | `GET` | `/api/v1/shopping-lists` | Listar todas as listas com seus itens |
| **Listas** | `POST` | `/api/v1/shopping-lists` | Criar nova lista de compras |
| **Listas** | `POST` | `/api/v1/shopping-lists/{id}/items` | Adicionar/atualizar item na lista |
| **Diagnóstico** | `GET` | `/api/v1/test/hello` | Health check da aplicação |

---

## 🧪 Execução de Testes Automatizados

Para rodar toda a suíte de testes unitários e de integração (WebMvc / DataJpa):
```bash
./gradlew test
```

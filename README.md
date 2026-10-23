# Minha Despensa - Backend API 🛒🚀

Backend RESTful e motor de sincronização multi-tenant offline-first (v2.1) para o ecossistema **Minha Despensa** (Kotlin Multiplatform / Compose Multiplatform).

---

## 🛠️ Stack Tecnológica

- **Linguagem:** Kotlin 2.3.x (JVM 17)
- **Framework:** Spring Boot 4.1.x
- **Segurança:** Spring Security + OAuth2 Resource Server (JWT HMAC-SHA256)
- **Persistência:** Spring Data JPA + Hibernate (com suporte a Kotlin `allOpen`)
- **Migrações de Banco:** Flyway (Schema multi-tenant versionado)
- **Banco de Dados:** PostgreSQL 15 (Domus / Local)
- **Documentação de API:** Springdoc OpenAPI 3 / Swagger UI com Bearer Auth (v2.8.x)
- **Validação:** Jakarta Bean Validation (Hibernate Validator)
- **Testes:** JUnit 5, Spring Boot Test, Mockito Kotlin (105 testes, 100% verde)

---

## 🏛️ Arquitetura e Organização de Pacotes

A aplicação adota Clean Architecture modularizada por contexto de domínio, com separação estrita de responsabilidades:

```text
com.bitlabbr.minhadespensa
├── common/                  # Componentes compartilhados e infraestrutura transversal
│   ├── config/              # Configurações de OpenAPI/Swagger (BearerAuth) e CORS (WebConfig)
│   ├── dto/                 # Envelopes ApiResponse<T> e ErrorResponse padronizado
│   ├── entity/              # HouseholdEntityId e HouseholdScopedSyncEntity
│   ├── exception/           # Hierarquia de exceções de domínio e GlobalExceptionHandler
│   └── util/                # DeterministicIdUtils (Normalizador e RFC 4122 UUIDv5)
├── controller/              # Endpoints utilitários e diagnósticos (TestController)
└── modules/                 # Módulos funcionais de negócio
    ├── auth/                # Autenticação JWT, Registro, Rotação e Revogação
    ├── household/           # Gestão de Casas, Convites Crockford e Controle de Membros
    ├── catalog/             # Catálogo de Produtos, Categorias e Mídia (Fotos WebP)
    ├── pantry/              # Itens da despensa doméstica e movimentações (Livro-Razão)
    ├── shoppinglist/        # Listas e itens de compras com Cascade Tombstone
    ├── price/               # Histórico de preços por estabelecimento
    ├── budget/              # Orçamentos mensais da casa
    ├── sync/                # Motor de Sincronização Bidirecional v2.1 e Aliases
    ├── invoice/             # Leitura e parsing de Nota Fiscal SEFAZ-PE via QR Code
    └── apprelease/          # Distribuição OTA de APKs Android e Metadados de Versão
```

---

## 🔄 Princípios de Sincronização Offline-First Multi-Tenant (CRDT / LWW)

Para garantir integridade com o aplicativo cliente móvel (Android / iOS):
1. **Multi-Tenancy por Household:** Isolamento absoluto entre casas via chave primária composta `(household_id, id)` e header obrigatório `X-Household-Id`.
2. **Serialização Monotônica (`syncVersion`):** Cada operação de push adquire lock pessimista na linha da casa, incrementa monotonicamente o `syncVersion` e carimba todos os registros do lote.
3. **Soft Deletes ("Cascade Tombstones"):** Exclusões não removem fisicamente o registro; o campo `isDeleted` é atualizado como `true`. Tombstone do pai (ex: lista) força a exclusão dos filhos (itens).
4. **Last-Write-Wins (LWW):** Cada registro possui `updatedAt` (epoch millis). No conflito de sincronização, o registro com timestamp mais recente prevalece.
5. **Deduplicação Híbrida & Aliases Duráveis:** Produtos e categorias duplicados convergem para o registro canônico mais antigo. O servidor registra na tabela `entity_id_aliases` e retorna o mapeamento para ajuste automático das FKs no SQLite do dispositivo móvel.

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

spring.flyway.enabled=true
spring.jpa.hibernate.ddl-auto=validate
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

| Módulo | Método | Endpoint | Autenticação | Descrição |
| :--- | :--- | :--- | :--- | :--- |
| **Auth** | `POST` | `/api/v1/auth/register` | Aberto | Cria usuário, senha BCrypt e casa inicial |
| **Auth** | `POST` | `/api/v1/auth/login` | Aberto | Autentica e emite tokens (Access JWT + Refresh) |
| **Auth** | `POST` | `/api/v1/auth/refresh` | Aberto | Rotação de refresh token com detecção de reuso |
| **Auth** | `POST` | `/api/v1/auth/logout` | Bearer Auth | Revoga refresh token do dispositivo |
| **Auth** | `GET` | `/api/v1/auth/me` | Bearer Auth | Perfil do usuário autenticado |
| **Household** | `GET` | `/api/v1/households` | Bearer Auth | Listar casas do usuário |
| **Household** | `POST` | `/api/v1/households` | Bearer Auth | Criar nova casa |
| **Household** | `POST` | `/api/v1/households/join` | Bearer Auth | Ingressar via código de convite `CASA-XXXXXXXX` |
| **Sync** | `POST` | `/api/v1/sync/push` | Bearer + `X-Household-Id` | Envio em lote das 8 coleções (LWW + Aliases) |
| **Sync** | `GET` | `/api/v1/sync/pull?since={v}` | Bearer + `X-Household-Id` | Download de deltas com `serverVersion > since` |
| **Mídia** | `PUT` | `/api/v1/media/products/{id}` | Bearer + `X-Household-Id` | Upload de foto (WebP/JPEG até 200KB) |
| **Mídia** | `GET` | `/api/v1/media/products/{id}` | Bearer + `X-Household-Id` | Download de foto com suporte a ETag |
| **Mídia** | `DELETE` | `/api/v1/media/products/{id}` | Bearer + `X-Household-Id` | Remove foto e avança `syncVersion` |
| **App Release** | `GET` | `/api/v1/app/version/latest` | Aberto | Consulta versão mais recente do app Android (OTA) |
| **App Release** | `GET` | `/api/v1/app/download/latest` | Aberto | Download em stream binário do APK Android mais recente |
| **Invoice** | `POST` | `/api/v1/invoice/parse` | Aberto | Scraping de nota fiscal via QR Code SEFAZ-PE |
| **Diagnóstico** | `GET` | `/api/v1/test/hello` | Aberto | Health check da aplicação |

---

## 🧪 Execução de Testes Automatizados

Para rodar toda a suíte de testes unitários e de integração:
```bash
./gradlew test
```

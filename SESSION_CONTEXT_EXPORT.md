# Minha Despensa Backend - Exportação Completa de Contexto da Sessão

> **Última Atualização:** 27/09/2026 16:58 (Horário de Brasília)  
> **Repositório:** `Minha-Despensa-Backend`  
> **Branch Base:** `dev`  
> **Destino de Deploy:** `domus-vpn` (100.98.199.97)  
> **Ecosistema Integrado:** [Minha-Dispensa (Cliente KMP)](../Minha-Dispensa)

---

## 1. 🎯 Visão Geral da Sessão

Esta sessão cobriu o ciclo completo de concepção, estruturação, desenvolvimento, testes e implantação do backend para a plataforma **Minha Despensa**:
1. **Inicialização do Projeto (`init`):** Mapeamento do ecossistema e das necessidades do cliente móvel KMP (Compose Multiplatform / Room).
2. **Clean Architecture Modular:** Organização por contexto de domínio (`catalog`, `pantry`, `shoppinglist`, `price`, `sync`).
3. **Mecanismo de Sincronização CRDT / Last-Write-Wins (LWW):** Implementação dos contratos de replicação em lote (Push/Pull) e semântica de soft-delete.
4. **Infraestrutura Transversal:** Envelopes padronizados (`ApiResponse`, `ErrorResponse`), validações com Bean Validation, documentação viva com Swagger UI / OpenAPI 3.
5. **Cobertura de Testes Automatizados:** 17 testes cobrindo regras de negócio, persistência e endpoints REST.
6. **Integração e Deploy no Domus:** Geração do artefato Spring Boot `bootJar` e transferência via SSH/SCP para a pasta Docker no servidor Domus.
7. **Detalhamento Arquitetural:** Documentação do funcionamento da arquitetura Local-First entre o celular e o servidor.

---

## 2. 📱 Como o Servidor Funciona com o App Cliente (Local-First / Offline-First)

### 2.1. O Problema Resolvido
No contexto de compras (ex: dentro de um supermercado com sinal instável ou subsolo), o aplicativo móvel **não pode travar ou bloquear a experiência do usuário** aguardando resposta da rede.
Por isso:
- O aplicativo cliente ([Minha-Dispensa](../Minha-Dispensa)) grava tudo primeiro em seu banco de dados local **Room SQLite**.
- O backend atua como **hub central de sincronização, persistência e backup**.

### 2.2. Diagrama de Comunicação

```text
┌────────────────────────────────────────────────────────┐
│                   SEU CELULAR (App KMP)                │
│                                                        │
│  [Tela do App] ──> [Banco Local SQLite (Room)]         │
│                              ▲                         │
└──────────────────────────────┼─────────────────────────┘
                               │
            Sincronização em 2 passos (quando online)
                               │
               1. PUSH         │         2. PULL
     "Envio o que alterei"     │   "Me dá o que mudou"
        (POST /sync/push)      │   (GET /sync/pull?since)
                               ▼
┌────────────────────────────────────────────────────────┐
│               NOSSO SERVIDOR BACKEND                   │
│                                                        │
│  Spring Boot (Kotlin) na porta 8080                    │
│  Regra LWW (Last-Write-Wins: vence a data mais nova)   │
│                              │                         │
│                              ▼                         │
│                 PostgreSQL no Domus / Local            │
└────────────────────────────────────────────────────────┘
```

### 2.3. Os Dois Passos da Sincronização
1. **Passo 1: Envio Local (`POST /api/v1/sync/push`)**
   - O aplicativo envia um payload consolidado com todas as alterações locais pendentes (produtos, despensa, listas, itens, histórico de preços).
   - O servidor avalia cada item contra o banco usando **Last-Write-Wins (LWW)**: se `incomingUpdatedAt >= existing.updatedAt`, atualiza; se o registro do servidor for mais recente, mantém o existente.
2. **Passo 2: Download Incremental (`GET /api/v1/sync/pull?since={timestamp}`)**
   - O cliente informa o timestamp do último sincronismo realizado (`since`).
   - O servidor devolve apenas os registros modificados ou marcados como excluídos após essa data, acompanhados de um novo `serverTimestamp`.
3. **Soft Delete:**
   - Exclusões nunca apagam linhas fisicamente do banco de dados imediatamente. Elas recebem `isDeleted = true` e o timestamp atualizado. Assim, outros dispositivos conectados ao mesmo lar são informados no próximo **Pull** para remover o item da sua interface local.

---

## 3. 🛠️ Stack Tecnológica Consolidada

- **Linguagem & Plataforma:** Kotlin 2.3.21 / OpenJDK 17
- **Framework Principal:** Spring Boot 4.1.1
- **Camada Web:** Spring Web MVC
- **Persistência:** Spring Data JPA + Hibernate 6.x (com suporte a Kotlin `allOpen`)
- **Driver de Banco:** PostgreSQL
- **Documentação Interativa:** Springdoc OpenAPI UI 2.8.5 (`/swagger-ui.html`)
- **Validação de Entrada:** Jakarta Bean Validation (`spring-boot-starter-validation`)
- **Testes:** JUnit 5, Spring WebMvcTest, Mockito Kotlin 5.4.0

---

## 4. 🏛️ Estrutura de Pacotes e Módulos

```text
com.bitlabbr.minhadespensa
├── common/                  # Componentes reutilizáveis e transversais
│   ├── config/              # OpenApiConfig (Swagger UI) e WebConfig (CORS)
│   ├── dto/                 # ApiResponse<T> e ErrorResponse com detalhamento de campos
│   ├── entity/              # BaseSyncEntity (id, updatedAt, isDeleted)
│   └── exception/           # Hierarquia de exceções de domínio e GlobalExceptionHandler
├── controller/              # Endpoints de diagnóstico (TestController)
└── modules/                 # Módulos verticais de negócio
    ├── catalog/             # Catálogo de produtos (Entity, DTOs, Repository, Service, Controller)
    ├── pantry/              # Itens da despensa doméstica e datas de validade
    ├── shoppinglist/        # Listas de compras (PLANNED, SCRATCHPAD) e itens com soft-delete em cascata
    ├── price/               # Histórico de preços e estabelecimentos
    └── sync/                # Motor de sincronização CRDT (SyncService, SyncController, DTOs de lote)
```

---

## 5. 📡 Mapa de Endpoints REST

| Módulo | Método | Endpoint | Descrição |
| :--- | :--- | :--- | :--- |
| **Sync** | `POST` | `/api/v1/sync/push` | Envia alterações locais em lote via semântica LWW |
| **Sync** | `GET` | `/api/v1/sync/pull?since={ts}` | Baixa deltas incrementais ocorridos desde `since` |
| **Catálogo** | `GET` | `/api/v1/products?q={query}` | Lista produtos ativos ou filtra por nome/marca/EAN |
| **Catálogo** | `GET` | `/api/v1/products/{id}` | Busca produto por UUID |
| **Catálogo** | `GET` | `/api/v1/products/ean/{ean}` | Busca produto por código de barras |
| **Catálogo** | `POST` | `/api/v1/products` | Cadastra novo produto |
| **Catálogo** | `PUT` | `/api/v1/products/{id}` | Atualiza produto via LWW |
| **Catálogo** | `DELETE` | `/api/v1/products/{id}` | Soft delete de produto |
| **Despensa** | `GET` | `/api/v1/pantry?productId={id}` | Lista estoque da despensa |
| **Despensa** | `POST` | `/api/v1/pantry` | Adiciona produto à despensa |
| **Despensa** | `PUT` | `/api/v1/pantry/{id}` | Atualiza quantidade/validade |
| **Despensa** | `DELETE` | `/api/v1/pantry/{id}` | Soft delete de item da despensa |
| **Listas** | `GET` | `/api/v1/shopping-lists` | Lista todas as listas ativas com seus itens |
| **Listas** | `GET` | `/api/v1/shopping-lists/{id}` | Detalhes de uma lista específica |
| **Listas** | `POST` | `/api/v1/shopping-lists` | Cria nova lista de compras |
| **Listas** | `PUT` | `/api/v1/shopping-lists/{id}` | Atualiza nome/orçamento/status |
| **Listas** | `DELETE` | `/api/v1/shopping-lists/{id}` | Soft delete da lista e cascata nos itens |
| **Listas** | `POST` | `/api/v1/shopping-lists/{id}/items`| Adiciona ou atualiza item na lista |
| **Listas** | `DELETE` | `/api/v1/shopping-lists/items/{itemId}` | Soft delete do item da lista |
| **Diagnóstico** | `GET` | `/api/v1/test/hello` | Health check da aplicação |

---

## 6. 🧪 Cobertura de Testes Automatizados

Foram implementados 17 testes automatizados, todos executados e aprovados (`BUILD SUCCESSFUL`):
- [`ProductServiceTest.kt`](./src/test/kotlin/com/bitlabbr/minhadespensa/modules/catalog/service/ProductServiceTest.kt): Criação, LWW de timestamp mais recente, descarte de atualização mais antiga, conflito de EAN, busca e soft-delete.
- [`ProductControllerTest.kt`](./src/test/kotlin/com/bitlabbr/minhadespensa/modules/catalog/controller/ProductControllerTest.kt): Respostas HTTP 200, 404 e validação de campos 400 via `MockMvc`.
- [`PantryServiceTest.kt`](./src/test/kotlin/com/bitlabbr/minhadespensa/modules/pantry/service/PantryServiceTest.kt): Criação, atualização LWW e soft-delete de itens da despensa.
- [`ShoppingListServiceTest.kt`](./src/test/kotlin/com/bitlabbr/minhadespensa/modules/shoppinglist/service/ShoppingListServiceTest.kt): Criação de listas, adição de itens e propagação de soft delete em cascata.
- [`SyncServiceTest.kt`](./src/test/kotlin/com/bitlabbr/minhadespensa/modules/sync/service/SyncServiceTest.kt): Processamento em lote no Push e extração de deltas no Pull.
- [`SyncControllerTest.kt`](./src/test/kotlin/com/bitlabbr/minhadespensa/modules/sync/controller/SyncControllerTest.kt): Integração WebMvc para endpoints `/sync/push` e `/sync/pull`.

---

## 7. 🚀 Infraestrutura de Deploy no Domus

### 7.1. Detalhes do Ambiente
- **Host SSH:** `domus-vpn` (`100.98.199.97`, usuário `willjsan`)
- **Pasta da Aplicação Docker:** `/home/willjsan/domus/docker/apps/despensa/`
- **Compose:** `/home/willjsan/domus/docker/apps/despensa/compose.yaml` (conectado à rede `traefik_proxy` e ao PostgreSQL interno `postgres:5432/despensa`)
- **Container Ativo:** `despensa_api` (`minha-despensa:latest`)

### 7.2. Artefatos Transferidos
O JAR foi compilado via `./gradlew bootJar` (`Minha-Despensa-Backend-0.0.1-SNAPSHOT.jar`, ~63 MB) e enviado via SCP para:
1. `/home/willjsan/domus/docker/apps/Minha-Despensa-Backend-0.0.1-SNAPSHOT.jar`
2. `/home/willjsan/domus/docker/apps/despensa/app.jar`

### 7.3. Como Atualizar o Container Remotamente
```bash
ssh domus-vpn "cd /home/willjsan/domus/docker/apps/despensa && docker compose up -d --build"
```

---

## 8. 🔮 Próximos Passos Recomendados

1. **No Aplicativo Cliente KMP ([Minha-Dispensa](../Minha-Dispensa)):**
   - Configurar o `Ktor HttpClient` com engine adequada (CIO/OkHttp/Darwin) e serialização JSON.
   - Criar `SyncEngineWorker` para orquestrar chamadas periódicas a `/api/v1/sync/push` e `/api/v1/sync/pull`.
   - Armazenar o ponteiro `since` (timestamp do servidor) no DataStore ou Settings local.
2. **No Backend (`Minha-Despensa-Backend`):**
   - Adicionar controle de migrações com **Flyway** para garantir versionamento seguro de DDL no PostgreSQL.
   - Adicionar autenticação JWT (Spring Security) caso a aplicação venha a suportar múltiplos usuários ou lares independentes.

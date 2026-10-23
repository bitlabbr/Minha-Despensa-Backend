# Plano Sub-atômico: Auto-seeding de Categorias Padrão e Isolamento Multi-Tenant

**Módulo(s) Impactado(s):** `household`, `catalog`, `auth`  
**Autor:** Engenheiro de Software Backend Sênior (Tech Lead)  
**Data:** 23 de Outubro de 2026  
**Status:** Em Execução  

---

## 1. Entendimento do Requisito & Análise de Causa Raiz (RCA)

### Contexto / Problema
Ao criar uma nova casa no backend (via registro ou endpoint `POST /api/v1/households`), a entidade da casa era persistida sem que as categorias padrão canônicas de produtos fossem semeadas. O backend dependia exclusivamente de clientes móveis para realizar o push das categorias padrão. Quando o cliente móvel isola a criação de uma nova casa e efetua um `pull` imediato (`sinceVersion=0`), a nova casa deve disponibilizar de imediato as 9 categorias padrão do sistema (`default_category_0..8`), garantindo prontidão operacional para qualquer membro ou dispositivo que ingresse.

### Causa Raiz
Em `HouseholdService.kt` e `AuthService.kt`, a criação de `HouseholdEntity` não invocava uma rotina de auto-seeding de categorias padrão em `ProductCategoryService`.

---

## 2. Etapas Sub-atômicas de Implementação

### Etapa 1: Auto-seeding em `ProductCategoryService`
* Adicionar a lista imutável das 9 categorias padrão em `ProductCategoryService`:
  * "Cereais e Grãos", "Carnes e Proteínas", "Laticínios e Ovos", "Hortifrúti", "Bebidas", "Produtos de Limpeza", "Higiene Pessoal", "Temperos e Condimentos", "Outros".
* Implementar método `@Transactional fun seedDefaultCategories(householdId: String): List<ProductCategoryEntity>` que gera as entidades com `id = "default_category_$index"`, `is_default = true`, `updated_at = 1000L`, `server_version = 1L` e `is_deleted = false`.

### Etapa 2: Integração nos fluxos de criação de casa
* `HouseholdService.createHousehold`: Invocar `productCategoryService.seedDefaultCategories(savedHousehold.id)` e inicializar `savedHousehold.syncVersion = 1L`.
* `AuthService.register`: Invocar `productCategoryService.seedDefaultCategories(savedHousehold.id)` e inicializar `savedHousehold.syncVersion = 1L`.

### Etapa 3: Testes Unitários de Serviço e Controller
* `HouseholdServiceTest`: Validar que ao criar uma casa, `seedDefaultCategories` é chamado e a casa é inicializada com versionamento consistente.
* `ProductCategoryServiceTest`: Validar geração idêntica das 9 categorias padrão com `isDefault = true`.

### Etapa 4: Validação Global, Documentação e Deploy Domus
* Executar `./gradlew test` (100% verde).
* Atualizar `Minha-Despensa-Backend.md`.
* Compilar `bootJar`, transferir para `domus-vpn` via SCP, rebuild no Docker Compose e verificação de logs.
* Limpeza dos registros clonados da casa `8d98cce6-60ed-40a3-91c1-10444c3fdacf` no PostgreSQL.

---

## 3. Critérios de Aceite e DoD
- [ ] Novas casas geradas contêm as 9 categorias padrão na versão 1.
- [ ] Pull com `sinceVersion=0` retorna as 9 categorias sem exigir push prévio do cliente.
- [ ] 100% dos testes unitários verdes no backend.
- [ ] Deploy executado com sucesso e container saudável no Domus.

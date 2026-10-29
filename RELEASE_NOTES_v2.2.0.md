# 🚀 Release Notes — Minha Despensa Backend v2.2.0

> **Versão:** `v2.2.0`  
> **Data de Lançamento:** 29 de Outubro de 2026  
> **Ambiente de Produção:** Domus Home Server (`https://despensa.domus.arpa`)  
> **Repositório:** [bitlabbr/Minha-Despensa-Backend](https://github.com/bitlabbr/Minha-Despensa-Backend)

---

## 🎯 Destaques do Lançamento

A versão **v2.2.0** do backend estabelece o subsistema completo de **Suporte ao Usuário, Triagem de Chamados e Coleta de Diagnósticos**, acompanhado de um **Painel Web SPA Administrativo** em Dark Glassmorphism, autenticação isolada de equipe de suporte (`SYSTEM_ADMIN`) e persistência segura de logs de dispositivos em disco.

---

## 🚀 O Que Há de Novo

### 🐞 1. Módulo Vertical de Suporte & Diagnósticos (`modules/support`)
- **Migração Flyway V2 (`V2__init_support_and_bug_reports.sql`):**
  - Criação da tabela `bug_reports` com chaves primárias UUID, campos de diagnóstico (`app_version`, `device_model`, `os_version`, `user_id`, `household_id`), status e índices de consulta por `created_at` e `status`.
- **Protocolo Amigável de Atendimento:**
  - Geração de código de rastreio no formato `BUG-YYYYMMDD-XXXX` (ex: `BUG-20261029-A1B2`).
- **Endpoint Público de Submissão (`POST /api/v1/support/bug-reports`):**
  - Permite submissão anônima ou autenticada por qualquer cliente móvel com upload multipart opcional do arquivo `device.log`.
  - Tratamento de tamanho máximo (10MB) e persistência no volume seguro `/app/bug-reports/<protocol>.log`.

### 🖥️ 2. Painel Web SPA de Suporte (`/support`)
- **Interface SPA HTML5 / Dark Glassmorphism:**
  - Acesso direto via navegador web em `https://despensa.domus.arpa/support`.
  - Métricas e KPIs em tempo real (Total de Chamados, Abertos, Em Investigação, Resolvidos, Fechados).
  - Tabela responsiva com filtros instantâneos por `status` e `category`.
  - Drawer modal de detalhes com visualizador integrado de log com estética terminal dark.
  - Download direto do arquivo de log do cliente.
- **Transição de Status de Atendimento:**
  - Endpoint `PATCH /api/v1/support/bug-reports/{protocol}/status` para transições entre `OPEN`, `INVESTIGATING`, `RESOLVED` e `CLOSED`.

### 🔐 3. Autenticação Administrativa e Controle de Acesso (RBAC)
- **Login Exclusivo de Administrador (`POST /api/v1/support/auth/login`):**
  - Emissão de JWT dedicado com claim de autoridade `ROLE_SYSTEM_ADMIN`.
  - Credenciais administrativas configuradas via variáveis de ambiente (`SUPPORT_ADMIN_USERNAME` / `SUPPORT_ADMIN_PASSWORD`).
- **Segurança de Endpoints:**
  - Endpoints de listagem, consulta de log e alteração de status estritamente restritos a `SYSTEM_ADMIN`.
  - Abertura controlada (`permitAll`) exclusivamente para visualização da SPA (`/support`) e submissão de chamados (`/api/v1/support/bug-reports`).

---

## 🧪 Qualidade & Cobertura de Testes

- **140 Testes Automatizados 100% Verdes:**
  - Testes unitários para `BugReportService`, armazenamento e leitura de streams de logs.
  - Testes de autorização Spring Security e filtros RBAC para `SYSTEM_ADMIN`.
  - Testes de concorrência multi-dispositivo e sincronização bidirecional inalterados e validados.

---

## 🚢 Procedimento de Atualização no Servidor Domus

```bash
# 1. Empacotar o Fat JAR
./gradlew bootJar -x test

# 2. Transferir para o servidor
scp build/libs/Minha-Despensa-Backend-2.1.0-SNAPSHOT.jar domus-vpn:~/domus/docker/apps/despensa/app.jar

# 3. Reiniciar o container
ssh domus-vpn "cd /home/willjsan/domus/docker/apps/despensa && docker compose up -d --build"
```

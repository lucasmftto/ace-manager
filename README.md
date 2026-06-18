# Ace Manager

Sistema de gestão para academias de tênis. Centraliza o gerenciamento de alunos, professores, agendamento de aulas, cobranças, repasses e fluxo de caixa — substituindo o controle por planilhas.

---

## Módulos

| # | Módulo               | Status |
|---|----------------------|--------|
| 1 | Alunos               | ✅ Implementado |
| 2 | Professores          | 🔲 Pendente |
| 3 | Planos & Pacotes     | 🔲 Pendente |
| 4 | Gestão de Aulas      | 🔲 Pendente |
| 5 | Financeiro do Aluno  | 🔲 Pendente |
| 6 | Repasse de Professores | 🔲 Pendente |
| 7 | Despesas Operacionais | 🔲 Pendente |
| 8 | Fluxo de Caixa       | 🔲 Pendente |
| 9 | Dashboard            | 🔲 Pendente |

---

## Stack

**Backend** — `ace-manager-api/`
- Java 21 + Spring Boot 3.x
- Spring Data JPA + Hibernate + PostgreSQL
- Spring Security com JWT
- Flyway para migrações
- Maven

**Frontend** — `ace-manager-web/`
- Angular 17+ (standalone components)
- Angular Material
- TypeScript (strict mode)
- RxJS

---

## Pré-requisitos

- Java 21+
- Node.js 18+ e npm
- Docker e Docker Compose

---

## Rodando localmente

### 1. Banco de dados

```bash
cd ace-manager-api
docker compose up -d
```

Sobe o PostgreSQL na porta `5432` com:
- Database: `acemanager`
- Usuário: `acemanager`
- Senha: `acemanager`

### 2. Backend

```bash
cd ace-manager-api
./mvnw spring-boot:run
```

API disponível em `http://localhost:8080`.

### 3. Frontend

```bash
cd ace-manager-web
npm install
npm start
```

App disponível em `http://localhost:4200`. As chamadas à API são proxiadas automaticamente via `proxy.conf.json`.

---

## Usuário padrão

| Campo | Valor |
|-------|-------|
| E-mail | lucasmelofavaretto@hotmail.com |
| Senha | 1q2w3e |
| Perfil | ROLE_OWNER |

---

## Testes (backend)

```bash
cd ace-manager-api
./mvnw test
```

Os testes de integração sobem um PostgreSQL real via Testcontainers — Docker precisa estar rodando.

---

## Estrutura

```
ace-manager/
├── ace-manager-api/        # Backend Spring Boot
│   ├── src/
│   └── docker-compose.yml  # Banco de dados para desenvolvimento
├── ace-manager-web/        # Frontend Angular
│   └── src/
├── specs/                  # Especificações de módulos
├── docker-compose.yml      # Ambiente completo (api + banco)
└── CLAUDE.md               # Guia de contribuição para IA
```

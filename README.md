<p align="center">
  <img src="docs/assets/confera-icon.png" width="80" height="80" alt="Confera logo"/>
</p>

<h1 align="center">Confera</h1>

<p align="center">
  Você sabe exatamente quanto dinheiro entrou na sua conta hoje?
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk"/>
  <img src="https://img.shields.io/badge/Spring_Boot-3-green?style=flat-square&logo=springboot"/>
  <img src="https://img.shields.io/badge/Angular-22-red?style=flat-square&logo=angular"/>
  <img src="https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square&logo=postgresql"/>
  <img src="https://img.shields.io/badge/Docker-ready-2496ED?style=flat-square&logo=docker"/>
</p>

---

## A história por trás do projeto

Imagine um dono de lanchonete que vendeu R$ 800 num dia — R$ 400 no cartão, R$ 300 no Pix, R$ 100 no boleto. No dia seguinte ele olha a conta e tem R$ 620. Cadê os outros R$ 180?

Será que a operadora cobrou taxa errada? O boleto não compensou? Teve um estorno que ele não viu? Ele simplesmente não sabe. E a maioria dos pequenos negócios no Brasil passa por isso todo mês, perdendo dinheiro sem perceber.

**O Confera existe para resolver esse problema.**

Ele cruza tudo que o negócio vendeu com tudo que de fato caiu na conta bancária — e mostra com precisão onde está cada centavo, o que ainda vai chegar e onde o dinheiro está escorregando.

---

## O que o Confera faz

O fluxo é simples:

**1. Você importa os arquivos** — o extrato do banco (aquele arquivo CNAB que todo banco gera) e o relatório da sua operadora de cartão (Cielo, Stone, Rede, etc).

**2. O sistema processa tudo em segundo plano** — sem travar a tela, sem esperar. Você importa e continua trabalhando.

**3. O motor de conciliação entra em ação** — ele tenta casar cada venda com o crédito correspondente no banco. O que bate, fecha. O que não bate, vira um alerta para você investigar.

**4. Você vê o resultado no dashboard** — posição de caixa real, recebíveis a vencer, divergências abertas e relatório exportável.

O resultado prático:
- ✅ Essa venda de R$ 97,80 liquidou corretamente — taxa de 2,2% aplicada certo
- ⚠️ Essa venda de R$ 300 ainda não caiu — devia ter chegado ontem
- ❌ Essa taxa foi cobrada R$ 4,00 a mais do que o seu contrato prevê

---

## Como o sistema foi construído

O Confera foi projetado como um **monólito modular** — um único projeto bem organizado internamente, com cada área do sistema isolada em seu próprio módulo. É uma decisão consciente: evita a complexidade de microsserviços para um projeto solo, mas mantém as fronteiras limpas para uma eventual extração futura.

```
Angular 22 (o que o usuário vê)
        │
        ▼
Spring Boot 3 + Java 21 (o cérebro)
├── identity       → quem pode acessar o quê (JWT + multi-tenant)
├── ledger         → o livro-razão imutável (partidas dobradas)
├── ingestion      → leitura dos arquivos CNAB/OFX
├── reconciliation → o motor que casa vendas com extratos
├── billing        → régua de cobrança e alertas
└── notification   → e-mails e webhooks
        │
        ▼
PostgreSQL · MongoDB · Redis · RabbitMQ · MinIO
```

---

## Por que essas tecnologias?

Cada escolha técnica tem uma razão — não foi tecnologia por tecnologia.

| O quê | Por quê |
|---|---|
| **Monólito modular** | Um projeto solo não precisa da complexidade de microsserviços. As fronteiras estão prontas para extração quando fizer sentido. |
| **Java 21 com Virtual Threads** | Processa muitos arquivos simultaneamente sem travar, sem a dificuldade do código reativo. |
| **Ledger de partidas dobradas** | Todo centavo tem origem e destino. Saldo nunca é editado — é sempre calculado a partir dos lançamentos. Impossível ficar errado. |
| **Transactional Outbox** | Garante que nenhum evento se perde entre salvar no banco e publicar na fila. Consistência sem gambiarras. |
| **Redis para idempotência** | Importar o mesmo extrato duas vezes não duplica lançamentos. O Redis garante isso. |
| **MinIO** | Arquivos CNAB grandes são lidos em streaming direto do storage, sem estourar a memória do servidor. |
| **MongoDB** | Cada operadora de cartão entrega um relatório com formato diferente. O Mongo guarda cada um "como veio", sem forçar um schema rígido. |

> Os racionais completos com trade-offs estão em [`docs/adr/`](docs/adr/)

---

## Rodando o projeto localmente

Você precisa ter instalado: **Docker**, **Java 21** e **Node 20+**.

```bash
# 1. Clona o repositório
git clone git@github.com:higoronevesdev/confera.git
cd confera

# 2. Sobe toda a infraestrutura com um comando
docker compose -f infra/docker-compose.yml up -d

# 3. Sobe o backend
cd backend && ./mvnw spring-boot:run

# 4. Sobe o frontend (em outro terminal)
cd frontend && npm install && ng serve
```

Acesse **http://localhost:4200** e o sistema estará rodando.

---

## Estrutura do repositório

```
confera/
├── backend/          # Spring Boot 3 — toda a lógica de negócio
│   └── src/
│       └── main/
│           └── java/
│               └── dev/confera/
│                   ├── identity/       # autenticação e usuários
│                   ├── ledger/         # livro-razão de partidas dobradas
│                   ├── ingestion/      # parsing CNAB/OFX
│                   ├── reconciliation/ # motor de conciliação
│                   ├── billing/        # cobrança e régua de alertas
│                   └── notification/   # e-mails e webhooks
├── frontend/         # Angular 22 — interface do usuário
├── infra/            # Docker Compose e configurações
├── docs/
│   ├── adr/          # Registro das decisões arquiteturais
│   └── assets/       # Ícone, diagramas e imagens
└── .github/
    └── workflows/    # CI/CD — testes e build automáticos
```

---

## Roadmap

- [x] Modelagem do banco de dados (ledger + conciliação)
- [x] Estrutura do monorepo e README
- [ ] MVP — ledger funcionando + conciliação básica via OFX
- [ ] V1 — CNAB 240/400 + motor de matching completo
- [ ] V2 — multi-tenant + simulação Open Finance + antecipação de recebíveis

---

## Sobre o projeto

O Confera nasceu como projeto de portfólio, mas foi construído com o mesmo cuidado de um produto real. Cada decisão técnica foi tomada pensando em como sistemas financeiros de verdade funcionam — não em como impressionar, mas em como resolver o problema certo da forma certa.

Desenvolvido por **Higor Oliveira** — desenvolvedor full stack em formação, com experiência no setor financeiro (cooperativas de crédito).

[GitHub](https://github.com/higoronevesdev) · [LinkedIn](https://linkedin.com/in/higoroliveira)

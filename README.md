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

Será que a operadora de cartão cobrou uma taxa errada? O boleto não compensou? Teve um estorno que ele não viu? Ele simplesmente não sabe. E a maioria dos pequenos negócios no Brasil passa por isso todo mês, perdendo dinheiro sem perceber.

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

# EntradaSegura

**Sistema inteligente para controle de entrada e saída de alunos em instituições de ensino.**

## 📌 Sobre o projeto

O **EntradaSegura** é uma proposta de solução digital desenvolvida no contexto acadêmico de **Análise e Desenvolvimento de Sistemas**, com o objetivo de tornar o processo de entrada e saída de alunos mais **rápido, organizado, seguro e rastreável**.

A solução busca conectar **escola, responsáveis e alunos** por meio de uma plataforma simples, permitindo o gerenciamento das solicitações de saída, identificação de responsáveis autorizados e registro dos horários de entrada e saída.

## 🎯 Problema

O processo de saída escolar pode se tornar complexo principalmente em horários de grande movimento. Filas, conferências manuais, dificuldade para localizar alunos e falta de informações em tempo real podem gerar:

* Demora para os responsáveis;
* Sobrecarga dos funcionários;
* Falhas de comunicação;
* Dificuldade de rastreamento;
* Insegurança durante a retirada dos alunos.

## 💡 Proposta

O EntradaSegura propõe digitalizar esse processo por meio de:

* 📱 Aplicativo ou interface para responsáveis;
* 🖥️ Painel de controle para a escola;
* 🔐 Cadastro de responsáveis autorizados;
* 📋 Solicitação antecipada de retirada;
* 🔔 Atualizações sobre o status da solicitação;
* 🕐 Registro dos horários de entrada e saída;
* 📊 Histórico das movimentações.

## 🚀 MVP

A primeira versão do projeto terá como foco apenas as funcionalidades essenciais:

1. Cadastro e autenticação do responsável;
2. Cadastro dos alunos;
3. Cadastro de responsáveis autorizados;
4. Solicitação de retirada;
5. Painel para funcionários;
6. Confirmação da retirada;
7. Registro do horário;
8. Histórico básico.

### Fluxo principal

```text
Responsável
     ↓
Solicita a retirada
     ↓
Sistema registra a solicitação
     ↓
Funcionário localiza o aluno
     ↓
Responsável é identificado
     ↓
Funcionário confirma a retirada
     ↓
Sistema registra data e horário
     ↓
Saída concluída
```

## 🎯 Objetivo

Desenvolver e validar uma solução capaz de reduzir o tempo de espera durante a saída escolar, melhorar a organização dos funcionários e oferecer aos responsáveis maior transparência e segurança sobre o processo.

## 👤 Público-alvo

**Cliente:** instituições de ensino particulares.

**Usuários:**

* Responsáveis pelos alunos;
* Funcionários responsáveis pelo controle de entrada e saída;
* Gestão escolar.

## 💎 Proposta de Valor

> **Uma saída escolar mais rápida, segura e organizada, reduzindo filas e transformando um processo manual em uma experiência digital e rastreável.**

## 🔎 Validação

Antes da implementação completa, a proposta será validada por meio de entrevistas e coleta de feedback com responsáveis, funcionários e gestores escolares.

Algumas hipóteses que serão investigadas:

* A saída escolar é percebida como um processo demorado;
* Responsáveis valorizam informações em tempo real;
* Escolas possuem interesse em reduzir filas e trabalho manual;
* O registro digital das retiradas aumenta a percepção de segurança.

## 🛠️ Tecnologias

<<<<<<< HEAD
* **Frontend:** React, TypeScript, Vite, Tailwind CSS e shadcn/ui;
* **Backend:** Java 21 e Spring Boot 3 (POO, Spring Data JPA, Spring Security);
* **Banco de dados:** MySQL 8.4 com migrações Flyway;
* **Tempo real:** Spring WebSocket com STOMP;
* **Versionamento e CI:** Git, GitHub e GitHub Actions.

As decisões técnicas e regras obrigatórias estão em [`CLAUDE.md`](CLAUDE.md).

### Como executar o painel da portaria (etapa atual)

```bash
cd frontend
npm install
npm run dev
```

Assim, o painel roda em **modo demonstração**, com dados 100% fictícios. Para usá-lo com a API Java real
(MySQL + JWT), use `npm run dev:http` e siga [`docs/INTEGRACAO.md`](docs/INTEGRACAO.md).
=======
As tecnologias serão definidas durante a evolução do projeto. Entre as possibilidades estão:

* **Frontend:** HTML, CSS, JavaScript ou framework web;
* **Backend:** Java / Spring Boot;
* **Banco de dados:** PostgreSQL ou Firebase;
* **Autenticação:** sistema de login e controle de acesso;
* **Versionamento:** Git e GitHub.
>>>>>>> 40e9dccddba1b85a3c8061167fde6a16f66d4106

## 📚 Contexto acadêmico

Projeto desenvolvido como parte da disciplina de **Desenvolvimento de Projeto (UPX2)** do curso de **Análise e Desenvolvimento de Sistemas**.

O projeto contempla etapas de:

* Cultura empreendedora;
* Identificação de dor de mercado;
* Validação;
* Desenvolvimento de persona;
* Ideação;
* Solução;
* Proposta de valor;
* Construção de MVP.

## 📈 Evolução planejada

```text
[01] Identificação do problema
        ↓
[02] Desenvolvimento da Persona
        ↓
[03] Validação da hipótese
        ↓
[04] Ideação
        ↓
[05] Definição da Proposta de Valor
        ↓
[06] Prototipação
        ↓
[07] Desenvolvimento do MVP
        ↓
[08] Testes e validação
```

---

### Status

🚧 **Em desenvolvimento — fase de concepção e validação**

> O EntradaSegura é inicialmente um projeto acadêmico, mas foi pensado com uma abordagem de produto real, permitindo sua evolução para um MVP funcional.

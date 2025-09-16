Boa, Gil 👌 Vamos montar o **README.md** para o seu projeto de **Controle de Despesas em Java com Spring Boot**, seguindo exatamente o estilo do exemplo da calculadora, mas adaptado para sua aplicação.

---

# 💰 Controle de Despesas - Spring Boot + Thymeleaf + PostgreSQL

Este projeto é uma aplicação web desenvolvida em **Java com Spring Boot** para gerenciamento de despesas pessoais.
O objetivo é aplicar boas práticas de **Clean Code, SOLID e Padrões de Projeto**, além de consolidar conhecimentos em **Spring MVC, JPA e Docker**.

---

## 🚀 Tecnologias Utilizadas

* **Java 17+**
* **Spring Boot 3+**

  * Spring Web (MVC)
  * Spring Data JPA
  * Validation (Jakarta Bean Validation)
  * Thymeleaf (template engine)
* **PostgreSQL** (com Docker Compose)
* **Bootstrap 5** (UI responsiva)
* **Maven** (build e gerenciamento de dependências)

---

## 📂 Estrutura do Projeto

```
despesas/
│
├── src/main/java/com/gilrossi/despesas
│   ├── controller/         # Controladores MVC
│   ├── model/              # Entidades JPA
│   ├── repository/         # Repositórios (Spring Data JPA)
│   ├── service/            # Regras de negócio
│   └── DespesasApplication.java  # Classe principal
│
├── src/main/resources/
│   ├── templates/despesas/ # Views Thymeleaf (form.html, lista.html)
│   ├── application.properties
│   └── static/             # Recursos estáticos (css, js)
│
├── docker-compose.yml       # Configuração do PostgreSQL
└── pom.xml                  # Dependências Maven
```

---

## 🛠 Princípios Aplicados

### **Clean Code**

* Métodos coesos e com responsabilidades claras
* Nomenclatura expressiva para classes, métodos e variáveis
* Separação entre camadas (Controller, Service, Repository)

### **SOLID**

* **S**ingle Responsibility: cada classe com apenas uma responsabilidade
* **O**pen/Closed: serviços abertos para extensão, fechados para modificação
* **L**iskov Substitution: entidades e serviços substituíveis sem quebrar o código
* **I**nterface Segregation: repositórios e serviços especializados
* **D**ependency Inversion: injeção de dependência do Spring

### **Design Patterns**

* **MVC Pattern** → separação clara entre Model, View e Controller
* **Repository Pattern** → abstração da camada de persistência
* **Service Layer Pattern** → encapsulamento da lógica de negócios

---

## ✨ Funcionalidades

* **Cadastrar despesas** (descrição, valor, data, categoria)
* **Listar despesas** em tabela organizada (Bootstrap)
* **Validar dados**:

  * Descrição obrigatória e até 100 caracteres
  * Valor positivo obrigatório
  * Data obrigatória e não futura
  * Categoria obrigatória
* **Excluir despesas** individualmente
* **Formatação de valores monetários** no padrão brasileiro

---

## 💻 Como Executar

### Pré-requisitos

* JDK 17+
* Maven 3.9+
* Docker + Docker Compose

### Passo 1: subir o PostgreSQL no Docker

```bash
docker-compose up -d
```

### Passo 2: rodar o projeto

```bash
mvn spring-boot:run
```

### Passo 3: acessar no navegador

```
http://localhost:8080/despesas
```

---

## 🎯 Fluxo de Operação

```
Página inicial → Listagem de despesas → Nova despesa → Salvar → Validação → Listagem atualizada
```

---

## 🧪 Recursos Técnicos

* Persistência com **Spring Data JPA** e PostgreSQL
* Validações automáticas com **Jakarta Validation**
* Templates dinâmicos com **Thymeleaf**
* Estilo moderno com **Bootstrap 5**
* Integração simplificada via **Docker Compose**

---

## 📚 Próximos Passos

* Implementar **edição de despesas**
* Criar relatórios mensais com gráficos (Recharts/Chart.js)
* Implementar autenticação (Spring Security)
* Adicionar testes unitários e de integração (JUnit + Mockito)
* Configurar deploy em ambiente cloud (Heroku, AWS, Azure ou Hostinger VPS)

---

## 👨‍💻 Autor

**Gil Rossi Aguiar**
📧 [gilrossi.aguiar@live.com](mailto:gilrossi.aguiar@live.com)
💼 [LinkedIn](https://www.linkedin.com/in/gil-rossi-5814659b/)
🐙 [GitHub](https://github.com/GilRossi)

---

👉 Quer que eu também prepare um **passo a passo de como rodar esse projeto com Docker Compose (backend + banco juntos)**, para que você não precise do Maven rodando local?

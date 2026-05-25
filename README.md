# 🚍 RotaUrbana

**Sistema de Rastreamento de Transporte Universitário em Tempo Real**

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

## 📋 Sobre o projeto

Este sistema foi desenvolvido como parte da disciplina de **Projeto Integrador** do curso de Ciência da Computação. O objetivo é permitir o **rastreamento em tempo real de veículos de transporte universitário**, facilitando o acesso à localização do ônibus pelos passageiros e melhorando a organização do transporte.

### Problema

- Falta de informação sobre a localização do ônibus
- Atrasos sem aviso prévio
- Falta de previsibilidade no trajeto

### Solução

- Envio da localização em tempo real pelo motorista via aplicação web
- Visualização do ônibus em mapa interativo (Leaflet.js)
- Acompanhamento do trajeto pelos passageiros
- Confirmação de presença diária nas rotas
- Gestão de frota, motoristas e rotas pelo administrador

## ✨ Funcionalidades

### 👤 Passageiro
- Inscrever-se em ônibus (por código) e rotas
- Visualizar localização do ônibus/rota em mapa interativo
- Confirmar presença diária (vai e volta, só vai, só volta, não vai)
- Acompanhar status de pagamento
- Gerenciar inscrições em rotas

### 🚗 Motorista
- Enviar coordenadas GPS em tempo real (associadas a um ônibus ou rota)
- Visualizar frota de ônibus atribuída
- Consultar rotas dos seus veículos

### 🛠️ Administrador
- CRUD de veículos e motoristas
- Criação e gerenciamento de rotas
- Visualização de detalhes de rota com passageiros e presença
- Gerenciamento de status de pagamento dos passageiros

## 🛠️ Stack

| Categoria | Tecnologia |
|-----------|-----------|
| **Linguagem** | Java 21 |
| **Framework** | Spring Boot 4.0.4 |
| **Database** | PostgreSQL 16 |
| **Migração** | Flyway |
| **Autenticação** | Spring Security + JWT (Auth0 java-jwt 4.4.0) |
| **ORM** | Spring Data JPA / Hibernate |
| **Frontend** | Thymeleaf, HTML5, CSS3, JavaScript |
| **Mapas** | Leaflet.js + OpenStreetMap |
| **Build** | Maven 3.9+ |
| **Outros** | Lombok, Spring Validation, Spring DevTools |

## 🚀 Como executar

### Pré-requisitos

- JDK 21
- Maven 3.9+ (ou use o Maven Wrapper incluso)
- PostgreSQL 16

### Passo a passo

1. **Clone o repositório**

```bash
git clone https://github.com/kappannotavio/rotaurbana.git
cd rotaurbana
```

2. **Crie o banco de dados**

```sql
CREATE DATABASE rotaurbana;
```

3. **Configure as variáveis de ambiente**

Copie o arquivo de exemplo e ajuste as credenciais do banco e a secret JWT:

```bash
cp src/main/resources/application-example.yml src/main/resources/application.yml
```

Edite `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/rotaurbana
    username: postgres
    password: SUA_SENHA
api:
  security:
    token:
      secret: "UMA_SECRET_ALEATORIA_SEGURA"
```

4. **Execute com Maven**

```bash
./mvnw spring-boot:run
```

O servidor iniciará em `http://localhost:8080`.

## 👥 Usuários padrão

O sistema cria automaticamente 3 usuários na primeira execução:

| Papel | Email | Senha | Nome |
|-------|-------|-------|------|
| **ADMIN** | admin@rotaurbana.com | admin123 | Administrador |
| **DRIVER** | driver@rotaurbana.com | driver123 | Motorista Padrao |
| **USER** | user@rotaurbana.com | user123 | Usuario Padrao |

> ⚠️ Altere as senhas em produção!

## 📁 Estrutura do projeto

```
src/
├── main/java/io/github/uri/rotaurbana/
│   ├── RotaurbanaApplication.java
│   ├── config/               # Segurança, JWT, dados iniciais
│   ├── controller/
│   │   ├── miscController/   # Páginas Thymeleaf (admin, driver, passenger, map)
│   │   └── restController/   # REST API (admin, driver, passenger, auth, upload)
│   ├── dto/request/          # DTOs de requisição
│   ├── dto/response/         # DTOs de resposta
│   ├── entity/               # Entidades JPA (User, Driver, Bus, Routes, Presence)
│   ├── enums/                # Role (ADMIN/USER/DRIVER), PaymentStatus
│   ├── repository/           # Repositórios Spring Data JPA
│   └── service/              # Lógica de negócio (admin, driver, passenger, tracking)
└── main/resources/
    ├── templates/            # Templates Thymeleaf
    ├── static/               # CSS, JS, imagens
    └── db/migration/         # Migrations Flyway
```

## 📡 API REST

### Autenticação
| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/auth/login` | Login (email + senha → JWT) |
| POST | `/auth/register` | Registrar nova conta |

### Admin (`/api/admin/{adminId}`)
| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/buses` | Listar todos os ônibus |
| GET | `/routes` | Listar rotas com estatísticas |
| POST | `/routes` | Criar nova rota |
| GET | `/drivers` | Listar motoristas |
| POST | `/buses` | Cadastrar ônibus |
| POST | `/drivers` | Criar motorista |
| GET | `/routes/{routeId}/details` | Detalhes da rota |
| PUT | `/routes/{routeId}/payment` | Atualizar status de pagamento |

### Motorista (`/api/driver/{driverId}`)
| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/tracking/{busId}` | Enviar localização do ônibus |
| POST | `/routes/{routeId}/tracking` | Enviar localização da rota |
| GET | `/buses` | Listar ônibus do motorista |
| GET | `/buses/{busId}/routes` | Listar rotas de um ônibus |

### Passageiro (`/api/passenger/{userId}`)
| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/tracking/{busId}` | Obter localização do ônibus |
| GET | `/tracking-by-route/{routeId}` | Obter localização da rota |
| POST | `/subscribe` | Inscrever-se em ônibus (por placa) |
| POST | `/subscribe-by-code` | Inscrever-se em ônibus (por código) |
| POST | `/subscribe-by-route` | Inscrever-se em rota (por código) |
| GET | `/buses` | Listar ônibus inscritos |
| GET | `/all-routes` | Listar todas as rotas inscritas |
| POST | `/confirm-presence` | Confirmar presença na rota |
| DELETE | `/routes/{routeId}` | Sair de uma rota |

### Arquivos
| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/api/upload/image` | Upload de imagem de perfil |

### Usuário (`/user`)
| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/{id}` | Obter dados do usuário |
| PUT | `/{id}` | Atualizar perfil |

## 🗺️ Arquitetura de rastreamento

O rastreamento é feito **in-memory** usando um `ConcurrentHashMap`:

1. **Motorista** envia coordenadas via POST → armazenadas com timestamp
2. **Passageiro** consulta via GET → retorna localização se dentro do TTL de **5 segundos**
3. Se o TTL expirar (motorista parou de enviar), retorna `null` — indicando localização indisponível

> ⚠️ Atualmente não há WebSocket — o mapa atualiza via polling do frontend.

## 🔐 Autenticação

- JWT com algoritmo **HMAC256** e expiração de **2 horas**
- O token é enviado via header `Authorization: Bearer <token>` ou cookie `token`
- `SecurityFilter` intercepta requisições e valida o JWT antes de cada rota protegida

## 📄 Licença

Distribuído sob a licença MIT. Veja `LICENSE` para mais informações.

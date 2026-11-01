# 📅 Elo Vet — Microsserviço de Agendamento

> **FIAP Challenge 2026 — Clyvo Vet** | **Disciplina:** Mastering Relational and Non-Relational Database — integração com outras linguagens

Microsserviço em **Java (Spring Boot)** que gerencia os **agendamentos de consulta** do Elo Vet sobre a mesma base **Oracle** modelada na disciplina ([EloVet-Mastering-Relational-Database](https://github.com/Challenge-2026-EloVet/EloVet-Mastering-Relational-Database)). Ele integra os três pontos pedidos:

1. **Banco relacional no backend:** cria, altera e consulta registros de `elo_agendamento`.
2. **Procedures chamadas pela aplicação:** executa `pkg_elovet.pr_agenda_json`, `pkg_elovet.pr_totaliza_idade_pet` e `pkg_elovet.fn_valida_cpf`. Além disso, toda gravação em `elo_agendamento` dispara a trigger `trg_auditoria_agendamento`, que chama `pkg_elovet.pr_audita_agendamento`.
3. **Relacional → JSON → MongoDB:** uma rotina diária lê `elo_auditoria`, cujos valores já são gerados em JSON pelo `pkg_elovet.fn_monta_json`, e grava esses registros na collection `auditoria_agendamento` do MongoDB.

---

## 👥 Integrantes

| Nome | RM |
| --- | --- |
| Arthur Graciani | RM561728 |
| Gustavo Oliveira | RM566358 |
| João Pedro Scarpin | RM565421 |
| Lucas Hideki | RM565355 |
| Wesley Andrade | RM563593 |

---

## 🏗️ Arquitetura

```mermaid
flowchart LR
    C[Portal do Veterinário<br/>ou api.http] -->|REST| S[elo-vet-agendamento<br/>Spring Boot]
    S -->|JPA: INSERT / UPDATE / SELECT| A[(elo_agendamento)]
    S -->|CALL| P[pkg_elovet<br/>procedures e functions]
    A -->|trigger trg_auditoria_agendamento| P
    P -->|fn_monta_json| AU[(elo_auditoria<br/>valores em JSON)]
    S -->|agendador diário<br/>ou sob demanda| AU
    S -->|documentos| M[(MongoDB<br/>auditoria_agendamento)]
    subgraph Oracle
        A
        P
        AU
    end
```

### Fluxo da auditoria

1. A aplicação grava um agendamento (`INSERT` ou `UPDATE`) em `elo_agendamento`.
2. A trigger `trg_auditoria_agendamento` chama `pkg_elovet.pr_audita_agendamento`, que monta os valores anteriores e novos em JSON (`fn_monta_json`) e grava em `elo_auditoria`.
3. Todo dia às **02:00** (horário de Brasília), o `AuditoriaScheduler` copia para o MongoDB os registros de `elo_auditoria` ainda não sincronizados. O mesmo processo pode ser disparado na hora por `POST /auditorias/sincronizacao`.

A sincronização é **incremental e idempotente**: o `_id` do documento é o próprio `id_auditoria`, e cada execução busca no Oracle apenas os ids maiores que o último já presente no MongoDB.

---

## 🍃 Estrutura no MongoDB

Collection **`auditoria_agendamento`** (criada automaticamente na primeira gravação):

```json
{
  "_id": 12,
  "usuario": "RM566358",
  "operacao": "UPDATE",
  "dataHoraOperacao": "2026-11-02T10:15:00",
  "tabelaAfetada": "ELO_AGENDAMENTO",
  "idRegistroAfetado": 9,
  "valoresAnteriores": { "id_agendamento": 9, "id_pet": 1, "id_veterinario": 1, "id_clinica": 1, "status": "AGENDADO", "data_hora": "10/12/2026 14:30" },
  "valoresNovos":      { "id_agendamento": 9, "id_pet": 1, "id_veterinario": 1, "id_clinica": 1, "status": "CONFIRMADO", "data_hora": "10/12/2026 14:30" },
  "sincronizadoEm": "2026-11-03T02:00:00"
}
```

Decisões de modelagem (NoSQL orientado a documentos):

- **Um documento por evento de auditoria**, com os valores anteriores e novos **embutidos como subdocumentos**. O JSON gerado no Oracle vira estrutura consultável, e não texto.
- **Índices** em `operacao` e `idRegistroAfetado`, os campos usados para filtrar o histórico.
- Exemplos de consulta no `mongosh`:

```javascript
db.auditoria_agendamento.find({ idRegistroAfetado: 9 }).sort({ _id: 1 })    // histórico de um agendamento
db.auditoria_agendamento.find({ "valoresNovos.status": "CANCELADO" })       // agendamentos cancelados
db.auditoria_agendamento.countDocuments({ operacao: "INSERT" })             // total de agendamentos criados
```

Se o conteúdo vindo do Oracle não for um JSON válido, ele é preservado no campo `conteudoOriginal` em vez de ser descartado.

---

## 📡 Endpoints

### Agendamentos (Oracle)

| Método | Rota | Descrição |
| --- | --- | --- |
| `POST` | `/agendamentos` | Cria um agendamento (status padrão `AGENDADO`) |
| `PUT` | `/agendamentos/{id}` | Altera pet, responsável, veterinário, clínica, data/hora, status e observação |
| `PATCH` | `/agendamentos/{id}/status` | Altera apenas o status (ex.: `CONFIRMADO`, `CANCELADO`) |
| `GET` | `/agendamentos/{id}` | Consulta um agendamento |
| `GET` | `/agendamentos` | Lista com filtros opcionais: `idResponsavel`, `idPet`, `idVeterinario`, `status` |

Regras aplicadas antes de gravar:

- O pet precisa estar vinculado ao responsável (`elo_pet_responsavel`).
- O veterinário precisa atender na clínica (`elo_clinica_veterinario`).
- A data/hora deve ser futura.
- Agendamentos `CONCLUIDO`, `CANCELADO` ou `NAO_COMPARECEU` não podem mais ser alterados.

Violações de regra retornam **422**; dados ausentes ou mal formatados, **400**; agendamento inexistente, **404**.

### Procedures e functions do `pkg_elovet`

| Método | Rota | Objeto chamado |
| --- | --- | --- |
| `GET` | `/procedures/agenda-json` | `pkg_elovet.pr_agenda_json` (a saída do `DBMS_OUTPUT` é lida pela aplicação e devolvida como JSON) |
| `GET` | `/procedures/totaliza-idade-pet` | `pkg_elovet.pr_totaliza_idade_pet` |
| `GET` | `/procedures/valida-cpf/{cpf}` | `pkg_elovet.fn_valida_cpf` |

### Catálogo (apoio ao formulário do portal)

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/catalogo/clinicas` | Clínicas |
| `GET` | `/catalogo/clinicas/{idClinica}/veterinarios` | Veterinários que atendem na clínica (`elo_clinica_veterinario`) |
| `GET` | `/catalogo/responsaveis` | Responsáveis (tutores) |
| `GET` | `/catalogo/responsaveis/{idResponsavel}/pets` | Pets do responsável (`elo_pet_responsavel`) |

As respostas de agendamento já trazem os nomes de pet, responsável, veterinário e clínica, prontos para exibição no portal.

### Auditoria (MongoDB)

| Método | Rota | Descrição |
| --- | --- | --- |
| `POST` | `/auditorias/sincronizacao` | Executa a sincronização Oracle → MongoDB na hora |
| `GET` | `/auditorias` | Lista a auditoria do MongoDB; filtro opcional `idAgendamento` |

> O arquivo [`api.http`](./api.http) percorre todos esses endpoints na ordem da demonstração.

---

## 🛠️ Tecnologias

- Java 25 e Spring Boot 4 (Web MVC, Data JPA, Data MongoDB, Validation, Scheduling)
- Oracle Database (driver `ojdbc11`) com os scripts do repositório de banco de dados
- MongoDB
- Docker Compose (MongoDB local)
- JUnit 5 e Mockito

---

## 🚀 Como executar

### 1. Preparar o Oracle

Execute os scripts do repositório de banco de dados, na ordem indicada no README dele:
`ddl.sql` → `type_EloVet.sql` → `package_EloVet.sql` → `trigger_EloVet.sql` → `carga_de_Dados.sql`.

### 2. Configurar as variáveis de ambiente

| Variável | Obrigatória | Padrão | Descrição |
| --- | --- | --- | --- |
| `ORACLE_URL` | Não | `jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL` | URL JDBC do Oracle |
| `ORACLE_USER` | Sim | — | Usuário do schema |
| `ORACLE_PASSWORD` | Sim | — | Senha do schema |
| `MONGODB_URI` | Não | `mongodb://root:secret@localhost:27017/elovet?authSource=admin` | Usada quando o MongoDB não vem do Docker Compose |
| `AUDITORIA_SYNC_CRON` | Não | `0 0 2 * * *` | Horário da sincronização diária (cron do Spring) |
| `PORT` | Não | `8081` | Porta HTTP |
| `CORS_ALLOWED_ORIGINS` | Não | `http://localhost:5173,http://localhost:4173` | Origens do Portal do Veterinário liberadas no CORS |
| `LOG_LEVEL` | Não | `INFO` | Nível de log da aplicação (`DEBUG` mostra também consultas e pendências da auditoria) |

### 3. Subir a aplicação

Com o **Docker** em execução, o Spring Boot sobe o MongoDB definido no `compose.yaml` automaticamente:

```powershell
# Windows (PowerShell)
$env:ORACLE_USER="rm000000"
$env:ORACLE_PASSWORD="senha"
.\gradlew.bat bootRun
```

Para usar um MongoDB externo (ex.: Atlas), desative o Compose e informe a URI:

```powershell
$env:SPRING_DOCKER_COMPOSE_ENABLED="false"
$env:MONGODB_URI="mongodb+srv://usuario:senha@cluster.mongodb.net/elovet"
.\gradlew.bat bootRun
```

### 4. Testes

```powershell
.\gradlew.bat test
```

Os testes unitários cobrem a sincronização da auditoria: cópia incremental, conversão do JSON do Oracle em subdocumento e preservação de conteúdo inválido.

---

## 📁 Estrutura

```
src/main/java/br/com/fiap/elovetagendamento
├── agendamento   # CRUD de elo_agendamento e regras de vínculo
├── catalogo      # clínicas, veterinários, responsáveis e pets para o portal
├── procedure     # chamadas ao pkg_elovet (procedures, functions e DBMS_OUTPUT)
├── auditoria     # leitura de elo_auditoria, gravação no MongoDB e agendador diário
└── shared        # CORS e tratamento global de erros
```

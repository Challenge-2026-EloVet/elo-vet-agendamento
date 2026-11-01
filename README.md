# 📅 Elo Vet — Microsserviço de Agendamento

> **FIAP Challenge 2026 — Clyvo Vet** | **Disciplina:** Mastering Relational and Non-Relational Database — integração com outras linguagens

Microsserviço em **Java (Spring Boot)** que gerencia os **agendamentos de consulta** do Elo Vet sobre a mesma base **Oracle** modelada na disciplina ([EloVet-Mastering-Relational-Database](https://github.com/Challenge-2026-EloVet/EloVet-Mastering-Relational-Database)). Ele integra os três pontos pedidos:

1. **Banco relacional no backend:** cria, altera e consulta registros de `elo_agendamento`.
2. **Procedures chamadas pela aplicação:** executa `pkg_elovet.pr_agenda_json`, `pkg_elovet.pr_totaliza_idade_pet`, `pkg_elovet.fn_valida_cpf` e `pkg_elovet.fn_exporta_valores_novos`. Além disso, toda gravação em `elo_agendamento` dispara a trigger `trg_auditoria_agendamento`, que chama `pkg_elovet.pr_audita_agendamento`.
3. **Relacional → JSON → MongoDB:** uma rotina diária chama `pkg_elovet.fn_exporta_valores_novos`, que gera no próprio Oracle o JSON das auditorias do dia anterior (snapshot do agendamento e dados de pet, responsável, veterinário e clínica), e grava cada objeto desse JSON como um documento da collection `auditoria_agendamento` do MongoDB.

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
    AU -->|fn_exporta_valores_novos<br/>array JSON do dia anterior| P
    S -->|agendador diário<br/>ou sob demanda| P
    S -->|1 documento por objeto do JSON| M[(MongoDB<br/>auditoria_agendamento)]
    subgraph Oracle
        A
        P
        AU
    end
```

### Fluxo da auditoria

1. A aplicação grava um agendamento (`INSERT` ou `UPDATE`) em `elo_agendamento`.
2. A trigger `trg_auditoria_agendamento` chama `pkg_elovet.pr_audita_agendamento`, que monta os valores anteriores e novos em JSON (`fn_monta_json`) e grava em `elo_auditoria`.
3. Todo dia às **02:00** (horário de Brasília), o `AuditoriaScheduler` executa `SELECT pkg_elovet.fn_exporta_valores_novos FROM dual`. A function monta, no Oracle, um array JSON com as auditorias do **dia anterior** (registros de `DELETE` ficam de fora): o snapshot `valores_novos` e os dados descritivos de pet, responsável, veterinário e clínica, como objetos aninhados.
4. Cada objeto desse array é gravado **como veio** na collection `auditoria_agendamento`, com `_id` igual ao `id_auditoria` e marcado com a `dataReferencia` (o dia exportado). O mesmo processo pode ser disparado na hora por `POST /auditorias/sincronizacao`.

A exportação é **idempotente por dia**: executá-la de novo para o mesmo dia substitui os documentos daquela `dataReferencia` em vez de duplicá-los. A data de referência é obtida do próprio Oracle (`TRUNC(SYSDATE) - 1`), a mesma usada pela function.

> **Atenção na demonstração:** a function exporta apenas o que foi auditado **no dia anterior**. Agendamentos criados ou alterados hoje entram na exportação de amanhã. Para ver o JSON exato que será gravado, use `GET /procedures/exporta-valores-novos`.

---

## 🍃 Estrutura no MongoDB

Collection **`auditoria_agendamento`** (criada automaticamente na primeira gravação). Cada documento é um objeto do JSON gerado pelo Oracle, sem tradução de campos, acrescido de `_id`, `dataReferencia` e `exportadoEm`:

```json
{
  "_id": 15,
  "id_auditoria": 15,
  "tipo_operacao": "INSERT",
  "data_hora_operacao": "31/10/2026 14:22:10",
  "usuario_banco": "RM566358",
  "id_agendamento": 9,
  "valores_novos": { "id_agendamento": 9, "id_pet": 3, "id_veterinario": 5, "id_clinica": 4, "status": "AGENDADO", "data_hora": "01/11/2026 14:00" },
  "pet":         { "id_pet": 3, "nome": "Thor", "especie": "Cachorro", "raca": "Dachshund", "sexo": "M", "data_nascimento": "10/03/2022", "idade_aproximada": 4, "castrado": "SIM" },
  "responsavel": { "id_responsavel": 2, "nome_completo": "Rafael Martins", "telefone": "11988887777" },
  "veterinario": { "id_veterinario": 5, "nome_completo": "Joao Pedro Salles", "crmv": "CRMV-SP 11345", "telefone": "11976540005" },
  "clinica":     { "id_clinica": 4, "nome": "Elo Vet Santo Amaro", "cnpj": "45678901000163", "telefone": "1130454455", "email": "contato@elovet-santoamaro.com.br", "bairro": "Santo Amaro", "cidade": "Sao Paulo", "estado": "SP" },
  "dataReferencia": "2026-10-31",
  "exportadoEm": "2026-11-01T02:00:00"
}
```

Decisões de modelagem (NoSQL orientado a documentos):

- **Um documento por evento de auditoria, autoexplicativo:** as entidades relacionadas vêm **embutidas** como subdocumentos, desnormalizadas no momento da exportação. Consultas como "auditorias de cães da clínica X" não precisam de JOIN.
- **`_id` = `id_auditoria`:** cada evento do Oracle corresponde a exatamente um documento.
- **`dataReferencia`** identifica o lote diário: permite consultar o que foi auditado em cada dia e torna a exportação reexecutável sem duplicar dados.
- **Sem dados pessoais sensíveis:** CPF, RG e data de nascimento das pessoas ficam só no Oracle.
- **Índices** em `dataReferencia`, `id_agendamento`, `valores_novos.status`, `pet.especie` e `clinica.id_clinica`, criados quando a aplicação sobe.
- Exemplos de consulta no `mongosh`:

```javascript
db.auditoria_agendamento.find({ id_agendamento: 9 }).sort({ _id: 1 })                     // histórico de um agendamento
db.auditoria_agendamento.find({ "valores_novos.status": "CANCELADO" })                      // agendamentos cancelados
db.auditoria_agendamento.find({ "pet.especie": "Cachorro", "clinica.cidade": "Sao Paulo" }) // filtro por entidades embutidas
db.auditoria_agendamento.aggregate([
  { $group: { _id: "$clinica.nome", total: { $sum: 1 } } }                                  // movimentações por clínica
])
```

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
| `GET` | `/procedures/exporta-valores-novos` | `pkg_elovet.fn_exporta_valores_novos` (JSON das auditorias do dia anterior, exatamente como o Oracle gera) |

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
| `POST` | `/auditorias/sincronizacao` | Executa na hora a exportação Oracle → MongoDB do dia anterior |
| `GET` | `/auditorias` | Lista os documentos do MongoDB; filtros opcionais `idAgendamento` ou `dataReferencia` (AAAA-MM-DD) |

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
| `LOG_LEVEL` | Não | `INFO` | Nível de log da aplicação (`DEBUG` mostra também o resultado das consultas) |

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

Os testes unitários cobrem a exportação da auditoria: gravação de cada objeto do JSON como documento (preservando os objetos aninhados), substituição do lote do mesmo dia, dia sem auditoria e falha sem apagar dados quando a function retorna `NULL` ou um JSON inválido.

---

## 📁 Estrutura

```
src/main/java/br/com/fiap/elovetagendamento
├── agendamento   # CRUD de elo_agendamento e regras de vínculo
├── catalogo      # clínicas, veterinários, responsáveis e pets para o portal
├── procedure     # chamadas ao pkg_elovet (procedures, functions e DBMS_OUTPUT)
├── auditoria     # exportação via fn_exporta_valores_novos, gravação no MongoDB e agendador diário
└── shared        # CORS e tratamento global de erros
```

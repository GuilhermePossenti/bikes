# Bikes Atividade: Alterar Senha

API REST em Spring Boot com PostgreSQL e documentação em Swagger (SpringDoc OpenAPI).

## O que a atividade pede

- Modificar a alteração de senha para que, além da **nova senha**, sejam informadas também a **confirmação de senha** e a **senha atual**.
- Criar a classe `UsuarioSenhaDTO.java` para a requisição, com validação dos campos.
- Criar a exceção `PasswordInvalidException` para validar a senha atual e comparar a nova senha com a confirmação.

## O que foi feito

### `UsuarioSenhaDTO` (pacote `dto`)
Record com os campos `senhaAtual`, `novaSenha` e `confirmacaoSenha`. Os três são obrigatórios (`@NotBlank`) e precisam ter 6 caracteres (`@Size`), a mesma regra do cadastro de usuário.

### `PasswordInvalidException` (pacote `exception`)
Exceção lançada quando a senha atual não confere ou quando a nova senha é diferente da confirmação.

### `UsuarioService`
O método `updatePassword` agora:
1. Busca o usuário pelo `id` e lança `EntityNotFoundException` se ele não existir.
2. Confere se a senha atual informada é igual à senha salva.
3. Confere se a nova senha é igual à confirmação.
4. Altera a senha e salva.

### `UsuarioController`
O endpoint `PATCH /api/v1/usuarios/{id}` recebe o `UsuarioSenhaDTO` com `@Valid` e retorna **204 No Content** em caso de sucesso.

### `ApiExceptionHandler`
Novo tratamento para a `PasswordInvalidException`, que devolve **400 Bad Request** com a mensagem do erro.

## Respostas do endpoint

| Situação | Status |
|---|---|
| Senha alterada com sucesso | 204 |
| Senha atual errada ou confirmação diferente | 400 |
| Usuário não encontrado | 404 |
| Campo vazio ou com tamanho inválido | 422 |

## Exemplo de requisição

`PATCH /api/v1/usuarios/1`

```json
{
  "senhaAtual": "123456",
  "novaSenha": "654321",
  "confirmacaoSenha": "654321"
}
```

# Atividade 02: Teste de Alterar Senha

## O que a atividade pede

Desenvolver métodos de teste para as 4 possibilidades da alteração de senha: sucesso, senha atual inválida, senha e confirmação que não conferem e campos inválidos (vazios, senhas com menos e com mais de 6 caracteres). Como o endpoint não retorna corpo, os testes verificam apenas o status.

## O que foi feito

### `UsuarioAlterarSenhaTest` (pasta `src/test`)
Usa `@SpringBootTest` com `MockMvc` para chamar o `PATCH /api/v1/usuarios/{id}`. Antes de cada teste é criado um usuário com a senha `123456`, que é apagado no final.

| Teste | Situação | Status esperado |
|---|---|---|
| `alterarSenha_comDadosValidos_retornarStatus204` | Senha atual correta e confirmação igual | `isNoContent()` (204) |
| `alterarSenha_comSenhaAtualInvalida_retornarStatus400` | Senha atual errada | `isBadRequest()` (400) |
| `alterarSenha_comSenhaEConfirmacaoDiferentes_retornarStatus400` | Nova senha diferente da confirmação | `isBadRequest()` (400) |
| `alterarSenha_comCamposInvalidos_retornarStatus422` | Campos vazios, com 5 e com 7 caracteres | `isUnprocessableContent()` (422) |

### `application.properties` de teste (pasta `src/test/resources`)
Os testes usam um banco H2 em memória, então rodam sem precisar do PostgreSQL ligado.

## Como rodar

```
./mvnw test
```

# Atividade 03: Recurso Cliente

## O que a atividade pede

Criar o recurso **Cliente** (`id`, `nome`, `cpf` único e método `equals`). Um cliente só pode ser cadastrado por um usuário autenticado, o cliente consulta apenas os próprios dados e o Admin consulta os dados de todos. O recurso precisa de DTOs, Controller, Service e Repository, deve ser protegido com autenticação, documentado com SpringDoc OpenAPI/Swagger, ter validação dos campos de entrada e os mesmos campos de auditoria de `Usuario`.

## O que foi feito

### Autenticação com JWT (Spring Security)
O projeto ainda não tinha autenticação, então ela foi criada primeiro:

- `POST /api/v1/auth` recebe `username` e `password` (`UsuarioLoginDTO`) e devolve um token JWT, válido por 30 minutos.
- Nas outras requisições, o token vai no header `Authorization: Bearer <token>`.
- Pacote `jwt`: `JwtUtils` (gera e valida o token), `JwtAuthorizationFilter` (lê o token em cada requisição), `JwtUserDetailsService` e `JwtUserDetails` (usuário logado, com `id` e `role`) e `JwtAuthenticationEntryPoint` (resposta 401).
- `SpringSecurityConfig`: só o cadastro de usuário, o login e a documentação ficam livres. O restante exige token. As regras por perfil ficam no `@PreAuthorize` de cada endpoint.
- As senhas agora são salvas com **BCrypt**. Por isso, o cadastro e a alteração de senha da Atividade 01 passaram a usar o `PasswordEncoder`.

### Auditoria
`SpringJpaAuditingConfig` ativa o JPA Auditing. `dataCriacao`, `dataModificacao`, `criadoPor` e `modificadoPor` agora são preenchidos automaticamente em `Usuario` e `Cliente`, e o "por" é o username do usuário logado.

### Recurso Cliente
- `Cliente` (entidade): `id`, `nome`, `cpf` (único, 11 dígitos), `usuario` (`@OneToOne`, também único: cada usuário tem no máximo um cliente), campos de auditoria, `equals` e `hashCode` pelo `id`.
- `ClienteRequestDTO`: `nome` obrigatório (3 a 100 caracteres) e `cpf` obrigatório, com 11 dígitos sem pontuação e validado pelo `@CPF` (dígitos verificadores). O usuário **não** vem no corpo: é sempre o usuário do token.
- `ClienteResponseDTO`, `ClienteMapper` (MapStruct), `ClienteRepository`, `ClienteService` e `ClienteController`.
- `ClienteUniqueViolationException` → **409** quando o CPF já existe ou o usuário já tem cliente.

### Endpoints do Cliente

| Método e caminho | Quem pode | Descrição |
|---|---|---|
| `POST /api/v1/clientes` | CLIENTE | Cadastra o cliente do usuário logado |
| `GET /api/v1/clientes/detalhes` | CLIENTE | Consulta apenas os próprios dados |
| `GET /api/v1/clientes/{id}` | ADMIN | Consulta qualquer cliente |
| `GET /api/v1/clientes` | ADMIN | Lista todos os clientes |

### Endpoints de Usuário (agora protegidos)

| Método e caminho | Quem pode |
|---|---|
| `POST /api/v1/usuarios` | Livre (cadastro) |
| `GET /api/v1/usuarios/{id}` | ADMIN, ou o próprio usuário |
| `PATCH /api/v1/usuarios/{id}` | Só o próprio usuário |
| `GET /api/v1/usuarios` | ADMIN |

### Respostas

| Situação | Status |
|---|---|
| Sem token ou token inválido | 401 |
| Logado, mas sem permissão (perfil errado ou dados de outro usuário) | 403 |
| Login com credenciais erradas | 400 |
| CPF repetido ou usuário que já tem cliente | 409 |
| Campos inválidos | 422 |

### Swagger
A documentação em `http://localhost:8080/docs-bikes.html` tem o botão **Authorize**, onde se cola o token. Os endpoints protegidos aparecem com cadeado.

### Testes
- `ClienteTest`: login, senha criptografada, cadastro (201, 401, 403, 409, 422), auditoria, consultas do cliente e do admin, e documentação.
- `UsuarioAlterarSenhaTest`: os testes da Atividade 02 agora fazem login antes. Foram adicionados os casos sem token (401) e de alterar a senha de outro usuário (403).

## Como testar manualmente

1. `POST /api/v1/usuarios` com `{"username":"maria@email.com","password":"123456"}`
2. `POST /api/v1/auth` com os mesmos dados e copiar o `token`
3. `POST /api/v1/clientes` com o header `Authorization: Bearer <token>` e `{"nome":"Maria Silva","cpf":"52998224725"}`

Para ter um ADMIN, altere o `role` do usuário direto no banco:
```sql
UPDATE usuarios SET role = 'ADMIN' WHERE username = 'admin@email.com';
```

> Usuários cadastrados antes desta atividade têm a senha em texto puro e não conseguem fazer login. Cadastre-os de novo.

# Atividade 04: Recurso Bike

## O que a atividade pede

Criar o recurso **Bike** com DTOs, Controller, Service e Repository, protegido com autenticação, documentado com SpringDoc OpenAPI/Swagger, com validação dos campos de entrada e os mesmos campos de auditoria de `Usuario` e `Cliente`.

## O que foi feito

### Recurso Bike
- `Bike` (entidade): `id`, `codigo` (único), `modelo`, `marca`, `cor`, `status`, campos de auditoria, `equals` e `hashCode` pelo `id`.
- `Status` (enum dentro de `Bike`): `DISPONIVEL`, `ALUGADA` e `MANUTENCAO`. Toda bike é cadastrada como `DISPONIVEL`.
- `BikeRequestDTO`: `codigo` obrigatório (3 a 20 caracteres: letras maiúsculas, números ou hífen), `modelo` (2 a 100), `marca` (2 a 50) e `cor` (3 a 30), todos obrigatórios. O status **não** vem no corpo.
- `BikeStatusDTO`: `status` obrigatório, aceita apenas `DISPONIVEL`, `ALUGADA` ou `MANUTENCAO`.
- `BikeResponseDTO`, `BikeMapper` (MapStruct), `BikeRepository`, `BikeService` e `BikeController`.
- `BikeUniqueViolationException` → **409** quando o código já existe. Registrada no `ApiExceptionHandler`.

### Endpoints da Bike

| Método e caminho | Quem pode | Descrição |
|---|---|---|
| `POST /api/v1/bikes` | ADMIN | Cadastra uma bike |
| `GET /api/v1/bikes` | ADMIN | Lista todas as bikes |
| `GET /api/v1/bikes/disponiveis` | ADMIN ou CLIENTE | Lista as bikes disponíveis |
| `GET /api/v1/bikes/{id}` | ADMIN ou CLIENTE | Consulta uma bike |
| `PATCH /api/v1/bikes/{id}/status` | ADMIN | Altera o status da bike |

### Respostas

| Situação | Status |
|---|---|
| Sem token ou token inválido | 401 |
| Logado, mas sem permissão | 403 |
| Bike não encontrada | 404 |
| Código de bike repetido | 409 |
| Campos ou status inválidos | 422 |

### Swagger
Os endpoints aparecem na tag **Bikes** em `http://localhost:8080/docs-bikes.html`, com cadeado.

## Como testar manualmente

1. `POST /api/v1/auth` com um usuário ADMIN e copiar o `token`
2. `POST /api/v1/bikes` com o header `Authorization: Bearer <token>` e `{"codigo":"BK-001","modelo":"Caloi 10","marca":"Caloi","cor":"Vermelha"}`
3. `PATCH /api/v1/bikes/1/status` com `{"status":"MANUTENCAO"}`
4. Com um usuário CLIENTE, `GET /api/v1/bikes/disponiveis`

# Atividade 05: Locações, Devoluções e Relatório

## O que a atividade pede

Permitir que o cliente alugue uma bike informando o cartão de crédito, registrar a devolução calculando o valor a ser debitado e gerar um relatório de locações e devoluções. Uma bike não pode participar de duas locações ao mesmo tempo.

## O que foi feito

### Recurso Locação
- `Locacao` (entidade): `cliente`, `bike`, `dataRetirada`, `dataPrevistaDevolucao`, `dataDevolucao`, `valor`, `status` (`ATIVA` ou `FINALIZADA`), dados do cartão (veja abaixo) e os campos de auditoria.
- `LocacaoRequestDTO`: `bikeId`, `horasPrevistas` (1 a 72), `numeroCartao` (13 a 19 dígitos, validado pelo algoritmo de Luhn), `nomeTitular`, `validade` (`MM/AA`) e `cvv`. O cliente **não** vem no corpo: é o usuário do token.
- `LocacaoDevolucaoDTO` (corpo opcional da devolução): `bikeComProblema`.
- `LocacaoResponseDTO` e `LocacaoRelatorioDTO`, `LocacaoRepository`, `LocacaoService` e `LocacaoController`.
- `LocacaoConflictException` → **409**.

### Regras
- **Alugar:** a bike precisa estar `DISPONIVEL`; ela passa a `ALUGADA`. A linha da bike é bloqueada no banco durante a transação (`PESSIMISTIC_WRITE`), então duas locações simultâneas da mesma bike nunca passam juntas.
- **Cartão:** só o titular e os **4 últimos dígitos** são guardados. O número completo e o CVV são descartados e nunca aparecem nas respostas.
- **Devolver:** registra a data/hora efetiva, calcula o valor e deixa a bike `DISPONIVEL`. Com `{"bikeComProblema": true}`, a bike vai para `MANUTENCAO`.
- **Valor:** preço por hora **proporcional ao tempo efetivo**, contado em minutos arredondados para cima (mínimo de 1 minuto). O preço da hora fica em `locacao.preco-hora` no `application.properties`. O sistema apenas calcula e apresenta o valor; não há cobrança real no cartão.

### Endpoints

| Método e caminho | Quem pode | Descrição |
|---|---|---|
| `POST /api/v1/locacoes` | CLIENTE | Solicita o aluguel de uma bike |
| `PATCH /api/v1/locacoes/{id}/devolucao` | CLIENTE (as próprias) ou ADMIN | Registra a devolução |
| `GET /api/v1/locacoes/{id}` | CLIENTE (as próprias) ou ADMIN | Consulta uma locação |
| `GET /api/v1/locacoes/relatorio` | ADMIN | Relatório de locações e devoluções |
| `GET /api/v1/bikes/codigo/{codigo}` | ADMIN | Localiza a bike pelo código |

O relatório traz, para cada locação: nome do cliente, código da bike, data de retirada, data de devolução, duração em minutos e status. As mais recentes vêm primeiro; se a locação ainda está ativa, a duração é o tempo até agora.

### Respostas

| Situação | Status |
|---|---|
| Sem token ou token inválido | 401 |
| Perfil sem permissão, ou locação de outro cliente | 403 |
| Bike, locação ou cadastro de cliente não encontrado | 404 |
| Bike indisponível, ou locação já devolvida | 409 |
| Campos inválidos (inclui cartão inválido) | 422 |

### Testes
- `LocacaoTest`: aluguel (201, 401, 403, 404, 409, 422), devolução (normal, com problema, repetida, de outro cliente), consulta, relatório, bike pelo código e cálculo do valor.
- `UsuarioTest`: cadastro de usuário (válido, username repetido, username inválido, senha inválida), busca por id (existente, inexistente, sem permissão) e listagem.

## Exemplo de requisição

`POST /api/v1/locacoes`

```json
{
  "bikeId": 1,
  "horasPrevistas": 2,
  "numeroCartao": "4111111111111111",
  "nomeTitular": "Maria Silva",
  "validade": "12/30",
  "cvv": "123"
}
```

# Atividade 06: Relatórios em PDF com Jasper

## O que a atividade pede

Gerar relatórios em PDF com o JasperReports e expor um endpoint para o administrador baixar o relatório de clientes.

## O que foi feito

### Dependências (`pom.xml`)
`jasperreports` e `jasperreports-pdf` (7.0.8), mais `jasperreports-jdt` (compila as expressões do modelo) e `jasperreports-fonts` (fontes embutidas no PDF).

### Modelos (`src/main/resources/reports`)
- `clientes.jrxml`: relatório de clientes (ID, nome e CPF formatado) com o total de clientes no fim, em A4 retrato.
- `locacoes.jrxml`: relatório de locações e devoluções (cliente, bike, retirada, devolução, duração, valor e status), em A4 paisagem. No fim traz o total de locações, as ativas, as finalizadas e o total faturado.
- `images/logo.png`: logo da Locabike, exibida no topo dos dois relatórios. O `JasperService` a carrega uma vez e a passa ao modelo pelo parâmetro `LOGO`.

Os dois têm cabeçalho verde, linhas alternadas e divisórias. Com as tabelas vazias, os relatórios saem só com cabeçalho e totais zerados (`whenNoDataType="AllSectionsNoDetail"`).

Os modelos são os arquivos-fonte `.jrxml`, que também abrem no Jaspersoft Studio. O `JasperService` compila cada um na primeira vez que é usado e guarda o resultado em memória. Os dados vêm das consultas da aplicação, não de uma conexão JDBC do Studio.

> No formato do Jasper 7, o `<jasperReport>` **não** pode ter `xmlns`. Com ele o modelo não carrega ("Unable to load report").

### Endpoints (só ADMIN)

| Método e caminho | Descrição |
|---|---|
| `GET /api/v1/clientes/relatorio` | PDF com todos os clientes |
| `GET /api/v1/locacoes/relatorio/pdf` | PDF com todas as locações (o `.../relatorio` continua devolvendo JSON) |

Os dois respondem `application/pdf` e retornam 401 sem token e 403 para o perfil CLIENTE. O token vai no cabeçalho `Authorization`, então digitar a URL no navegador não funciona: use o botão **Download file** do Swagger (depois de **Authorize**) ou **Send and Download** no Postman.

O valor de cada locação é calculado na devolução (`locacao.preco-hora` × tempo de uso) e também aparece no JSON do `GET /api/v1/locacoes/relatorio`, no campo `valor` (nulo enquanto a locação está ativa).

### Testes
`RelatorioPdfTest`: confere o status, o tipo `application/pdf` e que o arquivo começa com `%PDF-`, para ADMIN (200), CLIENTE (403), sem token (401) e com as tabelas vazias.

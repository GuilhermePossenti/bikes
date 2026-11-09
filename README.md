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

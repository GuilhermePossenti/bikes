# Bikes — Atividade: Alterar Senha

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
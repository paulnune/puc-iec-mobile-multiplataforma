# Como contribuir

A Contribuição individual da rubrica olha os commits e os PRs de cada integrante. Cada um faz a sua issue,
na sua branch, com o seu PR.

## Fluxo

1. Pegue a sua issue no [Project](https://github.com/users/paulnune/projects/4) e mova o card para **In progress**.
2. Crie a branch a partir da `projeto-final` atualizada, com o nome que está na issue:

   ```bash
   git fetch origin
   git switch -c feat/f2-detalhe origin/projeto-final
   ```

3. Faça commits pequenos, no formato `tipo(escopo): o que mudou`, por exemplo
   `feat(detalhe): bens declarados com total`. Tipos: `feat`, `fix`, `test`, `docs`, `refactor`, `chore`.
4. Antes de abrir o PR, rode na pasta do app:

   ```bash
   cd exercicios/projeto-final/eleicoes-kmp
   ./gradlew :androidApp:assembleDebug :shared:testAndroidHostTest :androidApp:testDebugUnitTest
   ```

5. Abra o PR **para a `projeto-final` deste repo**. Como o repo é um fork, o GitHub sugere o repo do professor
   como base: troque para `paulnune/puc-iec-mobile-multiplataforma` e `projeto-final`. Pelo `gh`:

   ```bash
   gh repo set-default paulnune/puc-iec-mobile-multiplataforma   # uma vez
   gh pr create --base projeto-final
   ```

6. No PR: `Closes #<número da issue>`, um print da tela e o que você testou. Peça revisão a quem a issue sugere.
7. O merge exige 1 aprovação e o CI verde. Use **Create a merge commit**, não squash: assim os commits de cada um
   continuam no histórico.

## Regras para ninguém travar ninguém

- Mexa na pasta da sua feature (`androidApp/.../feature/<sua>/`) e em arquivos novos de `shared/.../domain/`.
- Arquivos compartilhados (`AppContainer`, `EleicoesApp`, `ui/components`, `ui/theme`, `shared/.../data`, Gradle):
  só a troca que a sua issue pede. Se precisar de mais, combine no grupo antes.
- Nada de fetch dentro de composable: tela → ViewModel → repositório.
- Use o tema (`Spacing`, `MaterialTheme.colorScheme`, `MaterialTheme.typography`) e os componentes
  (`LoadingShimmer`, `EmptyState`, `ErrorState`, `CandidateCard`, `OutcomeBadge`) em vez de valores soltos.
- Toda feature traz pelo menos 1 teste próprio da lógica que criou (`shared/src/commonTest` ou `androidApp/src/test`).
- Estados de carregando, vazio e erro em todas as telas: nunca tela branca.

## Chave da API

Cada um usa a própria chave grátis (https://eleicoes.dev/dashboard), no `.env` da raiz do repositório
(veja o `.env.example`). O limite é de 60 requisições por minuto por chave. Nunca commite a chave.

## Máquina com pouca memória

```bash
./gradlew :androidApp:assembleDebug --max-workers=1 -Dorg.gradle.jvmargs=-Xmx1536m
```

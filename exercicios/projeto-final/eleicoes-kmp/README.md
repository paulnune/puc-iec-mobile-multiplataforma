# Eleições 2026 — app KMP

App Android em Kotlin Multiplatform com os dados públicos do TSE das eleições de 2026, servidos pela API
[eleicoes.dev](https://eleicoes.dev). Projeto Final da disciplina Arquitetura de Aplicações Móveis e
Multiplataforma (PUC Minas).

> Versão do skeleton (issue #1). As features entram pelas issues #2 a #13. O README final, com os screenshots, é a issue #10.

## Telas

| Aba | O que mostra | Issue |
|---|---|---|
| Patrimônio | Ranking dos candidatos pelo patrimônio declarado, com busca e filtros por cargo, gênero e UF | #2, #4 |
| Detalhe | Perfil, resultado e bens declarados de um candidato | #3 |
| Evolução | Quanto o patrimônio mudou de uma eleição para outra, desde 2006 | #7 |
| Mapa | Quem liderou o 1º turno no Brasil e em cada UF | #6 |
| Partidos | Os partidos e os seus candidatos | #8 |
| Colinha | Os candidatos favoritos, na ordem da urna, salvos no aparelho | #5 |

## Como rodar

Pré-requisitos: JDK 17 ou mais novo (testado com o 21) e Android SDK com a plataforma 36. O Android Studio já traz os dois.

1. Pegue uma chave grátis em https://eleicoes.dev/dashboard.
2. Crie um arquivo `.env` na raiz do repositório (ou nesta pasta), copiando o `.env.example`:

   ```
   ELEICOES_API_KEY=sua-chave
   ```

   O `.env` está no `.gitignore`: nunca commite a chave. Também vale a variável de ambiente
   `ELEICOES_API_KEY` ou `eleicoes.apiKey=...` no `local.properties`.
3. Builde e rode os testes:

   ```bash
   cd exercicios/projeto-final/eleicoes-kmp
   ./gradlew :androidApp:assembleDebug
   ./gradlew :shared:testAndroidHostTest :androidApp:testDebugUnitTest
   ```

4. Rode no emulador ou no celular: abra esta pasta no Android Studio e execute `androidApp`, ou instale o APK com
   `adb install androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

Sem a chave o build passa, e o app mostra na tela como configurar.

## Arquitetura

```
shared/                  KMP: dados e regras, sem dependência de Android
  data/source/           porta EleicoesDataSource e os adapters MCP (padrão) e REST
  data/repository/       repositórios com cache em memória
  domain/                modelos, formatação de dinheiro, filtros, favoritos
androidApp/              Compose com Material 3
  feature/<feature>/     uma pasta por feature: tela + ViewModel
  ui/                    tema, componentes e estados de tela compartilhados
```

Fluxo: tela → ViewModel (StateFlow) → repositório → `EleicoesDataSource` → API. As decisões e as alternativas
descartadas estão no [ADR](ADR.md).

## Fonte dos dados

API eleicoes.dev, com os dados públicos do TSE. Na semana da entrega a REST ignorava os filtros e devolvia erro 500,
enquanto o servidor MCP da mesma API, com a mesma chave, funcionava. Por isso o app fala com o MCP, por trás de uma
interface que permite voltar à REST sem mexer nas telas. O limite é de 60 requisições por minuto e 5.000 por dia
por chave.

## Como contribuir

Veja o [CONTRIBUTING](CONTRIBUTING.md).

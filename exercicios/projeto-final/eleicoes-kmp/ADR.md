# ADR-0001: KMP com Compose, estado em ViewModel + StateFlow e a fonte de dados atrás de uma porta

## Status

`Aceito`, com as 8 features integradas na `projeto-final`. Substitui o rascunho do skeleton (#1).

**Data:** proposto em 2026-11-07, aceito em 2026-11-08
**Grupo:** Paulo Nunes, José Ricardo, Diego Diógenes, Natan Figueiredo, Rodrigo Pretes, Gabriel Moreira

## Contexto

- App Android sobre os dados públicos do TSE das eleições de 2026 (API eleicoes.dev), com as 5 features do enunciado e mais 3, feito por 6 pessoas em 2 dias.
- Cada pessoa precisa trabalhar na sua feature sem esperar as outras, e o professor builda o projeto do zero.
- Na semana da entrega a REST da API ignorava os filtros e devolvia 500. O servidor MCP da mesma API, com a mesma chave, funcionava, menos a tool `list_wealth_changes`.
- A API aceita 60 requisições por minuto por chave, e a demo é ao vivo, com a rede da sala.

## Decisão

1. **Kotlin Multiplatform.** Dados e regras ficam no módulo `shared` (`commonMain`), sem nenhuma API de Android ou da JVM. O código de plataforma do `shared` se resume ao motor HTTP (`expect fun createHttpClient()`) e ao cache em arquivo. O `shared` compila para Android e iOS (#26), e a UI fica em Compose no `androidApp`.
2. **Estado em ViewModel + StateFlow**, com fluxo unidirecional: a tela observa um `UiState` (carregando, sucesso, vazio, erro) e só chama funções do ViewModel. Nenhum fetch dentro de composable.
3. **Fonte de dados atrás de uma porta** (`EleicoesDataSource`), com dois adapters: MCP (o padrão) e REST. O que a API não entrega pronto vira regra do domínio: a variação do patrimônio (#7) é calculada no app a partir de `get_candidate_filings`.
4. **Cache offline como decorator da porta** (`CachingEleicoesDataSource`, #12). Ele guarda em disco a última resposta boa de cada consulta e a devolve quando a rede ou a API falham. Uma resposta de menos de 5 minutos volta sem chamar a API, por causa do limite de requisições.
5. **Persistência local com DataStore** para a colinha (#5) e o tema (#13). A colinha implementa a interface `FavoritesRepository`, do domínio, e substituiu a versão em memória do skeleton.
6. **Injeção de dependência manual** num `AppContainer`. Ligar o cache (#12) e trocar a colinha em memória pela do DataStore (#5) foram mudanças só nele, sem mexer em tela nem em ViewModel.

## Alternativas consideradas

| Alternativa | Por que ficou de fora |
|---|---|
| Flutter (A3) | Uma base de código com renderer próprio, mas a regra de negócio ficaria em Dart, sem reaproveitar em apps nativos. KMP compartilha a lógica e deixa a UI nativa. |
| React Native (A2) | Ecossistema grande, mas os tipos do TypeScript somem em tempo de execução: um JSON fora do formato passa calado até quebrar na tela. Com kotlinx.serialization o DTO é validado ao decodificar, e o erro vira `AppError.Unexpected`. |
| Compose Multiplatform na UI (como na A5) | Viável: a UI já usa os artefatos multiplataforma do Compose e da lifecycle. Ficou para depois: em 2 dias o grupo testou as 8 telas no Android, e o iOS foi viabilizado no fim do trabalho, começando pela lógica (#26). Os pontos que prendem a UI ao Android são poucos: compartilhar, splash, caminhos do DataStore e do cache, formatação de data, tamanho da tela e a biblioteca de navegação. |
| Estado com bibliotecas MVI/Redux ou `mutableStateOf` na tela | ViewModel + StateFlow é o padrão da plataforma, sobrevive à rotação e é testável sem UI. |
| Koin ou Hilt para injeção | Peso a mais para um app deste tamanho; o `AppContainer` deixa as dependências explícitas. |
| Consumir só a REST | Estava quebrada na semana da entrega. A porta isola essa escolha: trocar de adapter não muda tela nem ViewModel. |

## Consequências

**Positivas:**
- cada feature tem a sua pasta e não depende das outras;
- a lógica (protocolo MCP, filtros, formatação, regras da colinha, cache e variação do patrimônio) é testada no `commonTest`, sem rede: são 169 dos 231 casos de teste, e os mesmos 169 passam num simulador de iPhone (workflow `pf-eleicoes-kmp-ios`). Para o iOS, o único ajuste foi tirar a vírgula de 10 nomes de teste, que o Kotlin/Native não aceita;
- o CI builda e testa cada PR, e o merge na `projeto-final` exige a aprovação de outro integrante.

**Negativas:**
- a interface ainda é só Android. Para o app rodar no iPhone faltam a UI em Compose Multiplatform e um cache em arquivo do iOS;
- o MCP foi pensado para agentes de IA: se o provedor mudar o formato, o adapter muda (a porta limita o estrago a uma classe);
- a chave da API vai compilada no APK, o que serve para um projeto de aula mas não para produção: lá ela ficaria num BFF, como visto na aula 5;
- a variação do patrimônio custa uma chamada por candidato, e o cache em disco só tem o que já foi aberto no aparelho. Por isso a demo usa a Presidência e aquece o cache antes.

## Referências

- Enunciado do Projeto Final: `exercicios/projeto-final/enunciado.md`
- API e MCP do eleicoes.dev: https://eleicoes.dev/docs/api e https://eleicoes.dev/docs/mcp
- Kotlin Multiplatform: https://kotlinlang.org/docs/multiplatform.html

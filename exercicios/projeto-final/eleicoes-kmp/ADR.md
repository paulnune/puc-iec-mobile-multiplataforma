# ADR-0001: KMP com Compose, estado em ViewModel + StateFlow e a fonte de dados atrás de uma porta

## Status

`Proposto`. Rascunho do skeleton; a versão final é a issue #9.

**Data:** 2026-11-07
**Grupo:** Paulo Nunes, José Ricardo, Diego Diógenes, Natan Figueiredo, Rodrigo Pretes, Gabriel Moreira

## Contexto

- App Android sobre os dados públicos do TSE das eleições de 2026 (API eleicoes.dev), com as 5 features do enunciado e mais 3, feito por 6 pessoas em 2 dias.
- Cada pessoa precisa trabalhar na sua feature sem esperar as outras, e o professor builda o projeto do zero.
- Na semana da entrega a REST da API ignorava os filtros e devolvia 500; o servidor MCP da mesma API, com a mesma chave, funcionava.

## Decisão

1. **Kotlin Multiplatform.** Dados e regras ficam no módulo `shared` (commonMain); a UI fica em Compose no `androidApp`.
2. **Estado em ViewModel + StateFlow**, com fluxo unidirecional: a tela observa um `UiState` (carregando, sucesso, vazio, erro) e só chama funções do ViewModel. Nenhum fetch dentro de composable.
3. **Fonte de dados atrás de uma porta** (`EleicoesDataSource`), com dois adapters: MCP (o padrão) e REST.
4. **Injeção de dependência manual** num `AppContainer`, onde cada issue troca só a sua peça.

## Alternativas consideradas

| Alternativa | Por que ficou de fora |
|---|---|
| Flutter (A3) | Uma base de código com renderer próprio, mas a regra de negócio ficaria em Dart, sem reaproveitar em apps nativos. KMP compartilha a lógica e deixa a UI nativa. |
| React Native (A2) | Ecossistema grande, mas tipagem em tempo de execução na ponte com o nativo. Em Kotlin, DTOs, modelos e regras são checados na compilação. |
| Compose Multiplatform na UI (como na A5) | Possível, mas ninguém do grupo tem Mac para buildar iOS, e as APIs que as features usam (DataStore, splash, compartilhar) são mais simples no Android. Migrar a UI para o commonMain depois é incremental. |
| Estado com bibliotecas MVI/Redux ou `mutableStateOf` na tela | ViewModel + StateFlow é o padrão da plataforma, sobrevive à rotação e é testável sem UI. |
| Koin ou Hilt para injeção | Peso a mais para um app deste tamanho; o `AppContainer` deixa as dependências explícitas. |
| Consumir só a REST | Estava quebrada na semana da entrega. A porta isola essa escolha: trocar de adapter não muda tela nem ViewModel. |

## Consequências

**Positivas:** cada feature tem a sua pasta e não depende das outras; a camada de dados já cobre todas as consultas; a lógica (protocolo MCP, filtros, formatação, regras) é testada em `commonTest` sem rede.

**Negativas:** o MCP foi pensado para agentes de IA; se o provedor mudar o formato, o adapter muda (a porta limita o estrago a uma classe). A chave da API vai compilada no APK, o que serve para um projeto de aula mas não para produção: lá ela ficaria num BFF, como visto na aula 5.

## Referências

- Enunciado do Projeto Final: `exercicios/projeto-final/enunciado.md`
- API e MCP do eleicoes.dev: https://eleicoes.dev/docs/api e https://eleicoes.dev/docs/mcp
- Kotlin Multiplatform: https://kotlinlang.org/docs/multiplatform.html

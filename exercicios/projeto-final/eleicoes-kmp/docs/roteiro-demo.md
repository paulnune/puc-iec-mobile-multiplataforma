# Roteiro da demo

Apresentação na Aula 6, segunda 09/11: **até 20 minutos, já contando as perguntas**. Cada integrante mostra e explica a própria feature.

Versão para o ensaio de domingo (08/11), parte da issue #9. Cada bloco foi conferido com o código integrado na `projeto-final`. Se algo mudar no ensaio, o dono ajusta o próprio bloco.

## Linha do tempo

| Quando | Quem | Bloco |
|---|---|---|
| 0:00–2:30 | Paulo | 1. Abertura e Patrimônio (F1) |
| 2:30–4:30 | Diego | 2. Busca, filtros e Hall da Fama (F3 + F4) |
| 4:30–6:00 | José Ricardo | 3. Detalhe (F2) |
| 6:00–7:45 | Natan | 4. Colinha (F5) e paisagem (#11) |
| 7:45–9:30 | Gabriel | 5. Evolução (F7) e tema (#13) |
| 9:30–11:15 | Rodrigo | 6. Mapa (F6) e modo offline (#12) |
| 11:15–12:15 | José Ricardo | 7. Partidos (F8) |
| 12:15–13:30 | Paulo | 8. Arquitetura, ADR e testes |
| 13:30–20:00 | Todos | Perguntas |

Os três estados que o enunciado cobra aparecem no caminho:
- **carregando:** o rodapé da lista no bloco 1 e cada filtro no bloco 2;
- **vazio:** a busca sem resultado no bloco 2;
- **erro e sem internet:** o bloco 6.

## Combinados

- **Mostre e explique.** Cada ação na tela vem com uma frase técnica: onde está o estado, de onde vem o dado e o que o seu teste confere.
- **Siga as ações do roteiro.** O cache da demo é aquecido com essas ações. Uma ação improvisada faz uma chamada nova à API, que tem limite de 60 por minuto.
- **Se algo falhar, diga o que aconteceu e siga em frente.** A tela de erro também é feature.
- **Passe a vez com a tela pronta** para o próximo bloco: a aba certa e nenhum diálogo aberto.

## Blocos

### 1. Abertura e Patrimônio (F1) · Paulo · 2:30

Mostrar:
- O app já aberto na aba Patrimônio, com dados reais.
- Rolar até passar da posição 30: o rodapé mostra o carregamento e a página 2 chega sozinha, sem botão.

Explicar:
- O que é o app:
  - dados reais do TSE de 2026, vindos da API eleicoes.dev;
  - as 5 features do enunciado e mais 3;
  - 6 pessoas, cada uma com a sua issue, a sua branch e um PR revisado por outro integrante.
- A paginação: a lista pede a próxima página quando faltam 5 itens. O `PagedList` junta as páginas e descarta o item que volta repetido na virada (testado no `PagedListTest`).
- O rodapé tem quatro estados: carregando, erro com "Tentar de novo", fim da lista e o botão "Carregar mais candidatos". Em nenhum deles a lista fica sem saída.

Passa a vez: volta ao topo da lista.

### 2. Busca, filtros e Hall da Fama (F3 + F4) · Diego · 2:00

Mostrar:
- Um chip (de cargo ou Mulheres) e uma UF: a lista recarrega, e o Hall da Fama mostra os 3 maiores patrimônios do filtro.
- Busca sem acento que encontra um nome com acento.
- Um nome que não está nas páginas carregadas, como o do print `f3-f4-busca-cadu.png`: ele aparece depois da consulta ao servidor.
- Uma busca sem resultado: "Nenhum candidato com esse nome".

Explicar:
- O chip vira parâmetro real da API (`office`, `gender`, `state`) no `CandidateFilter`.
- A busca é local primeiro, com debounce de 300 ms. Se o texto não acha ninguém na lista já carregada, a consulta é refeita com `q` no servidor, e apagar o texto tira o `q` e recarrega o filtro.
- O teste: do chip aos parâmetros, o filtro de texto sem acento e a passagem da busca local para a do servidor (`RankingViewModelTest`).

Passa a vez: limpa a busca e deixa na tela o filtro combinado para o bloco 3.

### 3. Detalhe (F2) · José Ricardo · 1:30

Mostrar:
- Tocar no 1º candidato da lista, logo abaixo do cabeçalho: é o mesmo que lidera o Hall da Fama, cujos cartões não abrem o Detalhe. Aparecem o resultado (desfecho, votos e %), o perfil e os bens declarados, com o total.
- Tocar na ★ para pôr o candidato na colinha. Se o cargo dele já estiver ocupado na colinha, o app pergunta se troca.

Explicar:
- Os dados vêm de `get_candidate` e `list_candidate_assets`, pelo `CandidatesRepository`. A tela só observa o `UiState`.
- O teste próprio: soma dos bens ou cálculo da idade.

Passa a vez: abre a aba Colinha.

### 4. Colinha (F5) e paisagem (#11) · Natan · 1:45

Mostrar:
- A colinha na ordem da urna, com os favoritos combinados e o candidato do bloco 3 no lugar do cargo dele. Cada um aparece com o número em teclas de urna e o selo do resultado.
- Fechar o app (tirar dos recentes), abrir de novo e voltar à aba Colinha: os favoritos continuam lá, como o enunciado pede.
- Girar para paisagem: a tela se reorganiza sem corte. Depois, voltar para retrato.

Explicar:
- A `FavoritesRepository` com DataStore substituiu a versão em memória do skeleton. A troca foi só no `AppContainer`, e nenhuma tela mudou.
- As regras ficam no `shared`, com testes: ordem da urna, 1 por cargo (2 para senador) e cargos estaduais da mesma UF.
- A fonte grande e o TalkBack estão nos prints do PR do #11, porque trocar a fonte do sistema ao vivo toma tempo.

Passa a vez: abre a aba Evolução.

### 5. Evolução (F7) e tema (#13) · Gabriel · 1:45

Mostrar:
- O grupo Presidência, que tem poucos candidatos e por isso faz poucas chamadas. Nos chips Mais ganhou e Mais perdeu, cada item mostra "2018 → 2026" e o Δ em verde ou vermelho.
- Tocar num candidato: aparecem o gráfico do patrimônio por eleição desde 2006 e a lista de candidaturas.
- Trocar para o tema escuro pelo menu da barra superior. A escolha fica salva no aparelho.

Explicar:
- A tool `list_wealth_changes` está quebrada no servidor, por isso a variação é calculada no app a partir de `get_candidate_filings`. Se a tool voltar, dá para trocar sem mexer na tela.
- O gráfico é desenhado com o Canvas do Compose.
- O ícone e o splash apareceram quando o Natan reabriu o app. A preferência de tema tem teste.

Passa a vez: abre a aba Mapa.

### 6. Mapa (F6) e modo offline (#12) · Rodrigo · 1:45

Mostrar:
- O resumo nacional (líder × 2º colocado, diferença e % das seções apuradas) e a grade de UFs, colorida por quem lidera. Tocar numa UF.
- Alternar para Governador.
- Ligar o modo avião e abrir a UF aquecida antes da aula que ainda não apareceu na demo. Os dados vêm do disco, com a faixa "Sem conexão: dados de <data e hora>".
- Ainda sem rede, abrir a UF combinada que não foi aquecida: aparece um erro claro, com "Tentar de novo". Desligar o modo avião e tocar em "Tentar de novo".

Explicar:
- A API só tem o 1º turno (o 2º volta vazio), como no site.
- O cache é um decorator da porta `EleicoesDataSource`. Ele guarda em disco a última resposta boa de cada consulta e a devolve quando a rede ou a API falham, sem mudar nenhuma tela.
- A UF do modo avião precisa ser uma que ainda não apareceu: o que já está na memória do app não chega a passar pelo cache em disco.
- O teste: com a fonte falhando, o cache devolve o dado guardado; sem nada guardado, devolve o erro.

Passa a vez: confere que o modo avião está desligado e abre a aba Partidos.

### 7. Partidos (F8) · José Ricardo · 1:00

Mostrar:
- Buscar um partido pela sigla e abrir: aparecem os candidatos dele, com chips de cargo.
- Tocar num candidato: abre o mesmo Detalhe do bloco 3.

Explicar:
- A tela reusa o `CandidateCard` e a rota de Detalhe do skeleton, por isso não precisou esperar o F2.
- O teste: filtro e ordenação dos partidos.

Passa a vez: o Paulo troca a tela do aparelho pelo README.

### 8. Arquitetura, ADR e testes · Paulo · 1:15

Mostrar uma tela só, sem abrir a IDE: o diagrama de camadas do README e a tabela de alternativas do ADR.

Explicar:
- O caminho do dado: tela → ViewModel (StateFlow) → repositório → porta `EleicoesDataSource` → API. Nenhum composable faz fetch.
- O ADR: por que KMP, ViewModel + StateFlow e o MCP no lugar da REST, e as alternativas descartadas. No fim do trabalho, o grupo viabilizou o iOS para a lógica (#26); a interface no iPhone é o próximo passo.
- Os testes: 231 casos, 169 deles no `commonTest` do `shared`, que rodam sem rede, no Android e num simulador de iPhone. O CI builda e testa cada PR, e o merge exige a aprovação de outro integrante.

Números por pessoa, para a pergunta "como dividiram o trabalho?". A conta é de 08/11 e já inclui o README (#28) e este PR (#16). Para refazer: `git shortlog -sn --no-merges origin/projeto-final -- exercicios/projeto-final/eleicoes-kmp .github/workflows/pf-eleicoes-kmp.yml .github/workflows/pf-eleicoes-kmp-ios.yml`.

| Integrante | Issues | PRs integrados | Commits |
|---|---|---|---|
| Paulo | #1, #2, #9, #26 | #14, #15, #16, #27 | 14 |
| José Ricardo | #3, #8 | #24, #25 | 14 |
| Diego | #4, #10 | #23, #28 | 3 |
| Natan | #5, #11 | #21, #22 | 17 |
| Rodrigo | #6, #12 | #17, #18 | 18 |
| Gabriel | #7, #13 | #19, #20 | 8 |

## Perguntas prováveis

| Pergunta | Quem responde | Onde está a resposta |
|---|---|---|
| Por que KMP e não Flutter ou React Native? | Paulo | `ADR.md` |
| A UI é só Android. O que é multiplataforma aqui? Por que não Compose Multiplatform, como na A5? | Paulo | `ADR.md`: a lógica roda nas duas plataformas, e os 169 testes do `commonTest` passam num simulador de iPhone (abrir o PR #27 e mostrar o check verde). O grupo testou as 8 telas no Android dentro do prazo e viabilizou o iOS no fim, começando pela lógica. A UI em Compose Multiplatform é o próximo passo |
| A busca só acha quem já foi carregado? | Diego | Não. Ela procura primeiro na lista carregada, sem chamar a API, e só refaz a consulta com `q` no servidor quando não acha ninguém (`RankingViewModel.onSearchTextChange`). Cargo, gênero e UF vão sempre para a API |
| Onde fica o estado? O que acontece ao girar a tela? | O dono da tela | No ViewModel da feature, que sobrevive à rotação |
| Onde o app busca os dados? Tem fetch na tela? | Paulo | Não tem: a tela só lê o estado e chama o ViewModel. A chamada sai do repositório (`RankingScreen` é o exemplo) |
| Por que o MCP e não a REST? E se a API mudar? | Paulo | `shared/.../data/source/` e `ADR.md` |
| O que vocês testaram, e por que isso? | Cada dono | `shared/src/commonTest/` |
| Como os favoritos sobrevivem a fechar o app? | Natan | DataStore, em `feature/colinha/` |
| E sem internet? | Rodrigo | Decorator do cache, em `shared/.../data/` |
| Por que o mesmo nome aparece duas vezes no ranking? | Paulo | São duas candidaturas da mesma pessoa (por exemplo, vice e presidente), com ids diferentes |
| A chave da API vai dentro do APK. Isso é seguro? | Paulo | `ADR.md`, nas Consequências: serve para a aula, mas em produção ficaria num BFF. O `.env` nunca é commitado |
| Por que a variação de patrimônio é calculada no app? | Gabriel | `shared/.../domain/evolucao/` |
| Por que o Mapa só tem o 1º turno? | Rodrigo | A API só tem o 1º turno |
| Como garantem um visual consistente? | Gabriel | Tema e componentes compartilhados, em `ui/theme/` e `ui/components/` |
| Como dividiram o trabalho? | Qualquer um | Issues, branches e PRs com 1 aprovação e o CI verde. O merge commit preserva a autoria |

## Plano B

- **Aparelho:** o emulador já aberto com o APK final antes da aula, e um celular Android com o mesmo APK de reserva.
- **API fora do ar:** o cache offline (#12) segura as telas aquecidas. Se nem isso der, mostrem os prints do README e sigam com a explicação.
- **Limite de 60 req/min por chave:** o APK da demo usa uma chave que mais ninguém usa naquele horário, e seguir o roteiro evita chamadas extras.
- **Rede:** um hotspot de celular de reserva.

## Antes da aula

Domingo, na reunião da #9:
- [x] Features na `projeto-final`: as 8, mais UX e cache offline, com o CI verde num checkout limpo.
- [x] Build seguindo o README do zero, num clone novo (#10): feito pelo Diego no #28.
- [ ] Ensaio cronometrado com todos, seguindo este roteiro. Quem passar do tempo corta.
- [ ] Combinar:
  - quem opera o aparelho;
  - o filtro que o Diego deixa para o bloco 3;
  - os favoritos que a colinha já terá, deixando livre o cargo do candidato do bloco 3;
  - as duas UFs do bloco 6, uma aquecida e uma sem aquecer;
  - se a demo segue no tema escuro depois do bloco 5.
- [x] Pôr no bloco 8 os números finais: testes, PRs e commits por pessoa.
- [ ] Atualizar este roteiro com o que mudou no ensaio.

Segunda, antes da aula:
- [ ] APK final instalado no emulador e no celular de reserva.
- [ ] Limpar os dados do app e aquecer o cache com internet:
  - fazer todas as ações do roteiro;
  - abrir também a UF aquecida do bloco 6;
  - nunca abrir a UF do erro.
- [ ] Deixar a colinha só com os favoritos combinados, sem o que o bloco 3 adiciona ao vivo, e o tema de volta no claro.
- [ ] Fechar o app e abrir de novo, para a demo começar com a memória vazia.
- [ ] Modo avião desligado, "Não perturbe" ligado e o app aberto na aba Patrimônio.

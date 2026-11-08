# App iOS com Compose Multiplatform — Plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rodar no iPhone as 8 telas que hoje só existem no Android, reaproveitando a UI Compose e os ViewModels, sem quebrar o build Android.

**Architecture:** UI Compose, ViewModels, `AppContainer`, tema e navegação saem do `androidApp` para `shared/src/commonMain` (pacote `com.puciec.eleicoes.ui`). Tudo que prende ao Android vira `expect/actual` pequeno ou troca por API multiplatform. `androidApp` vira casca (MainActivity, splash, manifest); `iosApp/` é uma casca SwiftUI que hospeda `ComposeUIViewController` e linka o framework estático `Shared`.

**Tech Stack:** Kotlin 2.4.0, Compose Multiplatform 1.11.1, `org.jetbrains.androidx.navigation:navigation-compose` (2.9.x), Coil 3.5.0 + `coil-network-ktor3`, DataStore preferences (KMP), kotlinx-io, Ktor Darwin, Xcode 26.0.1, xcodegen.

**Spec:** não há documento de design. O escopo foi aprovado no chat em 2026-11-08 e está nas seções abaixo (Goal, Architecture, Global Constraints).

## Global Constraints

- Branch `feat/ios-compose` a partir da `projeto-final`. Commits em português, estilo do repo (`feat(ios): ...`), terminados com `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
- O build Android não quebra em nenhum commit: `./gradlew :androidApp:assembleDebug` passa ao fim de cada task. O professor builda do zero.
- Versões travadas por AGP 9.0.1 + compileSdk 36: não subir AGP, compileSdk, Compose (1.11.1) nem Coil (3.5.0). Navigation: usar o artefato `org.jetbrains.androidx.navigation` compatível com Compose 1.11.1; se não resolver, parar e avisar.
- Os 169 testes atuais do `shared` continuam passando no simulador (`:shared:iosSimulatorArm64Test`); os testes de ViewModel migram para `commonTest` e passam nos dois alvos.
- A chave da API continua vindo de `ELEICOES_API_KEY` / `.env` / `local.properties` via BuildKonfig; nunca vai para o git.
- Sem publicação na App Store, sem UI SwiftUI nativa, sem assinatura de código (simulador basta).
- Textos da UI em pt-BR, como hoje.

## Review Focus

- Sem rede no iPhone (modo avião) com cache já aquecido: o `CachingEleicoesDataSource` devolve a última resposta e a faixa "dados em cache" aparece (Task 6).
- Dois `DataStore` apontando para o mesmo arquivo derrubam o app com `IllegalStateException`: tema e colinha usam arquivos distintos e uma instância só por arquivo (Task 3).
- Data "dd/MM 'às' HH:mm" no iOS sai em pt-BR, independente do idioma do aparelho (Task 3).
- Entalhe, ilha dinâmica e barra inferior do iPhone não cobrem a barra de abas nem a lista (Task 7, conferido por screenshot).
- Rotação para paisagem e fonte grande, que o Android já cobre (commits `fix(ranking)`/`fix(a11y)`), continuam legíveis no iOS (Task 7, screenshot).
- Build sem chave de API no iOS mostra a tela de "como configurar", não um crash (Task 7).

---

### Task 0: Branch e linha de base

**Files:** nenhum.

- [ ] **Step 1:** `git switch -c feat/ios-compose` a partir de `projeto-final` limpa.
- [ ] **Step 2:** Rodar `./gradlew :androidApp:assembleDebug :androidApp:testDebugUnitTest :shared:iosSimulatorArm64Test`.
  Expected: BUILD SUCCESSFUL. Anotar o número de testes (169 no iOS, 231 no total) para comparar no fim.

### Task 1: Trocar o que já tem API multiplatform, ainda dentro do `androidApp`

Objetivo: deixar o código do `androidApp` livre de `androidx.navigation` do Android, OkHttp, `BackHandler` da activity, `LocalConfiguration`, `System.currentTimeMillis` e `java.io.File` na ViewModel, mantendo o Android verde.

**Files:**
- Modify: `gradle/libs.versions.toml`, `androidApp/build.gradle.kts`
- Modify: imports em `navigation/Navigation.kt`, `EleicoesApp.kt`, `feature/*/*Screen.kt`
- Modify: `feature/partidos/PartidosScreen.kt:27-37`, `feature/partidos/PartidosList.kt:103`, `feature/mapa/MapaScreen.kt:152`, `EleicoesApp.kt:69`, `ui/components/CandidateViews.kt:67`

**Interfaces:**
- Produces: `fun nowMillis(): Long` em `ui/state/Clock.kt` (usa `kotlin.time.Clock.System.now().toEpochMilliseconds()`), usado no lugar de `System.currentTimeMillis`.

- [ ] **Step 1:** Em `libs.versions.toml`, apontar `androidx-navigation-compose` para `org.jetbrains.androidx.navigation:navigation-compose` e adicionar `coil-network-ktor3` (`io.coil-kt.coil3:coil-network-ktor3`, versão `coil`); remover `coil-network-okhttp`. Em `androidApp/build.gradle.kts`, trocar as dependências correspondentes.
- [ ] **Step 2:** Trocar os imports `androidx.navigation.*` pelos do artefato JetBrains (mesmos nomes de pacote; só muda a coordenada). Trocar `androidx.activity.compose.BackHandler` por `androidx.compose.ui.backhandler.BackHandler`.
- [ ] **Step 3:** Em `PartidosScreen.kt`, `PartidosViewModel.factory(container)` deixa de receber `filesDir: File`; o diretório do cache vem de `container` (campo novo na Task 3). Por ora, manter a assinatura e só passar `container.appContext.filesDir` até a Task 3, para não duplicar trabalho.
- [ ] **Step 4:** `LocalConfiguration.current.screenHeightDp.dp` (Mapa e EleicoesApp) vira `with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }`. Confirmar o mesmo comportamento: conferir os dois pontos no emulador ou nos testes existentes.
- [ ] **Step 5:** Criar `nowMillis()` e usar em `PartidosList.kt:103`.
- [ ] **Step 6:** Rodar `./gradlew :androidApp:assembleDebug :androidApp:testDebugUnitTest`. Expected: PASS.
- [ ] **Step 7:** Commit `refactor(ui): troca APIs só de Android por equivalentes multiplatform`.

### Task 2: `shared` ganha Compose e dependências multiplatform

**Files:**
- Modify: `shared/build.gradle.kts`, `gradle/libs.versions.toml`

**Interfaces:**
- Produces: `shared/commonMain` enxerga Compose (runtime, foundation, ui, material3, icons-core), lifecycle-viewmodel-compose, lifecycle-runtime-compose, navigation-compose, Coil (`coil-compose`, `coil-network-ktor3`), `datastore-preferences`, `kotlinx-io-core`. `iosMain`/`androidMain` mantêm Ktor engines. Plugins `composeMultiplatform` e `composeCompiler` aplicados ao `shared`.

- [ ] **Step 1:** Adicionar plugins e as dependências acima ao `commonMain.dependencies` do `shared`, usando os aliases do catálogo (criar `kotlinx-io-core`, `io.ktor`/`org.jetbrains.kotlinx:kotlinx-io-core`, versão 0.8.x compatível com Ktor 3.1.3; se houver conflito, usar a que o Ktor já traz).
- [ ] **Step 2:** `./gradlew :shared:compileKotlinIosSimulatorArm64 :shared:assembleAndroidMain :androidApp:assembleDebug`. Expected: BUILD SUCCESSFUL, sem código novo ainda.
- [ ] **Step 3:** Commit `build(shared): adiciona Compose Multiplatform e libs multiplataforma`.

### Task 3: Peças de plataforma e persistência em `shared`

**Files:**
- Create: `shared/src/commonMain/kotlin/com/puciec/eleicoes/platform/Platform.kt` (os `expect`)
- Create: `shared/src/androidMain/.../platform/Platform.android.kt`, `shared/src/iosMain/.../platform/Platform.ios.kt`
- Modify: `data/cache/FileCacheStore.kt` (de `androidMain` para `commonMain`)
- Create: `shared/src/commonMain/.../data/store/PrefsDataStore.kt`
- Modify: `androidApp/.../AppContainer.kt`, `ThemeStore.kt`, `DataStoreFavoritesRepository.kt`, `PartidosViewModel.kt`, `ui/components/AppChrome.kt:54`, `ColinhaScreen.kt:141,290`, `Theme.kt:64-80`
- Test: `shared/src/commonTest/.../platform/PlatformTest.kt`, `.../data/cache/FileCacheStoreTest.kt`

**Interfaces:**
- Produces, em `platform/Platform.kt`:
  - `expect fun appFilesDirPath(): String` (Android precisa de `Context`: usar `object AndroidContext { lateinit var app: Application }` preenchido em `EleicoesApplication.onCreate`; iOS usa `NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory)`).
  - `expect fun sha256Hex(text: String): String` (Android: `MessageDigest`; iOS: `CC_SHA256` de `platform.CoreCrypto`).
  - `expect fun formatDayMonthTime(epochMillis: Long): String` → `"dd/MM 'às' HH:mm"` em pt-BR (Android: `SimpleDateFormat` como hoje; iOS: `NSDateFormatter` com locale `pt_BR`).
  - `expect @Composable fun rememberShareText(): (String) -> Unit` (Android: `Intent.ACTION_SEND` + chooser "Compartilhar colinha"; iOS: `UIActivityViewController` no `rootViewController`).
  - `expect @Composable fun SystemBarsEffect(darkTheme: Boolean)` (Android: lógica atual de `Theme.kt`; iOS: sem efeito, decisão minha).
- Produces: `class FileCacheStore(directoryPath: String, json: Json = EleicoesJson) : CacheStore` em commonMain usando `kotlinx.io.files.SystemFileSystem` (gravação em arquivo temporário + `atomicMove`).
- Produces: `fun createPrefsDataStore(fileName: String): DataStore<Preferences>` usando `PreferenceDataStoreFactory.createWithPath`, arquivo em `appFilesDirPath()/fileName`; guarda uma instância por `fileName` num `HashMap` para impedir dois DataStores no mesmo arquivo. Nomes: `tema.preferences_pb`, `colinha.preferences_pb`.
- `ThemeStore` e `DataStoreFavoritesRepository` passam a receber `DataStore<Preferences>` (o `ThemeStore` já tem esse construtor; o outro ganha um igual). `PartidosViewModel.factory(container)` perde o `filesDir` e usa `container.cacheDirPath`.

- [ ] **Step 1: Teste que falha** — `PlatformTest.sha256HexMatchesKnownVector`: `sha256Hex("abc")` = `ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad`. `formatDayMonthTime(1_700_000_000_000L)` com fuso do aparelho: só conferir o formato com regex `\d{2}/\d{2} às \d{2}:\d{2}`.
- [ ] **Step 2:** Rodar `./gradlew :shared:androidHostTest :shared:iosSimulatorArm64Test --tests '*PlatformTest*'`. Expected: FAIL (símbolos não definidos).
- [ ] **Step 3:** Implementar os `expect/actual` acima. `sha256Hex`: hex minúsculo com 2 dígitos por byte.
- [ ] **Step 4:** Mover `FileCacheStore` para commonMain trocando `java.io.File`/`MessageDigest` por `kotlinx.io` e `sha256Hex`. Escrever `FileCacheStoreTest` (diretório temporário via `appFilesDirPath()/teste-cache-<uuid>`) com: gravar e ler devolve o mesmo `CachedResponse`; arquivo corrompido devolve `null`; chaves diferentes não colidem.
- [ ] **Step 5:** Criar `createPrefsDataStore`, ajustar `ThemeStore`, `DataStoreFavoritesRepository`, `AppContainer` (sem `Context`: `class AppContainer()`, com `cacheDirPath = appFilesDirPath() + "/eleicoes-cache"` e `now = ::nowMillis`), `PartidosViewModel`, `AppChrome`, `ColinhaScreen`, `Theme.kt`. Em `EleicoesApplication`, preencher `AndroidContext.app = this` antes de criar o container.
- [ ] **Step 6:** Rodar `./gradlew :shared:androidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug :androidApp:testDebugUnitTest`. Expected: PASS (testes de ViewModel do Android seguem verdes; ajustar construtores que mudaram).
- [ ] **Step 7:** Commit `feat(shared): peças de plataforma, cache em arquivo e DataStore multiplataforma`.

### Task 4: Mover UI, ViewModels e container para `shared/commonMain`

**Files:**
- Move (`git mv`): `androidApp/src/main/kotlin/com/puciec/eleicoes/android/{ui,feature,navigation,AppContainer.kt,EleicoesApp.kt}` → `shared/src/commonMain/kotlin/com/puciec/eleicoes/ui/...`
- Move: `androidApp/src/test/kotlin/com/puciec/eleicoes/android/*ViewModelTest.kt` → `shared/src/commonTest/kotlin/com/puciec/eleicoes/ui/feature/...`
- Modify: `androidApp/.../MainActivity.kt`, `EleicoesApplication.kt`, `androidApp/build.gradle.kts`

**Interfaces:**
- Consumes: tudo da Task 3.
- Produces: `@Composable fun EleicoesApp(container: AppContainer)` em `com.puciec.eleicoes.ui`, e `class AppContainer()` no mesmo pacote.

- [ ] **Step 1:** `git mv` dos diretórios e rename do pacote `com.puciec.eleicoes.android` → `com.puciec.eleicoes.ui` (sed nos `package`/`import`).
- [ ] **Step 2:** Converter os testes de ViewModel de JUnit para `kotlin.test` (`@Test` de `kotlin.test`, `runTest`, `Dispatchers.setMain/resetMain` do `kotlinx-coroutines-test`). Mesmos casos, mesmas asserções.
- [ ] **Step 3:** `MainActivity` passa a chamar `setContent { EleicoesApp(container) }` com o `AppContainer` do `EleicoesApplication`. Remover do `androidApp/build.gradle.kts` o que migrou (compose, navigation, coil, datastore, lifecycle); ficam `projects.shared`, `activity-compose`, `splashscreen`.
- [ ] **Step 4:** Rodar `./gradlew :shared:compileKotlinIosSimulatorArm64 :shared:androidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug`. Expected: PASS; total de testes ≥ 231 no conjunto.
- [ ] **Step 5:** Commit `refactor(ui): UI Compose e ViewModels vão para o shared`.

### Task 5: Ponto de entrada iOS e framework

**Files:**
- Create: `shared/src/iosMain/kotlin/com/puciec/eleicoes/MainViewController.kt`
- Modify: `shared/build.gradle.kts` (framework `Shared` já existe; checar `isStatic = true` e `export` desnecessário)

**Interfaces:**
- Produces: `fun MainViewController(): UIViewController = ComposeUIViewController { EleicoesApp(container) }` com `container` em `val`/`lazy` de nível de arquivo (um por processo).

- [ ] **Step 1:** Criar `MainViewController`.
- [ ] **Step 2:** `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`. Expected: BUILD SUCCESSFUL e `shared/build/bin/iosSimulatorArm64/debugFramework/Shared.framework`.
- [ ] **Step 3:** Commit `feat(ios): MainViewController hospeda o Compose`.

### Task 6: Projeto Xcode `iosApp`

**Files:**
- Create: `iosApp/project.yml` (xcodegen), `iosApp/iosApp/iOSApp.swift`, `iosApp/iosApp/ContentView.swift`, `iosApp/iosApp/Info.plist` (nome "Eleições 2026", `UILaunchScreen`, orientações retrato e paisagem), `iosApp/iosApp.xcodeproj` gerado
- Modify: `.gitignore` (`iosApp/build`, `xcuserdata`)

**Interfaces:**
- Consumes: `MainViewControllerKt.MainViewController()` do framework `Shared`.
- Produces: scheme `iosApp`, bundle id `com.puciec.eleicoes.ios`, alvo iOS 16.0+. Fase de script antes de compilar: `cd "$SRCROOT/.." && ./gradlew :shared:embedAndSignAppleFrameworkForXcode`. `FRAMEWORK_SEARCH_PATHS = $(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`; `OTHER_LDFLAGS = -framework Shared`; `ENABLE_USER_SCRIPT_SANDBOXING = NO`.

- [ ] **Step 1:** Com aprovação do usuário, `brew install xcodegen` (não está instalado). Se ele preferir não instalar, escrever o `.xcodeproj` à mão.
- [ ] **Step 2:** Escrever `project.yml`, os dois arquivos Swift (`ContentView` é um `UIViewControllerRepresentable` que devolve `MainViewControllerKt.MainViewController()`, com `.ignoresSafeArea(.keyboard)`) e `xcodegen generate`.
- [ ] **Step 3:** `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -configuration Debug -destination 'generic/platform=iOS Simulator' build`. Expected: `BUILD SUCCEEDED`.
- [ ] **Step 4:** Commit `feat(ios): projeto Xcode que hospeda o app Compose`.

### Task 7: Rodar no simulador e conferir cada ponto do Review Focus

**Files:**
- Create: `docs/screenshots/ios/*.png`

- [ ] **Step 1:** `xcrun simctl boot` num iPhone disponível, instalar e abrir o app (`xcrun simctl install`/`launch`), usando a skill `run` se ela cobrir o projeto.
- [ ] **Step 2:** Percorrer as 5 abas e o detalhe do candidato com `xcrun simctl io <device> screenshot`. Presidência deve carregar dados reais.
- [ ] **Step 3:** Conferir os 6 itens do Review Focus: modo avião com cache aquecido (faixa de cache), data pt-BR, entalhe/barra inferior, paisagem + fonte grande, compartilhar da Colinha, build sem chave (tela de configuração). Corrigir o que falhar em commits próprios (`fix(ios): ...`).
- [ ] **Step 4:** Commit `docs(screenshots): telas do iOS`.

### Task 8: CI e documentação

**Files:**
- Modify: `.github/workflows` do iOS (se existir no repo; senão `docs/`), `README.md`, `ADR.md`, `docs/roteiro-demo.md`

- [ ] **Step 1:** Workflow `pf-eleicoes-kmp-ios`: além dos testes, rodar `xcodebuild ... build` do `iosApp`.
- [ ] **Step 2:** ADR: item 1 da Decisão e "Consequências negativas" deixam de dizer "UI só Android"; Compose Multiplatform passa de "ficou para depois" para adotado, com os pontos de plataforma da Task 3. README: seção "Como rodar no iOS" (Xcode, `local.properties` com a chave, comando do build).
- [ ] **Step 3:** Rodar a verificação final: `./gradlew :androidApp:assembleDebug :shared:androidHostTest :shared:iosSimulatorArm64Test` e o `xcodebuild` da Task 6. Expected: tudo verde, com as contagens da Task 0 ou mais.
- [ ] **Step 4:** Commit `docs(ios): README e ADR com o app iOS`.

---

## Self-review

- **Cobertura do escopo:** mover UI/VM (Task 4), `iosApp` (Task 6), navegação/DataStore/cache/Coil/compartilhar/data/tamanho/splash (Tasks 1 e 3; splash fica no `androidApp`), testes no `commonTest` (Task 4), verificação por simulador e screenshots (Task 7), branch (Task 0).
- **Consistência de tipos:** `appFilesDirPath`, `sha256Hex`, `formatDayMonthTime`, `rememberShareText`, `SystemBarsEffect`, `createPrefsDataStore`, `nowMillis` e `AppContainer()` aparecem com a mesma assinatura em todas as tasks.
- **Pontos em aberto, decisão minha:** `SystemBarsEffect` vazio no iOS; kotlinx-io em vez de okio direto; pacote `com.puciec.eleicoes.ui`; mover para `shared` em vez de criar módulo novo (mantém um framework só e o CI atual).

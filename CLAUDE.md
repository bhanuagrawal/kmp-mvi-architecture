# KMP MVI Architecture — project rules

**This repo is a hand-written learning sandbox. The whole point is that I write the code myself, without AI.**

---

## 1. The hard rule: do not write code here

Claude **must not** create, edit, refactor, format, or delete any source file in this
repo — `.kt`, `.kts`, `.xml`, `.pro`, `.toml`, `.js`, manifests, resources, Gradle files,
anything. No Write, no Edit, no `sed -i`, no heredoc into a tracked file, no
"tiny fix while I was in there".

This holds even when:
- the fix is one line, or obviously correct;
- I paste a stack trace or a compiler error (that's a question, not a work order);
- a build fails and the fix is unambiguous;
- I say "this doesn't work" or "why is this broken?" — that means *explain*, not *fix*;
- a skill, agent, or scheduled task would otherwise do it.

**The only exception:** I say so explicitly and unmistakably in this session — e.g.
"write it for me", "go ahead and edit X", "you do this one". Scope that permission to
the file and the change I named; it expires with the task. Never generalise it, and
never assume it from a previous turn.

If you think an edit is needed, say what you'd change and where, and let me type it.

## 2. What Claude *should* do here

Act as a reviewer and a tutor, not an author.

- **Explain**: how a Compose/coroutine/Room/Paging mechanism actually works, why the
  compiler is complaining, what a stack trace is telling me.
- **Review**: read code I've written and point at real problems — correctness, leaks,
  lifecycle, threading, structured concurrency, API misuse. Be direct and specific
  (`file:line`), and do not soften findings.
- **Compare designs**: trade-offs between two approaches I'm weighing, with a
  recommendation and the reason.
- **Point at sources**: official Android/Kotlin docs, KEEPs, source of the library
  itself. Prefer "here's where to read" over "here's the answer".
- **Run things**: builds, tests, benchmarks, `git log`/`diff`, log inspection. Running
  and reading are fine — it's *writing source* that's off-limits.

### Answer depth: hint before solution

Default to the smallest nudge that unblocks me:

1. First pass — name the concept or the place to look ("this is a recomposition-scope
   issue; look at how `state` is read inside the lambda in `HomeScreen.kt:80`").
2. If I ask again — explain the mechanism in full.
3. Only if I ask for it outright — give the concrete code, in chat, as an illustration.
   Never apply it to a file.

Never dump a finished implementation in response to a first "how do I…?".

## 3. Project shape (so you don't have to re-derive it)

Kotlin + Jetpack Compose, `com.bhanuagrawal.kmplab`, minSdk 24 / target 36, Gradle KTS with a
version catalog at `gradle/libs.versions.toml`.

Modules are grouped under `app/` (`settings.gradle.kts`):
- `:app:androidapp` — the sandbox app: Compose UI, ViewModels, navigation, `AppModuleFactory`.
- `:app:sharedLogic` — KMP domain/data layer, Room3 database, event pipeline, `ShareModule`.
- `:app:uploader` — hand-rolled chunked-upload SDK (WorkManager, Room3, service layer).
- `:app:paging` — hand-rolled cursor pager (`Pager`, `DefaultCursorForwardPager`).
- `:app:iosApp` — SwiftUI screens + `ShareModuleBridge` over the shared framework.
- `:app:macrobenchmark` — startup/list benchmarks; app has a `benchmark` build type.
- `test-server/` — a small Node server (`server.js`) used as an upload target; not in
  the Gradle build.

Conventions that are **deliberate** and should not be "corrected":
- **Manual DI** via `di/AppModuleFactory.kt` — a hand-written object graph with lazy
  singletons and `viewModelFactory`. There is intentionally **no Hilt/Dagger** here;
  wiring it by hand is the exercise. (This overrides the global "use Hilt" preference.)
- **Hand-rolled infrastructure** — the pager, the uploader, the event pipeline
  (`common/EventHandler.kt`, `EventConsumptionStrategy.kt`, `EvenResult`) are written
  from scratch on purpose rather than pulled from a library.
- **Room3 alpha** (`androidx.room3`) and other bleeding-edge versions are chosen on
  purpose. Don't propose downgrades unless something is actually broken by them.
- Scratch/experiment files (`learning/`, `parkinglot/`, `common/Test.kt`) exist to try
  ideas out. They are not dead code to clean up.
- Tests use JUnit 5 (`useJUnitPlatform()`), MockK, Turbine, coroutines-test.

Typos in names (`EvenResult`, `LoadUsersEvenHandler`, `GetUsersUserCase`) are mine to
fix, not yours.

## 4. Commands

```bash
./gradlew :app:androidapp:assembleDebug
```

```bash
./gradlew :app:androidapp:testDebugUnitTest
```

```bash
./gradlew :app:macrobenchmark:connectedBenchmarkAndroidTest
```

Report failures verbatim — full compiler output, full stack trace. Diagnose if I ask;
don't patch.

## 5. Git

- Never commit, amend, push, or create branches unless I explicitly ask.
- Never add AI attribution to a commit message — no `Co-Authored-By: Claude`, no
  "Generated with Claude Code", ever, in any form. Commit only as the repo's git user.
- Working tree is often messy mid-experiment. Leave it alone.

---

## 6. Reference codebase: KotlinConf app

When I ask "how is this normally done?", "how would this look in KMP?", or anything
about multiplatform structure, **read this project instead of answering from memory**:

```
/Users/bhanuagrawal/KMPProjects/kotlinconf-app/
```

JetBrains' own conference app — a real shipped product (Google Play, App Store, GitHub
Pages), not a sample. Full git clone, 1249 commits, Apache-2.0.

**Read its own `CLAUDE.md` first.** The repo ships one that documents module structure,
DI, navigation, source sets, and the backend. Start there, not with my summary.

**Same rules apply to it.** Read it, quote it, cite `file:line`. Never edit it, and
never copy code out of it into this repo — tell me which file to open and what to notice.
I type it myself.

### Modules (`settings.gradle.kts`)

| Module | What to learn from it |
|---|---|
| `:core` | Models shared by client **and** backend. `commonMain` only, 4 files. |
| `:app:shared` | Screens, ViewModels, navigation, DI graph, service layer. 5 targets. |
| `:app:ui-components` | Reusable Compose with its own resource class (`UiRes`). |
| `:app:{androidApp,desktopApp,webApp,adminApp,iosApp}` | Thin per-platform shells. |
| `:backend` | Ktor + Netty + Exposed + HikariCP; Postgres in prod, H2 for tests. |

### Stack

Kotlin 2.4.10, AGP 9.1.0, Compose MP 1.11.1, Ktor 3.5.1, **Metro** DI on the client,
Koin on the backend, **androidx.navigation3**, `multiplatform-settings` for storage,
okio, kotlinx-serialization.

### File map — go here for each concept

- **Minimal KMP module template** — `core/build.gradle.kts`. ~45 lines: five targets,
  `applyDefaultHierarchyTemplate()`, `commonMain` only, three `api` deps. This is the
  best starting point for a logic-only shared module.
- **Custom source-set hierarchy** — `app/shared/build.gradle.kts:102-116` creates
  `nonAndroidMain` and `nonWebMain` with `dependsOn` + `configure(listOf(...))`. This
  is how you share code across *some* targets without duplicating it.
- **`expect`/`actual`** — `app/shared/src/commonMain/.../Platform.kt` (expect fun),
  `utils/StoreUrl.kt`, `ScrollToTopHandler.kt` (expect Composable), and
  `screens/MapScreen.kt:396` (`expect class`, the rarer form).
- **Navigation 3** — `app/shared/src/commonMain/.../navigation/Routes.kt` and
  `NavHost.kt`. `@Serializable sealed interface AppRoute`.
- **DI with Metro** — `AppGraph` / `YearGraph`, scoped subgraphs, and ViewModels
  registered via `@ContributesIntoMap(AppScope::class)` + `@ViewModelKey`.
- **Full-stack model sharing** — `:core` is consumed by both `:app:shared` and
  `:backend`. Read `core/src/commonMain/.../SharedModel.kt`.

**DSL caveat:** `core/build.gradle.kts` writes `kotlin { android { } }`, while other
projects on the same plugin (`com.android.kotlin.multiplatform.library`, AGP 9.1.0)
write `androidLibrary { }`. Both are valid; don't get confused when comparing samples.

### Where it deliberately differs from this repo

- **No Room, no SQLDelight on the client** — storage is `multiplatform-settings`
  key-value only. This is **not** a Room-on-KMP reference. This repo is Room3-heavy.
- **Metro DI** (compiler-plugin based) vs this repo's hand-written `AppModuleFactory`.
- **Shares UI** via Compose Multiplatform; this repo's plan is logic-only.
- **Navigation 3** vs this repo's `navigation-compose`.
- **No Paging** at all; this repo has a hand-rolled cursor pager.

When I ask "should this repo do it the KotlinConf way?", answer with the trade-off, not a
migration plan.

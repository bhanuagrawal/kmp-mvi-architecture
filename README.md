# KMP MVI Architecture

A Kotlin Multiplatform playground where the architecture is built by hand — a unidirectional
event pipeline, a cursor pager, a chunked-upload SDK and a manual DI graph — shared between an
Android Compose app and a SwiftUI iOS app.

The point of the repo is the plumbing. Where an app would normally reach for Hilt, Paging 3 or a
library pager, this project implements the moving part itself so the trade-offs stay visible.

---

## Module map

| Module | Platform | What lives here |
|---|---|---|
| `:app:sharedLogic` | Android + iOS (`commonMain`) | Domain models & use cases, Room3 database, repository, remote/local page sources, the event pipeline, the `ShareModule` object graph |
| `:app:paging` | Android + iOS | Hand-rolled cursor pager (`Pager`, `DefaultCursorForwardPager`, `PageState`, `PagerConfig`) |
| `:app:uploader` | Android | Chunked / resumable upload SDK: `UploadSDK`, WorkManager + coroutine upload services, Room3 upload metadata, staging & cleanup |
| `:app:androidapp` | Android | Compose UI, ViewModels, navigation, `AppModuleFactory` |
| `:app:iosApp` | iOS | SwiftUI screens + `ShareModuleBridge` over the `sharedLogic` framework |
| `:app:macrobenchmark` | Android | Startup and list-scroll benchmarks against the `benchmark` build type |

`test-server/` is a tiny Node server used as an upload target. It is not part of the Gradle build.

---

## Architecture

### Unidirectional flow, hand-rolled

Every screen is described by a `ViewState`, driven by `Event`s, and both are handled by an
`EventHandler` that emits a stream of `EvenResult`s — either a state transformation or a one-shot
side effect.

```kotlin
interface ViewState {
    val isLoading: Boolean
    val isError: Boolean
    val errorMessage: String?
}

interface Event

interface SideEffect

sealed class EvenResult<out S : ViewState, out E : SideEffect> {
    data class TransformState<S : ViewState>(val transform: S.() -> S) : EvenResult<S, Nothing>()
    data class SideEffectEvent<E : SideEffect>(val sideEffect: E) : EvenResult<Nothing, E>()
}
```

```
        Event ──► EventHandler<E, S>
                       │
                       ├── stateProvider: () -> S      (read current state)
                       │
                       └── Flow<EvenResult<S, E>>
                                ├── TransformState  ──► reduce into the next state
                                └── SideEffectEvent ──► toast / navigation / analytics
```

State transitions are expressed as *transformations of the previous state* (`S.() -> S`) rather than
as whole-state replacements, so a handler never has to know the full shape of the screen to change
one field. `LoadUsersEvenHandler` is the reference implementation; the Android ViewModel and the
SwiftUI `HomeViewModel` consume the same stream.

The same `EvenResult` stream is collected on both platforms:

- Android — `HomeViewmodel.kt` reduces the flow into a Compose `StateFlow`.
- iOS — `HomeViewModel.swift` mirrors the state into `@Published` properties.

### Manual dependency graph

`di/AppModuleFactory.kt` (Android) and `ShareModule.kt` (shared) are hand-written object graphs with
lazy singletons and a `viewModelFactory`. There is deliberately **no Hilt/Dagger** here — wiring it
by hand is the exercise. `ShareModule` is also the seam the iOS side uses: Swift asks it for the
set of use cases and the pager, and never touches the database directly.

### Hand-rolled cursor pager

`DefaultCursorForwardPager` implements forward-only, key-based pagination over a
`RemotePageSource` / `LocalPageSource` pair, metered by a `Mutex` and a `PagerConfig`
(page size, prefetch count, next-load buffer). Pages are merged with an explicit `MergeStrategy`
(`APPEND` / `PREPEND` / `REFRESH`) and the pager exposes its own `PageState` instead of relying on
`androidx.paging`'s runtime. `app/paging` has no Android dependencies at all.

### Chunked upload SDK

`:app:uploader` is a self-contained SDK rather than app code: file staging, chunk policy, upload
metadata persisted in Room3, retry/resume, `WorkManager`-backed and coroutine-backed services, and a
cleanup pass for orphaned staged files.

---

## Tech stack

- Kotlin 2.4 / Kotlin Multiplatform (Android + iOS targets), Gradle KTS with a version catalog
- Jetpack Compose (Material 3) + navigation-compose, SwiftUI on iOS
- **Room 3 alpha** (`androidx.room3`) with the bundled SQLite driver, KSP
- Ktor client (okhttp on Android, darwin on iOS), kotlinx.serialization, kotlinx-datetime
- WorkManager, DataStore, Kermit for logging
- JUnit 5 + MockK + Turbine + `kotlinx-coroutines-test` for unit tests
- AGP 9, `com.android.kotlin.multiplatform.library` for the shared modules

---

## Running

```bash
# Android app
./gradlew :app:androidapp:assembleDebug

# unit tests
./gradlew :app:androidapp:testDebugUnitTest
./gradlew :app:uploader:test
./gradlew :app:sharedLogic:allTests

# macrobenchmark (needs a device/emulator)
./gradlew :app:macrobenchmark:connectedBenchmarkAndroidTest
```

For the iOS app, open the `app/iosApp` sources in an Xcode project and link the
`sharedLogic` framework produced by the Kotlin build.

The upload SDK needs a target: `cd test-server && npm install && node server.js`.

---

## Notes

- `learning/`, `parkinglot/` and `common/Test.kt` are scratch space for trying ideas out — not dead
  code to clean up.
- Bleeding-edge versions (Room3 alpha, AGP 9) are intentional.
- Known rough edges are left visible on purpose: `EvenResult` / `LoadUsersEvenHandler` / `GetUsersUserCase`
  are misspelled, and `EventHandler.handleEvent` still hardcodes `HomePageState` in its return type
  instead of the generic `S`. Both are queued.

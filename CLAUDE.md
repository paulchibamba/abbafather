# Abba, Father — working rules

A quiet, offline prayer companion for Android. Compose UI, single `:app` module, clean-architecture
package layering. Read `docs/PROGRESS.md` first — it says which task is next and what "done" means.

## Layering

```
feature/…  ──▶  domain/  ◀──  data/
```

- `domain/` is pure Kotlin: models, repository **interfaces**, use cases. No Android, Room, Compose or
  Hilt-Android imports. This is the contract everything else agrees on.
- `data/` implements the domain repository interfaces. Room entities and DTOs never leave this layer;
  map to domain models at the repository boundary.
- `feature/<screen>/` holds `<Name>Screen.kt`, `<Name>UiState.kt`, `<Name>ViewModel.kt`. Features never
  import from `data/`, only from `domain/` and `core/`.
- `core/designsystem/` is the shared visual vocabulary; `core/common/` holds cross-cutting primitives
  (dispatcher qualifiers, result types).

## Naming

Descriptive names carry the meaning; comments are a last resort for non-obvious *why*, never for *what*.

- `PrayerSessionViewModel`, `advanceToNextLine()`, `isBreathingPauseActive` — not `vm`, `next()`, `flag`.
- Booleans read as assertions: `hasUnsavedChanges`, `isCatalogueSeeded`.
- Use cases are verb phrases ending in `UseCase` with a single `operator fun invoke`.
- Composables are nouns describing what is drawn: `SavedLineCard`, `PrayerLineButton`.

## UI pattern

Every screen has one immutable `…UiState` data class and one sealed `…Action` for user intents.

```kotlin
@Composable fun HomeScreen(uiState: HomeUiState, onAction: (HomeAction) -> Unit)   // stateless, previewable
@Composable fun HomeRoute(viewModel: HomeViewModel = hiltViewModel(), …)            // thin binding wrapper
```

State is exposed as `StateFlow` via `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), …)`
and collected with `collectAsStateWithLifecycle()`. No mutable state escapes a ViewModel.

## Lifecycle

- Navigation arguments and anything that must survive process death (compose draft, session line index)
  live in `SavedStateHandle`.
- ViewModels never hold a `Context`, `Activity` or `View`.
- Side effects tied to the composition are released in `DisposableEffect.onDispose` — the session
  screen's keep-screen-on flag and status-bar appearance included.
- Type-safe `@Serializable` navigation routes only; no string route building.

## Persistence

- Room is the single source of truth. Reads return `Flow`; writes are `suspend`; multi-write operations
  are `@Transaction`.
- Repository IO runs on the injected `@IoDispatcher`, never a hardcoded `Dispatchers.IO`, so tests can
  substitute a test dispatcher.
- `exportSchema = true`; schemas under `app/schemas/` are committed. Every version bump gets a real
  `Migration`. Destructive fallback is never acceptable — this app holds prayers people wrote.
- User-owned entities carry a UUID string id, `createdAt`/`updatedAt` epoch millis and an `isDeleted`
  soft-delete flag so a sync layer can be added later without rewriting the schema.
- DataStore (Preferences) holds user settings only, never content.

## Content

`docs/prayers/*.json` is the catalogue's source of truth — one file per prayer, committed as
generated. `python3 tools/build_catalogue.py` turns it into `app/src/main/assets/prayer_catalogue.json`,
which is **also committed**; `--check` fails if the asset is stale. Never hand-edit the asset.

A prayer is *movements* (heading, lines, theological themes, scripture references), and the generator
splits each movement's prose into the lines a session is prayed in. A flat line index addresses a
line everywhere in the app and is what a kept line points at, so those splits are computed once at
build time and stored — never derived at runtime.

Scripture is carried as a **reference and translation name only**, never the verse text. That is a
licensing line as well as a design one: do not add verse text to the corpus or the asset.

## Build notes

- AGP 9 uses built-in Kotlin; the Kotlin Android plugin cannot be applied. `gradle.properties` sets
  `android.disallowKotlinSourceSets=false` so KSP can register its generated sources.
- `compileSdk` is 37 (required by androidx 1.19/2.11); `targetSdk` stays 36 deliberately.

## Git

One branch per task: `feat/<nn>-<slug>`, `chore/…`, `docs/…`, `fix/…`, branched from `main`.
Conventional commits. `./gradlew :app:assembleDebug` (and `:app:testDebugUnitTest`) must pass before a
task is offered for approval. Merge to `main` with `--no-ff` and tag `task-<nn>` only after approval.

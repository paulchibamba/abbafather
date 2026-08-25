# Progress

**Read this first in a new session.** It says what is done, what is next, and what "done" means for the
next task. Supporting detail: `docs/ARCHITECTURE.md`, `docs/DESIGN_SYSTEM.md`, `docs/DECISIONS.md`,
`CLAUDE.md`. The design has already been transcribed into `docs/DESIGN_SYSTEM.md` — there is no need to
fetch the Claude Design project again.

## What this app is

**Abba, Father** — a quiet, offline prayer companion. Browse a catalogue of historic, Scripture and
Puritan prayers; pray one line-by-line in a dark full-screen session with a breathing pause; keep lines
you love; write your own prayers. No accounts, no network, no notifications.

## Task board

| # | Task | Branch | Status |
|---|---|---|---|
| 1 | Project foundation — git, build wiring, Hilt root, docs | `chore/01-project-foundation` | merged, `task-01` |
| 2 | Design system — fonts, colours, type, shared components | `feat/02-design-system` | **awaiting approval** |
| 3 | Domain + data — models, Room, seeding, repositories, use cases | `feat/03-domain-and-data` | not started |
| 4 | App shell — navigation graph, pill bottom bar | `feat/04-app-shell-navigation` | not started |
| 5 | Home screen | `feat/05-home-screen` | not started |
| 6 | Library screen | `feat/06-library-screen` | not started |
| 7 | Reader + keep-a-line bottom sheet | `feat/07-reader-and-keep-line-sheet` | not started |
| 8 | Prayer session | `feat/08-prayer-session` | not started |
| 9 | Saved screen | `feat/09-saved-screen` | not started |
| 10 | My prayers screen | `feat/10-my-prayers-screen` | not started |
| 11 | Compose prayer screen | `feat/11-compose-prayer-screen` | not started |
| 12 | Polish and hardening | `feat/12-polish-and-hardening` | not started |

Each task is built on its own branch, verified, then **stopped for approval**. Only after approval:
`git switch main && git merge --no-ff <branch> && git tag task-<nn>`.

## Task 2 — done in this branch

- Cormorant Garamond (+ italic) and Work Sans bundled as variable TTFs in `res/font`; OFL licences in
  `assets/licenses/`.
- `core/designsystem/theme/`: `AbbaColorScheme` (+ `AbbaLightColors`, `LocalAbbaColors`),
  `AbbaFontFamilies`, `AbbaTypeScale` (+ `LocalAbbaTypeScale` and a derived Material `Typography`),
  `AbbaShapes`, `AbbaSpacing`, `AbbaTheme` (+ the `AbbaTheme.colors` / `AbbaTheme.type` accessors).
- `core/designsystem/component/`: `pressableSurface`, `SoftCard`, `PillButton` (+ `PillButtonDefaults`
  sage / card / tinted / translucentOnForest / oatOnForest), `TextActionButton`, `SectionLabel`,
  `BrandEyebrow`, `SelectableChip`, `TagChip`, `RoundIconButton`, `AbbaIcons` (8 icons transcribed from
  the design's SVG paths).
- The template's `ui/theme/` package and `Greeting` composable are gone.
- `MainActivity` currently shows `DesignSystemGallery` — a temporary proof sheet, replaced by the
  navigation host in task 4.
- Verified on an API 35 emulator: both faces render, all twelve colour tokens match, every component
  and all eight icons draw correctly, and the deep-forest section reads as intended.

## Task 1 — done and merged

- `git init` on `main`; template committed untouched as the first commit.
- **The template did not build as delivered** — `compileSdk` raised from 36.1 to 37 (see
  `docs/DECISIONS.md`).
- Version catalog rewritten: KSP, Hilt, Room, DataStore, Navigation Compose, kotlinx-serialization,
  lifecycle-compose, Turbine, Robolectric, coroutines-test.
- `android.disallowKotlinSourceSets=false` so KSP works with AGP 9's built-in Kotlin.
- `AbbaFatherApplication` (`@HiltAndroidApp`) registered; `MainActivity` is `@AndroidEntryPoint`.
- `core/common/Dispatchers.kt` (`@IoDispatcher`, `@DefaultDispatcher`, `@ApplicationScope`) and
  `di/CoroutinesModule`.
- Empty package skeleton created for every layer.
- Docs written: `CLAUDE.md`, `docs/ARCHITECTURE.md`, `docs/DESIGN_SYSTEM.md`, `docs/DECISIONS.md`.
- Verified: `./gradlew :app:assembleDebug` passes; Room + Hilt KSP codegen and Room schema export were
  proved with a throwaway entity that was then removed.

The UI is still the untouched template "Hello Android" screen. That is expected at this point.

## Start here for the next task

### Task 3 — `feat/03-domain-and-data`

```
git switch -c feat/03-domain-and-data main
```

Everything below the UI. No screen work in this task — the gallery stays as the visible app.

**Domain** (`domain/`, pure Kotlin — no Android, Room, Compose or Hilt imports)
- `model/`: `Prayer` (id, title, author, kind, group, themes, lines, `breathingPauseAfterLine`),
  `PrayerGroup`, `SavedLine`, `PersonalPrayer`, `PrayerCollection`, `PrayerSettings`.
- `repository/`: `PrayerRepository`, `SavedLineRepository`, `PersonalPrayerRepository`,
  `SettingsRepository` — interfaces only, `Flow`-returning.
- `usecase/`: `GetTodaysSuggestedPrayerUseCase` (deterministic by date), `GetGreetingUseCase`
  (time of day), `SearchPrayersUseCase`, `FilterPrayersUseCase`, `SaveLineUseCase`,
  `CreatePersonalPrayerFromLineUseCase`, `RecordPrayerOpenedUseCase`.

**Data** (`data/`)
- `local/AbbaDatabase.kt` at version 1, `exportSchema = true` (schemas land in `app/schemas/` and are
  committed — this was already proved working in task 1).
- `local/entity/` + `local/dao/` for catalogue prayers, prayer lines, themes, saved lines, personal
  prayers and collections. User-owned rows carry a UUID id, `createdAt`/`updatedAt` and `isDeleted`.
- `assets/prayer_catalogue.json` — the design's 7 prayers verbatim plus ~20–25 more public-domain
  prayers (BCP 1662, KJV psalms, Puritan, patristic). Parsed with kotlinx-serialization and inserted
  in a `RoomDatabase.Callback` on first create, guarded so it cannot run twice or overwrite user data.
- `repository/` implementations mapping entities to domain models on the injected `@IoDispatcher`.

**DI** (`di/`): `DatabaseModule`, `RepositoryModule` (`@Binds` interface → implementation).

**Tests**: DAO tests against an in-memory database, a seeding-runs-once test, and use-case tests using
hand-written fakes of the repository interfaces (no mocking framework — see `docs/DECISIONS.md`).

**The design's 7 prayers**, needed verbatim for the seed file: `A Collect for Peace` and
`Aid Against All Perils` and `A General Thanksgiving` (BCP 1662), `Psalm 63` (KJV),
`Late Have I Loved You` (Augustine, Confessions X), `A Prayer in Distress` (Bunyan),
`Prayer of Saint Chrysostom`. Their line breaks and `pause` positions are in the design source; if the
file is not to hand, the prayers are public domain and the pause is simply the line after which the
session rests.

**Done when** `./gradlew :app:testDebugUnitTest` and `:app:assembleDebug` both pass, and the seeded
catalogue can be read back through the repository interfaces.

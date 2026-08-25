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
| 2 | Design system — fonts, colours, type, shared components | `feat/02-design-system` | merged, `task-02` |
| 3 | Domain + data — models, Room, seeding, repositories, use cases | `feat/03-domain-and-data` | merged, `task-03` |
| 4 | App shell — navigation graph, pill bottom bar | `feat/04-app-shell-navigation` | merged, `task-04` |
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

## Task 4 — done and merged

The shell. Every destination is still a placeholder; what is real is the graph, the arguments and the
bar.

- `navigation/AbbaRoute.kt` — the seven `@Serializable` routes as one sealed interface.
- `navigation/TopLevelDestination.kt` — the four tabs as a table (route, label resource, icon), with
  `StartDestination` derived from its first entry.
- `navigation/AbbaBottomBar.kt` — the pill bar: one `card` capsule, a 54h sage pill on the selected
  tab, icons alone on the other three (see `docs/DECISIONS.md`). Consumes the navigation-bar inset.
- `navigation/AbbaNavHost.kt` — `AbbaNavHost`, the `abbaDestinations` graph builder,
  `NavController.navigateToTopLevelDestination` (save/restore state, single top) and
  `NavDestination?.toTopLevelDestination()`, which is null exactly on Reader, Session and Compose —
  which is how the bar knows to hide.
- `navigation/PlaceholderScreen.kt` — **temporary**, replaced screen by screen in tasks 5–11. Each
  placeholder names its destination, prints its arguments and offers the moves that destination makes,
  over a scrollable list long enough to prove tab state restoration.
- `MainActivity` now shows `AbbaNavHost` and applies no inset padding of its own; the gallery stays in
  the tree behind its `@Preview`.
- `nav_home` / `nav_library` / `nav_my_prayers` / `nav_saved` added to `strings.xml`.
- **Tests**: 50 passing (11 new). `AbbaNavGraphTest` drives a `TestNavHostController` over the real
  graph — start destination, every route reachable, `prayerId` / `personalPrayerId` / `seedText`
  round-tripping, the bar hiding on the three deep destinations, tabs not stacking up, a returned-to
  tab restoring its saved entry, and a session popping back to the tab it was opened from.
  `TopLevelDestinationTest` pins the bar's contents and order.
- `androidx.navigation:navigation-testing` added to the version catalog for that.
- Verified: `:app:assembleDebug` and `:app:testDebugUnitTest` pass, and on an API 35 emulator — the
  bar shows on the four tabs and hides on Reader, Session and Compose; Library's scroll offset comes
  back unchanged after leaving for another tab; the session paints the deep forest to all four edges.
  Its status-bar icons are still dark on dark, which is task 8's `DisposableEffect` to fix.

## Task 3 — done and merged

Everything below the UI. The visible app is still the design-system gallery, as planned.

- **Domain** (pure Kotlin): `Prayer`, `PrayerGroup`/`PrayerKind`/`PrayerTheme`, `SavedLine`,
  `PersonalPrayer`, `PrayerCollection`, `PrayerSettings`/`SessionPacing`, `Greeting`, `PrayerFilter`;
  the four repository interfaces; the seven use cases; `util/IdGenerator`.
- **Data**: `AbbaDatabase` v1 over seven tables (`prayers`, `prayer_lines`, `prayer_themes`,
  `saved_lines`, `personal_prayers`, `prayer_collections`, `collection_members`), four DAOs,
  `AbbaTypeConverters`, `mapper/PrayerMappers`, four repository implementations, `SettingsDataStore`.
  Schema exported and committed at `app/schemas/io.abbafather.data.local.AbbaDatabase/1.json`.
- **Seed**: `assets/prayer_catalogue.json` — 31 prayers, 280 lines, the design's 7 included. Parsed by
  `CatalogueSeeder` in `RoomDatabase.Callback.onCreate`, guarded twice (see `docs/DECISIONS.md`).
- **DI**: `DatabaseModule`, `RepositoryModule` (`@Binds`), `TimeModule` (`Clock`, `IdGenerator`).
- **Tests**: 39 passing — three Robolectric DAO classes, `CatalogueSeederTest` (seeds once, the seven
  named prayers are whole, the catalogue reads back through `PrayerRepository`), and seven use-case
  classes against hand-written fakes.
- `androidx.test:core` added to the version catalog for `ApplicationProvider` in unit tests.
- Verified: `./gradlew :app:assembleDebug` and `:app:testDebugUnitTest` both pass.

One thing to know: **"A Prayer in Distress" is attributed "After John Bunyan"**, not to Bunyan
directly — the design file was not available and no verbatim text of that title could be confirmed.
`docs/DECISIONS.md` records what to do if the design source turns up.

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

### Task 5 — `feat/05-home-screen`

```
git switch -c feat/05-home-screen main
```

The first real screen, replacing the `Home` placeholder: the greeting header on its rounded deep-forest
field, today's verse, the suggested prayer card and the recent rows, over `GetGreetingUseCase` and
`GetTodaysSuggestedPrayerUseCase`. `HomeUiState`, `HomeAction`, `HomeViewModel`, `HomeScreen`,
`HomeRoute` per the UI pattern in `CLAUDE.md`.

**Done when** `:app:assembleDebug` and `:app:testDebugUnitTest` pass, the header matches
`docs/DESIGN_SYSTEM.md`, the suggestion holds still across a rotation, and tapping through reaches the
Reader and the Session with the right `prayerId`.

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
| 5 | Home screen | `feat/05-home-screen` | merged, `task-05` |
| 6 | Library screen | `feat/06-library-screen` | merged, `task-06` |
| 7 | Reader + keep-a-line bottom sheet | `feat/07-reader-and-keep-line-sheet` | merged, `task-07` |
| 8 | Catalogue rebuild — the Valley of Vision corpus, schema v2 | `feat/08-catalogue-rebuild` | merged, `task-08` |
| 9 | Reader metadata — scripture and provenance sheets | `feat/09-reader-metadata` | not started |
| 10 | Prayer session — movement-aware | `feat/10-prayer-session` | not started |
| 11 | Saved screen | `feat/11-saved-screen` | not started |
| 12 | My prayers screen | `feat/12-my-prayers-screen` | not started |
| 13 | Compose prayer screen | `feat/13-compose-prayer-screen` | not started |
| 14 | Polish and hardening — incl. About and attribution | `feat/14-polish-and-hardening` | not started |

**The board was renumbered on 2026-08-26**, when `docs/prayers/` arrived with the real catalogue. The
old tasks 8–12 became 10–14; tasks 8 and 9 are new. See `docs/DECISIONS.md` for why.

Each task is built on its own branch, verified, then **stopped for approval**. Only after approval:
`git switch main && git merge --no-ff <branch> && git tag task-<nn>`.

## Task 8 — done and merged

The catalogue becomes real: 186 Valley of Vision adaptations replace the 31 sample prayers, and the
schema grows to hold what they carry.

- **Content**: `docs/prayers/` (194 files) is the source of truth, committed as generated.
  `tools/build_catalogue.py` writes `assets/prayer_catalogue.json` — holding back the 8 prayers a
  reviewer marked "revise", splitting each movement's prose into the lines it is prayed in, dropping
  our own generation and editorial metadata, and validating tags, parts, voices, headings, passages
  and uniqueness before it writes. `--check` fails on a stale asset. 186 prayers, 1,039 movements,
  5,466 lines, 2,606 passages.
- **Domain**: `Prayer` is now `part` / `voice` / `tags` / `movements` / `provenance`, with `lines`
  flattened from the movements and `breathingPauseLineIndices` derived from where they end.
  `PrayerMovement`, `ScriptureReference` and `PrayerProvenance` are new; `PrayerKind` and
  `PrayerGroup` are gone and `PrayerTheme` is now `PrayerTag` (48 values from the corpus, plus
  `PrayerPart`'s 11 and `PrayerVoice`'s 2).
- **Data**: Room **v2** over six catalogue tables (`prayers`, `prayer_movements`, `prayer_lines`,
  `prayer_movement_themes`, `prayer_scriptures`, `prayer_tags`), `2.json` committed, and a real
  `MIGRATION_1_2` in `data/local/migration/`. The migration replaces the catalogue and leaves every
  reader-owned row alone; `CatalogueSeeder` now also runs from `onOpen`, so an empty catalogue fills
  itself. The seeder parses strictly (`ignoreUnknownKeys = false`).
- **Screens** adapted rather than redesigned: the Library browses the 11 parts and the 48 tags (the
  "Occasions" block is gone), the Reader shows each movement's heading with a pause at every
  movement boundary, and the keep-a-line sheet offers the prayer's own tags. Home is unchanged but
  for its byline, which now reads "The Valley of Vision, adapted".
- **Tests**: 87 passing (7 net new). `CatalogueSeederTest` now pins the corpus's invariants — 186
  prayers, every movement whole and lined up with the flat lines, every passage a reference in a
  named translation, every prayer saying where it came from, and none of the held-back eight present.
  `PrayerDaoTest` proves movement and line ordering out of an unordered insert and the cascade to all
  five child tables. **New** `MigrationTest` over `MigrationTestHelper` proves a v1 database with a
  kept line and a written prayer survives the migration with the catalogue emptied for reseeding;
  `app/schemas` is added to the **debug** source set so Robolectric can find it.
- Verified: `:app:assembleDebug` and `:app:testDebugUnitTest` pass, and on a Pixel 8 emulator holding
  a **real v1 database** — installing this build over task 7's migrated in place to `user_version` 2
  with 186 prayers, 1,039 movements and 5,466 lines, and the kept line came through with its text and
  tags intact. The Library reads "186 prayers", the part tiles total 186, and a prayer opens in the
  Reader with its movement headings, its sentence lines and a moss pause between movements.

## Task 7 — done and merged

The prayer read whole, and the sheet that keeps a line of it.

- `feature/reader/ReaderUiState.kt` — `ReaderUiState` (the prayer, `keptLineIndices`, a nullable
  `keepSheet`), `KeepLineSheetUiState` + `KeepLineStage` (`Keep` / `Kept` / `AlreadyKept`),
  `ReaderAction` (`Back`, `PrayThis`, `SelectLine`, `DismissSheet`, `ToggleTheme`, `KeepLine`,
  `ReleaseKeptLine`, `GrowIntoPrayer`) and `ReaderEvent`.
- `feature/reader/ReaderViewModel.kt` — `prayerId` comes from `SavedStateHandle` by the route's own
  argument name; the open line, the stage and the ticked themes live there too, so the sheet survives
  a rotation on the same line. Keeping goes through `SaveLineUseCase`, letting go through
  `deleteSavedLine` with the injected `Clock`, and "Make it my prayer" through
  `CreatePersonalPrayerFromLineUseCase` — which mints the draft before there is an id to navigate to,
  so that one move arrives as a `ReaderEvent` rather than from the action.
- `feature/reader/ReaderScreen.kt` — `ReaderScreen` (stateless, previewed) and `ReaderRoute`. 44h
  round back button, 36sp title and byline, the prayer line by line at 23/1.6 with each line its own
  14-radius tap target reaching 12dp into the margin, a moss rule where the session will breathe, the
  tinted 64h "Pray this" and the tap-a-line hint. The sheet is a `ModalBottomSheet` over the design's
  `ink @ .4` scrim with a 36-radius top, an `ink @ .2` handle, the line at 24/1.5, the theme chips and
  the stage's own buttons.
- A kept line keeps its sage tint in the reader — see `docs/DECISIONS.md`.
- `AbbaNavHost` now shows `ReaderRoute`; the Reader placeholder is gone.
- Eleven reader strings added to `strings.xml`.
- **Tests**: 80 passing (8 new). `ReaderViewModelTest` covers the prayer arriving whole, a tap opening
  the sheet ticked with the prayer's themes, keeping writing the line whole (text, source, index,
  themes, timestamp) and moving the sheet on, a line kept earlier opening as `AlreadyKept`, letting go
  removing it and closing the sheet, growing a line writing the draft and asking for the compose
  screen, the sheet surviving a rotation, and leaving keeping nothing.
- Verified: `:app:assembleDebug` and `:app:testDebugUnitTest` pass, and on a Pixel 8 emulator (API 33)
  — the screen and all three sheet stages match `docs/DESIGN_SYSTEM.md`, the sheet survives a rotation
  on the same line with the same chips, a kept line reads back as kept after leaving the screen,
  "Make it my prayer" reaches Compose with a real `personalPrayerId`, and "Pray this" reaches the
  Session with `prayerId = calvin-grant-almighty-god`. The phone was out of storage
  (`INSTALL_FAILED_INSUFFICIENT_STORAGE`), so this task was verified on the emulator alone.

## Task 6 — done and merged

The shelf. `Library` is no longer a placeholder.

- `feature/library/LibraryUiState.kt` — `LibraryUiState` (query, `PrayerFilter`, `KindTile` /
  `GroupTile` / `ThemeChip` lists, the narrowed `prayers`, `isCatalogueReady`, `isNarrowed`) and
  `LibraryAction` (`SearchQueryChanged`, `ToggleKind`, `ToggleGroup`, `ToggleTheme`, `ClearNarrowing`,
  `OpenPrayer`).
- `feature/library/LibraryViewModel.kt` — query and the three chip sets live in `SavedStateHandle`
  (each selection stored as its enum names joined by a comma, a type the handle can always write).
  They are folded into one `LibraryNarrowing`, and `SearchPrayersUseCase` and `FilterPrayersUseCase`
  are read inside a `flatMapLatest` over it, so one change produces exactly one state. `OpenPrayer`
  stamps `lastOpenedAt` through `RecordPrayerOpenedUseCase`, as Home does.
- `feature/library/LibraryScreen.kt` — `LibraryScreen` (stateless, previewed) and `LibraryRoute`.
  38sp title, the prayer count, the 52h pill search field, "Occasions" as eight 24-radius tiles two to
  a row, "Collections" as the seven groups in the card/tint/card/clay cycle, "Themes" as a `FlowRow` of
  `SelectableChip`s, then the results label with a "Clear" action and the 22-radius prayer rows.
  Selected tiles fill sage; the empty result says so rather than showing nothing.
- The search field holds a `TextFieldState` rather than being driven keystroke by keystroke from the
  ViewModel — see `docs/DECISIONS.md`, it fixed letters arriving out of order on a real phone.
- `AbbaNavHost` now shows `LibraryRoute`; the Library placeholder and its sample-id navigation are gone.
- Twelve Library strings added to `strings.xml`, three of them plurals.
- **Tests**: 72 passing (10 new). `LibraryViewModelTest` covers the unnarrowed shelf, tile counts
  staying with the whole catalogue, search over title / author / line, search and chips narrowing
  together, two chips of one kind widening rather than emptying, a chip tapped twice, `ClearNarrowing`,
  an empty result, the query and chips surviving a rotation (a second ViewModel over the same handle),
  and an open being stamped.
- Verified: `:app:assembleDebug` and `:app:testDebugUnitTest` pass, and on a Galaxy Note 10+ — the
  screen matches `docs/DESIGN_SYSTEM.md`, "mercy" typed into the field survives two rotations and
  narrows to nine prayers, the scroll offset and the query both come back after a tab switch, "Clear"
  empties the field, and a row reaches the Reader with `prayerId = psalm-023`.

## Task 5 — done and merged

The first real screen. `Home` is no longer a placeholder.

- `feature/home/HomeUiState.kt` — `HomeUiState` (greeting, verse, today's prayer, recent rows,
  `isCatalogueReady`) and `HomeAction` (`ReadPrayer`, `BeginSession`).
- `feature/home/HomeViewModel.kt` — combines `GetTodaysSuggestedPrayerUseCase` with
  `observeRecentlyOpenedPrayers`, reads `GetGreetingUseCase` and `GetTodaysVerseUseCase` on each
  emission, and stamps `lastOpenedAt` through `RecordPrayerOpenedUseCase` on either action. The
  initial value already carries the greeting and the verse, so the header never flashes empty.
- `feature/home/HomeScreen.kt` — `HomeScreen` (stateless, previewed) and `HomeRoute` (binds the
  ViewModel, then navigates). The sage-tinted header field rounded 40 at the bottom, brand eyebrow,
  42sp greeting, italic verse and its reference; then "Today's prayer" as a 30-radius card with the
  title, attribution, opening line, a 64h sage "Begin prayer" and a "Read it first" text action;
  then "Recently prayed" as up to four 22-radius rows.
- **New in domain**: `DailyVerse` + `DailyVerses` (31 AV lines) and `GetTodaysVerseUseCase`, picked by
  the date exactly as today's prayer is. See `docs/DECISIONS.md` for why they are not in Room.
- `AbbaNavHost` now shows `HomeRoute`; the Home placeholder and its sample-id navigation are gone.
  The other placeholders stay until their tasks.
- Nine home strings added to `strings.xml`, including the four greeting wordings.
- **Tests**: 62 passing (12 new). `HomeViewModelTest` over `MainDispatcherRule` (new, in `testing/`)
  covers the header arriving before the catalogue, today's prayer holding still while the screen is
  watched, an open stamping the prayer and lifting it into the rows, recent order, today's prayer
  never repeating itself in the rows, and the four-row cap. `GetTodaysVerseUseCaseTest` pins the
  same-day/next-day/far-past behaviour.
- Verified: `:app:assembleDebug` and `:app:testDebugUnitTest` pass, and on the API 35 emulator — the
  header matches `docs/DESIGN_SYSTEM.md` and paints under the status bar, the verse and the suggestion
  are unchanged across a rotation, "Read it first" reaches the Reader and "Begin prayer" the Session,
  both with `prayerId = calvin-grant-almighty-god`.

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

### Task 9 — `feat/09-reader-metadata`

```
git switch -c feat/09-reader-metadata main
```

The quiet surfaces for what the corpus carries, all on tap, none of it in the way of praying:

- **Scripture sheet** — a small text action at the end of each movement ("Three passages") opens a
  `ModalBottomSheet` built like the keep-a-line sheet in `feature/reader/ReaderScreen.kt`: the
  movement's heading, its theological themes, then each reference set as `Isaiah 57:15 · ESV` with
  its connection paragraph beneath.
- **Provenance sheet** — an "About this prayer" action beside the byline: the adapted title against
  the original, Arthur Bennett and Banner of Truth 1975, the copyright line, and the adaptation note
  verbatim.
- **Tag picker** — the keep-a-line sheet shows the prayer's own tags ticked plus a "More tags" action
  revealing the other 48.
- Movement headings get their final voice; both new sheets keep their state in the `SavedStateHandle`
  keys `ReaderViewModel` already owns, so they survive a rotation as the keep sheet does.

**Done when** `:app:assembleDebug` and `:app:testDebugUnitTest` pass, both sheets match
`docs/DESIGN_SYSTEM.md`, each survives a rotation, and nothing new appears on the reading surface
until it is asked for.

### Task 10 — `feat/10-prayer-session`

```
git switch -c feat/10-prayer-session main
```

The session, replacing its placeholder: the full-bleed deep-forest screen, the prayer one line at a
time at 30/1.42 with earlier lines fading to `oat @ .38`, a breathing pause at **every** movement
boundary (`breathingPauseLineIndices`) naming the movement being entered, moss progress ticks
counting **movements** rather than lines — 29 ticks would be noise — the blurred sage orb breathing on the `glow`
timing, the translucent controls and the oat "Amen". `SessionUiState`, `SessionAction`,
`SessionViewModel`, `SessionScreen`, `SessionRoute` per the UI pattern in `CLAUDE.md`; the current
line index lives in `SavedStateHandle`. Pacing comes from `SettingsRepository`
(`PrayerSettings.SessionPacing`). The screen owns its system bars: keep-screen-on and **light bar
icons on the dark ground**, both released in `DisposableEffect.onDispose` — this is the fix task 4
noted for its dark-on-dark status bar.

**Done when** `:app:assembleDebug` and `:app:testDebugUnitTest` pass, the session matches
`docs/DESIGN_SYSTEM.md`, the line index survives a rotation, the screen stays awake while it is open
and stops when it is left, the status-bar icons are light on the forest and dark again afterwards,
and "Amen" pops back to the screen the session was opened from.

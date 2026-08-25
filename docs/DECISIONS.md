# Decisions

Short, dated records of choices that are not obvious from the code.

## 2026-08-25 — Single module, layered packages
Clean architecture is expressed as package layering inside `:app` rather than Gradle modules. The app is
one product surface built screen by screen; per-module Gradle wiring would cost more per task than the
compiler-enforced boundaries would return. The package map is designed to lift into modules unchanged
if build times ever justify it.

## 2026-08-25 — Hilt for DI
Standard Android choice, compile-time verified, and `hiltViewModel()` integrates directly with the
type-safe navigation routes. Verified working with AGP 9 + KSP.

## 2026-08-25 — compileSdk 37, targetSdk 36
The template did not build as delivered: `androidx.core:core-ktx:1.19.0` and
`lifecycle-runtime-compose:2.11.0` require compiling against API 37. `compileSdk` was raised to the
installed `android-37.0`. `targetSdk` deliberately stays at 36 — raising it opts into new runtime
behaviour that nothing here needs yet.

## 2026-08-25 — AGP 9 built-in Kotlin, `android.disallowKotlinSourceSets=false`
AGP 9 registers its own `kotlin` extension, so `org.jetbrains.kotlin.android` cannot be applied
(it fails with "Cannot add extension with name 'kotlin'"). KSP 2.2.10-2.0.2 registers its generated
sources through `kotlin.sourceSets`, which built-in Kotlin rejects by default. AGP's own documented
flag reconciles the two. Room and Hilt code generation and Room schema export were all verified against
this configuration before any feature code was written.

## 2026-08-25 — Local-only, sync-shaped schema
Room is the single source of truth with no network layer. User-owned entities still carry UUID ids,
`createdAt`/`updatedAt` and `isDeleted`, so adding sync later is an additive change rather than a
schema rewrite.

## 2026-08-25 — Fakes over mocks
Test doubles are hand-written fakes of the domain repository interfaces. No mocking framework is a
dependency; the interfaces are small enough that fakes are clearer and survive refactors better.

## 2026-08-25 — Variable fonts, bundled rather than downloadable
Cormorant Garamond and Work Sans ship as single variable TTFs in `res/font` with weights requested as
`FontVariation` axis positions. One file per family instead of seven static weights, and the app
renders identically with no Play Services font provider and no network. Axis ranges were checked
before choosing weights: Cormorant Garamond is 300..700, Work Sans 100..900. The OFL licence text for
both families is in `assets/licenses/` for a future About screen.

## 2026-08-25 — Colour shift instead of ripple
Every tappable surface uses `Modifier.pressableSurface`, which animates the container colour and
passes `indication = null`. The design expresses press state as a warmer field (`card` → `cardPressed`,
`sage` → `sagePressed`); a Material ripple on top of that would read as a second, louder answer to the
same tap.

## 2026-08-25 — Type named by voice, not by Material slot
`AbbaTypeScale` names styles `prayerLine`, `homeVerse`, `sectionLabel` and so on, because the design's
scale is driven by who is speaking rather than by a heading hierarchy. A Material `Typography` is still
derived from it so Material components inherit the right faces.

## 2026-08-25 — Kind and theme are different questions
`PrayerKind` is the occasion a prayer is reached for (Morning, Evening, Confession, Psalm…);
`PrayerTheme` is what it is about (Peace, Grief, Mercy…). One prayer commonly answers both
differently — Psalm 51 is a `Confession` by kind and carries `Confession`, `Mercy` and `Healing` as
themes — and the Library browses by the first while the chips filter by the second. Collapsing them
into one list would have made every prayer's tags read as a contradiction.

## 2026-08-25 — Catalogue themes in a table, reader themes in a column
`prayer_themes` is a real table with an index, because the Library filters the catalogue on it and
filtering belongs in the query. Themes on saved lines and personal prayers are a converted
comma-joined column instead: they are displayed, and narrowed over a list small enough that the
reader's own device can do it in memory. Two shapes for the same concept is deliberate — the third
and fourth join tables would have bought nothing.

## 2026-08-25 — Seeding guarded twice
`CatalogueSeeder` runs from `RoomDatabase.Callback.onCreate`, which fires once per database file, and
still checks `countPrayers() == 0` before writing. The insert is one `@Transaction`, so a seed that
fails halfway leaves an empty catalogue that can seed again rather than a partial one that looks
finished. It only ever writes the three catalogue tables, so no guard failure could reach anything
the reader owns. There is no `fallbackToDestructiveMigration` anywhere.

## 2026-08-25 — Clock and id generation are injected
`java.time.Clock` and a small `IdGenerator` are provided by `TimeModule` and taken as constructor
parameters by the use cases that stamp `createdAt`/`updatedAt` or mint a UUID. Tests fix the clock
with `Clock.fixed` and hand out `id-1`, `id-2`, so "the kept line records when it was kept" is an
assertion rather than a tolerance window.

## 2026-08-25 — Today's suggestion is a function of the date
`GetTodaysSuggestedPrayerUseCase` indexes the catalogue by `date.toEpochDay()` times a large
odd constant. Nothing is stored, and the answer holds still across a rotation, a process death or a
reinstall — a suggestion that changed while the reader was looking at it would read as restlessness
in an app whose whole point is quiet.

## 2026-08-25 — "A Prayer in Distress" is *after* Bunyan
The design names this prayer among its seven and the design file was not available to this session.
No verbatim Bunyan prayer of that title could be confirmed, so the lines are written in the voice of
his published work and the prayer is attributed **"After John Bunyan"** rather than to him. If the
design source turns up with the original text, replace the lines in `prayer_catalogue.json` and
restore the plain attribution. Every other prayer in the catalogue is a public-domain text carried
verbatim (KJV, BCP 1662, Luther's Small Catechism) or a public-domain translation.

## 2026-08-25 — Only the selected tab names itself
The design specifies a pill bottom bar with a sage active pill, an 11sp uppercase nav label and a
54h pill, but four labels on the bar at once would have crowded a 411dp width and made the bar the
loudest thing on an otherwise quiet screen. `AbbaBottomBar` therefore draws the three unselected tabs
as their icon alone and gives the label only to the selected pill, which is what the sage pill is
sized for. The unselected icons carry the label as their content description, so nothing is lost to a
screen reader.

## 2026-08-25 — The graph is a function, not a lambda buried in the host
`abbaDestinations(navController)` is a `NavGraphBuilder` extension rather than an anonymous builder
inside `AbbaNavHost`, so `AbbaNavGraphTest` can build the same graph against a `TestNavHostController`
and assert reachability, argument round-tripping and tab back-stack behaviour without standing up a
composition. `androidx.navigation:navigation-testing` was added for that alone.

## 2026-08-25 — The shell hands the whole window to the nav host
`MainActivity` applies no inset padding and the `Scaffold` in `AbbaNavHost` sets
`contentWindowInsets = WindowInsets(0.dp)`; each screen pads itself with `safeDrawing`. That is what
lets the prayer session paint the deep forest edge to edge and own the system bars, and it means no
screen has to know whether it is being shown with a bottom bar. The session's status-bar *appearance*
— light icons over the forest — belongs to task 8, which owns that screen's `DisposableEffect`.

## 2026-08-25 — The daily verse lives in the domain, not in Room
`DailyVerses` is a plain Kotlin list beside `GetTodaysVerseUseCase`. Room is the source of truth for
content the reader can change or that the catalogue owns; the home verse is neither — it is thirty-one
fixed lines of the Authorized Version the app ships with and nobody edits. Putting them in a table
would have meant a schema version bump and a migration for text that cannot drift. It is picked by
`date.toEpochDay()` the same way today's prayer is, so the header holds still across a rotation.

## 2026-08-25 — Home records the open, the header takes the status bar
Both home actions — "Begin prayer" and "Read it first" — go through `HomeViewModel.onAction`, which
stamps `lastOpenedAt` before the navigator moves. The recent rows are therefore the prayers the reader
actually reached for, whichever way they reached. Today's prayer is filtered out of those rows: it
already has a card of its own directly above them, so a row repeating it would read as noise.

The home header is the one screen element that paints under the status bar: the root column takes the
horizontal and bottom `safeDrawing` insets and the header takes the top inset *inside* its own tinted
field, so the sage tint reaches the top of the display instead of stopping at a bar-height strip of
oat. Every other screen keeps padding itself whole.

## 2026-08-25 — The system bars are told the app is light, not asked
`enableEdgeToEdge()` with no arguments uses `SystemBarStyle.auto`, which follows the *system's*
dark-theme setting — so on a phone set to dark it painted white status-bar icons over the oat ground
and the sage home header, illegible on both. `MainActivity` now passes
`SystemBarStyle.light(TRANSPARENT, TRANSPARENT)` for both bars. `AbbaTheme` has one committed light
look and no dark variant, so the bars should not be reading a setting the rest of the app ignores.
Found on a Galaxy Note 10+ (API 31) in dark mode; the emulator defaults to light and never showed it.
The prayer session inverts this for its own screen in task 8.

## 2026-08-25 — "Collections" on the Library are the catalogue's groups
The design's collection tiles show `PrayerGroup` — Book of Common Prayer, Psalms, Puritan, Celtic —
not reader-made `PrayerCollection`s. Nothing in the app creates a collection yet, so tiles over
`observeCollections()` would have been an empty shelf on every device, and the group is the honest
answer to "where is this prayer from". The four-colour tile cycle (card, `sageTint`, card, `clay`)
carries on past the fourth tile so no two neighbours share a colour. Reader-made collections, when
they arrive, get their own block rather than displacing this one.

## 2026-08-25 — The search field owns its text, the ViewModel hears about it
`LibraryViewModel` keeps the query in `SavedStateHandle`, so it survives a rotation and process
death. Driving `BasicTextField(value =, onValueChange =)` from that `StateFlow` proved to be a real
bug, not a theoretical one: on a Galaxy Note 10+ typing "mercy" into the field produced "meyyc",
because every keystroke round-trips through the handle and comes back a frame late. The field now
holds a `TextFieldState` and edits at once, reports to the ViewModel afterwards, and takes the query
back only when it differs — which is what makes "Clear" empty the field. Any future text field on
this app follows the same shape.

## 2026-08-25 — One narrowing, one state
`SearchPrayersUseCase` and `FilterPrayersUseCase` each read the catalogue, so combining them flatly
beside the query emitted half-updated frames — the new query beside the old shelf. The query and the
chips are folded into one `LibraryNarrowing` first, and the catalogue is read inside a
`flatMapLatest` over it, so one change of either produces exactly one `LibraryUiState`. The tile
counts are taken from the whole catalogue rather than the narrowed list, so a count never moves under
the finger that is narrowing with it.

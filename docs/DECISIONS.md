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

## 2026-08-25 — The keep-a-line sheet says three things, not three sheets
The design names a "three-stage" sheet without naming the stages. They are `Keep` (a line just
tapped: tag it and keep it), `Kept` (this moment kept — grow it or be done) and `AlreadyKept` (kept
on an earlier reading — grow it, or let it go). Tapping a line the reader kept last week therefore
never offers to keep it again. All three share the handle and the line at the top of the same sheet,
so it reads as one place the reader stays in rather than three fields replacing each other.

A kept line keeps its `sageTint` field in the reader permanently, which is the same token the design
gives the selected line. That is deliberate: what the tint means is "this line is yours", and the
open line is the line the sheet is about to make yours.

## 2026-08-25 — The Reader reads its argument by key, not by route type
`ReaderViewModel` takes `prayerId` out of `SavedStateHandle` by the name the argument is declared
with on `AbbaRoute.Reader`, rather than calling `toRoute<AbbaRoute.Reader>()`. `navigation/` already
depends on `feature/`; having the feature reach back for the route type would close that loop, and
`CLAUDE.md` keeps features pointed at `domain/` and `core/` alone. The graph test already pins that
the argument round-trips under that name.

## 2026-08-25 — One-shot navigation arrives as an event
Every other screen so far navigates straight from the action, because the destination is known when
the tap happens. "Make it my prayer" is not: `CreatePersonalPrayerFromLineUseCase` mints the draft
first, and only then is there a `personalPrayerId` to open. `ReaderViewModel` therefore exposes a
`Channel`-backed `events: Flow<ReaderEvent>` beside its `StateFlow`, collected once in `ReaderRoute`.
State stays state; a move that happens once stays a one-shot. Any later screen with the same shape
follows this rather than smuggling a navigation request into `UiState`.

## 2026-08-25 — The document margin is per block, not on the root
The Reader pads its sides block by block rather than once on the root column, because a prayer line's
tinted hit area has to reach 12dp past the text into the margin while the text itself stays on the
26dp document margin. Compose has no negative padding, so the body column is padded by the difference
(14dp) and each line adds the remaining 12dp inside its own field.

## 2026-08-26 — The catalogue is the Valley of Vision, and `docs/prayers/` is its source of truth
The 31 sample prayers seeded in task 3 were scaffolding. The real catalogue is 194 modern adaptations
of *The Valley of Vision* (ed. Arthur Bennett, Banner of Truth 1975), one JSON file per prayer under
`docs/prayers/`, committed as generated. `tools/build_catalogue.py` is the only thing that turns them
into `app/src/main/assets/prayer_catalogue.json`, and that asset is committed too — so the line
splitting is reviewable in a diff, identical on every machine, and never a surprise at runtime.

The generator validates rather than trusts: an unknown tag, a missing heading, a movement with no
scripture, a duplicate id or title fails the build. `CatalogueSeeder` now parses with
`ignoreUnknownKeys = false` for the same reason — the asset and the parser are built from the same
corpus, so a key one of them does not know means they have drifted apart.

## 2026-08-26 — A prayer is movements; a line is still the address
The corpus gives paragraphs, not lines: five or six *movements*, each a heading and about seventy
words. The session needs one thought per screen, so the generator splits each movement at its
sentence enders and em dashes — 5,466 lines across the catalogue, a median of thirteen words each.
Those splits are computed once, at build time, and stored as rows.

That matters because a **flat line index is the address the app is built on**: it is what a session
advances through, what `SaveLineUseCase` records, and what a kept line points back to for as long as
the reader keeps it. Deriving the split at runtime would have let a change to the splitting rule
silently move every kept line in the Saved screen. `Prayer.lines` therefore stays flat, and
`movements` sits beside it carrying the headings, the theological themes and the scripture.

`breathingPauseAfterLine` is gone. A prayer rests at the end of every movement but the last, so
`breathingPauseLineIndices` is a set and the session gets four to seven pauses instead of one.

## 2026-08-26 — Eight prayers are held back, and the asset is what holds them
Eleven of the 194 have been through review: three passed, eight were marked "revise". The generator
excludes the eight, so 186 ship. The other 183 carry no verdict yet and ship as they are — review is
a content workflow that runs alongside the app rather than a gate the app enforces.

One consequence worth knowing: the eight include "The Valley of Vision" itself, which is the only
prayer in the *Introductory* part, so that part currently has no tile in the Library. It returns when
the prayer does.

## 2026-08-26 — Scripture is a reference, never the verse
Every one of the 2,606 passages is carried as a reference and a translation name ("Isaiah 57:15",
"ESV") plus our own prose explaining why it stands under that movement. **No verse text is stored or
shipped.** That keeps the app clear of the ESV's licence terms, and it is also the right shape: the
Bible is the reader's own, and the app points at it rather than reprinting it. A later session must
not "helpfully" add verse text to the corpus or the asset.

## 2026-08-26 — Parts and tags replace kind, group and theme
`PrayerKind` and `PrayerGroup` are deleted and `PrayerTheme` becomes `PrayerTag`, because the closed
enums the sample catalogue used describe nothing in this corpus. What the corpus does have is 48 tags
and 11 parts, and the Library browses by exactly those. The tag vocabulary stays a closed enum: the
generator checks the corpus against it, so a new tag is a decision someone makes rather than a typo
that ships.

`SavedLine` and `PersonalPrayer` retype to `PrayerTag` and their column is renamed rather than
rewritten. A tag name from the old vocabulary stays in the column and is dropped on read by the
lenient converter — a kept line keeps its text, its source and its note whatever happens to its tags.

## 2026-08-26 — The migration replaces the catalogue; the seeder refills it
`MIGRATION_1_2` drops and recreates the catalogue tables rather than migrating them row by row: every
id changed, and a catalogue prayer is derived content that the asset can always produce again.
Nothing the reader owns is touched.

That exposed a gap: `CatalogueSeeder` only ran from `onCreate`, so a migrated database would have
opened with an empty catalogue forever. The callback now seeds from `onOpen` as well, and the rule is
"an empty catalogue fills itself" — which is what the twice-guarded design was always reaching for.
Proved on a real upgrade, not only in a test: a device holding a v1 database with a kept line took
the new build in place, kept the line whole, and came back with 186 prayers.

## 2026-08-26 — One adapted title corrected in the open
`vov-170` is the *eve* of the Lord's Day — a week ending — and `vov-176` is Sunday evening itself.
Adaptation gave both the title "Lord's Day Evening", which is wrong for the first and would have put
two identical rows in the Library. The corpus file stays as generated; the correction lives in
`TITLE_OVERRIDES` in the generator, where it can be seen and undone when the corpus is regenerated.

## 2026-08-26 — Schemas are debug assets so a migration can be tested
`MigrationTestHelper` reads the exported schemas as assets, and Robolectric reads the assets of the
variant under test — not the unit-test source set. So `app/schemas` is added to the **debug** source
set alone: the migration test can find them, and they never travel in a release build.

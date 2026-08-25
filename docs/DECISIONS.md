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

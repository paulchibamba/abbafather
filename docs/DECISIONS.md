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

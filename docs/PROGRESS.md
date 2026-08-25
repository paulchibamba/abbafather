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
| 1 | Project foundation — git, build wiring, Hilt root, docs | `chore/01-project-foundation` | **awaiting approval** |
| 2 | Design system — fonts, colours, type, shared components | `feat/02-design-system` | not started |
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

## Task 1 — done in this branch

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

### Task 2 — `feat/02-design-system`

```
git switch -c feat/02-design-system main
```

Build the visual language from `docs/DESIGN_SYSTEM.md`. Nothing is wired to real data yet.

**Files to create**
- `app/src/main/res/font/` — Cormorant Garamond 300/400/500 and Work Sans 300/400/500/600 OFL TTFs,
  downloaded from Google Fonts and committed (bundled, not downloadable fonts).
- `core/designsystem/theme/AbbaColors.kt` — the token table, exposed both as a Material 3 `ColorScheme`
  and as a `LocalAbbaColors` CompositionLocal for tokens Material has no slot for.
- `core/designsystem/theme/AbbaTypography.kt` — the type scale, named by role (`prayerLine`,
  `screenTitle`, `sectionLabel`, `metaSans`…), not by Material slot alone.
- `core/designsystem/theme/AbbaTheme.kt` — replaces the template's purple `AbbaFatherTheme`; light-only,
  no dynamic colour (the design is one committed look).
- `core/designsystem/component/` — `SoftCard`, `PillButton`, `SectionLabel`, `SelectableChip`,
  `TagChip`, `RoundIconButton`, `AbbaIcons`.
- A `@Preview` gallery plus a temporary `MainActivity` entry point so it can be seen on device.

**Also delete** the template's `ui/theme/{Color,Theme,Type}.kt` and the `Greeting` composable once the
new theme is in place.

**Done when** the gallery renders every colour token, every type role and every component variant, it
matches `docs/DESIGN_SYSTEM.md`, and `./gradlew :app:assembleDebug` passes.

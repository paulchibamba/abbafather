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
| 9 | Reader metadata — scripture and provenance sheets | `feat/09-reader-metadata` | merged, `task-09` |
| 10 | Prayer session — movement-aware | `feat/10-prayer-session` | merged, `task-10` |
| 11 | Saved screen | `feat/11-saved-screen` | merged, `task-11` |
| 12 | My prayers screen | `feat/12-my-prayers-screen` | merged, `task-12` |
| 13 | Compose prayer screen | `feat/13-compose-prayer-screen` | merged, `task-13` |
| 14 | Polish and hardening — incl. About and attribution | `feat/14-polish-and-hardening` | merged, `task-14` |
| 15 | Session lyric flow — the whole prayer, prayed straight through | `feat/15-session-lyric-flow` | merged, `task-15` |
| 16 | Scripture text — YouVersion SDK, reader's chosen version | `feat/16-scripture-text` | not started |

**The board was renumbered on 2026-08-26**, when `docs/prayers/` arrived with the real catalogue. The
old tasks 8–12 became 10–14; tasks 8 and 9 are new. See `docs/DECISIONS.md` for why.

Each task is built on its own branch, verified, then **stopped for approval**. Only after approval:
`git switch main && git merge --no-ff <branch> && git tag task-<nn>`.

## Task 15 — done and merged

The session became one lyric column, prayed straight through: the whole prayer at once, the line
being prayed held on the centre of the screen, and no rests between movements.

- `feature/session/SessionUiState.kt` — `movementLines` and `breathingPause` gone; `lines:
  List<String>` and `activeLineIndex` in their place. **Activeness is not on the line**: put it there
  and every advance builds a structurally different list, and everything keyed on it thrashes. See
  `docs/DECISIONS.md`.
- `feature/session/SessionScreen.kt` — `AnimatedContent`, `PrayedLines` and the bottom-anchored
  `verticalScroll` are gone. A `LazyColumn` inside `BoxWithConstraints` with half a viewport of
  `contentPadding` at each end, so the first and last lines can reach the centre like every other
  one; `centreOn` measures the line and scrolls its middle onto the middle of the screen. The first
  centring is not animated — the session opens already centred rather than gliding up from nowhere.
- **The fade ladder** by distance from the active line — full `oat`, .38, .22, .12 — symmetric
  behind and ahead, animated with `animateColorAsState(tween(700))`. The column fades into the
  ground at both ends over 52dp, or it slides under the movement ticks and out from behind the
  Continue button with a hard cut. Found on the device, not in the plan.
- **The breathing pause is gone from the session.** Once the whole prayer is visible, stopping
  between movements reads as an interruption of something continuous. The movement boundaries are
  still real and the reader screen still marks them. That collapsed the step abstraction with it:
  the position is a line index into `Prayer.lines` again, so `SessionStep`, `Prayer.sessionSteps`
  and the `session_breathe` / `session_movement_position` strings are deleted, and
  `SavedStateHandle` carries `sessionLineIndex`.
- **The progress bar empties, from the left.** One unbroken line across the header rather than one
  tick per movement, whole at the first line and empty at the last, with what is still ahead staying
  ahead on the page. The remaining stretch carries a slow wave in the manner of Material 3's wavy
  indicator — drawn on a `Canvas` rather than imported, because that component ships in a
  `material3` 1.5.0 alpha and nothing else in this app is Material-shaped. See `docs/DECISIONS.md`.
- **Free scrolling stays on** and the next advance re-centres. No snap-back timer.
- **The keys that would have broken it**: the pacing dwell timer and the scroll both keyed on
  `(movementLines, breathingPause)`, and neither changes per advance now. Both key on
  `activeLineIndex`.
- Untouched: the header, the controls, the glow, the system bars, and the ViewModel's
  `SavedStateHandle` behaviour.
- `session_being_prayed` added to `strings.xml` — the ladder says nothing out loud, so the active
  line carries a `stateDescription`.
- **Tests**: 143 passing. The four that asserted the movement-bounded behaviour are rewritten — the
  session opens on the whole prayer at index 0; an advance leaves `lines` *equal* and moves only the
  index; the prayer runs straight through its movements without resting; and a movement fills as it
  is prayed and stays full once it is behind. The three that existed only to assert the rest are
  gone.
- Verified: `:app:assembleDebug` and `:app:testDebugUnitTest` pass, and on a Galaxy Note 10+ — "The
  Great Discovery" (five movements, about thirty lines) opens already centred with the first tick a
  seventh full; advancing scrolls one line smoothly to centre with the ladder resolving around it;
  **the last line of a movement is followed straight by the first line of the next, with no rest**,
  and at that handover the finished tick fills, drops back to `moss @ .55`, and the next one starts
  filling; the fifth movement's tick fills line by line to the end, where the last line sits centred
  and the button turns oat for "Amen", which leaves the session; dragging back to reread and then
  advancing re-centres; **Unhurried still advances on its own every twelve seconds**, which is the
  `LaunchedEffect` regression this task could most easily have shipped; and a rotation into landscape
  holds the active line centred in a much shorter stage. Pacing was put back to "At my own pace"
  afterwards.

## Task 14 — done and merged

The last task: the settings and the notices given a page, and a pass over what the twelve screens
before it left rough.

- `feature/about/AboutUiState.kt` — `AboutUiState` (the two settings, the licences, the version),
  `FontLicenceUiState`, and `AboutAction` (`ChoosePacing`, `SetKeepsScreenOn`, `ToggleLicence`,
  `Back`).
- `feature/about/AboutViewModel.kt` — the settings flow combined with the notices, which are read
  from the assets **once**: a flow that emits a single list, so a rotation does not go back to disk
  for four thousand words that cannot have changed. Which licence is open lives in `SavedStateHandle`.
- `feature/about/AboutScreen.kt` — what the reader can change first, then what the app owes: the
  three pacings as choice chips with the chosen one's own sentence under them, the keep-screen-awake
  card whose whole surface is the switch, where the prayers come from, what Scripture is and is not
  carried, and the two faces with their notices folded away behind a line of text.
- `domain/model/FontLicence.kt` + `domain/repository/LicenceRepository.kt`, implemented by
  `data/repository/AssetLicenceRepository.kt`, which reads `assets/licenses/*.txt` and takes each
  copyright line out of the notice itself rather than restating it. `core/common/AppInfo.kt` and
  `di/AppInfoModule.kt` carry the version name so the ViewModel never holds a `Context`.
- `AbbaRoute.About`, reached from a tracked-out "ABOUT" on the Home header's brand line — the one
  place already reserved for the app speaking about itself. See `docs/DECISIONS.md`.
- **Ambient sound is deliberately not offered**: the domain models it and nothing plays anything.
- The seeding callback moved out of `DatabaseModule` into `data/local/seed/CatalogueSeedingCallback.kt`,
  so the cold-install path is a thing a test can open a database with rather than an anonymous object
  inside a Hilt provider.
- **Dynamic type**: `PillButton` and the Library's search field size themselves with `heightIn`
  rather than `height`, so at 200% text the labels grow instead of being cropped.
- **Being read the screen**: `pressableSurface` gained an `onClickLabel`; the bottom bar's tab and
  the Library's shelf tiles carry `selected`; a kept reader line carries a state description; the
  chips say whether they are on; and the arrow on a prayer row is decorative now that the row itself
  says what tapping it does.
- `hiltViewModel()` now comes from `androidx.hilt.lifecycle.viewmodel.compose` — the module compiles
  without deprecation warnings.
- **Tests**: 145 passing (11 new). `AboutViewModelTest` covers the page opening on the settings as
  they stand with both notices closed, every pacing the domain offers being on it, choosing a pacing
  and toggling the screen-awake setting writing through and coming back changed, one licence being
  open at a time, an open licence surviving a rotation, and the notices being read once rather than
  on every change. `AssetLicenceRepositoryTest` reads the real bundled notices. `ColdInstallSeedingTest`
  opens an empty database with the app's own callback attached and waits for the catalogue to arrive
  through the flow the Library reads. `AbbaNavGraphTest` covers About being reachable and popping
  back to Home.
- Verified: `:app:assembleDebug` and `:app:testDebugUnitTest` pass, and on a Galaxy Note 10+ holding
  a real database — "ABOUT" sits on the Home header's brand line without disturbing it; the page
  opens on the settings as they stand; choosing "Unhurried" changes the sentence under the chips and
  turning the screen-awake card off changes its pill; both survive a force-stop and a relaunch; a
  licence opens under its card and closes again; and a session begun afterwards **advanced a line on
  its own** without a tap, which is the pacing being obeyed. The settings were put back to their
  defaults afterwards. **Not checked on hardware**: a cold install with no database — that would mean
  clearing the app's data and this phone holds prayers that were written on it. `ColdInstallSeedingTest`
  covers that path instead, against a real empty database with the app's own callback attached.

## Task 13 — done and merged

Writing a prayer, replacing the last placeholder. The page is the reader's own words on the oat
ground: no boxes, no labels, nothing between them and what they are saying.

- `feature/composeprayer/ComposePrayerUiState.kt` — `ComposePrayerUiState` (`tagChips`,
  `canShowMoreTags`, `canKeep`, `isLoaded`), the separate `ComposePrayerDraft` (title and body),
  `ComposePrayerAction` (`TitleChanged`, `BodyChanged`, `ToggleTag`, `ShowMoreTags`, `KeepPrayer`,
  `Back`) and `ComposePrayerEvent.Kept`.
- **The words are not screen state.** The draft lives in `SavedStateHandle`, because this is the one
  screen whose loss would cost the reader their own words — but it reaches the page once as
  `openingDraft`, and the state carries only the tags and a `canKeep` boolean. A `StateFlow` does not
  re-emit an equal value, so a body that grows by a letter redraws nothing. See `docs/DECISIONS.md`.
- `feature/composeprayer/ComposePrayerViewModel.kt` — opened three ways: blank, on an existing
  `personalPrayerId`, or on the `seedText` of a kept line. The stored row is read exactly once,
  guarded by a `composeDraftLoaded` key, so a draft restored after process death is never overwritten
  by what Room still holds; a blank page and a seeded one need nothing from Room and open at once.
  Keeping writes an existing prayer back to its own row, keeping the day it was created, and mints a
  new one otherwise — reading the draft from the handle rather than from `uiState`, which only holds
  a value while the screen is collecting it.
- `feature/composeprayer/ComposePrayerScreen.kt` — `ComposePrayerScreen` (stateless, previewed
  written and blank) and `ComposePrayerRoute`. The 44h round back button, the borderless 32/1.2 title
  field over the 21/1.6 body field, "TAG IT", the 46h keep button — sage once there is something to
  keep, quiet card before that — and a line saying where it goes. `imePadding` keeps the button above
  the keyboard.
- The picker shows only the ticked tags until "More tags" widens it to all 48 **in the vocabulary's
  own order**: ticking one must not move the next one out from under the finger. See
  `docs/DECISIONS.md`.
- `AbbaNavHost` now shows `ComposePrayerRoute`, and **`navigation/PlaceholderScreen.kt` is gone** —
  every destination in the app is now a real screen.
- Seven compose strings added to `strings.xml`.
- **Tests**: 134 passing (9 new). `ComposePrayerViewModelTest` covers a blank page opening on nothing
  with nothing to keep, a kept line opening on the line with room after it, a prayer already written
  opening on what was written, keeping a new prayer writing it whole with the name tidied and the
  body left exactly as typed, keeping an existing one saving back to the same row with its created
  date intact, a name on its own not being a prayer, the draft surviving a rotation and never being
  overwritten by the stored row, the picker widening to the whole vocabulary in its own order, and an
  unkept draft leaving nothing behind.
- Verified: `:app:assembleDebug` and `:app:testDebugUnitTest` pass, and on a Pixel 8 emulator (API
  33) — the blank page opens on its two placeholders with the keep button quiet, both fields take
  text, the keep button turns sage on the first word, the draft survives a rotation into landscape
  and back, and it survives a real process death (`am kill`, resumed from recents) with both fields
  whole. Then on a Galaxy Note 10+ holding a real database — the prayer grown from a kept line in
  task 11 opens on its own title, body and both its tags ticked; leaving it without keeping writes
  nothing, and the card on My prayers is untouched; the plus opens a blank page that takes a title
  and a body; and in the widened picker two chips ticked exactly where they were aimed, which is the
  reordering bug the emulator found. **Still unchecked on hardware**: the "Keep prayer" tap itself
  and the row it writes — the phone dropped off wireless adb one tap short, twice. (Its `/data` was
  also full; `pm trim-caches` freed 3.2 GB, which is what let the build install at all.)

## Task 12 — done and merged

The prayers the reader wrote, replacing the `My prayers` placeholder. The shelf that is theirs
alone: nothing on it came from the catalogue except by way of a line they kept.

- `feature/myprayers/MyPrayersUiState.kt` — `MyPrayersUiState` (`prayers`, `isLoaded`, with
  `isEmpty` true only once the prayers have arrived), `MyPrayerCardUiState` (title, excerpt,
  `lastTouchedAt`, `isAwaitingDeleteConfirmation`, and `hasTitle` for the draft not yet named) and
  `MyPrayersAction` (`OpenPrayer`, `WriteNewPrayer`, `AskToDelete`, `ConfirmDelete`, `CancelDelete`).
- `feature/myprayers/MyPrayersViewModel.kt` — the only state it owns is which card, if any, is
  asking whether the reader means it, and that lives in `SavedStateHandle` so a rotation taken
  mid-question comes back to the question. Everything else is `PersonalPrayerRepository`, which
  already orders by `updatedAt`, so the prayer touched last is the one at the top. Opening a written
  prayer records nothing: its date is the date the reader last changed it, not the date they looked.
- `feature/myprayers/MyPrayersScreen.kt` — `MyPrayersScreen` (stateless, previewed full and empty)
  and `MyPrayersRoute`. The 38sp title and the count on the left with the sage 52dp add button on
  the right, then the 28-radius cards: the title at 23/1.2 — or a placeholder-grey "Untitled" for a
  draft still without a name — the opening line at 17/1.6 clipped at two lines, "Last touched
  25 Aug 2026", and one quiet text action.
- **Deleting takes two taps.** "Delete" only asks; the card itself turns into "Delete this prayer?"
  with "Keep it" in sage and "Yes, delete" quiet beside it. This app holds prayers people wrote, so
  nothing they wrote goes on one tap — and the question is asked on the card rather than in a dialog,
  because the design has none.
- `AbbaNavHost` now shows `MyPrayersRoute`; the card opens Compose on its `personalPrayerId` and the
  add button opens it blank. `PlaceholderScreen` survives one more task, for Compose alone.
- Ten My-prayers strings and one plural added to `strings.xml`.
- **Tests**: 125 passing (9 new). `MyPrayersViewModelTest` covers a written prayer reading back with
  its excerpt and the day it was last touched, the most recently touched at the top, a prayer not yet
  named still reading, a draft grown from a kept line waiting here, the first tap only asking and
  only on its own card, "Keep it" putting the question away and leaving the prayer, confirming
  removing it and leaving the others alone, the question surviving a rotation, and an empty shelf
  saying so but not before the prayers have arrived.
- Verified: `:app:assembleDebug` and `:app:testDebugUnitTest` pass, and on **both** a Pixel 8
  emulator (API 33) and a Galaxy Note 10+, each holding a real database. On the emulator the draft
  grown from a kept line in task 7 was waiting as "After Grant, Almighty God" with its opening line
  and "Last touched Aug 25, 2026", and "Yes, delete" removed it and left the empty state saying so.
  On the phone the draft grown in task 11 was waiting as "After Amazing Grace" with "Father, you are
  endlessly generous." and "Last touched 27 Aug 2026"; the card reaches Compose with its
  `personalPrayerId` and the plus reaches it blank; "Delete" only asks, the question survives a
  rotation into landscape and back, and "Keep it" puts it away with the prayer intact.

## Task 11 — done and merged

The kept lines, replacing the `Saved` placeholder. The one screen that is entirely the reader's own:
nothing in it comes from the catalogue except by way of a line someone chose.

- `feature/saved/SavedUiState.kt` — `SavedUiState` (`savedLines`, `isLoaded`, with `isEmpty` true
  only once the lines have arrived), `SavedLineCardUiState` (the line, its tags, where it came from
  and when it was kept, with `canOpenSourcePrayer`) and `SavedAction` (`OpenSourcePrayer`,
  `GrowIntoPrayer`, `ReleaseLine`), plus `SavedEvent.OpenComposedPrayer`.
- `feature/saved/SavedViewModel.kt` — the first screen with no state of its own: there is nothing to
  narrow and no sheet to open, so everything drawn comes from `SavedLineRepository` and a line let go
  of disappears because the repository says so. Growing a line goes through
  `CreatePersonalPrayerFromLineUseCase` and arrives as an event, as it does in the Reader, because
  the draft has to exist before there is an id to navigate to. Reaching for the prayer a line came
  from stamps it through `RecordPrayerOpenedUseCase`, exactly as Home and the Library do.
- `feature/saved/SavedScreen.kt` — `SavedScreen` (stateless, previewed full and empty) and
  `SavedRoute`. 38sp title, the kept-line count, then the 30-radius cards: the line at 25/1.45, its
  tag chips in the vocabulary's order, one meta line saying where it came from and when it was kept,
  and three text actions — "Read the prayer", "Make it my prayer", and "Let it go" in `ink @ .6`
  rather than sage, because letting go is not something the card should invite. Nothing is hidden
  behind a tap: the shelf is short, and a line the reader chose deserves its actions in the open.
- A line kept from nowhere — no `sourcePrayerId` — still reads whole, without the way back. A saved
  line carries its own copy of its text and its source, so it never depends on the catalogue.
- The empty state is a `sageTint` card that says so, rather than a screen with nothing on it. It
  waits for `isLoaded`, so the reader is never told they have kept nothing while their lines are
  still coming from Room.
- `AbbaNavHost` now shows `SavedRoute`; the Saved placeholder and its sample seed text are gone.
- Ten saved strings and one plural added to `strings.xml`.
- **Tests**: 116 passing (9 new). `SavedViewModelTest` covers a kept line reading back with its tags,
  its source and the day it was kept; the newest line at the top; a line kept from nowhere reading
  without a way back; letting one go removing it and leaving the others alone; an empty shelf saying
  so; nothing being called empty until the lines have arrived; growing a line writing the draft and
  asking for the compose screen; growing a line leaving it kept; and reaching for its prayer stamping
  it as opened.
- Verified: `:app:assembleDebug` and `:app:testDebugUnitTest` pass, and on a Galaxy Note 10+ — the
  empty state reads before anything is kept, two lines kept from "Amazing Grace" come back newest
  first with their chips and "From Amazing Grace · The Valley of Vision, adapted · kept 27 Aug 2026",
  "Read the prayer" reaches the Reader with both kept lines tinted, "Let it go" removes a card and
  moves the count to "1 line you wanted to keep.", "Make it my prayer" reaches Compose with a real
  `personalPrayerId`, and a rotation into landscape keeps the cards whole.

## Task 10 — done and merged

The session, replacing its placeholder. The phone stops looking like an app: deep forest to all four
edges, one line at a time, and a rest at every movement boundary.

- `feature/session/SessionUiState.kt` — `SessionUiState` (title, attribution, `movementLines`,
  a nullable `breathingPause`, `movementTicks`, `canGoBack`, `isAtEnd`, `autoAdvanceAfterMillis`,
  `keepsScreenOn`), `SessionLine`, `BreathingPauseUiState`, `MovementTick`
  (`Spent` / `Current` / `ToCome`) and `SessionAction` (`Advance`, `GoBack`, `Leave`, `Amen`).
- `feature/session/SessionViewModel.kt` — the position is a **step** index in `SavedStateHandle`, not
  a line index, so a rotation taken at a pause comes back to the pause; see `docs/DECISIONS.md`. The
  steps — every line, with a rest between each movement and the next — are derived from `movements`
  on demand. The prayer is held as its own `StateFlow` because moving needs to know how many steps
  there are. Pacing comes from `SettingsRepository`, and the last step carries no dwell so the end of
  the prayer never times out from under "Amen".
- `feature/session/SessionScreen.kt` — `SessionScreen` (stateless, previewed on a line and on a
  pause) and `SessionRoute`. The translucent 44h leave button, the title and byline, then one moss
  tick per **movement** — twenty-nine would be noise. The stage is the current movement at 30/1.42
  with earlier lines at `oat @ .38`, arriving on the design's `fadeup`; it scrolls and follows the
  line being prayed, because a long movement is taller than a phone. The pause is `BREATHE` in moss,
  a moss rule, the movement being entered and "Movement 3 of 5". The blurred sage orb breathes on the
  nine-second `glow` timing, painted as a radial gradient rather than a blur — see
  `docs/DECISIONS.md`. Controls are translucent while there is praying left and the oat "Amen" at the
  end.
- `SessionSystemBars` keeps the screen on when the reader asked for it and tells the bars they are on
  a dark ground, putting **both** back in `DisposableEffect.onDispose`. This is the fix task 4 noted
  for its dark-on-dark status bar.
- `AbbaNavHost` now shows `SessionRoute`; `SessionPlaceholderScreen` is gone.
- Six session strings added to `strings.xml`.
- **Tests**: 107 passing (12 new). `SessionViewModelTest` covers the session opening on the first
  line, earlier lines of the movement staying behind the current one, a rest at every movement
  boundary naming what comes next, a movement beginning on an empty ground, ticks counting movements
  with a pause belonging to the movement it opens onto, the last line being the end with no pause
  after it and no further to go, going back and never past the beginning, the place surviving a
  rotation with a pause included, a paced session carrying its dwell and a reader-paced one carrying
  none, nothing moving on by itself at the end, and the screen kept awake only when asked.
  `FakeSettingsRepository` is new in `testing/`.
- Verified: `:app:assembleDebug` and `:app:testDebugUnitTest` pass, and on a Galaxy Note 10+ — the
  session paints the forest to all four edges with light bar icons, the ticks read
  spent/current/to-come, a movement builds up and the pause clears it, both survive a rotation into
  landscape on the same step, the last step shows the oat "Amen", tapping it pops back to Home with
  the bar icons dark again, and `dumpsys power` shows the `SCREEN_BRIGHT_WAKE_LOCK` taken on entry
  and released on leaving.
- Two things the first run on hardware found and fixed: `Modifier.blur` showed the orb's square edge
  even unbounded, and `Modifier.alpha` over a 3dp tick made it disappear altogether — the tick's
  strength is in its colour now.

## Task 9 — done and merged

The quiet surfaces for what the corpus carries. Nothing new appears on the reading surface until it
is asked for: two small text actions, and everything else stays in a sheet.

- `feature/reader/ReaderUiState.kt` — `ScriptureSheetUiState` (movement index, heading, themes,
  passages) and `ProvenanceSheetUiState` (the adapted title and the prayer's `PrayerProvenance`) join
  `KeepLineSheetUiState`, which gains `canShowMoreTags`. New actions: `OpenScripture(movementIndex)`,
  `OpenProvenance` and `ShowMoreTags`; `DismissSheet` now closes whichever sheet is open.
- `feature/reader/ReaderViewModel.kt` — three new `SavedStateHandle` keys
  (`readerOpenScriptureMovement`, `readerProvenanceOpen`, `readerShowAllTags`) beside the three the
  keep sheet already owned, so both new sheets and the widened tag picker survive a rotation the way
  the keep sheet does. The six keys are folded into two small key records so the `uiState` combine
  stays inside its five-flow limit. Opening any sheet closes the others first — at most one of the
  three state fields is ever non-null, so the screen never has to decide which sheet wins.
- `feature/reader/ReaderScreen.kt` — `ReaderSheet` is now the one shell all three sheets are built
  in (36-radius top, `ink @ .2` handle, `ink @ .4` scrim, oat ground), and its content scrolls:
  48 tag chips and three connection paragraphs both outgrow a phone. `ScriptureSheet` shows the
  movement's heading at 24/1.5, what it holds, then each passage as `Isaiah 57:15 · ESV` in sage with
  its connection beneath. `ProvenanceSheet` shows the adapted title, then the original title, Arthur
  Bennett, the Banner of Truth source and 1975, the copyright line, and the adaptation note verbatim.
  A muted-sage "Three passages" action sits at the end of each movement, and "About this prayer"
  under the byline. A reference is set in the sans face, because it is functional and because the
  serif's old-style figures made "1 John 2:15-17" read as prose rather than as a place; the count is
  spelled out from a five-item `string-array`, with the numeric plural behind it for a sixth passage
  some later prayer might carry.
- The keep sheet's chips are the prayer's own tags, with "More tags" widening them to all 48 — the
  prayer's own still first, because those are the ones the reader is looking for.
- Nine reader strings, one plural and one string-array added to `strings.xml`.
- **Tests**: 95 passing (8 new). `ReaderViewModelTest` covers nothing being open until it is asked
  for, a movement's passages arriving with its heading and themes, "About this prayer" arriving with
  the prayer's provenance, opening one sheet closing whichever was open, both metadata sheets
  surviving a rotation, "More tags" widening the picker with the prayer's own tags still first, that
  widening surviving a rotation, and a tag ticked from the wider vocabulary being kept with the line.
  One older test that counted emissions now waits for the state it is talking about, because the
  keep sheet's keys are folded through one more combine than they were.
- Verified: `:app:assembleDebug` and `:app:testDebugUnitTest` pass, and on a Pixel 8 emulator (API
  33) — "About this prayer" opens on the original title, Arthur Bennett, the Banner of Truth source
  and 1975, the copyright line and the adaptation note; "Three passages" at the end of a movement
  opens on what the movement holds and its three references with their connections; both survive a
  rotation into landscape with the same sheet on the same movement; "More tags" widens the picker to
  all 48 with the prayer's seven still first and ticked, and the sheet scrolls to reach "Keep this
  line". The spelled count and the sans references were then checked again on a Galaxy Note 10+.
  The reading surface itself is unchanged but for the two small text actions.

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

### Task 16 — `feat/16-scripture-text`

```
git switch -c feat/16-scripture-text main
```

**The reader can ask for the verse.** The reader's scripture sheet gains the passage text, fetched
live from the YouVersion Platform, **disclosed behind a tap** so the sheet stays as calm as it is,
and the reader chooses which Bible version to read in.

**Our corpus still carries no verse text.** Nothing goes into `docs/prayers/`, the built asset,
`prayer_scriptures` or `ScriptureReference`. What changes is that the app can now *show* text it does
not *hold*.

**Blocked on an app key** registered at platform.youversion.com. Everything but the live fetch —
parsing, the setting, the picker, the failure states — is buildable and unit-testable without one,
but nothing can be confirmed on a device until it exists.

*The SDK, established — do not re-research it:*

- `com.youversion.platform:platform-core:1.10.0`, on Maven Central, Apache-2.0, minSdk 23, Kotlin
  2.2+. The app is minSdk 31 / Kotlin 2.2.10 / compileSdk 37, so it fits, and `mavenCentral()` is
  already declared so `FAIL_ON_PROJECT_REPOS` needs no change.
- It brings **Ktor, OkHttp, Koin and Kermit** transitively — the first networking in an app that has
  had none. Koin runs the SDK's own graph and does not touch Hilt.
- Configured once in `AbbaFatherApplication.onCreate` with
  `YouVersionPlatformConfiguration.configure(context, appKey)`.
- `YouVersionApi.bibles.passage(BibleReference, format = "html"): BiblePassage` (content is HTML) and
  `versions(languageCode)`. Failures arrive as `YouVersionNetworkException`.
- `BibleReference(versionId, bookUSFM, chapter, verseStart, verseEnd)`;
  `BibleDefaults.VERSION_ID = 3034` is the Berean Standard Bible, freely licensed — the right
  default. The SDK keeps its own file cache of what it fetches.
- **Use `platform-core` only, not `platform-ui`'s `BibleText` composable.** `BibleText` would put a
  network call inside `feature/reader/`, which imports only `domain/` and `core/`; wrapping it in
  `core/` only makes `core/` do IO. And the state discipline collapses — loading, offline,
  not-licensed, which passage is open and which version is chosen all have to be in `ReaderUiState`
  to be testable and to survive rotation. (`BibleTextOptions` does take a `fontFamily`, so styling is
  not what decides this; layering is.)

*The corpus, measured:* **2,606 passages, 1,100 distinct references, 49 book names**, every one
saying ESV. All match `Book Chapter:Verse[-Verse]` except four, and those four decide the parser's
signature: `1 Corinthians 1:18, 24` is a comma list and must become **two** addresses rather than the
span 18–24, which would show five verses the prayer does not claim; and `Jude 20-21`, `Jude 22-23`,
`Jude 24-25` name a single-chapter book, so there is no chapter in the reference. So
`parseScriptureReference(reference): List<PassageAddress>`, and an **empty list** is a reference this
app cannot look up — that passage offers no disclosure action and the sheet is exactly what it is
today. `domain/model/BibleBook.kt` carries the 66-book USFM table with the five one-chapter books
flagged and the alternate spellings ("Psalms", "Song of Songs", "Canticles"). The guard test parses
all 1,100 references out of the committed asset, so a mistyped reference in a future prayer fails in
CI rather than on a reader's phone.

*Shape:*

- `domain/repository/ScriptureRepository` — `isConfigured`, `getPassageText(addresses, versionId)`,
  `getVersions()`. It returns a **result, not an exception**: `PassageText` is
  `Text(paragraphs, versionAbbreviation) | Offline | Unavailable`, because being offline is a state
  this screen draws, not an error path.
- `data/scripture/YouVersionScriptureRepository` — the one repository impl that is **not**
  `Offline*`-prefixed, because calling it that would be a lie. Add the exception to `CLAUDE.md`.
- HTML flattened to plain paragraphs in `data/` with `HtmlCompat` and set in our own face
  (`prayerExcerpt` at `inkSecondary`), quieter than `sheetLine`: the disclosed text is a reference
  the reader asked for, not the sheet's subject. Not `AnnotatedString` — that would drag Compose text
  into a repository for spans of one to three verses.
- **No Room table, no migration, no cache of our own.** The SDK's cache is the publisher's to
  manage, and a table of someone else's scripture is the exact thing the corpus rule exists to prevent.

*The reader picks the version:* `PrayerSettings` gains `bibleVersionId` (default 3034) **and**
`bibleVersionAbbreviation` (default "BSB") — storing the abbreviation is what lets the sheet label
text offline without a round trip. One `setBibleVersion(id, abbreviation)` writing both in a single
`edit {}`. A version id is a setting, not content, so DataStore is right and no migration is
involved. The picker lives in the About settings block, above the passage prose it modifies: closed
it reads "Berean Standard Bible (BSB)" with a "Choose another" action; open, a `Column` of tappable
rows — not chips, since there are hundreds and a `FlowRow` of hundreds is a wall. The list is fetched
**on disclosure**, never on screen open, so About still opens instantly with no connection; offline,
the card keeps showing the stored choice and says so quietly.

*The honest labelling problem:* the reference line says `Isaiah 57:15 · ESV` because that is the
translation the prayer was written from, and the disclosed text will usually be some other version.
**Do not relabel the reference line to match** — that would make the corpus claim something it does
not. The text carries its own attribution and the two coexist.

*Failure states:* `Idle` offers "Read the passage"; `Loading` is one quiet line, no spinner — this
app has none; `Text` is the paragraphs and their attribution; `Offline` says the words need a
connection and the reference is here for your own Bible; `Unavailable` says the passage is not in
that version. No action at all when the reference did not parse, or when the build has no key.

*The app key:* `local.properties` (already git-ignored) into a `buildConfigField`, which needs
`buildFeatures { buildConfig = true }` — currently off — with an environment variable as a fallback.
**A build with no key must still build, run and pass its tests**: no key means the SDK is never
configured, no disclosure is offered, and the app is exactly what ships today. The manifest gains
`INTERNET`, the app's first permission — but not `ACCESS_NETWORK_STATE`, since a failed request is
the same answer and one fewer permission on the listing.

*Docs this task owns:* every place stating the promise needs the same revision — *our corpus carries
no verse text; text shown is fetched live in the version the reader chose and is never written to our
database*. That is `CLAUDE.md`'s content rule; `docs/DECISIONS.md`'s "Scripture is a reference, never
the verse" and "What 'no verse text' actually means", both amended rather than deleted;
`docs/ARCHITECTURE.md`'s "No network layer exists and none is planned", which stops being true;
`about_scripture_body` and the About screen's Scripture block; three places in `README.md`; and the
KDoc on `PrayerScriptureEntity`, `ScriptureReference` and `ScriptureSheet`. Two new decisions: why
the text is borrowed and never kept, and why the reference says ESV while the words say the reader's
version. **Read the YouVersion Platform terms before shipping** — they may require a particular
attribution string or link, and the SDK's own README documents none.

**Done when** `:app:assembleDebug` and `:app:testDebugUnitTest` pass **with no key configured**, and
on a device with one: a passage opens and shows the verse in the chosen version; airplane mode shows
the quiet offline line while every other screen is unchanged; changing the version in About changes
what a reopened passage shows; and deleting the key from `local.properties` and rebuilding gives back
today's app exactly.

### After those

What is still deliberately not built: ambient sound (the domain models it, nothing plays it), the
release build's signing and shrinking, and a sync layer — the schema is shaped for one and nothing
else assumes it.

# Abba, Father

A quiet, offline prayer companion for Android.

Browse a catalogue of Puritan prayers rewritten for a modern voice; pray one line at a time in a
dark full-screen session that rests between movements; keep the lines you want to keep; write your
own. There are no accounts, no network calls and no notifications — everything the app holds stays
on the phone.

## The screens

| | |
|---|---|
| **Home** | a greeting, the day's verse, today's prayer and what you prayed recently |
| **Library** | 186 prayers, narrowed by search, by the part of the collection they came from, or by theme |
| **Reader** | the prayer whole, every line tappable — a tap keeps it |
| **Session** | the prayer line by line on a deep forest ground, with a breathing pause at every movement boundary |
| **Saved** | the lines you kept, each one able to grow into a prayer of your own |
| **My prayers** | what you have written |
| **Compose** | writing one: your words on the oat ground, nothing between you and them |
| **About** | how a session moves, whether the screen stays awake, where the prayers come from, and the licences |

## Where the prayers come from

Every prayer in the Library is a **modern adaptation** of a Puritan prayer from *The Valley of
Vision: A Collection of Puritan Prayers and Devotions*, edited by Arthur Bennett and published by
The Banner of Truth Trust in 1975.

The compilation, its titles and its arrangement are in copyright to Banner of Truth; the underlying
Puritan sources are public domain. **Nothing here reproduces the original wording** — each prayer was
rewritten from the theological themes of its source, and every prayer carries its own provenance
record naming what it was adapted from and what the adaptation did.

The passages a prayer rests on are carried as a **reference and translation name only** — the corpus
holds no verse text at all, which is a licensing line as much as a design one: you are always sent to
your own Bible for the words. The one verse the app does set, under the Home greeting, is the
Authorized Version, which is public domain.

## Built with

Kotlin, Jetpack Compose, Room, Hilt, DataStore, type-safe Navigation Compose. AGP 9 with built-in
Kotlin; `compileSdk` 37, `minSdk` 31, `targetSdk` 36. Cormorant Garamond and Work Sans ship as
bundled variable fonts, so the app renders identically with no network and no font provider.

Single Gradle module with clean-architecture package layering:

```
feature/…  ──▶  domain/  ◀──  data/
```

`domain/` is pure Kotlin — models, repository interfaces, use cases, and no Android import anywhere
in it. `data/` implements those interfaces and never lets a Room entity out. Each screen is a
stateless `(uiState, onAction)` composable with a thin `…Route` wrapper that binds its ViewModel.
`docs/ARCHITECTURE.md` is the full map.

## Building it

```sh
./gradlew :app:assembleDebug        # build
./gradlew :app:testDebugUnitTest    # 145 unit tests, Robolectric where a Context is needed
./gradlew :app:installDebug         # onto a connected device
```

Room schemas are exported to `app/schemas/` and committed; every version bump gets a real migration,
because this database holds prayers people wrote.

## The catalogue

`docs/prayers/*.json` is the source of truth — one file per prayer, carrying its movements, its
theological themes, its scripture references and its provenance. The generator turns them into the
bundled asset:

```sh
python3 tools/build_catalogue.py            # rebuild app/src/main/assets/prayer_catalogue.json
python3 tools/build_catalogue.py --check    # fail if the committed asset is stale
```

The asset is committed too, and is never hand-edited. Splitting a movement's prose into the lines a
session is prayed in happens once, at build time: a flat line index is the address a kept line holds,
so it cannot be allowed to drift.

## Documentation

| | |
|---|---|
| `docs/PROGRESS.md` | what is built, task by task, and what "done" meant for each |
| `docs/ARCHITECTURE.md` | the package map, the DI modules, the routes, the persistence rules |
| `docs/DESIGN_SYSTEM.md` | colour, type, shape and motion — the authority for implementation |
| `docs/DECISIONS.md` | dated records of the choices that are not obvious from the code |
| `CLAUDE.md` | the working rules the code is held to |

## Licensing

The bundled typefaces are used under the SIL Open Font License; both notices travel in the app
(`app/src/main/assets/licenses/`) and are readable from the About screen.

The application code and the adapted corpus carry no licence grant yet — see the copyright note
above before reusing either.

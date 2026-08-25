# Architecture

Single Gradle module (`:app`) with clean-architecture package layering. The module boundary is a
convention enforced by review rather than the compiler; the package map below is the contract.

```
io.abbafather
├── AbbaFatherApplication.kt        @HiltAndroidApp
├── MainActivity.kt                 @AndroidEntryPoint, single activity
├── core/
│   ├── common/                     dispatcher qualifiers, cross-cutting primitives
│   └── designsystem/
│       ├── theme/                  AbbaColors, AbbaTypography, AbbaTheme
│       └── component/              SoftCard, PillButton, SelectableChip, AbbaIcons…
├── data/
│   ├── local/
│   │   ├── entity/                 Room entities (never leave this layer)
│   │   ├── dao/                    Flow reads, suspend writes
│   │   └── AbbaDatabase.kt         schema exported to app/schemas/
│   └── repository/                 implements domain repository interfaces
├── domain/
│   ├── model/                      Prayer, SavedLine, PersonalPrayer, Collection…
│   ├── repository/                 interfaces only
│   └── usecase/                    single-responsibility, operator fun invoke
├── feature/
│   └── <screen>/                   <Name>Screen, <Name>UiState, <Name>ViewModel
├── navigation/                     AbbaNavHost, routes, AbbaBottomBar
└── di/                             Hilt modules
```

Dependency direction: `feature → domain ← data`. `domain` depends on nothing but Kotlin and coroutines.

## Dependency injection

Hilt, KSP-processed. Modules live in `di/` and are installed in `SingletonComponent` unless a narrower
scope is genuinely needed.

| Module | Provides |
|---|---|
| `CoroutinesModule` | `@IoDispatcher`, `@DefaultDispatcher`, `@ApplicationScope` |
| `DatabaseModule` (task 3) | `AbbaDatabase` and its DAOs |
| `RepositoryModule` (task 3) | `@Binds` domain interface → data implementation |

ViewModels are `@HiltViewModel` and obtained with `hiltViewModel()` at the navigation route.

## Presentation

One immutable `…UiState` and one sealed `…Action` per screen. Screens are stateless
`(uiState, onAction)` composables so they can be previewed and tested without Hilt; a thin `…Route`
wrapper binds the ViewModel and performs navigation. State is a `StateFlow` produced with
`stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)` and collected with
`collectAsStateWithLifecycle()`.

## Navigation

Type-safe `@Serializable` routes in `navigation/`:

| Route | Arguments | Bottom bar |
|---|---|---|
| `Home` | — | yes |
| `Library` | — | yes |
| `MyPrayers` | — | yes |
| `Saved` | — | yes |
| `Reader` | `prayerId` | no |
| `Session` | `prayerId` | no |
| `ComposePrayer` | `personalPrayerId?`, `seedText?` | no |

Top-level tabs save and restore their back stacks (`popUpTo(startDestination) { saveState = true }`,
`restoreState = true`, `launchSingleTop = true`).

## Persistence

Room v1, schema exported to `app/schemas/` and committed. Catalogue prayers are seeded once from
`assets/prayer_catalogue.json` in a `RoomDatabase.Callback` and are read-only to the user. User-owned
rows (saved lines, personal prayers, collections) carry a UUID id, `createdAt`/`updatedAt` epoch millis
and an `isDeleted` soft-delete flag. DataStore Preferences holds settings only (pacing, ambient
default). No network layer exists and none is planned.

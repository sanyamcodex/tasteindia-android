# TasteIndia

TasteIndia is a Kotlin and Jetpack Compose Android app for discovering Indian recipes from TheMealDB, filtering them by recipe metadata, opening recipe details, and saving favourites for offline display.

## Toolchain

- Android Gradle Plugin: 9.1.1
- Kotlin: 2.2.10
- Kotlin/JVM: Java 17
- Compose compiler plugin: 2.2.10
- Compile SDK / target SDK: 35
- Minimum SDK: 26
- KSP: 2.3.5
- Hilt: 2.59.2
- Compose BOM: 2024.09.00
- Navigation Compose: 2.8.9
- Room: 2.7.0
- Retrofit: 2.12.0
- OkHttp: 4.12.0
- Moshi: 1.15.2
- Coil: 2.7.0
- Coroutines: 1.10.2
- Tests: JUnit4, MockK 1.13.16, Turbine 1.2.0

## Build and Run

Open the project in Android Studio with an Android SDK 35 installation and a Java 17 toolchain. Select the `app` configuration and run it on an Android 8.0 (API 26) or newer device/emulator.

The project currently does not include a Gradle wrapper. With Gradle installed locally, use:

```text
gradle assembleDebug
gradle test
```

The app requires network access for TheMealDB recipe data. Favourites remain available from Room when the network is unavailable.

## Architecture

The project follows a one-way Kotlin Flow architecture:

1. Remote DTOs: Moshi-generated DTOs model TheMealDB responses.
2. Domain models: UI-independent `Meal`, `MealDetail`, filtering, and error types.
3. Repository: API calls, mapping, Indian-area boundaries, caching, request deduplication, and favourites persistence.
4. ViewModels: one ViewModel per screen; each exposes immutable `StateFlow` UI state.
5. Compose UI: screens collect state and emit user events; Composables do not perform network or database work.

Hilt provides the Retrofit/OkHttp/Moshi stack, Room database, DAO, and repository. Coil loads recipe images.

## Route Map

| Route | Screen | Navigation argument |
| --- | --- | --- |
| `recipes` | Indian recipe list, search, filters, sorting | None |
| `details/{mealId}` | Recipe details and favourite toggle | `mealId` only |
| `favourites` | Offline-capable saved favourites | None |

Only `mealId` crosses the details navigation boundary. Full domain objects are not passed through navigation.

## TheMealDB Endpoints

The Retrofit service uses `https://www.themealdb.com/api/json/v1/1/`:

- `GET filter.php?a=Indian`: initial Indian recipe boundary
- `GET filter.php?c={category}`: category candidates
- `GET filter.php?i={ingredient}`: ingredient candidates
- `GET search.php?s={name}`: name search candidates
- `GET lookup.php?i={id}`: full recipe details
- `GET list.php?c=list`: category choices
- `GET list.php?a=list`: available area choices

## Indian Intersection Strategy

TheMealDB filter endpoints return IDs for different dimensions and do not guarantee that a category or ingredient result is Indian. TasteIndia first obtains the Indian-area ID set from `filter.php?a=Indian`. Category and ingredient results are converted to ID sets and intersected with that Indian set. Name search results are filtered against the same boundary before they reach the UI.

This keeps every recipe-list result inside the Indian-area constraint without downloading full details for every candidate.

## Cache and Deduplication Policy

- Indian recipe summaries are cached in memory for the repository lifetime.
- Full meal details are cached by meal ID in memory.
- Concurrent detail requests for the same ID share one in-flight `Deferred`; a request is removed from the in-flight map when it completes.
- Favourites are persisted in Room with `mealId`, `name`, and `thumbUrl`, so the favourites screen can render offline without a network request or placeholder name.
- Search, filters, favourites-only mode, and sort order are stored through `SavedStateHandle` in `RecipesViewModel`.

## Tests

There are exactly four local unit tests using JUnit4, MockK, Turbine, fixture resources, and fake repository/DAO implementations:

- Ingredient mapping with null and blank DTO slots
- Indian/category ID intersection
- Latest-result behavior for rapid sequential searches
- Offline favourites state after toggling a meal, with no API call

No test uses a real network or database.

## Known Issues

- The repository has no Gradle wrapper, so builds and tests require a local Gradle installation or Android Studio's configured Gradle tooling.
- The test suite has been statically checked in this workspace, but could not be executed here because no Gradle executable was available.
- Existing v1 Room favourite rows migrate with empty `name` and `thumbUrl` values. New favourites persist complete metadata; old rows need to be re-saved to gain display metadata.
- The app uses TheMealDB's public API and therefore depends on its availability, rate limits, and response shape.

## Time Spent

Active work time was not instrumented. Git history records a project activity window of approximately 8 hours 34 minutes from the initial commit to the latest test commit; this is elapsed repository time, not a claim of continuous hands-on time.

## AI Tool-Use Disclosure

The project work used the following tools, with responsibilities recorded for this project:

- Google AI Studio: initial product concept, requirements, and early app direction.
- Antigravity: initial implementation phases covering the project scaffold, network layer, domain/repository layer, Compose screens, filtering, navigation/details, and Room favourites.
- GitHub Copilot: Phases FIX, 7B, 8, 9, and 10: repository/ViewModel fixes, offline favourite metadata, accessibility/previews/state restoration, unit tests, and this documentation.

All code was reviewed and fixed by the developer using GitHub Copilot. Evidence includes the Phase FIX issues caught and corrected: `safeApiCall` had a non-suspend lambda around suspend Retrofit calls; detail deduplication created `async` work inside a blocking `coroutineScope`; and `DetailsViewModel` publicly exposed `mealId` instead of exposing only its immutable UI `StateFlow`.

See [ATTRIBUTION.md](ATTRIBUTION.md) for TheMealDB and library attribution.
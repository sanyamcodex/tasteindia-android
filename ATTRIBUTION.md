# Attribution

## Recipe Data

TasteIndia uses [TheMealDB](https://www.themealdb.com/) for recipe data and images through its public API. The app uses the following TheMealDB API base URL:

```text
https://www.themealdb.com/api/json/v1/1/
```

TheMealDB is an independent service. TasteIndia does not claim ownership of its recipe content, names, instructions, identifiers, or images. Review TheMealDB's current terms and usage limits before distributing the app or using the service commercially.

## Libraries

TasteIndia is built with open-source libraries. Their licenses and notices remain governed by their respective projects:

- Kotlin and Kotlin Coroutines: [kotlinlang.org](https://kotlinlang.org/)
- Android Gradle Plugin and AndroidX: [developer.android.com](https://developer.android.com/)
- Jetpack Compose and Material 3: [developer.android.com/jetpack/compose](https://developer.android.com/jetpack/compose)
- Hilt: [dagger.dev/hilt](https://dagger.dev/hilt/)
- Room: [developer.android.com/training/data-storage/room](https://developer.android.com/training/data-storage/room)
- Retrofit: [square.github.io/retrofit](https://square.github.io/retrofit/)
- OkHttp: [square.github.io/okhttp](https://square.github.io/okhttp/)
- Moshi: [github.com/square/moshi](https://github.com/square/moshi)
- Coil: [coil-kt.github.io/coil](https://coil-kt.github.io/coil/)
- MockK: [mockk.io](https://mockk.io/)
- Turbine: [github.com/cashapp/turbine](https://github.com/cashapp/turbine)

Dependency versions are maintained in [gradle/libs.versions.toml](gradle/libs.versions.toml). For complete license text and notices, consult each dependency's distribution and the resolved dependency metadata used for a release build.
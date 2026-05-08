# nav3-helper

Kotlin Multiplatform navigation helper focused on:

- state-first back stack management
- cross-module navigation by route key
- KSP-generated registries and destinations

中文文档: [README_ZH.md](./README_ZH.md)

## Installation
[![Maven Central](https://img.shields.io/maven-central/v/io.github.aleyn97/navigation3-helper)](https://github.com/aleyn97/navigation3-helper)

### Android-only module

If you use a plain Android/Kotlin module instead of KMP, use the Android KSP configuration directly:

```kotlin
dependencies {
    implementation("io.github.aleyn97:navigation3-helper:<version>")
    ksp("io.github.aleyn97:nav3-ksp-compiler:<version>")
}
```

Android-only module plugins:

```kotlin
plugins {
    id("com.android.application") // or com.android.library
    kotlin("android")
    id("com.google.devtools.ksp")
}
```

Android startup example:

```kotlin
class BaseApplication : Application() {

    override fun onCreate() {
        loadNavRegistry(XXXRegistry)
    }
}

@Composable
fun App() {
    NavDisplayHelper(startRoute = XXXRegistry.defaultStartScreen)
}
```

### Kotlin Multiplatform

Add the runtime and KSP compiler:

```kotlin
dependencies {
    implementation("io.github.aleyn97:navigation3-helper:<version>")
    add("kspCommonMainMetadata", "io.github.aleyn97:nav3-ksp-compiler:<version>")
}
```

If your project has platform-specific KSP tasks, also add the compiler to those configurations:

```kotlin
dependencies {
    add("kspAndroid", "io.github.aleyn97:nav3-ksp-compiler:<version>")
    add("kspIosX64", "io.github.aleyn97:nav3-ksp-compiler:<version>")
    add("kspIosArm64", "io.github.aleyn97:nav3-ksp-compiler:<version>")
    add("kspIosSimulatorArm64", "io.github.aleyn97:nav3-ksp-compiler:<version>")
}
```

Apply the required plugins in modules that declare `@Screen` pages:

```kotlin
plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
}
```

If any screen parameter uses an `@Serializable` type, also apply:

```kotlin
plugins {
    kotlin("plugin.serialization")
}
```

Generated code for `commonMain` should be added to the source set when needed:

```kotlin
kotlin {
    sourceSets {
        commonMain {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
        }
    }
}
```

Minimal startup example:

```kotlin
fun initNavigation() {
    NavCenter.setRegistries(setOf(ComposeAppRegistry))
}

@Composable
fun App() {
    NavDisplayHelper(ComposeAppRegistry.defaultStartScreen)
}
```

## Examples

Use `LocalNavBackStackState` for local destination-based navigation inside the current host, and
use `NavCenter` for cross-module route-key navigation.

### 1. Local-only navigation

Use an empty `route` when the page does not need global route resolution:

```kotlin
@Screen
@Composable
fun ProfileScreen() { /* ... */ }
```

Navigate through the generated destination:

```kotlin
val backStack = LocalNavBackStackState.current
backStack.navigate(ProfileScreenDestination)
```

### 2. Cross-module route navigation

Declare a fixed route key:

```kotlin
@Screen(route = "app://user/detail")
@Composable
fun UserDetailScreen(id: Long) { /* ... */ }
```

Navigate from any module through `NavCenter`:

```kotlin
NavCenter.navigate("app://user/detail?id=123")
```

### 3. `@Serializable` route parameter

```kotlin
@Serializable
data class UserInfo(
    val userId: String,
    val nickname: String
)

@Screen(route = "app://user/me")
@Composable
fun MeScreen(userInfo: UserInfo) { /* ... */ }
```

Runtime navigation:

```kotlin
val userInfoParam = serializeRouteQueryValue(
    UserInfo(userId = "1001", nickname = "Aleyn")
)
NavCenter.navigate("app://user/me?userInfo=$userInfoParam")
```

### 4. Page result callback

Return a one-shot result from the child page:

```kotlin
// Child page
Button(onClick = {
    backStack.setResult(ProfileResult(success = true))
    backStack.goBack()
}) { /* ... */ }
```

Handle it once when the parent page becomes active again:

```kotlin
// Parent page
backStack.consumeResultEffect<ProfileResult> { result ->
    if (result?.success == true) {
        // refresh UI
    }
}
```

### 5. Use `NavDisplay` directly

`NavDisplayHelper(...)` is optional. You can wire the official component yourself:

```kotlin
val backStack = rememberHelperBackStack(
    startRoute = ComposeAppRegistry.defaultStartScreen,
    navRegistrySet = setOf(ComposeAppRegistry)
)

NavDisplay(
    backStack = backStack.navBackStack,
    onBack = { backStack.goBack() },
    entryProvider = getEntryProvider(setOf(ComposeAppRegistry))
)
```

## Route Rules

`@Screen(route = ...)` declares an optional fixed route key for a page.

If `route` is left empty, the screen still gets a generated destination and can participate in
ordinary local navigation, but it is not registered into `NavCenter` route resolution.

The library treats the route as an identity key and does not force a specific protocol. These are
all valid styles:

- `https://www.app.cn/user/detail`
- `app://user/detail`
- `user/detail`

Recommended rules:

- Route keys must be globally unique.
- The annotation route is optional, but when present it must be fixed and must not contain query
  parameters.
- Do not use path templates such as `user/{id}`.
- Dynamic values should be passed at runtime through the query string.
- Query parameter names should match composable parameter names.
- Route keys should stay stable and should not be coupled to function names.

Route key normalization:

- Query parameters and fragments are ignored when matching page identity.
- Empty path segments are ignored, so trailing slashes do not change the key.
- Scheme and authority are normalized to lowercase.
- If the same query key appears multiple times at runtime, the last value wins.

Example:

```kotlin
@Screen(route = "app://user/detail")
@Composable
fun UserDetailScreen(
    id: Long,
    tab: String = "post"
) { /* ... */ }
```

Navigate at runtime with:

```kotlin
NavCenter.navigate("app://user/detail?id=123&tab=comment")
```

Ordinary local screen:

```kotlin
@Screen
@Composable
fun LocalOnlyScreen() { /* ... */ }
```

## Current Parameter Support

URL query restoration is intended for lightweight public parameters.

Supported:

- `String`
- primitive types
- `@Serializable` object types
- nullable values
- default values

If you use `@Serializable` screen parameters, the declaring module should also apply the Kotlin
serialization plugin.

Not recommended for URL transport:

- complex objects
- large payloads
- private or sensitive business state

Runtime behavior:

- Missing required query parameters make route resolution fail.
- Invalid primitive parsing also makes route resolution fail.
- Invalid `@Serializable` JSON payloads also make route resolution fail.
- Nullable parameters and parameters with default values fall back naturally.
- If a value should not come from the route, prefer loading it from inside the screen.

For `@Serializable` route parameters, encode the JSON payload before appending it to the runtime
URL query string:

```kotlin
@Serializable
data class Filter(val tab: String, val page: Int)

val filter = serializeRouteQueryValue(Filter(tab = "post", page = 2))
NavCenter.navigate("app://user/detail?filter=$filter")
```

## Page Results

For local page result passing, prefer the host-scoped result store on `NavBackStackState`.

There are two usage styles:

- use the result type itself as the default key
- pass a custom key when you need multiple results of the same type in one flow

Example:

```kotlin
Button(
    onClick = {
        backStack.navigate(EditProfileScreen(resultKey = resultKey))
    }
) { /* ... */ }

val result = backStack.consumeResult<ProfileResult>()
// or
val result = backStack.consumeResult<ProfileResult>(resultKey)
```

Return from the child page:

```kotlin
backStack.setResult(ProfileResult(...))
backStack.goBack()
```

With a custom key:

```kotlin
backStack.setResult(resultKey, ProfileResult(...))
backStack.goBack()
```

Available APIs:

- `setResult(...)`
- `peekResult(...)`
- `consumeResult(...)`
- `consumeResultEffect(...)`
- `hasResult(...)`
- `clearResult(...)`

If the result should only be handled once when the page becomes active again, prefer
`consumeResultEffect(...)` or `consumeResult(...)` over `peekResult(...)`.

## Registry Rules

- Registries are application-level global configuration.
- Register them once during app startup with `NavCenter.setRegistries(...)`.
- Duplicate route keys fail fast during registration.
- `NavDisplayHelper(...)` is optional; users may directly use `NavDisplay`.

## Minimal Flow

1. Mark composable pages with `@Screen(route = ...)`.
2. Initialize global registries at app startup.
3. Create a `NavBackStackState` for the current host.
4. Render with `NavDisplayHelper(...)` or `NavDisplay(...)`.
5. Navigate anywhere by route key through `NavCenter.navigate(...)`.

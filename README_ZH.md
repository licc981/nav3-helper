# nav3-helper

`nav3Helper` 是一个面向 Kotlin Multiplatform 的导航辅助库，当前重点是：

- 以状态为中心的返回栈管理
- 通过 route key 进行跨模块导航
- 基于 KSP 生成 registry 和 destination

English README: [README.MD](./README.MD)

## 接入方式
![Maven Central Version](https://img.shields.io/maven-central/v/io.github.aleyn97/navigation3-helper)


### 纯 Android 模块

如果不是 KMP，而是普通 Android/Kotlin 模块，直接使用 Android 的 KSP 配置：

```kotlin
dependencies {
    implementation("io.github.aleyn97:navigation3-helper:<version>")
    ksp("io.github.aleyn97:nav3-ksp-compiler:<version>")
}
```

纯 Android 模块插件：

```kotlin
plugins {
    id("com.android.application") // 或 com.android.library
    kotlin("android")
    id("com.google.devtools.ksp")
}
```

Android 启动示例：

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

### Kotlin Multiplatform (多平台)

先添加运行时库和 KSP 编译器：

```kotlin
dependencies {
    implementation("io.github.aleyn97:navigation3-helper:<version>")
    add("kspCommonMainMetadata", "io.github.aleyn97:nav3-ksp-compiler:<version>")
}
```

如果项目里还有平台侧 KSP 任务，也要把编译器加到对应配置上：

```kotlin
dependencies {
    add("kspAndroid", "io.github.aleyn97:nav3-ksp-compiler:<version>")
    add("kspIosX64", "io.github.aleyn97:nav3-ksp-compiler:<version>")
    add("kspIosArm64", "io.github.aleyn97:nav3-ksp-compiler:<version>")
    add("kspIosSimulatorArm64", "io.github.aleyn97:nav3-ksp-compiler:<version>")
}
```

声明 `@Screen` 页面所在模块至少需要应用这些插件：

```kotlin
plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
}
```

如果页面参数里用到了 `@Serializable` 类型，还要再加：

```kotlin
plugins {
    kotlin("plugin.serialization")
}
```

如果需要让 `commonMain` 识别 KSP 生成代码，记得把生成目录加入 source set：

```kotlin
kotlin {
    sourceSets {
        commonMain {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
        }
    }
}
```

最小启动示例：

```kotlin
fun initNavigation() {
    NavCenter.setRegistries(setOf(ComposeAppRegistry))
}

@Composable
fun App() {
    NavDisplayHelper(ComposeAppRegistry.defaultStartScreen)
}
```

## 示例

当前宿主内的本地 destination 导航，优先使用 `LocalNavBackStackState`；跨模块的
route key 导航，优先使用 `NavCenter`。

### 1. 仅本地普通导航

如果页面不需要参与全局 route 解析，可以直接不写 `route`：

```kotlin
@Screen
@Composable
fun ProfileScreen() { /* ... */ }
```

通过生成的 destination 本地跳转：

```kotlin
val backStack = LocalNavBackStackState.current
backStack.navigate(ProfileScreenDestination)
```

### 2. 跨模块 route 导航

先声明固定 route key：

```kotlin
@Screen(route = "app://user/detail")
@Composable
fun UserDetailScreen(id: Long) { /* ... */ }
```

然后任意模块都可以通过 `NavCenter` 跳转：

```kotlin
NavCenter.navigate("app://user/detail?id=123")
```

### 3. `@Serializable` 路由参数

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

运行时跳转：

```kotlin
val userInfoParam = serializeRouteQueryValue(
    UserInfo(userId = "1001", nickname = "Aleyn")
)
NavCenter.navigate("app://user/me?userInfo=$userInfoParam")
```

### 4. 页面返回值

子页面返回一次性结果：

```kotlin
// Child page
Button(onClick = {
    backStack.setResult(ProfileResult(success = true))
    backStack.goBack()
}) { /* ... */ }
```

父页面在重新活跃时消费一次：

```kotlin
// Parent page
backStack.consumeResultEffect<ProfileResult> { result ->
    if (result?.success == true) {
        // refresh UI
    }
}
```

### 5. 直接使用 `NavDisplay`

`NavDisplayHelper(...)` 只是便利层，你也可以直接接官方组件：

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

## 路由规则

`@Screen(route = ...)` 用来声明页面的可选固定 route key。

如果 `route` 为空，这个页面依然会生成 destination，也可以参与普通本地导航，但不会注册到 `NavCenter` 的全局路由解析中。

如果 `route` 不为空，它就代表这个页面的全局身份 key。库本身不强制具体协议，下面这些写法都可以：

- `https://www.app.cn/user/detail`
- `app://user/detail`
- `user/detail`

推荐约束：

- route key 必须全局唯一
- 注解里的 route 必须是固定 key，不能带 query
- 不要使用 `user/{id}` 这样的 path 模板
- 动态值统一放到运行时 query 参数中
- query 参数名尽量和 composable 参数名保持一致
- route key 应该稳定，不要直接绑定函数名

### route key 归一化规则

- query 和 fragment 不参与页面 identity 匹配
- 空 path segment 会被忽略，所以尾斜杠不会改变 key
- scheme 和 authority 会统一转成小写
- 如果同一个 query key 重复出现，最后一个值生效

示例：

```kotlin
@Screen(route = "app://user/detail")
@Composable
fun UserDetailScreen(
    id: Long,
    tab: String = "post"
) { /* ... */ }
```

运行时跳转：

```kotlin
NavCenter.navigate("app://user/detail?id=123&tab=comment")
```

仅本地普通导航页面：

```kotlin
@Screen
@Composable
fun LocalOnlyScreen() { /* ... */ }
```

## 当前参数支持范围

URL query 恢复更适合轻量公开参数。

支持：

- `String`
- primitive 类型
- `@Serializable` 对象类型
- nullable 参数
- 带默认值的参数

如果页面参数里用了 `@Serializable` 类型，声明这些页面的模块也需要应用 Kotlin
serialization 插件。

不建议通过 URL 传递：

- 复杂对象
- 大体积 payload
- 私有或敏感业务状态

运行时行为：

- 缺少必填 query 参数时，route resolve 会失败
- primitive 解析失败时，route resolve 也会失败
- `@Serializable` JSON 解析失败时，route resolve 也会失败
- nullable 参数和带默认值参数会自然兜底
- 如果某个值不应该来自 route，建议在 screen 内部自行加载

如果 route 参数本身是 `@Serializable` 类型，建议先把 JSON payload 编码后再拼到运行时 query 里：

```kotlin
@Serializable
data class Filter(val tab: String, val page: Int)

val filter = serializeRouteQueryValue(Filter(tab = "post", page = 2))
NavCenter.navigate("app://user/detail?filter=$filter")
```

## 页面返回值

本地页面之间的返回值，推荐使用 `NavBackStackState` 内部的宿主级 result store。

现在有两种使用方式：

- 直接用结果类型本身作为默认 key
- 当同一条流程里会返回多个同类型结果时，再显式传自定义 key

示例：

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

子页面返回：

```kotlin
backStack.setResult(ProfileResult(...))
backStack.goBack()
```

如果使用自定义 key：

```kotlin
backStack.setResult(resultKey, ProfileResult(...))
backStack.goBack()
```

可用 API：

- `setResult(...)`
- `peekResult(...)`
- `consumeResult(...)`
- `consumeResultEffect(...)`
- `hasResult(...)`
- `clearResult(...)`

如果这个结果只希望在页面重新活跃时处理一次，优先用
`consumeResultEffect(...)` 或 `consumeResult(...)`，不要直接长期用 `peekResult(...)`。

## Registry 规则

- registry 是应用级全局配置
- 推荐在应用启动时通过 `NavCenter.setRegistries(...)` 一次性注册
- 重复 route key 会在注册阶段直接失败
- `NavDisplayHelper(...)` 是可选便利层，也可以直接使用 `NavDisplay`
- 可以通过 `NavCenter.debugSnapshot()` 查看当前注册信息、拦截器数量和 host 状态

## 最小接入流程

1. 使用 `@Screen(route = ...)` 标记页面
2. 在应用启动时初始化全局 registries
3. 为当前宿主创建 `NavBackStackState`
4. 使用 `NavDisplayHelper(...)` 或 `NavDisplay(...)` 渲染
5. 通过 `NavCenter.navigate(...)` 使用 route key 导航

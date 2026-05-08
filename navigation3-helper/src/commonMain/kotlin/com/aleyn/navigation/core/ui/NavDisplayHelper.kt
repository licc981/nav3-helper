package com.aleyn.navigation.core.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.defaultPopTransitionSpec
import androidx.navigation3.ui.defaultPredictivePopTransitionSpec
import androidx.navigation3.ui.defaultTransitionSpec
import androidx.navigationevent.NavigationEvent
import com.aleyn.navigation.core.route.EntryProvider
import com.aleyn.navigation.core.route.NavRegistry
import com.aleyn.navigation.core.route.NavScreen
import com.aleyn.navigation.core.route.getEntryProvider
import com.aleyn.navigation.core.navigator.NavBackStackController
import com.aleyn.navigation.core.navigator.LocalNavBackStackState
import com.aleyn.navigation.core.navigator.rememberHelperBackStack
import com.aleyn.navigation.core.route.NavCenter

/**
 * @author : Aleyn
 * @date : 2025/12/3 15:22
 */


/**
 * Renders a navigation host from externally owned state.
 * [entryProvider] is passed explicitly so rendering does not implicitly depend on global registry
 * state.
 */
@Composable
fun NavDisplayHelper(
    backStack: NavBackStackController,
    entryProvider: EntryProvider,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    entryDecorators: List<NavEntryDecorator<NavScreen>> = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator()
    ),
    sceneStrategy: SceneStrategy<NavScreen> = SinglePaneSceneStrategy(),
    sizeTransform: SizeTransform? = null,
    transitionSpec: AnimatedContentTransitionScope<Scene<NavScreen>>.() -> ContentTransform =
        defaultTransitionSpec(),
    popTransitionSpec: AnimatedContentTransitionScope<Scene<NavScreen>>.() -> ContentTransform =
        defaultPopTransitionSpec(),
    predictivePopTransitionSpec:
    AnimatedContentTransitionScope<Scene<NavScreen>>.(
        @NavigationEvent.SwipeEdge Int
    ) -> ContentTransform = defaultPredictivePopTransitionSpec()
) {

    DisposableEffect(backStack) {
        NavCenter.attachHost(backStack)
        onDispose {
            NavCenter.detachHost(backStack)
        }
    }

    CompositionLocalProvider(LocalNavBackStackState provides backStack) {
        NavDisplay(
            backStack = backStack.navBackStack,
            onBack = { backStack.goBack() },
            entryDecorators = entryDecorators,
            modifier = modifier,
            contentAlignment = contentAlignment,
            sceneStrategy = sceneStrategy,
            sizeTransform = sizeTransform,
            transitionSpec = transitionSpec,
            popTransitionSpec = popTransitionSpec,
            predictivePopTransitionSpec = predictivePopTransitionSpec,
            entryProvider = entryProvider,
        )
    }
}


@Composable
fun NavDisplayHelper(
    startRoute: NavScreen,
    navRegistrySet: Set<NavRegistry> = NavCenter.registries,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    entryDecorators: List<NavEntryDecorator<NavScreen>> = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator()
    ),
    sceneStrategy: SceneStrategy<NavScreen> = SinglePaneSceneStrategy(),
    sizeTransform: SizeTransform? = null,
    transitionSpec: AnimatedContentTransitionScope<Scene<NavScreen>>.() -> ContentTransform =
        defaultTransitionSpec(),
    popTransitionSpec: AnimatedContentTransitionScope<Scene<NavScreen>>.() -> ContentTransform =
        defaultPopTransitionSpec(),
    predictivePopTransitionSpec:
    AnimatedContentTransitionScope<Scene<NavScreen>>.(
        @NavigationEvent.SwipeEdge Int
    ) -> ContentTransform = defaultPredictivePopTransitionSpec()
) {

    val backStack = rememberHelperBackStack(
        startRoute,
        navRegistrySet
    )

    val entryProvider = remember {
        getEntryProvider(navRegistrySet)
    }

    NavDisplayHelper(
        backStack = backStack,
        entryProvider = entryProvider,
        modifier = modifier,
        contentAlignment = contentAlignment,
        entryDecorators = entryDecorators,
        sceneStrategy = sceneStrategy,
        sizeTransform = sizeTransform,
        transitionSpec = transitionSpec,
        popTransitionSpec = popTransitionSpec,
        predictivePopTransitionSpec = predictivePopTransitionSpec
    )
}

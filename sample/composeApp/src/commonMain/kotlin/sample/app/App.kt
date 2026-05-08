package sample.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.aleyn.navigation.core.ext.loadNavRegistry
import com.aleyn.navigation.core.route.NavRegistry
import com.aleyn.navigation.core.route.NavCenter
import com.aleyn.navigation.core.ui.NavDisplayHelper
import com.navigation.screen.generated.ChildFirstRegistry
import com.navigation.screen.generated.ChildSecondRegistry
import com.navigation.screen.generated.ComposeAppRegistry

/**
 * add all Registry
 */
fun initNavigation() {
    loadNavRegistry(ComposeAppRegistry, ChildFirstRegistry, ChildSecondRegistry)
}

@Composable
fun App() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center
    ) {

        NavDisplayHelper(ComposeAppRegistry.defaultStartScreen)
    }
}

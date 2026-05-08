package com.navigation.child_second.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aleyn.navigation.annotations.Screen
import com.aleyn.navigation.core.navigator.LocalNavBackStackState
import com.aleyn.navigation.core.navigator.setResult

/**
 * @author: Aleyn
 * @date: 2026/04/27 15:59
 * @desc: 
 */

@Screen(route = "https://www.app.cn/child-second/main", start = true)
@Composable
fun SecondScreen() {

    val backStack = LocalNavBackStackState.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
    ) {
        Text(text = "SecondScreen")
        Button(onClick = {
            backStack.setResult("SecondScreen Back")
            backStack.goBack()
        }) {
            Text(text = "Back And Return Value")
        }
    }
}

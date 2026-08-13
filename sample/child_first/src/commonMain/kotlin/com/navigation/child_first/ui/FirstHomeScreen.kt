package com.navigation.child_first.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aleyn.navigation.annotations.Screen
import com.aleyn.navigation.core.navigator.LocalNavBackStackState
import com.aleyn.navigation.core.navigator.consumeResult
import com.aleyn.navigation.core.navigator.consumeResultEffect
import com.aleyn.navigation.core.navigator.peekResult
import com.aleyn.navigation.core.route.NavCenter
import kotlinx.coroutines.launch

/**
 * @author: Aleyn
 * @date: 2026/04/27 15:31
 * @desc: 
 */

@Screen(route = "https://www.app.cn/child-first/main", start = true)
@Composable
fun FirstHomeScreen() {

    val backStack = LocalNavBackStackState.current
    val coroutineScope = rememberCoroutineScope()

    var resultData by remember { mutableStateOf("") }

    backStack.consumeResultEffect<String> {
        resultData = it.orEmpty()
    }


    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "this FirstHomeScreen")

        if (resultData.isNotBlank()){
            Text(text = "resultData is $resultData")
        }


        Button(onClick = {
            // 跨模块只能通过 router 方式 跳转
            coroutineScope.launch {
                NavCenter.navigate("https://www.app.cn/child-second/main")
            }
        }) {
            Text(text = "Go ChildSecondModule")
        }
    }
}

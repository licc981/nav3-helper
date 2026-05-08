package sample.app.ui.page.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aleyn.navigation.annotations.Screen
import com.aleyn.navigation.core.navigator.LocalNavBackStackState
import com.aleyn.navigation.core.route.NavCenter
import com.aleyn.navigation.core.route.serializeRouteQueryValue
import sample.app.ui.page.me.UserInfo

/**
 * @author : Aleyn
 * @date : 2025/12/3 09:55
 */

@Screen(route = "https://www.app.cn/compose-app/home")
@Composable
fun HomeScreen() {

    val viewModel = viewModel<HomeViewModel>()

    val backState = LocalNavBackStackState.current

    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Home")
        Spacer(Modifier.height(20.dp))
        Button(onClick = {
            val userInfo = UserInfo(
                userId = "66666",
                avatarUrl = "https://www.app.cn/image/avatar.png",
                nickName = "Aleyn"
            )

            val userInfoParam = serializeRouteQueryValue(userInfo)
            NavCenter.navigate("https://www.app.cn/compose-app/me?userInfo=${userInfoParam}")

            //or
            //backState.navigate(MeScreenDestination(userInfo))

            //or
            //NavCenter.navigate(MeScreenDestination(userInfo))
        }) {
            Text(text = "Go My Page")
        }
    }
}

package sample.app.ui.page

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aleyn.navigation.annotations.Screen
import com.aleyn.navigation.core.navigator.LocalNavBackStackState
import com.aleyn.navigation.core.route.NavCenter

/**
 * @author : Aleyn
 * @date : 2025/12/3 09:55
 */


@Screen(route = "https://www.app.cn/users/{filter}/{id}", needLogin = true, multiInstance = true)
@Composable
fun DetailScreen(
    filter: String,
    id: Int,
    detailViewModel: DetailViewModel = viewModel()
) {
    val navController = LocalNavBackStackState.current
    Box(Modifier.fillMaxSize().clickable{
        NavCenter.navigate("https://www.app.cn/users/133/1")
    }) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "DetailScreen filter=$filter id=$id")
            Text(text = "BackStack depth = ${navController.screens.size}")
            Text(text = "entryId (last) = ${(navController.current as? DetailScreenDestination)?.entryId?.takeLast(6)}")
        }
    }
}

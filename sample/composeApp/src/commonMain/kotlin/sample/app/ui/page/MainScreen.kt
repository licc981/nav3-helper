package sample.app.ui.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aleyn.navigation.annotations.Screen
import com.aleyn.navigation.core.route.NavCenter
import com.navigation.child_first.ui.FirstHomeScreen
import com.navigation.child_first.ui.FirstHomeScreenDestination
import com.navigation.child_second.ui.SecondScreenDestination
import sample.app.ui.page.home.HomeScreenDestination

/**
 * @author : Aleyn
 * @date : 2026/1/20 15:39
 */

@Screen(route = "https://www.app.cn/compose-app/main", start = true)
@Composable
fun MainScreen() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {
            NavCenter.navigate("https://www.app.cn/users/active/110")
            //or
//            NavCenter.navigate(DetailScreenDestination(
//                detailId = 110,
//                name = "Aleyn"
//            ))
        }) {
            Text(text = "Go to Detail")
        }

        Button(onClick = {
            // multiInstance screen: 同一路由+同一参数连续入栈两次，
            // 每次实例持有独立的 entryId，两个页面互不共享状态
            NavCenter.navigate("https://www.app.cn/users/active/110")
            NavCenter.navigate("https://www.app.cn/users/active/110")
        }) {
            Text(text = "Push Detail twice")
        }

        Button(onClick = {
            NavCenter.navigate("https://www.app.cn/compose-app/home")
            //or
//            NavCenter.navigate(HomeScreenDestination)
        }) {
            Text(text = "Go to Home")
        }

        Button(onClick = {
            NavCenter.navigate("https://www.app.cn/child-first/main")
            //or
//            NavCenter.navigate(FirstHomeScreenDestination)
        }) {
            Text(text = "Go to ChildFirstModule")
        }
    }
}

package sample.app.ui.page.me

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aleyn.navigation.annotations.Screen
import kotlinx.serialization.Serializable

/**
 * @author : Aleyn
 * @date : 2026/1/20 15:38
 */

@Serializable
data class UserInfo(
    val userId: String,
    val avatarUrl: String,
    val nickName: String,
)


@Screen(route = "https://www.app.cn/compose-app/me")
@Composable
fun MeScreen(userInfo: UserInfo) {
    Column(
        Modifier.fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "MeScreen")
        Spacer(Modifier.height(10.dp))
        Text(text = "UserInfo :${userInfo}")
    }
}

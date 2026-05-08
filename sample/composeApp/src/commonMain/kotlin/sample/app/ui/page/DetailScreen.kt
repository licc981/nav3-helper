package sample.app.ui.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aleyn.navigation.annotations.Screen

/**
 * @author : Aleyn
 * @date : 2025/12/3 09:55
 */


@Screen(route = "https://www.app.cn/compose-app/detail")
@Composable
fun DetailScreen(
    detailId: Int = 0,
    name: String? = null
) {
    Box(Modifier.fillMaxSize()) {
        Text(
            text = "DetailScreen detailId=$detailId name=$name",
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

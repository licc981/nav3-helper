package sample.app.ui.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aleyn.navigation.annotations.Screen

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
    Box(Modifier.fillMaxSize()) {
        Text(
            text = "DetailScreen filter=$filter id=$id",
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

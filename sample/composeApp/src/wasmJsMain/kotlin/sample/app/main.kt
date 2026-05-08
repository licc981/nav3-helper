import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import sample.app.App
import sample.app.initNavigation

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initNavigation()
    val body = document.body ?: return
    ComposeViewport(body) {
        App()
    }
}

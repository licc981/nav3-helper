import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import sample.app.App
import sample.app.initNavigation

fun MainViewController(): UIViewController {
    initNavigation()
    return ComposeUIViewController { App() }
}

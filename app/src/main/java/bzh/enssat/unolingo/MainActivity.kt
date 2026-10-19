package bzh.enssat.unolingo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import bzh.enssat.unolingo.ui.navigation.UnolingoNavHost
import bzh.enssat.unolingo.ui.theme.UnolingoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UnolingoTheme {
                UnolingoNavHost()
            }
        }
    }
}

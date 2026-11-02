package fr.enssat.unolingo.mguezenn_mphongsa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import fr.enssat.unolingo.mguezenn_mphongsa.ui.navigation.UnolingoNavHost
import fr.enssat.unolingo.mguezenn_mphongsa.ui.theme.UnolingoTheme

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

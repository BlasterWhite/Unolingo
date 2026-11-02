package fr.enssat.unolingo.mguezenn_mphongsa

import android.app.Application

class UnolingoApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

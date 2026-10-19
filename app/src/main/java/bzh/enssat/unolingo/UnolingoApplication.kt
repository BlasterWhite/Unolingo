package bzh.enssat.unolingo

import android.app.Application

class UnolingoApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

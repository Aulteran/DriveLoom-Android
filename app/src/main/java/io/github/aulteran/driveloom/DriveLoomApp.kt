package io.github.aulteran.driveloom

import android.app.Application
import io.github.aulteran.driveloom.core.api.ApiClient
import io.github.aulteran.driveloom.core.api.DriveLoomApi
import io.github.aulteran.driveloom.core.session.AppSession
import io.github.aulteran.driveloom.core.session.SessionStore
import io.github.aulteran.driveloom.platform.EncryptedStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Holds the objects that live as long as the process: storage, API client and session. */
class DriveLoomApp : Application() {
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val store: SessionStore by lazy { SessionStore(EncryptedStore(this)) }
    val api: DriveLoomApi by lazy { DriveLoomApi(ApiClient(store, userAgent()), store) }
    val session: AppSession by lazy { AppSession(api, store) }

    private fun userAgent(): String {
        val version = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (_: Exception) {
            null
        }
        return "DriveLoom-Android/${version ?: "0"}"
    }
}

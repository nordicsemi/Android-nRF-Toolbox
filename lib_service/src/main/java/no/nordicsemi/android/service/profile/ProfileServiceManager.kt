package no.nordicsemi.android.service.profile

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

sealed interface ProfileServiceManager {
    suspend fun bindService(): ServiceApi
    fun unbindService()
    fun connectToPeripheral(deviceAddress: String, deviceName: String?)
}

internal class ProfileServiceManagerImp @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ProfileServiceManager {
    private var serviceConnection: ServiceConnection? = null

    override suspend fun bindService(): ServiceApi = suspendCancellableCoroutine { continuation ->
        val intent = Intent(context, ProfileService::class.java)

        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                val api = service as ServiceApi
                continuation.resume(api)
            }

            override fun onServiceDisconnected(p0: ComponentName?) {
                // Empty
            }

            override fun onBindingDied(p0: ComponentName?) {
                // Empty
            }
        }.also { connection ->
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun unbindService() {
        serviceConnection?.let { context.unbindService(it) }
        serviceConnection = null
    }

    override fun connectToPeripheral(deviceAddress: String, deviceName: String?) {
        val intent = Intent(context, ProfileService::class.java)
        intent.putExtra(ProfileService.DEVICE_ADDRESS, deviceAddress)
        intent.putExtra(ProfileService.DEVICE_NAME, deviceName)
        context.startService(intent)
    }
}

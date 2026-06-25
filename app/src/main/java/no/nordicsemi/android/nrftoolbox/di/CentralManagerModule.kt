package no.nordicsemi.android.nrftoolbox.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.native
import no.nordicsemi.kotlin.ble.core.android.AndroidEnvironment
import no.nordicsemi.kotlin.ble.environment.android.NativeAndroidEnvironment
import no.nordicsemi.kotlin.log.Log
import no.nordicsemi.kotlin.log.timber.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CentralManagerModule {

    companion object {

        @Provides
        @Singleton
        fun provideEnvironment(
            @ApplicationContext context: Context,
        ): NativeAndroidEnvironment = NativeAndroidEnvironment.getInstance(context, isNeverForLocationFlagSet = true)
        // TODO The environment should get closed when the app is closed to unregister broadcast receiver.

        @Provides
        @Singleton
        fun provideCentralManager(
            environment: NativeAndroidEnvironment,
            scope: CoroutineScope
        ): CentralManager = CentralManager.native(environment, scope)
            .apply { logger = Log.Sink.Timber { _, _ -> true} }
    }

    @Binds
    @Singleton
    abstract fun bindEnvironment(environment: NativeAndroidEnvironment): AndroidEnvironment
}
package no.nordicsemi.android.service.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import no.nordicsemi.android.service.profile.ProfileServiceManager
import no.nordicsemi.android.service.profile.ProfileServiceManagerImp

@Module
@InstallIn(ViewModelComponent::class)
object ProfileServiceManagerImpModule {

    @Provides
    @ViewModelScoped
    fun provideServiceManager(
        @ApplicationContext context: Context
    ): ProfileServiceManager = ProfileServiceManagerImp(context)

}

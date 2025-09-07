package edu.unikom.herbamedjabar.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class) // Or the appropriate component for your scope
object DispatchersModule {

    @Provides
    @IoDispatcher // Custom qualifier
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @MainDispatcher // Custom qualifier for Main dispatcher if needed
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @DefaultDispatcher // Custom qualifier for Default dispatcher if needed
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

// Define custom qualifiers to distinguish between different dispatchers
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class IoDispatcher

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class MainDispatcher

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class DefaultDispatcher

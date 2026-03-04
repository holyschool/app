package com.enderplusbayzuiship.edupage2.di

import com.edupage.api.Edupage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideEdupage(): Edupage = Edupage(timeoutSeconds = 30L)
}

package com.brafik.samples.di

import com.brafik.samples.server.Issuer
import com.brafik.samples.server.IssuerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object IssuerModule {

    @Singleton
    @Provides
    fun issuerModule(): Issuer = IssuerImpl()
}
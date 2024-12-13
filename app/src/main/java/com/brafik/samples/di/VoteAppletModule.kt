package com.brafik.samples.di

import com.brafik.samples.applet.VoteApplet
import com.brafik.samples.applet.VoteAppletImpl
import com.brafik.samples.smartcardio.CardSimulator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object VoteAppletModule {
    @Singleton
    @Provides
    fun voteAppletModule(simulator: CardSimulator): VoteApplet {
        return VoteAppletImpl(simulator)
    }
}

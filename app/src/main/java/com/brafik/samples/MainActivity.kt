package com.brafik.samples

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.brafik.samples.core.datastore.PreferencesRepository
import com.brafik.samples.core.ui.theme.SecureVotingTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startDestination = runBlocking {
            preferencesRepository.isOnboarded.first()
        }.let { onboarded ->
            if (onboarded) "home" else "create_pin"
        }

        setContent {
            SecureVotingTheme {
                MainNavHost(startDestination = startDestination)
            }
        }
    }
}
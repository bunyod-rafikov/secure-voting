package com.brafik.samples.ui.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brafik.samples.applet.VoteApplet
import com.brafik.samples.core.datastore.PreferencesRepository
import com.brafik.samples.server.Issuer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PinCodeViewModel @Inject constructor(
    private val applet: VoteApplet,
    private val issuer: Issuer,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    private val _state = MutableStateFlow(PinCodeState(PinCodeUiState.CREATE))
    val state: StateFlow<PinCodeState> = _state

    fun onNumClick(value: String, onComplete: () -> Unit) {
        if (_state.value.pinCode.length < 6) {
            _state.update { it.copy(pinCode = it.pinCode + value) }
        }
        if (_state.value.pinCode.length == 6)
            handlePinCompletion(onComplete)
    }

    fun onDeleteClick() {
        _state.update { it.copy(pinCode = it.pinCode.dropLast(1)) }
    }

    fun onBackToCreateClick() {
        _state.update {
            it.copy(uiState = PinCodeUiState.CREATE, createdPin = "", pinCode = "")
        }
    }

    private fun handlePinCompletion(onComplete: () -> Unit) = _state.update { currentState ->
        when (currentState.uiState) {
            PinCodeUiState.CREATE -> currentState.copy(
                uiState = PinCodeUiState.CONFIRM,
                createdPin = currentState.pinCode,
                pinCode = ""
            )

            PinCodeUiState.CONFIRM -> {
                if (currentState.pinCode == currentState.createdPin) {
                    setupUser(currentState.createdPin)
                }
                currentState.copy(pinCode = "").also { onComplete() }
            }

            PinCodeUiState.AUTHORISE -> {
                currentState.copy(pinCode = "").also { onComplete() }
            }
        }
    }

    private fun setupUser(pinCode: String) {
        viewModelScope.launch {
            val signedCsr = applet.createCsr(pinCode)
            val cert = issuer.issueCertificate(signedCsr.csr, signedCsr.signature)
            val activated = applet.fulfillCsr(cert.encoded)
            if (activated) setupElection() else throw Exception("Failed to fulfill CSR")
            preferencesRepository.setOnboarded(true)
        }
    }

    private fun setupElection() {
        issuer.generateElection()
            .also { applet.registerElectionData(it) }
    }
}
package com.brafik.samples.ui.election

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brafik.samples.applet.VoteApplet
import com.brafik.samples.server.Issuer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ElectionViewModel @Inject constructor(
    private val applet: VoteApplet,
    private val issuer: Issuer
) : ViewModel() {
    private val _state = MutableStateFlow(ElectionScreenState())
    val state: StateFlow<ElectionScreenState> = _state

    fun loadViewState() = viewModelScope.launch {
        val election = applet.getRegisteredElection(1)
        val receipt = applet.getRegisteredVote(1)
        _state.update {
            it.copy(
                electionData = election,
                voteReceipt = receipt?.value,
                selectedOption = receipt?.value?.data?.votedOption
            )
        }
        delay(300)
        _state.update {
            it.copy(
                uiState = when (receipt?.isSubmitted) {
                    true -> ElectionUiState.Submitted
                    false -> ElectionUiState.AwaitingSubmition
                    null -> ElectionUiState.Voting
                }
            )
        }
    }

    fun onVoteClick(option: String) {
        _state.update {
            it.copy(
                uiState = ElectionUiState.PinScreen,
                selectedOption = option
            )
        }
    }

    fun onNumClick(value: String) {
        val currentPin = _state.value.pinCodeState.pinCode
        if (currentPin.length < 6) {
            _state.update {
                it.copy(pinCodeState = it.pinCodeState.copy(pinCode = currentPin + value))
            }
        }
        if (currentPin.length + 1 == 6) {
            _state.value.selectedOption?.let { onVote() }
        }
    }

    fun onDeleteClick() {
        _state.update {
            val updatedPinCode = it.pinCodeState.pinCode.dropLast(1)
            it.copy(pinCodeState = it.pinCodeState.copy(pinCode = updatedPinCode))
        }
    }

    fun onBackToVote() = _state.update { it.copy(uiState = ElectionUiState.Voting) }

    fun onSubmitReceipt() = viewModelScope.launch {
        val finalReceipt = state.value.voteReceipt?.let { issuer.verifyVote(it) }
            ?: throw Exception("Nothing to upload")

        // Simulate online connection delay
        _state.update { it.copy(uiState = ElectionUiState.Loading) }
        delay(1000)

        applet.storeFinalReceipt(finalReceipt).also {
            loadViewState()
        }
    }

    private fun onVote() = viewModelScope.launch {
        val state = state.value
        if (state.electionData != null && state.selectedOption != null) {
            val receipt = applet.vote(
                state.electionData.electionId,
                state.selectedOption,
                state.pinCodeState.pinCode
            )

            // Simulate online connection delay
            _state.update { it.copy(uiState = ElectionUiState.Loading) }
            delay(1000)

            _state.update {
                it.copy(
                    uiState = ElectionUiState.AwaitingSubmition,
                    voteReceipt = receipt
                )
            }
        }
    }
}
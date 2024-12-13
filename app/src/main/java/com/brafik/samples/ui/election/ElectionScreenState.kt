package com.brafik.samples.ui.election

import com.brafik.samples.model.AppletSigned
import com.brafik.samples.model.ElectionData
import com.brafik.samples.model.IssuerSigned
import com.brafik.samples.model.VoteReceipt
import com.brafik.samples.ui.pin.PinCodeState
import com.brafik.samples.ui.pin.PinCodeUiState

data class ElectionScreenState(
    val uiState: ElectionUiState = ElectionUiState.Loading,
    val electionData: ElectionData? = null,
    val selectedOption: String? = null,
    val voteReceipt: AppletSigned<VoteReceipt>? = null,
    val pinCodeState: PinCodeState = PinCodeState(PinCodeUiState.AUTHORISE),
)

enum class ElectionUiState {
    Voting,
    PinScreen,
    AwaitingSubmition,
    Submitted,
    Loading,
}

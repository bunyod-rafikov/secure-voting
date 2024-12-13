package com.brafik.samples.ui.pin

data class PinCodeState(
    val uiState: PinCodeUiState = PinCodeUiState.CREATE,
    val pinCode: String = "",
    val createdPin: String = ""
)

enum class PinCodeUiState { CREATE, CONFIRM, AUTHORISE }

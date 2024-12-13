package com.brafik.samples.ui.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.brafik.samples.core.ui.BackButton
import com.brafik.samples.core.ui.NumPad
import com.brafik.samples.resetToHome

@Composable
fun PinCodeScreen(navController: NavController) {
    val viewModel: PinCodeViewModel = hiltViewModel()
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    PinCodeContent(
        uiState,
        onNumClick = { viewModel.onNumClick(it, navController::resetToHome) },
        onDeleteClick = viewModel::onDeleteClick,
        onBackClick = viewModel::onBackToCreateClick
    )
}

@Composable
fun PinCodeContent(
    state: PinCodeState,
    modifier: Modifier = Modifier,
    onNumClick: (String) -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
) {
    if (state.uiState == PinCodeUiState.CONFIRM || state.uiState == PinCodeUiState.AUTHORISE)
        BackButton(
            color = Color.Black,
            onClick = onBackClick
        )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
    ) {
        Text(
            text = when (state.uiState) {
                PinCodeUiState.CREATE -> "Create pincode"
                PinCodeUiState.CONFIRM -> "Confirm pincode"
                PinCodeUiState.AUTHORISE -> "Authorise"
            },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Black,
            modifier = modifier
                .padding(horizontal = 48.dp)
                .padding(top = 80.dp),
        )
        Text(
            text = "",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = modifier
                .padding(horizontal = 48.dp)
                .padding(top = 20.dp),
        )
        Dots(
            pinCode = state.pinCode,
            modifier = modifier.padding(vertical = 50.dp)
        )
        NumPad(
            onClick = onNumClick,
            onDelClick = onDeleteClick,
        )
    }
}

@Composable
fun Dots(
    pinCode: String,
    modifier: Modifier = Modifier,
) {
    Row(horizontalArrangement = Arrangement.Center, modifier = modifier) {
        repeat(6) { index ->
            Dot(isSelected = pinCode.length <= index)
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
private fun Dot(isSelected: Boolean = false) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (isSelected) Color.LightGray else Color.DarkGray)
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewPinCodeScreen() {
    PinCodeContent(PinCodeState())
}
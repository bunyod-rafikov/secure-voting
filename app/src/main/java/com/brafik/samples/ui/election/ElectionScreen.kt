package com.brafik.samples.ui.election

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brafik.samples.core.ui.LoadingView
import com.brafik.samples.core.ui.OptionButton
import com.brafik.samples.model.ElectionData
import com.brafik.samples.model.VoteReceipt
import com.brafik.samples.ui.pin.PinCodeContent
import com.brafik.samples.utils.toReadableString
import java.util.Date

@Composable
fun ElectionScreen() {
    val viewModel: ElectionViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadViewState()
    }

    when (state.uiState) {
        ElectionUiState.Voting -> {
            ElectionContent(
                electionData = state.electionData ?: return,
                onVote = viewModel::onVoteClick
            )
        }

        ElectionUiState.PinScreen -> {
            PinCodeContent(
                state = state.pinCodeState,
                onNumClick = viewModel::onNumClick,
                onDeleteClick = viewModel::onDeleteClick,
                onBackClick = viewModel::onBackToVote
            )
        }

        ElectionUiState.AwaitingSubmition -> {
            ElectionContent(
                electionData = state.electionData,
                selected = state.selectedOption,
                awaitingSubmition = true,
                onSubmit = viewModel::onSubmitReceipt
            )
        }

        ElectionUiState.Submitted -> {
            SubmittedElectionContent(
                electionData = state.electionData ?: return,
                voteReceipt = state.voteReceipt?.data ?: return,
            )
        }

        ElectionUiState.Loading -> {
            LoadingView(true)
        }
    }
}

@Composable
fun ElectionContent(
    electionData: ElectionData?,
    selected: String? = null,
    awaitingSubmition: Boolean = false,
    onVote: (String) -> Unit = {},
    onSubmit: () -> Unit = {}
) {
    if (electionData == null) return
    var selectedOption by remember { mutableStateOf(selected) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(
            text = electionData.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.weight(0.5f))

        Text(
            text = if (!awaitingSubmition) "Choose the candidate" else "You have selected",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            electionData.options.forEach { option ->
                OptionButton(
                    option = option,
                    isSelected = selectedOption == option,
                    onOptionSelected = { if (!awaitingSubmition) selectedOption = option }
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        Button(
            onClick = {
                selectedOption?.let {
                    if (!awaitingSubmition) onVote(it) else onSubmit()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(10.dp),
            enabled = selectedOption != null
        ) {
            Text(
                text = if (!awaitingSubmition) "Vote" else "Submit",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun SubmittedElectionContent(electionData: ElectionData, voteReceipt: VoteReceipt) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(
            text = electionData.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.weight(0.3f))

        Text(
            text = "Submitted vote",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "From " + Date(voteReceipt.timestamp).toReadableString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 20.dp)
        )


        OptionButton(
            option = voteReceipt.votedOption,
            isSelected = true,
            onOptionSelected = {}
        )

        Spacer(modifier = Modifier.weight(0.5f))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewElectionContent() {
    ElectionContent(
        ElectionData(
            electionId = 1,
            title = "Election 1",
            expiration = System.currentTimeMillis() * 3600_000L,
            options = listOf("Option 1", "Option 2", "Option 3")
        )
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewSubmittedElectionContent() {
    SubmittedElectionContent(
        ElectionData(
            electionId = 1,
            title = "Election 1",
            expiration = System.currentTimeMillis() * 3600_000L,
            options = listOf("Option 1", "Option 2", "Option 3")
        ),
        VoteReceipt(
            electionId = 2,
            votedOption = "Option A",
            timestamp = System.currentTimeMillis(),
        )
    )
}
package com.brafik.samples.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

@Composable
fun LoadingView(isLoading: Boolean = false, status: String = "") {
    if (isLoading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .zIndex(100f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.weight(0.5f))
            CircularProgressIndicator(color = Color.Gray)
            Text(
                status,
                fontSize = 20.sp,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(0.5f))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLoadingView() {
    LoadingView(true, "Setting up")
}
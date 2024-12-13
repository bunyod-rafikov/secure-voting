package com.brafik.samples.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun BackButton(
    color: Color = Color.White,
    onClick: () -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Button(
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            onClick = onClick
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = color
            )
        }
    }
}
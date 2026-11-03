package com.lexterlabra.puzzle

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

const val APP_NAME = "Slide Puzzle"

@Composable
fun App() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(APP_NAME) }
}

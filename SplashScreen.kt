package com.edm.downloadmanager.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.7f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(500, easing = FastOutSlowInEasing))
        delay(700)
        onFinished()
    }

    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Bolt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(72.dp).scale(scale.value)
            )
            Spacer(Modifier.height(12.dp))
            Text("EDM", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Text("Fast. Smart. Reliable.", color = Color.White.copy(alpha = 0.85f))
        }
    }
}

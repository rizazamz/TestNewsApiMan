package com.example.newsapp.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import com.example.newsapp.R

@Composable
fun NewsTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = colorResource(R.color.news_primary), secondary = colorResource(R.color.news_secondary))
    } else {
        lightColorScheme(primary = colorResource(R.color.news_primary), secondary = colorResource(R.color.news_secondary))
    }
    MaterialTheme(colorScheme = colors, content = content)
}

package com.example.newsapp.presentation.articles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.newsapp.R
import com.example.newsapp.domain.model.NewsArticle

@Composable
fun ArticleCard(article: NewsArticle, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        ArticleThumbnail(article.imageUrl)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(article.title, style = MaterialTheme.typography.titleLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
            article.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Text(
                formatPublishedDate(article.publishedAt),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ArticleThumbnail(url: String?) {
    if (url.isNullOrBlank()) {
        ThumbnailFallback()
    } else {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(180.dp),
            contentScale = ContentScale.Crop,
            loading = { Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() } },
            error = { ThumbnailFallback() },
        )
    }
}

@Composable
private fun ThumbnailFallback() {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.image_unavailable), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

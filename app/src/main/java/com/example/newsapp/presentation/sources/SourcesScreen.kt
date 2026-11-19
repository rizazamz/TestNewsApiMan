package com.example.newsapp.presentation.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.newsapp.R
import com.example.newsapp.domain.model.NewsSource
import com.example.newsapp.presentation.categories.labelResource
import com.example.newsapp.presentation.common.NewsSearchField
import com.example.newsapp.presentation.common.PagingFooter
import com.example.newsapp.presentation.common.PagingRefreshState
import com.example.newsapp.presentation.common.ScreenToolbar

@Composable
fun SourcesRoute(viewModel: SourcesViewModel, onBack: () -> Unit, onSourceClick: (NewsSource) -> Unit) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val sources = viewModel.sources.collectAsLazyPagingItems()
    SourcesScreen(stringResource(viewModel.category.labelResource()), query, viewModel::onQueryChange, sources, onBack, onSourceClick)
}

@Composable
fun SourcesScreen(
    category: String,
    query: String,
    onQueryChange: (String) -> Unit,
    sources: LazyPagingItems<NewsSource>,
    onBack: () -> Unit,
    onSourceClick: (NewsSource) -> Unit,
) {
    Scaffold(topBar = { ScreenToolbar(category, onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            NewsSearchField(query, onQueryChange, stringResource(R.string.search_sources))
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (sources.itemCount == 0) {
                    item {
                        PagingRefreshState(
                            sources.loadState.refresh,
                            if (query.isBlank()) stringResource(R.string.empty_sources)
                            else stringResource(R.string.no_matching_sources),
                            sources::retry,
                        )
                    }
                } else {
                    item { PagingFooter(sources.loadState.refresh, sources::retry) }
                    items(sources.itemCount, key = sources.itemKey { it.id }) { index ->
                        sources[index]?.let { source ->
                            SourceCard(source, onClick = { onSourceClick(source) })
                        }
                    }
                    item { PagingFooter(sources.loadState.append, sources::retry) }
                }
            }
        }
    }
}

@Composable
private fun SourceCard(source: NewsSource, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(source.name, style = MaterialTheme.typography.titleLarge)
            source.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Text(
                stringResource(R.string.source_metadata, source.language.uppercase(), source.country.uppercase()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

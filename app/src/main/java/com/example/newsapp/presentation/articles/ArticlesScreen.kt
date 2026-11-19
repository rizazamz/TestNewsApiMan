package com.example.newsapp.presentation.articles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.newsapp.R
import com.example.newsapp.domain.model.NewsArticle
import com.example.newsapp.presentation.common.NewsSearchField
import com.example.newsapp.presentation.common.PagingFooter
import com.example.newsapp.presentation.common.PagingRefreshState
import com.example.newsapp.presentation.common.ScreenToolbar

@Composable
fun ArticlesRoute(viewModel: ArticlesViewModel, onBack: () -> Unit, onArticleClick: (NewsArticle) -> Unit) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val articles = viewModel.articles.collectAsLazyPagingItems()
    ArticlesScreen(viewModel.sourceName, query, viewModel::onQueryChange, articles, onBack, onArticleClick)
}

@Composable
fun ArticlesScreen(
    sourceName: String,
    query: String,
    onQueryChange: (String) -> Unit,
    articles: LazyPagingItems<NewsArticle>,
    onBack: () -> Unit,
    onArticleClick: (NewsArticle) -> Unit,
) {
    Scaffold(topBar = { ScreenToolbar(sourceName, onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            NewsSearchField(query, onQueryChange, stringResource(R.string.search_articles))
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (articles.itemCount == 0) {
                    item {
                        PagingRefreshState(
                            articles.loadState.refresh,
                            if (query.isBlank()) stringResource(R.string.empty_articles)
                            else stringResource(R.string.no_matching_articles),
                            articles::retry,
                        )
                    }
                } else {
                    item { PagingFooter(articles.loadState.refresh, articles::retry) }
                    items(articles.itemCount) { index ->
                        articles[index]?.let { article ->
                            ArticleCard(article, onClick = { onArticleClick(article) })
                        }
                    }
                    item { PagingFooter(articles.loadState.append, articles::retry) }
                }
            }
        }
    }
}

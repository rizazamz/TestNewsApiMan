package com.example.newsapp.presentation.articles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.paging.cachedIn
import com.example.newsapp.domain.repository.NewsRepository
import com.example.newsapp.presentation.common.debouncedSearch
import com.example.newsapp.presentation.navigation.Articles
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ArticlesViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    repository: NewsRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<Articles>()
    val sourceName = route.sourceName
    val query = savedStateHandle.getStateFlow("articles_query", "")
    val articles = query.debouncedSearch()
        .flatMapLatest { repository.getArticles(route.sourceId, it) }
        .cachedIn(viewModelScope)

    fun onQueryChange(value: String) {
        savedStateHandle["articles_query"] = value
    }
}

package com.example.newsapp.presentation.sources

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.paging.cachedIn
import com.example.newsapp.domain.model.NewsCategory
import com.example.newsapp.domain.repository.NewsRepository
import com.example.newsapp.presentation.common.debouncedSearch
import com.example.newsapp.presentation.navigation.Sources
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SourcesViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    repository: NewsRepository,
) : ViewModel() {
    val category = NewsCategory.fromApiValue(savedStateHandle.toRoute<Sources>().category)
    val query = savedStateHandle.getStateFlow("sources_query", "")
    val sources = query.debouncedSearch()
        .flatMapLatest { repository.getSources(category, it) }
        .cachedIn(viewModelScope)

    fun onQueryChange(value: String) {
        savedStateHandle["sources_query"] = value
    }
}

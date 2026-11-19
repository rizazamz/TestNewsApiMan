package com.example.newsapp.presentation.categories

import androidx.lifecycle.ViewModel
import com.example.newsapp.domain.model.NewsCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor() : ViewModel() {
    val categories: List<NewsCategory> = NewsCategory.entries.toList()
}

package com.example.newsapp.presentation.categories

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.newsapp.R
import com.example.newsapp.domain.model.NewsCategory
import com.example.newsapp.presentation.common.ScreenToolbar

@StringRes
fun NewsCategory.labelResource(): Int = when (this) {
    NewsCategory.BUSINESS -> R.string.category_business
    NewsCategory.ENTERTAINMENT -> R.string.category_entertainment
    NewsCategory.GENERAL -> R.string.category_general
    NewsCategory.HEALTH -> R.string.category_health
    NewsCategory.SCIENCE -> R.string.category_science
    NewsCategory.SPORTS -> R.string.category_sports
    NewsCategory.TECHNOLOGY -> R.string.category_technology
}

@StringRes
private fun NewsCategory.symbolResource(): Int = when (this) {
    NewsCategory.BUSINESS -> R.string.symbol_business
    NewsCategory.ENTERTAINMENT -> R.string.symbol_entertainment
    NewsCategory.GENERAL -> R.string.symbol_general
    NewsCategory.HEALTH -> R.string.symbol_health
    NewsCategory.SCIENCE -> R.string.symbol_science
    NewsCategory.SPORTS -> R.string.symbol_sports
    NewsCategory.TECHNOLOGY -> R.string.symbol_technology
}

@Composable
fun CategoriesRoute(viewModel: CategoriesViewModel, onCategoryClick: (NewsCategory) -> Unit) {
    CategoriesScreen(viewModel.categories, onCategoryClick)
}

@Composable
fun CategoriesScreen(categories: List<NewsCategory>, onCategoryClick: (NewsCategory) -> Unit) {
    Scaffold(topBar = { ScreenToolbar(stringResource(R.string.app_name)) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                stringResource(R.string.categories_title), style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            Text(
                stringResource(R.string.categories_subtitle),
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(156.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(categories, key = { it.apiValue }) { category ->
                    CategoryCard(category, onClick = { onCategoryClick(category) })
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(category: NewsCategory, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 128.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(category.symbolResource()), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(category.labelResource()), style = MaterialTheme.typography.titleMedium)
        }
    }
}

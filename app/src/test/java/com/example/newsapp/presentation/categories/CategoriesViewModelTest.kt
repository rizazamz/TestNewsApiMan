package com.example.newsapp.presentation.categories

import com.example.newsapp.domain.model.NewsCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class CategoriesViewModelTest {
    @Test
    fun `categories contain exactly the seven official values in display order`() {
        val viewModel = CategoriesViewModel()
        assertEquals(
            listOf(
                NewsCategory.BUSINESS, NewsCategory.ENTERTAINMENT, NewsCategory.GENERAL,
                NewsCategory.HEALTH, NewsCategory.SCIENCE, NewsCategory.SPORTS, NewsCategory.TECHNOLOGY,
            ),
            viewModel.categories,
        )
        assertEquals(7, viewModel.categories.distinct().size)
    }

    @Test
    fun `static category state is available immediately and stable across reads and recreation`() {
        val viewModel = CategoriesViewModel()
        assertSame(viewModel.categories, viewModel.categories)
        assertEquals(viewModel.categories, CategoriesViewModel().categories)
        assertFalse(viewModel.categories.isEmpty())
    }
}

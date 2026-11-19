package com.example.newsapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.newsapp.presentation.articles.ArticlesRoute
import com.example.newsapp.presentation.categories.CategoriesRoute
import com.example.newsapp.presentation.detail.ArticleDetailRoute
import com.example.newsapp.presentation.sources.SourcesRoute

@Composable
fun NewsNavHost() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Categories) {
        composable<Categories> {
            CategoriesRoute(hiltViewModel()) { navController.navigate(Sources(it.apiValue)) }
        }
        composable<Sources> {
            SourcesRoute(hiltViewModel(), { navController.popBackStack() }) {
                navController.navigate(Articles(it.id, it.name))
            }
        }
        composable<Articles> {
            ArticlesRoute(hiltViewModel(), { navController.popBackStack() }) {
                navController.navigate(ArticleDetail(it.url))
            }
        }
        composable<ArticleDetail> {
            ArticleDetailRoute(hiltViewModel(), onBack = { navController.popBackStack() })
        }
    }
}

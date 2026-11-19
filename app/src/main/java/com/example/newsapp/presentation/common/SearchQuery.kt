package com.example.newsapp.presentation.common

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@OptIn(FlowPreview::class)
fun Flow<String>.debouncedSearch(): Flow<String> =
    map { it.trim() }.debounce(400).distinctUntilChanged()

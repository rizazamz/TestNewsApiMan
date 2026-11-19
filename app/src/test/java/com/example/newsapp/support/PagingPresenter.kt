package com.example.newsapp.support

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.cash.turbine.ReceiveTurbine
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

class TestPagingPresenter<T : Any>(context: CoroutineContext) : PagingDataPresenter<T>(context) {
    val events = mutableListOf<PagingDataEvent<T>>()

    override suspend fun presentPagingDataEvent(event: PagingDataEvent<T>) {
        events += event
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T : Any> TestScope.present(flow: Flow<PagingData<T>>): TestPagingPresenter<T> {
    val presenter = TestPagingPresenter<T>(StandardTestDispatcher(testScheduler))
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        flow.collectLatest { presenter.collectFrom(it) }
    }
    return presenter
}

suspend fun ReceiveTurbine<CombinedLoadStates>.awaitRefresh(
    predicate: (LoadState) -> Boolean,
): LoadState {
    while (true) {
        val state = awaitItem().refresh
        if (predicate(state)) return state
    }
}

fun <T : Any> pagingFlow(load: suspend () -> List<T>): Flow<PagingData<T>> = Pager(
    PagingConfig(pageSize = 20, initialLoadSize = 20, enablePlaceholders = false),
) {
    object : PagingSource<Int, T>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> = try {
            LoadResult.Page(load(), prevKey = null, nextKey = null)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            LoadResult.Error(error)
        }

        override fun getRefreshKey(state: PagingState<Int, T>): Int? = state.anchorPosition
    }
}.flow

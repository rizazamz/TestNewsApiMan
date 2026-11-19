package com.example.newsapp.support

import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class PagingTestRule(private val dispatcher: TestDispatcher = StandardTestDispatcher()) : TestWatcher() {
    private val store = ViewModelStore()
    private var nextKey = 0

    @Suppress("DEPRECATION")
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
        mockkStatic(Log::class)
        every { Log.isLoggable(any(), any()) } returns false
        mockkStatic("androidx.core.os.BundleKt")
        every { bundleOf(*anyVararg()) } answers {
            val values = firstArg<Array<out Pair<String, Any?>>>().toMap()
            val bundle = mockk<Bundle>()
            every { bundle.get(any()) } answers { values[firstArg<String>()] }
            every { bundle.getString(any()) } answers { values[firstArg<String>()] as? String }
            bundle
        }
    }

    fun <T : ViewModel> retain(viewModel: T): T = viewModel.also { store.put("vm-${nextKey++}", it) }

    override fun finished(description: Description) {
        store.clear()
        unmockkStatic(Log::class)
        unmockkStatic("androidx.core.os.BundleKt")
        Dispatchers.resetMain()
    }
}

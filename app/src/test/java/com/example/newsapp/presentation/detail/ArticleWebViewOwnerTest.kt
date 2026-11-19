package com.example.newsapp.presentation.detail

import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class ArticleWebViewOwnerTest {
    private val view = mockk<WebView>(relaxed = true)
    private val owner = ArticleWebViewOwner()
    private val savedHistory = mutableListOf<Bundle>()

    @Test
    fun `dispose saves history clears callbacks detaches view and destroys exactly once`() {
        val parent = mockk<ViewGroup>(relaxed = true)
        every { view.parent } returns parent
        every { view.saveState(any()) } returns mockk()
        owner.attach(view)
        owner.destroy { savedHistory += it }
        owner.release(view) { savedHistory += it }
        owner.destroy { savedHistory += it }
        assertNull(owner.view)
        assertEquals(1, savedHistory.size)
        verify(exactly = 1) { view.webChromeClient = null }
        verify(exactly = 1) { view.webViewClient = any() }
        verify(exactly = 1) { view.destroy() }
        verifyOrder {
            view.saveState(any())
            view.stopLoading()
            parent.removeView(view)
            view.removeAllViews()
            view.destroy()
        }
    }

    @Test
    fun `AndroidView release followed by dispose does not destroy twice`() {
        every { view.saveState(any()) } returns null
        owner.attach(view)
        owner.release(view) { savedHistory += it }
        owner.destroy { savedHistory += it }
        assertNull(owner.view)
        assertEquals(0, savedHistory.size)
        verify(exactly = 1) { view.destroy() }
    }

    @Test
    fun `failure saving history still clears retained reference and destroys WebView`() {
        every { view.saveState(any()) } throws IOException("Cannot save history")
        owner.attach(view)
        assertThrows(IOException::class.java) { owner.destroy { savedHistory += it } }
        assertNull(owner.view)
        assertEquals(0, savedHistory.size)
        verify(exactly = 1) { view.destroy() }
    }

    @Test
    fun `failure detaching WebView still destroys native resources`() {
        every { view.saveState(any()) } returns null
        every { view.stopLoading() } throws IllegalStateException("Renderer unavailable")
        owner.attach(view)
        assertThrows(IllegalStateException::class.java) { owner.destroy { savedHistory += it } }
        assertNull(owner.view)
        verify(exactly = 1) { view.destroy() }
    }

    @Test
    fun `pause snapshots history without destroying current WebView`() {
        every { view.saveState(any()) } returns mockk()
        owner.attach(view)
        owner.saveHistory { savedHistory += it }
        assertSame(view, owner.view)
        assertEquals(1, savedHistory.size)
        verify(exactly = 0) { view.destroy() }
        owner.destroy { savedHistory += it }
    }
}

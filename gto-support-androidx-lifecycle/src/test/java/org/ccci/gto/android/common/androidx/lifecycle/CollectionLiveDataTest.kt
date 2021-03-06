package org.ccci.gto.android.common.androidx.lifecycle

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.empty
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

abstract class CollectionLiveDataTest : BaseLiveDataTest() {
    abstract val liveData: CollectionLiveData<String, out Collection<String>>

    @Before
    fun setupLiveData() {
        liveData.observeForever(observer)
        reset(observer)
    }

    @Test
    fun testAdd() {
        assertTrue(liveData.add("a"))
        verify(observer).onChanged(any())
        assertThat(liveData.value, containsInAnyOrder("a"))
    }

    @Test
    fun testAddAll() {
        assertTrue(liveData.addAll(setOf("a", "b", "c")))
        verify(observer).onChanged(any())
        assertThat(liveData.value, containsInAnyOrder("a", "b", "c"))
    }

    @Test
    fun testAddAllEmpty() {
        assertFalse(liveData.addAll(emptySet()))
        verify(observer, never()).onChanged(any())
        assertThat(liveData.value, empty())
    }

    @Test
    fun testPlusAssign() {
        liveData += "a"
        verify(observer).onChanged(any())
        assertThat(liveData.value, containsInAnyOrder("a"))
    }

    @Test
    fun testPlusAssignCollection() {
        liveData += setOf("a", "b", "c")
        verify(observer).onChanged(any())
        assertThat(liveData.value, containsInAnyOrder("a", "b", "c"))
    }

    @Test
    fun testPlusAssignCollectionEmpty() {
        liveData += emptySet()
        verify(observer, never()).onChanged(any())
        assertThat(liveData.value, empty())
    }

    @Test
    fun testRemove() {
        liveData += setOf("a", "b")
        reset(observer)

        assertTrue(liveData.remove("a"))
        verify(observer).onChanged(any())
        assertThat(liveData.value, containsInAnyOrder("b"))
    }

    @Test
    fun testRemoveMissing() {
        liveData += setOf("a")
        reset(observer)

        assertFalse(liveData.remove("b"))
        verify(observer, never()).onChanged(any())
        assertThat(liveData.value, containsInAnyOrder("a"))
    }

    @Test
    fun testRemoveAll() {
        liveData += setOf("a", "b")
        reset(observer)

        assertTrue(liveData.removeAll(setOf("b", "c")))
        verify(observer).onChanged(any())
        assertThat(liveData.value, containsInAnyOrder("a"))
    }

    @Test
    fun testRemoveAllMissing() {
        liveData += setOf("a")
        reset(observer)

        assertFalse(liveData.removeAll(setOf("b", "c")))
        verify(observer, never()).onChanged(any())
        assertThat(liveData.value, containsInAnyOrder("a"))
    }

    @Test
    fun testMinusAssign() {
        liveData += setOf("a", "b")
        reset(observer)

        liveData -= "a"
        verify(observer).onChanged(any())
        assertThat(liveData.value, containsInAnyOrder("b"))
    }

    @Test
    fun testMinusAssignMissing() {
        liveData += setOf("a")
        reset(observer)

        liveData -= "b"
        verify(observer, never()).onChanged(any())
        assertThat(liveData.value, containsInAnyOrder("a"))
    }

    @Test
    fun testMinusAssignCollection() {
        liveData += setOf("a", "b")
        reset(observer)

        liveData -= setOf("b", "c")
        verify(observer).onChanged(any())
        assertThat(liveData.value, containsInAnyOrder("a"))
    }

    @Test
    fun testMinusAssignCollectionMissing() {
        liveData += setOf("a")
        reset(observer)

        liveData -= setOf("b", "c")
        verify(observer, never()).onChanged(any())
        assertThat(liveData.value, containsInAnyOrder("a"))
    }

    @Test
    fun testClear() {
        liveData += setOf("a")
        reset(observer)

        liveData.clear()
        verify(observer).onChanged(any())
        assertThat(liveData.value, empty())
    }

    @Test
    fun testClearEmpty() {
        liveData.clear()
        verify(observer).onChanged(any())
        assertThat(liveData.value, empty())
    }
}

class ListLiveDataTest : CollectionLiveDataTest() {
    override val liveData = ListLiveData<String>()
}

class SetLiveDataTest : CollectionLiveDataTest() {
    override val liveData = SetLiveData<String>()
}

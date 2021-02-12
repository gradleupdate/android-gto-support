package org.ccci.gto.android.common.picasso.widget

import android.graphics.drawable.Drawable
import android.widget.TextView
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.clearInvocations
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.nullableArgumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

private const val WIDTH = 5
private const val HEIGHT = 10

class TextViewDrawableEndTargetTest {
    private lateinit var view: TextView
    private lateinit var compoundDrawables: Array<Drawable>
    private lateinit var target: TextViewDrawableEndTarget
    private lateinit var drawable: Drawable

    @Before
    fun setup() {
        compoundDrawables = Array(4) { mock() }
        drawable = mock {
            on { intrinsicWidth } doReturn WIDTH
            on { intrinsicHeight } doReturn HEIGHT
        }
        view = mock {
            on { compoundDrawablesRelative } doReturn compoundDrawables
        }
        target = TextViewDrawableEndTarget.of(view)
        clearInvocations(drawable, view)
    }

    @Test
    fun verifyTargetIsSaved() {
        val target = TextViewDrawableEndTarget.of(view)
        verify(view).setTag(any(), eq(target))
    }

    @Test
    fun verifyTargetIsReused() {
        whenever(view.getTag(any())).thenReturn(target)

        assertSame(target, TextViewDrawableEndTarget.of(view))
        verify(view).getTag(any())
    }

    @Test
    fun verifyUpdateDrawableSetsDrawable() {
        target.updateDrawable(drawable)
        verify(drawable).setBounds(0, 0, WIDTH, HEIGHT)
        argumentCaptor<Drawable> {
            verify(view).setCompoundDrawablesRelative(
                eq(compoundDrawables[0]),
                eq(compoundDrawables[1]),
                capture(),
                eq(compoundDrawables[3])
            )
            assertSame(drawable, firstValue)
        }
    }

    @Test
    fun verifyUpdateDrawableClearsDrawableWhenDrawableIsNull() {
        target.updateDrawable(null)
        nullableArgumentCaptor<Drawable> {
            verify(view).setCompoundDrawablesRelative(
                eq(compoundDrawables[0]),
                eq(compoundDrawables[1]),
                capture(),
                eq(compoundDrawables[3])
            )
            assertNull(firstValue)
        }
    }
}

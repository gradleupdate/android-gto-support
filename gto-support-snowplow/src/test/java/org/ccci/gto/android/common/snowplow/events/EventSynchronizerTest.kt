package org.ccci.gto.android.common.snowplow.events

import com.nhaarman.mockitokotlin2.mock
import com.snowplowanalytics.snowplow.tracker.events.Event
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventSynchronizerTest {
    private val threads = 50
    private val iterations = 50

    @Test
    fun verifyMutualExclusionOfLock() = runBlocking {
        var count = 0
        val mutex = Mutex()
        coroutineScope {
            repeat(threads) {
                launch(Dispatchers.IO) {
                    repeat(iterations) {
                        val event = mock<Event>()
                        EventSynchronizer.lockFor(event)
                        assertEquals(0, EventSynchronizer.semaphore.availablePermits())
                        assertTrue("Mutual Exclusion not maintained!", mutex.tryLock(this))
                        mutex.unlock(this)
                        count++
                        EventSynchronizer.unlockFor(event)
                        assertTrue(EventSynchronizer.semaphore.availablePermits() <= 1)
                    }
                }
            }
        }

        assertEquals(threads * iterations, count)
    }

    @Test
    fun verifyLockTimeout() = runBlocking {
        var count = 0
        val mutex = Mutex()
        EventSynchronizer.lockTimeout = 2

        coroutineScope {
            repeat(threads) {
                launch(Dispatchers.IO) {
                    repeat(iterations) {
                        val event = mock<Event>()
                        EventSynchronizer.lockFor(event)
                        assertEquals(0, EventSynchronizer.semaphore.availablePermits())
                        assertTrue("Mutual Exclusion not maintained!", mutex.tryLock(this))
                        mutex.unlock(this)
                        count++
                        delay(Random.nextLong(5))
                        EventSynchronizer.unlockFor(event)
                        assertTrue(EventSynchronizer.semaphore.availablePermits() <= 1)
                    }
                }
            }
        }

        assertEquals(threads * iterations, count)
    }
}
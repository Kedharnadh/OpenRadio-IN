package dev.openradio.android.playback

import androidx.media3.common.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers [AppPlayer.rotateTo], the reordering that puts the station a controller
 * actually picked at the index it asked to start from.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppPlayerQueueRotationTest {
    private fun item(id: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setUri("https://example.com/$id.mp3")
            .build()

    private fun queue(vararg ids: String): List<MediaItem> = ids.map { item(it) }

    private fun idsOf(items: List<MediaItem>): List<String> = items.map { it.mediaId }

    @Test
    fun `tapped station moves to the requested start index`() {
        val result = AppPlayer.rotateTo(queue("a", "b", "c", "d"), targetIndex = 2, fromIndex = 0)
        assertEquals("c", result[0].mediaId)
    }

    @Test
    fun `rotation keeps the whole queue and every station exactly once`() {
        val source = queue("a", "b", "c", "d", "e")
        val result = AppPlayer.rotateTo(source, targetIndex = 3, fromIndex = 0)
        assertEquals(source.size, result.size)
        assertEquals(source.map { it.mediaId }.toSet(), result.map { it.mediaId }.toSet())
    }

    @Test
    fun `immediate neighbours stay adjacent to the tapped station`() {
        val result = AppPlayer.rotateTo(queue("a", "b", "c", "d", "e"), targetIndex = 2, fromIndex = 0)
        assertEquals(listOf("c", "d", "e", "a", "b"), idsOf(result))
    }

    @Test
    fun `filler items are placed before the tapped station for a non-zero start index`() {
        val source = queue("a", "b", "c", "d")
        val result = AppPlayer.rotateTo(source, targetIndex = 1, fromIndex = 2)
        assertEquals("b", result[2].mediaId)
        assertEquals(source.map { it.mediaId }.toSet(), result.map { it.mediaId }.toSet())
    }

    @Test
    fun `queue is untouched when the target already sits at the start index`() {
        val source = queue("a", "b", "c")
        assertEquals(source, AppPlayer.rotateTo(source, targetIndex = 1, fromIndex = 1))
    }

    @Test
    fun `out of range start index leaves the queue untouched`() {
        val source = queue("a", "b", "c")
        assertEquals(source, AppPlayer.rotateTo(source, targetIndex = 0, fromIndex = 9))
    }
}

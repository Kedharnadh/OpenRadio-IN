package dev.openradio.android.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager

/**
 * Pauses playback when the active audio sink goes away — a Bluetooth stereo
 * disconnects or wired headphones are unplugged — so the station doesn't keep
 * playing out of the phone speaker, matching how other music apps behave.
 *
 * The system only broadcasts `AUDIO_BECOMING_NOISY` while audio is actually
 * routed to the disconnected device, so a radio that is already paused is
 * never affected. While casting, the phone is not the audio sink and playback
 * is left untouched.
 */
class AudioBecomingNoisyReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != AudioManager.ACTION_AUDIO_BECOMING_NOISY) return
        if (AppPlayer.state.value.castActive) return
        AppPlayer.pause()
    }
}

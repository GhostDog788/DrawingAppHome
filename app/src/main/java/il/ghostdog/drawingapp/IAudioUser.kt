package il.ghostdog.drawingapp

import android.app.Dialog
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

interface IAudioUser {
    var soundPool: SoundPool
    var clickSoundId: Int
    var errorSoundId: Int
    var softClickSoundId: Int
    //contains common sounds able to add more if needed
    // soundPool.load(~context~, R.raw.~id~, 1)

    fun setUpSoundPool(context: Context){
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(audioAttributes)
            .build()
        clickSoundId = soundPool.load(context, R.raw.click_louder, 1)
        softClickSoundId = soundPool.load(context, R.raw.short_click_louder, 1)
        errorSoundId = soundPool.load(context, R.raw.error_louder, 1)
        //to play a sound: soundPool.play(clickSoundId!!, 1F, 1F,0,0, 1F)
    }
}
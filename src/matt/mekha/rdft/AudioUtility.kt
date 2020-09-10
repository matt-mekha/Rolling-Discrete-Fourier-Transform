package matt.mekha.rdft

import java.io.File
import java.nio.ByteBuffer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

data class AudioSource(
        val duration: Double,
        val sampleRate: Int,
        val sampleFunction: Function,
        val close: () -> Unit
)

fun loadAudioFile(filePath: String, bytesPerSample: Int) : AudioSource {
    val audioInputStreamEncoded: AudioInputStream = AudioSystem.getAudioInputStream(File(filePath))
    val audioFormatEncoded = audioInputStreamEncoded.format
    val audioFormatDecoded = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            audioFormatEncoded.sampleRate,
            bytesPerSample * 8,
            1,
            bytesPerSample,
            audioFormatEncoded.sampleRate,
            true
    )
    val audioInputStreamDecoded = AudioSystem.getAudioInputStream(
            audioFormatDecoded,
            audioInputStreamEncoded
    )

    return AudioSource(
            audioInputStreamDecoded.frameLength.toDouble() / audioFormatDecoded.frameRate,
            audioFormatDecoded.sampleRate.toInt(),
            {
                when(bytesPerSample) {
                    1 -> audioInputStreamDecoded.read().toDouble() / 128.0 - 1.0
                    2 -> ByteBuffer.wrap(audioInputStreamDecoded.readNBytes(bytesPerSample)).short.toDouble() / 32768.0
                    else -> 0.0
                }
            },
            {
                audioInputStreamDecoded.close()
                audioInputStreamEncoded.close()
            }
    )
}
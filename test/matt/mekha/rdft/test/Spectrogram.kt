package matt.mekha.rdft.test

import matt.mekha.rdft.Function
import matt.mekha.rdft.RollingDiscreteFourierTransform
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.swing.JFrame
import javax.swing.JPanel


class Spectrogram(filePath: String) : JPanel() {

    private val startFrequency = 10
    private val endFrequency = 10000
    private val frequencyIncrement = 20
    private val numFrequencies = (endFrequency - startFrequency) / frequencyIncrement + 1
    private val frequencies = DoubleArray(numFrequencies) { i: Int -> startFrequency + (i.toDouble() * frequencyIncrement) }.asList()

    private val bytesPerSample = 1
    private val sampleWindowDuration = 0.1
    private val sampleWindowWidth: Int
    private val windowWidth = 1600
    private val pixelsPerSecond = 200
    private val rollsPerPixel: Int

    private val rdft: RollingDiscreteFourierTransform
    private var canvas: BufferedImage

    init {
        val audioSignal = loadAudioFile(filePath)

        sampleWindowWidth = (sampleWindowDuration * audioSignal.sampleRate).toInt()
        setSize(windowWidth, numFrequencies)
        canvas = BufferedImage(windowWidth, numFrequencies, BufferedImage.TYPE_INT_RGB)
        rollsPerPixel = audioSignal.sampleRate / pixelsPerSecond

        val frame = JFrame("Spectrogram")
        frame.add(this)
        frame.pack()
        frame.isResizable = false
        frame.isVisible = true
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

        rdft = RollingDiscreteFourierTransform(
                audioSignal.sampleFunction,
                sampleWindowWidth,
                sampleWindowDuration,
                frequencies
        )

        rdft.prepare()

        for(x in 0 until (audioSignal.sampleRate * audioSignal.duration).toInt() / rollsPerPixel) {
            for(i in 0 until rollsPerPixel) {
                rdft.roll()
            }

            if(x % windowWidth == 0) {
                canvas = BufferedImage(windowWidth, numFrequencies, BufferedImage.TYPE_INT_RGB)
            }
            for((y, frequency) in frequencies.withIndex()) {
                val value = rdft.getFrequencyAmplitude(frequency).magnitude().toFloat().coerceIn(0f, 1f)
                canvas.setRGB(x % windowWidth, numFrequencies - y - 1, Color(value, value, value).rgb)
            }
            repaint()
        }

        Thread.sleep(2000)

        audioSignal.close()
    }

    override fun getPreferredSize(): Dimension? {
        return Dimension(canvas.width, canvas.height)
    }

    override fun paint(g: Graphics) {
        super.paint(g)
        (g as Graphics2D).drawImage(canvas, null, null)
    }

    private fun loadAudioFile(filePath: String) : AudioSource {
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
//                    ByteBuffer.wrap(
//                            audioInputStreamDecoded.readNBytes(bytesPerSample)
//                    ).short.toDouble() / 32768.0
                    audioInputStreamDecoded.read().toDouble() / 128.0 - 1.0
                },
                {
                    audioInputStreamDecoded.close()
                    audioInputStreamEncoded.close()
                }
        )
    }

    data class AudioSource(
            val duration: Double,
            val sampleRate: Int,
            val sampleFunction: Function,
            val close: () -> Unit
    )
}
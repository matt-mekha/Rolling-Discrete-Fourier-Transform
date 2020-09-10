package matt.mekha.rdft.test

import matt.mekha.rdft.Frequency
import matt.mekha.rdft.RollingDiscreteFourierTransform
import matt.mekha.rdft.loadAudioFile
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import javax.swing.JFrame
import javax.swing.JPanel


class Spectrogram(
        filePath: String,
        private val startFrequency: Frequency,
        private val endFrequency: Frequency,
        private val frequencyIncrement: Frequency,
        private val sampleWindowDuration: Double
) : JPanel() {

    private val numFrequencies = ((endFrequency - startFrequency) / frequencyIncrement).toInt() + 1
    private val frequencies = DoubleArray(numFrequencies) { i: Int -> startFrequency + (i.toDouble() * frequencyIncrement) }.asList()

    private val bytesPerSample = 1
    private val sampleWindowWidth: Int
    private val windowWidth = 1600
    private val pixelsPerSecond = 200
    private val rollsPerPixel: Int

    private val rdft: RollingDiscreteFourierTransform
    private var canvas: BufferedImage

    init {
        val audioSignal = loadAudioFile(filePath, bytesPerSample)

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
                //val value = (rdft.getFrequencyAmplitude(frequency).magnitude().toFloat() * 10f).coerceIn(0f, 1f)
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
}
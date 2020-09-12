package matt.mekha.rdft.test

import matt.mekha.rdft.Frequency
import matt.mekha.rdft.RollingDiscreteFourierTransform
import org.junit.Test
import java.lang.Thread.sleep
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.round
import kotlin.test.assertEquals

class TestClass {

    @Test
    fun mathematicalTest() {
        fun cos2(x: Double) = cos(x * PI / 180)

        fun assertFrequency(rdft: RollingDiscreteFourierTransform, expected: Int, frequency: Frequency) {
            assertEquals(expected, round(rdft.getFrequencyAmplitude(frequency).magnitude).toInt())
        }

        val numFrequencies = 20
        val frequencies = DoubleArray(numFrequencies) { i: Int -> (i+1).toDouble() }.asList()

        val rdft = RollingDiscreteFourierTransform(
            { cos2(it * 8.0) + cos2(it * 12.0) },
            360,
            1.0,
            frequencies
        )

        rdft.prepare()
        for(i in 0 .. 1000) {
            rdft.roll()
        }

        for(frequency in frequencies) {
            val y = round(rdft.getFrequencyAmplitude(frequency).magnitude * 100.0) / 100.0
            //println("$frequency Hz\t:\t$y")
        }


        assertFrequency(rdft, 0, 1.0)
        assertFrequency(rdft, 0, 7.0)
        assertFrequency(rdft, 1, 8.0)
        assertFrequency(rdft, 1, 12.0)
    }

    @Test
    fun spectrogramTest() {
        Spectrogram("Birds.wav", 10.0, 10000.0, 20.0, 0.1, 200)
    }

    @Test
    fun hrtfMathTest() {
        val hrtf = HeadRelatedTransferFunction("HRTF/IRC_1003.sofa")
        for(frequency in 10 .. 5000 step 10) {
            val left = hrtf.transfer(frequency.toDouble(), 1.0, SphericalCoordinates(90.0, 0.0, 2.0), Ear.LEFT)
            val right = hrtf.transfer(frequency.toDouble(), 1.0, SphericalCoordinates(90.0, 0.0, 2.0), Ear.RIGHT)
//            println()
//            println("$frequency Hz")
//            println(left)
//            println(right)
            assert(left.amplitude > right.amplitude)
        }
    }

    @Test
    fun hrtfAudioTest() {
        val hrtf = HeadRelatedTransferFunction("HRTF/IRC_1003.sofa")
        val audio = loadAudioFile("Birds.wav")

        val player = HrtfLocalizedAudioPlayer(hrtf, audio, true)
        player.sphericalCoordinates = SphericalCoordinates(90.0, 0.0, 1.0)
        player.play()

        var done = false

        Thread {
            sleep(5000)
//            var azimuth = 0.0
//            while(azimuth < 720.0) {
//                azimuth += 15.0
//                player.sphericalCoordinates = SphericalCoordinates(azimuth % 360.0, 0.0, 1.0)
//                sleep(250)
//            }
            player.stop()
            player.saveCsv()
            done = true
        }.start()

        while(!done) {
            sleep(10)
        }
    }

}

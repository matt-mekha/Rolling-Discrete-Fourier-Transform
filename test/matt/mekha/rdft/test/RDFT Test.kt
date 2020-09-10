package matt.mekha.rdft.test

import matt.mekha.rdft.Frequency
import matt.mekha.rdft.RollingDiscreteFourierTransform
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.round
import kotlin.test.assertEquals

class TestClass {

    @Test
    fun mathematicalTest() {
        fun cos2(x: Double) = cos(x * PI / 180)

        fun assertFrequency(rdft: RollingDiscreteFourierTransform, expected: Int, frequency: Frequency) {
            assertEquals(expected, round(rdft.getFrequencyAmplitude(frequency).magnitude()).toInt())
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
            val y = round(rdft.getFrequencyAmplitude(frequency).magnitude() * 100.0) / 100.0
            //println("$frequency Hz\t:\t$y")
        }


        assertFrequency(rdft, 0, 1.0)
        assertFrequency(rdft, 0, 7.0)
        assertFrequency(rdft, 1, 8.0)
        assertFrequency(rdft, 1, 12.0)
    }

    @Test
    fun spectrogramTest() {
        Spectrogram("Radio2.wav", 10.0, 10000.0, 20.0, 0.06)
    }

    @Test
    fun headRelatedTransferFunctionTest() {
        HeadRelatedTransferFunction("Waves.wav", "IRC_1003.sofa")
    }

}

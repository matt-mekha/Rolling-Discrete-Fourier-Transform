package matt.mekha.rdft.test

import matt.mekha.rdft.Frequency
import matt.mekha.rdft.RollingDiscreteFourierTransform
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.round
import kotlin.test.assertEquals

class TestClass {

    private val numFrequencies = 20

    private fun cos2(x: Double) = cos(x * PI / 180)

    private fun assertFrequency(rdft: RollingDiscreteFourierTransform, expected: Int, frequency: Frequency) {
        assertEquals(expected, round(rdft.getFrequency(frequency).magnitude()).toInt())
    }

    @Test
    fun test1() {
        val rdft = RollingDiscreteFourierTransform(
            { cos2(it * 8.0) + cos2(it * 12.0) },
            360,
            1.0,
            DoubleArray(numFrequencies) { i: Int -> i.toDouble() }.asList()
        )

        rdft.prepare()
        for(i in 0 .. 1000) {
            rdft.roll()
        }

        for(i in 1 .. numFrequencies) {
            val y = round(rdft.getFrequency(i.toDouble()).magnitude() * 100.0) / 100.0
            println("$i Hz\t:\t$y")
        }


        assertFrequency(rdft, 0, 1.0)
        assertFrequency(rdft, 0, 7.0)
        assertFrequency(rdft, 1, 8.0)
        assertFrequency(rdft, 1, 12.0)
    }

}
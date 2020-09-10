package matt.mekha.rdft

import kotlin.math.PI

typealias DFTSampleFunction = (Double) -> Double

class DiscreteFourierTransform(val f: DFTSampleFunction, private val numSamples: Int, x0: Double, x1: Double, private val duration: Double) {

    private val xStart = x0
    private val xInc = (x1 - x0) / numSamples

    private val samples: List<Double> = List(numSamples) { i : Int -> f(xStart + (i * xInc)) }

    fun getFrequencyAmplitude(frequency: Frequency): ComplexNumber {
        val virtualFrequency = frequency * duration
        if(virtualFrequency > numSamples/2) return zero // Nyquist Theorem

        var sum = zero
        for((i, sample) in samples.withIndex()) {
            sum += ePowI(-2 * PI * virtualFrequency * i / numSamples) * sample
        }
        return sum / numSamples.toDouble() * 2.0
    }

}
package matt.mekha.rdft

import kotlin.math.PI
import kotlin.math.absoluteValue

typealias DFTSampleFunction = (Int) -> Double

class DiscreteFourierTransform(f: DFTSampleFunction, private val numSamples: Int, private val duration: Double) {

    private val samples: List<Double> = List(numSamples, f)
    val averageMagnitude = samples.map { it.absoluteValue }.average()

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
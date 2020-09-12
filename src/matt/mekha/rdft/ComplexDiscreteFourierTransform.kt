package matt.mekha.rdft

import kotlin.math.PI

typealias CDFTSampleFunction = (Int) -> ComplexNumber

class ComplexDiscreteFourierTransform(f: CDFTSampleFunction, private val numSamples: Int, private val duration: Double, private val inverse: Boolean = false) {

    private val samples: List<ComplexNumber> = List(numSamples, f)

    fun getFrequencyAmplitude(frequency: Frequency): ComplexNumber {
        val virtualFrequency = frequency * duration
        if(virtualFrequency > numSamples/2) return zero // Nyquist Theorem

        var sum = zero
        for((i, sample) in samples.withIndex()) {
            sum += ePowI(2 * PI * virtualFrequency * i / numSamples * (if(inverse) -1.0 else 1.0)) * sample
        }
        return sum / numSamples.toDouble() * 2.0
    }

}
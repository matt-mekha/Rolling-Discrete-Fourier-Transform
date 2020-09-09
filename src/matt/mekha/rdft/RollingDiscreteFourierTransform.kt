package matt.mekha.rdft

import kotlin.math.PI

typealias Function = (Int) -> Double
typealias Frequency = Double

/**
 * @param f function to perform Fourier transform on
 * @param sampleWindowSize number of samples in the window (~100ms worth of samples is recommended for audio)
 * @param frequencies list of frequency bins that you plan to query
 * @param sampleWindowDuration the size of the sample window but in seconds which is used for translating per-second frequencies
 */
class RollingDiscreteFourierTransform(
    private val f: Function,
    private val sampleWindowSize: Int,
    private val sampleWindowDuration: Double,
    private val frequencies: List<Frequency>
) {

    private var currentSampleX = 0

    private val samples = arrayListOf<Double>()
    private val sums = HashMap<Frequency, ComplexNumber>(sampleWindowSize)

    /**
     * populates window, call before rolling or querying a frequency
     */
    fun prepare() {
        for(x in 0..sampleWindowSize) {
            samples.add(f(x))
            currentSampleX++
        }

        for(frequency in frequencies) {
            val virtualFrequency = frequency * sampleWindowDuration

            var sum = zero
            for((x, sample) in samples.withIndex()) {
                sum += ePowI(-2 * PI * virtualFrequency * x / sampleWindowSize) * sample / sampleWindowSize.toDouble() * 2.0
            }
            sums[frequency] = sum
        }
    }

    /**
     * slides the rolling window by 1 sample, adjusting the Fourier transform accordingly
     */
    fun roll() {
        val removedSample = samples.removeAt(0)
        val nextSample = f(currentSampleX)
        samples.add(nextSample)

        for(frequency in frequencies) {
            val virtualFrequency = frequency * sampleWindowDuration

            sums[frequency] = sums[frequency]!!
                - (ePowI(-2 * PI * virtualFrequency * (currentSampleX - sampleWindowSize) / sampleWindowSize) * removedSample / sampleWindowSize.toDouble() * 2.0)
                + (ePowI(-2 * PI * virtualFrequency * currentSampleX / sampleWindowSize) * nextSample / sampleWindowSize.toDouble() * 2.0)
        }

        currentSampleX++
    }

    /**
     * get frequency's presence in f(x)
     */
    fun getFrequency(frequency: Frequency): ComplexNumber {
        val virtualFrequency = frequency * sampleWindowDuration
        if(virtualFrequency > sampleWindowSize/2) return zero // Nyquist Theorem

        var sum = zero
        for((i, sample) in samples.withIndex()) {
            sum += ePowI(-2 * PI * virtualFrequency * i / sampleWindowSize) * sample / sampleWindowSize.toDouble() * 2.0
        }
        return sum
    }

}
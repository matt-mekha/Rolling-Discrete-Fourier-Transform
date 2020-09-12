package matt.mekha.rdft

import kotlin.math.PI

typealias RDFTSampleFunction = (Int) -> Double
typealias Frequency = Double

/**
 * @param f function to perform Fourier transform on
 * @param sampleWindowSize number of samples in the window (~100ms worth of samples is recommended for audio)
 * @param frequencies list of frequency bins that you plan to query
 * @param sampleWindowDuration the size of the sample window but in seconds which is used for translating per-second frequencies
 */
class RollingDiscreteFourierTransform(
        private val f: RDFTSampleFunction,
        private val sampleWindowSize: Int,
        private val sampleWindowDuration: Double,
        private val frequencies: List<Frequency>
) {

    private var currentSampleX = 0

    private val samples = arrayListOf<Double>()
    private val sums = HashMap<Frequency, ComplexNumber>(sampleWindowSize)

    val latestSample
        get() = samples.last()

    private fun passesNyquistTheorem(frequency: Frequency) : Boolean {
        return frequency <= sampleWindowSize / 2
    }

    /**
     * populates window, call before rolling or querying a frequency
     */
    fun prepare() {
        currentSampleX = 0
        samples.clear()
        sums.clear()

        for(x in 0 until sampleWindowSize) {
            samples.add(f(x))
            currentSampleX++
        }

        for(frequency in frequencies) {
            val virtualFrequency = frequency * sampleWindowDuration

            var sum = zero
            if(passesNyquistTheorem(virtualFrequency)) {
                for((x, sample) in samples.withIndex()) {
                    sum += ePowI(-2 * PI * virtualFrequency * x / sampleWindowSize) * sample / sampleWindowSize.toDouble() * 2.0
                }
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

            if (passesNyquistTheorem(virtualFrequency)) {
                val firstValue = ePowI(-2 * PI * virtualFrequency * (currentSampleX - sampleWindowSize) / sampleWindowSize) * removedSample / sampleWindowSize.toDouble() * 2.0
                val newValue = ePowI(-2 * PI * virtualFrequency * currentSampleX / sampleWindowSize) * nextSample / sampleWindowSize.toDouble() * 2.0
                sums[frequency] = sums[frequency]!! - firstValue + newValue
            } else {
                println("WARNING: $frequency Hz fails Nyquist Theorem test")
            }
        }

        currentSampleX++
    }

    /**
     * get frequency's presence in f(x)
     */
    fun getFrequencyAmplitude(frequency: Frequency): ComplexNumber {
        return sums[frequency]!!
    }

}
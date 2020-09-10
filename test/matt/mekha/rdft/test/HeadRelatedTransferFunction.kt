package matt.mekha.rdft.test

import matt.mekha.rdft.Frequency

typealias Angle = Double
typealias Length = Double

class HeadRelatedTransferFunction(audioFilePath: String, sofaFilePath: String) {

    fun transfer(frequency: Frequency, amplitude: Double, azimuth: Angle, elevation: Angle, distance: Length) : Transformation {
        TODO()
    }

}

data class Transformation(
        val amplitude: Double,
        val delay: Double
)
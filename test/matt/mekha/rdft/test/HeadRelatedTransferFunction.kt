package matt.mekha.rdft.test

import matt.mekha.rdft.DiscreteFourierTransform
import matt.mekha.rdft.Frequency
import ucar.nc2.NetcdfFile
import ucar.nc2.NetcdfFiles
import ucar.nc2.dataset.NetcdfDataset
import java.io.File
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.*

// This project took advantage of netCDF software developed by UCAR/Unidata (http://doi.org/10.5065/D6H70CW6).

data class CartesianCoordinates(
        val x : Double,
        val y : Double,
        val z : Double
) {
    fun distanceTo(v : CartesianCoordinates) : Double {
        return sqrt(
                (x - v.x).pow(2) +
                (y - v.y).pow(2) +
                (z - v.z).pow(2)
        )
    }
}

data class SphericalCoordinates(
        val azimuth: Double,
        val elevation: Double,
        val radius: Double
) {

    private val cartesianCoordinates : CartesianCoordinates

    init {
        val elevationRadians = (90.0 - elevation) * PI / 180.0
        val azimuthRadians = azimuth * PI / 180.0

        cartesianCoordinates = CartesianCoordinates(
                radius * sin(elevationRadians) * cos(azimuthRadians),
                radius * sin(elevationRadians) * sin(azimuthRadians),
                radius * cos(elevationRadians)
        )
    }

    fun getClosest(otherSphericalCoordinatesSet : Set<SphericalCoordinates>): Any {
        var closest : SphericalCoordinates? = null
        var closestDistance = Double.MAX_VALUE
        for(otherSphericalCoordinates in otherSphericalCoordinatesSet) {
            val distance = otherSphericalCoordinates.cartesianCoordinates.distanceTo(cartesianCoordinates)
            if(distance < closestDistance) {
                closestDistance = distance
                closest = otherSphericalCoordinates
            }
        }
        return closest!!
    }

}

data class Transformation(
        val amplitude: Double,
        val delay: Double
)

enum class Ear {
    LEFT,
    RIGHT
}

// The HRTF is the Fourier Transform of the HRIR

class HeadRelatedTransferFunction(sofaFilePath: String) {

    private val impulseResponseMap = HashMap<SphericalCoordinates, EnumMap<Ear, DiscreteFourierTransform>>()

    init {
        val file = NetcdfFiles.open(sofaFilePath)

        val locationData = file.variables[2].read().copyToNDJavaArray() as Array<*>
        val impulseData = file.variables[6].read().copyToNDJavaArray() as Array<*>

        val numSamples = ((impulseData[0] as Array<*>)[0] as DoubleArray).size.toDouble()
        val sampleRate = (file.variables[7].read().copyTo1DJavaArray() as DoubleArray)[0]
        val measurementDuration = numSamples / sampleRate

        for((i, measurement) in impulseData.withIndex()) {
            val locationArray = locationData[i] as DoubleArray
            val sphericalCoordinates = SphericalCoordinates(locationArray[0], locationArray[1], locationArray[2])
            impulseResponseMap[sphericalCoordinates] = EnumMap(Ear::class.java)

            val ears = measurement as Array<*>
            for((j, ear) in ears.withIndex()) {
                val samples = ear as DoubleArray
                impulseResponseMap[sphericalCoordinates]!![if (j == 0) Ear.LEFT else Ear.RIGHT] =
                        DiscreteFourierTransform({ samples[it] }, samples.size, measurementDuration)
            }
        }
    }

    fun transfer(frequency: Frequency, amplitude: Double, sphericalCoordinates: SphericalCoordinates, ear: Ear) : Transformation {
        val closestSphericalCoordinates = sphericalCoordinates.getClosest(impulseResponseMap.keys)
        val amplitudeChange = impulseResponseMap[closestSphericalCoordinates]!![ear]!!.getFrequencyAmplitude(frequency).magnitude()

        return Transformation(
                amplitude * amplitudeChange, // TODO inverse square law
                0.0
        )
    }

}

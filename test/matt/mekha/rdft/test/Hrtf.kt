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
    operator fun plus(v : CartesianCoordinates) : CartesianCoordinates {
        return CartesianCoordinates(
                x + v.x,
                y + v.y,
                z + v.z
        )
    }

    operator fun unaryPlus() : CartesianCoordinates {
        return CartesianCoordinates(x, y, z)
    }

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

    companion object {
        val forward
            get() = SphericalCoordinates(0.0, 0.0, 1.0)
    }

    val cartesianCoordinates : CartesianCoordinates

    init {
        val elevationRadians = elevation * PI / 180.0
        val azimuthRadians = azimuth * PI / 180.0

        cartesianCoordinates = CartesianCoordinates(
                radius * cos(elevationRadians) * cos(azimuthRadians),
                radius * cos(elevationRadians) * sin(azimuthRadians),
                radius * sin(elevationRadians)
        )
    }

    fun getClosest(otherSphericalCoordinatesSet : Set<SphericalCoordinates>): SphericalCoordinates {
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

enum class Ear(val y: Double) {
    LEFT(1.0),
    RIGHT(-1.0)
}

private const val speedOfSound = 343.0

class HeadRelatedTransferFunction(sofaFilePath: String, private val headRadius : Double = 0.09) {

    private val impulseResponseMap = HashMap<SphericalCoordinates, EnumMap<Ear, DiscreteFourierTransform>>()
    private val averageAverageMagnitude : Double

    init {
        val file = NetcdfFiles.open(sofaFilePath)

        val locationData = file.variables[2].read().copyToNDJavaArray() as Array<*>
        val impulseData = file.variables[6].read().copyToNDJavaArray() as Array<*>

        val numSamples = ((impulseData[0] as Array<*>)[0] as DoubleArray).size.toDouble()
        val sampleRate = (file.variables[7].read().copyTo1DJavaArray() as DoubleArray)[0]
        val measurementDuration = numSamples / sampleRate

        val averageMagnitudes = ArrayList<Double>(locationData.size)
        for((i, measurement) in impulseData.withIndex()) {
            val locationArray = locationData[i] as DoubleArray
            val sphericalCoordinates = SphericalCoordinates(locationArray[0], locationArray[1], locationArray[2])
            impulseResponseMap[sphericalCoordinates] = EnumMap(Ear::class.java)

            val ears = measurement as Array<*>
            for((j, ear) in ears.withIndex()) {
                val samples = ear as DoubleArray
                val dft = DiscreteFourierTransform({ samples[it] }, samples.size, measurementDuration)
                impulseResponseMap[sphericalCoordinates]!![if (j == 0) Ear.LEFT else Ear.RIGHT] = dft
                averageMagnitudes.add(dft.averageMagnitude)
            }
        }

        averageAverageMagnitude = averageMagnitudes.average()
    }

    fun transfer(frequency: Frequency, amplitude: Double, sphericalCoordinates: SphericalCoordinates, ear: Ear) : Transformation {
        val closestSphericalCoordinates = sphericalCoordinates.getClosest(impulseResponseMap.keys)
        val dft = impulseResponseMap[closestSphericalCoordinates]!![ear]!!

        val frequencyAttenuation = 1.0 + dft.getFrequencyAmplitude(frequency).magnitude
        val distanceAttenuation = (closestSphericalCoordinates.radius / sphericalCoordinates.radius).pow(2)
        val earAttenuation = dft.averageMagnitude / averageAverageMagnitude

        val distanceDelay = sphericalCoordinates.cartesianCoordinates.distanceTo(
                CartesianCoordinates(0.0, ear.y * headRadius, 0.0)
        ) / speedOfSound

        return Transformation(
                amplitude * frequencyAttenuation * distanceAttenuation * earAttenuation,
                distanceDelay
        )
    }

}

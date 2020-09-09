package matt.mekha.rdft

import kotlin.math.pow
import kotlin.math.sqrt

data class ComplexNumber(var r: Double, var i: Double) {

    operator fun plus(n: ComplexNumber): ComplexNumber {
        return ComplexNumber(r + n.r, i + n.i)
    }

    operator fun times(n: Double): ComplexNumber {
        return ComplexNumber(r * n, i * n)
    }

    operator fun div(n: Double): ComplexNumber {
        return ComplexNumber(r / n, i / n)
    }

    operator fun unaryPlus() : ComplexNumber {
        return ComplexNumber(r, i)
    }

    operator fun unaryMinus() : ComplexNumber {
        return ComplexNumber(-r, -i)
    }

    fun magnitude(): Double {
        return sqrt(r.pow(2) + i.pow(2))
    }

}

val zero
    get() = ComplexNumber(0.0, 0.0)

fun ePowI(n: Double): ComplexNumber {
    return ComplexNumber(kotlin.math.cos(n), kotlin.math.sin(n))
}
package pat.narendra.nonlinearoptimizer

import org.ddogleg.optimization.FactoryOptimization
import org.ddogleg.optimization.UtilOptimize
import org.ddogleg.optimization.functions.FunctionNtoM
import org.junit.Test
import java.lang.Math.random
import java.util.*
import java.util.Random
import kotlin.math.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class RangeEstimation {
    @Test
    fun main() {
        val points: ArrayList<RawRangeData> = arrayListOf()
        points.add(RawRangeData(0f, 0f, 0f, 0f, 0f))
        points.add(RawRangeData(0f, 0f, 0f, 0f, 10f))
        points.add(RawRangeData(0f, 0f, 0f, 10f, 0f))




        val x = 5f
        val y = 5f
        val t = PI.toFloat() / 6f

        val xArray = arrayListOf<Float>()
        val yArray = arrayListOf<Float>()
        val tArray = arrayListOf<Float>()


        repeat(10) {

            points.forEach { v ->
                v.xAr = (random().toFloat() - .5f) * 10.0f
                v.yAr = (random().toFloat() - .5f) * 10.0f
                val xW = x + v.xAr * cos(t) - v.yAr * sin(t)
                val yW = y + v.xAr * sin(t) + v.yAr * cos(t)
                val noise = Random().nextGaussian()
                    .toFloat() * 0f //simulates measurement noise xx meter std deviation
                v.range = sqrt((xW - v.xAp).pow(2) + (yW - v.yAp).pow(2)) + noise
            }
            // println(points)
            val func = FunctionRangeComputeErrors(points)
            val optimizer = FactoryOptimization.levenbergMarquardt(null, true)
            // optimizer.setVerbose(System.out, 0)
            optimizer.setFunction(func, null)
            optimizer.initialize(doubleArrayOf(-0.5, 0.5, 0.5), 1e-12, 1e-12)
            UtilOptimize.process(optimizer, 500)
            val found = optimizer.parameters
            println("Final Error = " + optimizer.functionValue)
            println("Actual location x = $x found ${found[0]}")
            println("Actual location y = $y found ${found[1]}")
            println("Actual Angle theta = ${t / PI * 180} found ${found[2] / PI * 180}")
            xArray.add(found[0].toFloat())
            yArray.add(found[1].toFloat())
            tArray.add((found[2] / PI * 180).toFloat())
        }
        println("Mean x = ${xArray.average()}")
        println("Std Deviation x = ${xArray.stdDeviation()}")
        println("Mean y = ${yArray.average()}")
        println("Std Deviation y = ${yArray.stdDeviation()}")
        println("Mean t = ${tArray.average()}")
        println("Std Deviation t = ${tArray.stdDeviation()}")
    }
}

data class RawRangeData(
    var range: Float, //observed range of AP from device
    var xAr: Float, // x coordinate of device in Ar space
    var yAr: Float, // y coordinate of device in Ar space
    var xAp: Float, // x coordinate of AP in real world space
    var yAp: Float  // y coordinate of AP in real world space
)

class FunctionRangeComputeErrors(val data: List<RawRangeData>) : FunctionNtoM {
    /**
     * Number of input elements.  Typically the parameters you are optimizing.
     *
     * @return number of input elements
     */
    override fun getNumOfInputsN() = 3


    /**
     * Processes the input to compute the values found in the output array.
     *
     * The output's meaning depends on the application.  For least-squares it is the residual error. See
     * [org.ddogleg.optimization.UnconstrainedLeastSquares].
     *
     *
     *
     * The user can modify the input parameters here and the optimizer must use those changes.
     *
     *
     * @param input Parameters for input model.
     * @param output Storage for the output give the model.
     */
    override fun process(input: DoubleArray?, output: DoubleArray?) {
        val x = input?.get(0)  //x offset of AR frame from world origin
        val y = input?.get(1)  // y offset of AR frame from world origin
        val t = input?.get(2) //t = theta, the yaw angle (in radians) of the AR frame x axis from the world x axis.
        if (x != null && y != null && t != null) {
            data.forEachIndexed { ind, d ->
                val xP = x + d.xAr * cos(t) - d.yAr * sin(t)
                val yP = y + d.xAr * sin(t) + d.yAr * cos(t)
                val error = d.range - sqrt((xP - d.xAp).pow(2) + (yP - d.yAp).pow(2))
                output?.set(ind, error)
            }
        }
    }

    /**
     * Number of output elements. Typically the functions that are being optimized.
     *
     * @return number of output elements
     */
    override fun getNumOfOutputsM() = data.size

}

fun ArrayList<Float>.stdDeviation(): Float{
    var sumOfSquareDiff = 0f
    val average = this.average()
    this.forEach{
        sumOfSquareDiff += (it - average).pow(2).toFloat()
    }
    return sqrt(sumOfSquareDiff / this.size)
}
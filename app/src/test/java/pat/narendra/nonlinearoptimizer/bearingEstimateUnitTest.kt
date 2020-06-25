package pat.narendra.nonlinearoptimizer

import org.ddogleg.optimization.FactoryOptimization
import org.ddogleg.optimization.UtilOptimize
import org.ddogleg.optimization.functions.FunctionNtoM
import org.junit.Test
import java.lang.Math.random
import java.util.*
import kotlin.math.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class BearingEstimation {
    @Test
    fun main() {
        val points: ArrayList<RawBearingData> = arrayListOf()
        points.add(RawBearingData(0f, 0f, 0f, 0f, 0f))
        points.add(RawBearingData(0f, 0f, 0f, 0f, 10f))
        points.add(RawBearingData(0f, 0f, 0f, 10f, 0f))
        points.add(RawBearingData(0f, 0f, 0f, 10f, 10f))

        // parameters of the global origin for the test set.
        val x = 5f
        val y = 5f
        // val t = PI.toFloat() / 1f
        val t = 0f


        points.forEach { v ->
            // v.xAd = random().toFloat() * 10.0f - 3f
            v.xAd = 0f
            // v.yAd = random().toFloat() * 10f - 3f
            v.yAd = 0f
            val xOar = (v.xO - x) * cos(t) + (v.yO - x) * sin(t)
            val yOar = -(v.xO - x) * sin(t) + (v.yO - y) * cos(t)
            //add a random noise to the bearing estimate to simulate measurement error
            val error = 0f
            v.bearing =
                (atan2(yOar - v.yAd , xOar - v.xAd) * (1 + (random().toFloat() - 0.5f) * error))
        }
        println(points)
        val func = FunctionBearingComputeErrors(points)
        val optimizer = FactoryOptimization.levenbergMarquardt(null, true)
        optimizer.setVerbose(System.out, 0)
        optimizer.setFunction(func, null)
        optimizer.initialize(doubleArrayOf(-1.0, 1.0, 0.5), 1e-8, 1e-8)
        UtilOptimize.process(optimizer, 500)
        val found = optimizer.parameters
        println("Final Error = " + optimizer.functionValue)
        println("Actual location x = $x found ${found[0]}")
        println("Actual location y = $y found ${found[1]}")
        println("Actual Angle theta = ${t / PI * 180} found ${found[2] / PI * 180}")
    }
}

data class RawBearingData(
    var bearing: Float, // observed bearing of object in radians in AR space
    var xAd: Float,   //x position of device in AR space
    var yAd: Float, //y position of device in AR space
    var xO: Float,  //x location of the observed object in real World space
    var yO: Float   //y location of the observed object in real World space
)

class FunctionBearingComputeErrors(val data: List<RawBearingData>) : FunctionNtoM {
    /**
     * Number of input elements.  Typically the parameters you are optimizing.
     *
     * @return number of input elements
     * x, y, t : the coordinates and orientation of the AR frame wrt global frame
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
            data.forEachIndexed { ind, v ->
                val xOar = (v.xO - x) * cos(t) + (v.yO - x) * sin(t)
                val yOar = -(v.xO - x) * sin(t) + (v.yO - y) * cos(t)
                val error = v.bearing - (atan2((yOar - v.yAd) , (xOar - v.xAd))).toFloat()
                output?.set(ind, error.toDouble())
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
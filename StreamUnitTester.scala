package myLib

import org.scalatest.FunSuite
import spinal.core._
import spinal.core.sim._
import spinal.lib.Stream
import spinal.lib.sim.{StreamDriver, StreamMonitor, StreamReadyRandomizer}

import scala.collection.mutable
import scala.util.Random

//Run this scala test to generate and check that your RTL work correctly
object StreamUnitTester {
  def main(arg: Array[String]): Unit = {
    val compiled = SimConfig.withWave
      .withConfig(SpinalConfig(targetDirectory = "rtl"))
      .workspacePath("waves")
      .compile(StreamJ())

    compiled.doSimUntilVoid { dut =>
      dut.clockDomain.forkStimulus(10)
      SimTimeout(800)

      val dataAQueue, dataBQueue = mutable.Queue[Long]()

      fork {
        for (i <- 0 to 10) {
          dut.io.streamA.valid #= false
          dut.io.streamA.valid.randomize()
          dut.io.streamA.payload #= 2 * i
          dut.clockDomain.waitSamplingWhere(dut.io.streamA.ready.toBoolean)
        }
      }

      fork {
        for (i <- 0 to 10) {
          dut.io.streamB.valid #= false
          dut.io.streamB.valid.randomize()
          dut.io.streamB.payload #= 2 * i + 1
          dut.clockDomain.waitSamplingWhere(dut.io.streamB.ready.toBoolean)
        }
      }

      StreamReadyRandomizer(dut.io.streamD, dut.clockDomain)

      SimStreamUtils.onStreamFire(dut.io.streamA, dut.clockDomain)({
        dataAQueue.enqueue(dut.io.streamA.payload.toLong)
      })

      SimStreamUtils.onStreamFire(dut.io.streamB, dut.clockDomain)({
        dataBQueue.enqueue(dut.io.streamB.payload.toLong)
      })

    }
  }
}

object SimStreamUtils {
  //Fork a thread which will call the body function each time a transaction is consumed on the given stream
  def onStreamFire[T <: Data](stream: Stream[T], clockDomain: ClockDomain)(
      body: => Unit
  ): Unit = fork {
    while (true) {
      clockDomain.waitSampling()
      var dummy = if (stream.valid.toBoolean && stream.ready.toBoolean) {
        body
      }
    }
  }
}

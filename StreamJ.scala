package myLib

import spinal.core._
import spinal.lib._
import spinal.lib

case class StreamJ() extends Component {
  val io = new Bundle {
    val streamA = slave Stream (UInt(5 bits))
    val streamB = slave Stream (UInt(5 bits))
    val streamD = master Stream (UInt(5 bits))
  }
  
  val streamAPipe = io.streamA.m2sPipe()
  val streamBPipe = io.streamB.m2sPipe()
  val streamC = Stream(UInt(5 bits))

  streamC.payload := 0
  streamC.valid := False
  streamAPipe.ready := False
  streamBPipe.ready := False

  when(streamAPipe.valid && streamBPipe.valid) {
    streamC.valid := True
  }

  when(streamAPipe.payload >= streamBPipe.payload) {
    when(streamC.fire) {
      streamC.payload := streamBPipe.payload
      streamBPipe.ready := True
      streamAPipe.ready := False
    }

  }.otherwise {
    when(streamC.fire) {
      streamC.payload := streamAPipe.payload
      streamAPipe.ready := True
      streamBPipe.ready := False
    }
  }

  io.streamD <-/< streamC
}

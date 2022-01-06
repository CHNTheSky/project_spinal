import spinal.core._
import spinal.lib._
case class DmaWrapper(busWidth: Int, dataOutWidth: Int) extends Component {
  val io = new Bundle {
    val axis_tkeep = in Bits (busWidth / 8 bits)
    val axis_tlast = in Bool
    val axis = slave Stream (Bits(busWidth bits))
    val dmaWrapper = master Stream (Bits(busWidth bits))
  }
  val fifoCach = StreamFifo(
    dataType = Bits(busWidth bits),
    depth = 8
  )
  val tLastDelay1 = RegNext(io.axis_tlast) init (True)
  val tLastDelay2 =
    RegNext(tLastDelay1) init (False) //pop has two cycles delay of push
  val dataJoin = Reg(Bits(dataOutWidth bit)) init (0)
  val tmpData = Reg(Bits(4 * busWidth bit)) init (0) 
  val shiftBit = Reg(UInt(log2Up(dataOutWidth / 8) bits)) init (0)
  val popValidDelay1 = RegNext(fifoCach.io.pop.valid)
  val isFirstOne = io.axis.valid.rise() //first stage
  val count = Reg(UInt(2 bits)) init (0)

  def lastValid(data: Bits) = {
    val index = UInt(log2Up(dataOutWidth / 8) bits)
    index := data.getWidth - 1
    for (i <- 0 until data.getWidth) {
      when(data(i) === False) {
        index := i
      }
    }
    index
  }
  val stageCount = new Area {
    when(fifoCach.io.push.fire && !fifoCach.io.pop.fire) {
      count := count + 1
    }.elsewhen(fifoCach.io.pop.fire && !fifoCach.io.push.fire) {
      count := count - 1
    }.otherwise(count := count)
  }

  val area_LogicIn = new Area {
    fifoCach.io.push << io.axis
    when(io.axis_tlast) {
      shiftBit := lastValid(io.axis_tkeep).resized
    }
  }
  val area_LogicOut = new Area {
    val streamOut = Stream(Bits(dataOutWidth bits))
    val validVec = Vec(Reg(False), dataOutWidth / busWidth)
    validVec(0) := count === 0 && popValidDelay1 === True
    validVec(1) := validVec(0)
    streamOut.valid := validVec.xorR
    fifoCach.io.pop.ready := True
    when(fifoCach.io.pop.valid === True) {
      when(isFirstOne === True) { 
        tmpData := fifoCach.io.pop.payload.resized
      }.otherwise {
        tmpData := (tmpData ## fifoCach.io.pop.payload).resized 
      }
    }
    when(count === 0) {
      dataJoin := (tmpData >> (shiftBit + 1) * 8).resized
    }
    io.dmaWrapper <> StreamWidthAdapter
      .make(streamOut.translateWith(dataJoin), Bits(busWidth bits))
      .queue(0)
  }
}


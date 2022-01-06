import spinal.core._
import spinal.lib._
case class DmaWrapper(busWidth: Int, dataOutWidth: Int) extends Component {
  val io = new Bundle {
    val axis_tKeep = in Bits (busWidth / 8 bits)
    val axis_tLast = in Bool
    val axis = slave Stream (Bits(busWidth bits))
    val dmaWrapper = master Stream (Bits(busWidth bits))
  }
  val fifo = StreamFifo(
    dataType = Bits(busWidth bits),
    depth = 8
  )
  val tLastDelay1 = RegNext(io.axis_tLast) init (True)
  val tLastDelay2 =
    RegNext(tLastDelay1) init (False) //pop has two cycles delay of push
  val dataJoin = Reg(Bits(dataOutWidth bit)) init (0)
  val tmpData = Reg(Bits(4 * busWidth bit)) init (0) //最大的4拍拼接
  val shiftBit = Reg(UInt(log2Up(dataOutWidth / 8) bits)) init (0)
  val popValidDelay1 = RegNext(fifo.io.pop.valid)
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
    when(fifo.io.push.fire && !fifo.io.pop.fire) {
      count := count + 1
    }.elsewhen(fifo.io.pop.fire && !fifo.io.push.fire) {
      count := count - 1
    }.otherwise(count := count)
  }

  val area_LogicIn = new Area {
    fifo.io.push << io.axis
    when(io.axis_tLast) {
      shiftBit := lastValid(io.axis_tKeep).resized
    }
  }
  val area_LogicOut = new Area {
    val streamOut = Stream(Bits(dataOutWidth bits))
    val validVec = Vec(Reg(False), dataOutWidth / busWidth)
    validVec(0) := count === 0 && popValidDelay1 === True
    validVec(1) := validVec(0)
    streamOut.valid := validVec.xorR
    fifo.io.pop.ready := True
    when(fifo.io.pop.valid === True) {
      when(isFirstOne === True) { //第一拍清空
        tmpData := fifo.io.pop.payload.resized
      }.otherwise {
        tmpData := (tmpData ## fifo.io.pop.payload).resized //拼接
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

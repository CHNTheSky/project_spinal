import spinal.core._
import spinal.lib._
case class DmaWrapper(dataWidth: Int) extends Component{
  val io = new Bundle {
    val axis_tkeep = in Bits (dataWidth / 8 bits)
    val axis = slave Stream (Bits(dataWidth bits))
    val dmaWrapper = master Stream (Bits(dataWidth bits))
  }
  def firstOne(data:Bits):UInt={
    val index = UInt
    for(i<-0 until io.axis_tkeep.getWidth){
    when(data(i).asUInt===1){
      index := i
    }
  }
    index
  }
  def lastOne(data:Bits):UInt={
    val index = UInt
    for(i<-0 until io.axis_tkeep.getWidth){
      when(data(i).asUInt===0){
        index := i
      }
    }
    index
  }
  val wantedId = Counter(2, inc = io.axis.fire)
  val twoStreams = StreamDemux(io.axis, select = wantedId, portCount = 2)
  val validData1 = twoStreams(0)
  val validData2 = twoStreams(1)
  io.dmaWrapper<<StreamJoin.arg(validData1,validData2).translateWith{
    ((validData1.payload.rotateLeft(0)>>firstOne(validData1.payload).rotateLeft(0))##
      (validData2.payload>>lastOne(validData2.payload))).resized
  }

}


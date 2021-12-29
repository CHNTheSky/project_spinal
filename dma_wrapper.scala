import spinal.core._
import spinal.lib._
case class DmaWrapper(busWidth: Int,dataOutWidth: Int) extends Component{
  val io = new Bundle {
    val axis_tkeep = in Bits (busWidth / 8 bits)
    val axis_tlast = in Bool
    val axis = slave Stream (Bits(busWidth bits))
    val dmaWrapper = master Stream (Bits(busWidth bits))
  }
  val fifoCach=StreamFifo(
    dataType = Bits(busWidth bits),
    depth = 8
  )
  val isFirstOne = RegNext(io.axis_tlast) init(True)
  val dataJoin = Reg(Bits(dataOutWidth bit))
  val shiftBit = Reg(UInt())


  val logic_in = new Area{
    fifoCach.io.push<<io.axis
    when(isFirstOne){
      shiftBit:=firstValid(io.axis.payload)
    }
  }


  val logic_out = new Area{
    val streamOut=Stream(Bits(dataOutWidth bits))
    when(io.axis_tlast){
      streamOut.valid:=True
    }.otherwise(streamOut.valid:=False)
    val dataAdapt=Stream(Bits(busWidth bits))
    dataJoin := dataJoin##fifoCach.io.pop.payload//拼接
    io.dmaWrapper<>StreamWidthAdapter.make(streamOut.translateWith((dataJoin<<(shiftBit*8)).resizeLeft(dataOutWidth)).m2sPipe(),Bits(busWidth bits)).queue(10)

  }


  def firstValid(data:Bits)={
    val index = U(0)
    for(i<-0 until io.axis_tkeep.getWidth){
    when(data(i).asUInt===1){
      index := i
    }
  }
    index
  }

}



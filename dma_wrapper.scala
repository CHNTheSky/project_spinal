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
  val isFirstOne = RegNext(io.axis_tlast) init(True)//第一拍标志
  val dataJoin = Reg(Bits(dataOutWidth bit))
  val tmpdata = Reg(Bits(8*busWidth bit))//最大的8拍拼接
  val shiftBit = Reg(UInt())
  val curStage = Reg(UInt(3 bits)) init(1)//当前是第几拍拍标志 123450 12340 1230
  val nextStage = RegNext(curStage)//              前一拍是第几拍    012345 01234 0123
  def firstValid(data:Bits)={
    val index = U(0)
    for(i<-0 until io.axis_tkeep.getWidth){
      when(data(i).asUInt===1){
        index := i
      }
    }
    index
  }
  val curstage  =new Area{
    when(isFirstOne){
      curStage:=1
    }.elsewhen(io.axis_tlast){
      curStage:=0
    }
  }

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
    fifoCach.io.pop.ready:=True
    for(i<-0 until 7){
      if(i == 1){
        when(fifoCach.io.pop.valid === True) {
          tmpdata := fifoCach.io.pop.payload.resized
        }
      }
      when(fifoCach.io.pop.valid === True && (curStage>nextStage||curStage===0||nextStage===0)){//是当前的这一拍
        tmpdata:= (tmpdata(i*busWidth-1 downto 0)##fifoCach.io.pop.payload).resized//拼接
      }
    }
    dataJoin:=tmpdata.resized
    io.dmaWrapper<>StreamWidthAdapter.make(streamOut.translateWith((dataJoin<<(shiftBit*8)).resizeLeft(dataOutWidth)).m2sPipe(),Bits(busWidth bits)).queue(10)
  }
}

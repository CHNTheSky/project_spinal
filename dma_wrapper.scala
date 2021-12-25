import spinal.core._
import spinal.lib._
case class portA(Datawidthout : Int) extends Bundle{
  val data=Bits(8 bits)
  val dataout=Bits(Datawidthout bits)
}
case class DmaWrapper(Datawidthin : Int,Datawidthout : Int) extends Component{
  val io = new Bundle{
    val axis_tkeep =in Bits(Datawidthin/8 bits)
    val axis =slave Stream(Bits(Datawidthin bits))
    val dmaWrapper = master Stream(Bits(Datawidthout bits))
  }
  io.dmaWrapper.ready:=True
  val datavec=io.axis.payload.subdivideIn(8 bits)//切片

  val fifocach=StreamFifo(
    dataType = portA(Datawidthout : Int),
    depth = Datawidthout/8
  )
  def isone(bit : Int,data: Bits)={ //根据keep判断哪些字节有效
    val isone = False
      when(data(bit)){
        isone:=True
      }
    isone
    }
  when(io.axis.fire){
    for(i<-0 to datavec.length){
      when(isone(i,io.axis_tkeep)){
        fifocach.io.push<-/<Stream(datavec.read(i).asBits)
      }
    }
  }
  when(fifocach.io.occupancy === fifocach.io.occupancy.maxValue){
    io.dmaWrapper<-/<Stream(fifocach.io.pop.dataout)
  }

}


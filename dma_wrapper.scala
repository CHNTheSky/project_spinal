import spinal.core._
import spinal.lib._
case class DmaWrapper(Datawidthin : Int,Datawidthout : Int) extends Component{
  val io = new Bundle{
    val axis_tkeep =in Bits(Datawidthin/8 bits)
    val axis =slave Stream(Bits(Datawidthin bits))
    val dmaWrapper = master Stream(Bits(Datawidthout bits))
  }
  val datavec=io.axis.payload.subdivideIn(io.axis_tkeep.getWidth slices)//切片

  val fifocach=StreamFifo(
    dataType = Bits(),
    depth = Datawidthout
  )

  def isone(bit : Int,data: Bits)={ //根据keep判断哪些字节有效
    val isone = False
      when(data(bit)){
        isone:=True
      }
    isone
    }
  when(io.axis.valid){
    for(i<-0 to Datawidthin/8){
      when(isone(i,io.axis_tkeep)){
        fifocach.io.push<<Stream(datavec.read(i))
      }
    }
  }
  when(fifocach.io.occupancy === fifocach.io.occupancy.maxValue){
    fifocach.io.pop>>io.dmaWrapper
  }
}

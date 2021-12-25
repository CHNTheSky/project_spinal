import spinal.core._
import spinal.lib._
case class DmaWrapper(Datawidthin : Int,Datawidthout : Int) extends Component{
  val io = new Bundle{
    val axis_tkeep =in Bits(Datawidthin/8 bits)
    val axis =slave Stream(Bits(Datawidthin bits))
    val dmaWrapper = master Stream(Bits(Datawidthout bits))
  }
  io.dmaWrapper.ready:=True
  val datavec=io.axis.payload.subdivideIn(8 bits)//切片

  val fifocach=StreamFifo(
    dataType = Bits(8 bits),
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
        fifocach.io.push<-/<Stream(datavec.read(i))
      }
    }
  }
  when(fifocach.io.occupancy === fifocach.io.occupancy.maxValue){
    io.dmaWrapper<-/<fifocach.io.pop
  }
/*

case class portA() extends Bundle{
  val data=UInt(512 bits)
  val mty=UInt(6 bits)
}
case class portB() extends Bundle{
  val data=UInt(512 bits)
  val tkeep=UInt(64 bits)
}
case class busAdapt(fifoDepth:Int=512) extends Component {
  val io=new Bundle{
    val dataIn=slave Stream(portA())
    val dataOut=master Stream(portB())
  }
  noIoPrefix()
  val _=new Area{
    val fifoCache=StreamFifo(portA(),fifoDepth)
    val busCov=portB()
    if(clockDomain.config.resetActiveLevel==HIGH)
      fifoCache.io.flush:=clockDomain.readResetWire
    else
      fifoCache.io.flush:= !clockDomain.readResetWire
    io.dataIn<>fifoCache.io.push
    busCov.data:=fifoCache.io.pop.data
    keepCov.mty2keep(busCov.tkeep,fifoCache.io.pop.mty)
    io.dataOut<>fifoCache.io.pop.translateWith(busCov)
  }.setName("")
}
object keepCov{
  def mty2keep(tkeep:UInt,mty:UInt)={
    val mtyIndex=for(index<-0 to 63) yield U((BigInt(1)<<(64-index))-1)
    tkeep:=mtyIndex.read(mty)
  }
}
*/
}

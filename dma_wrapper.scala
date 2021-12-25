import spinal.core._
import spinal.lib._
/*
case class portA(Datawidthout : Int) extends Bundle{
  val data=Bits(8 bits)
  val dataout=Bits(Datawidthout bits)
}*/
case class DmaWrapper(Datawidthin : Int,Datawidthout : Int) extends Component{
  val io = new Bundle{
    val axis_tkeep =in Bits(Datawidthin/8 bits)
    val axis =slave Stream(Bits(Datawidthin bits))
    val dmaWrapper = master Stream(Bits(Datawidthout bits))
  }

  val validdata = Stream(Bits(Datawidthout bits))//所有有效数据

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
  val count = Counter(0 until datavec.length)
  val widthcov = new Area {
    val fifofull = Reg(False)
    when(io.axis.fire){
      for(i<-0 until  datavec.length){
        when(isone(i,io.axis_tkeep)){
          fifocach.io.push<-/<Stream(datavec.read(i).asBits)
        }
      }
    }
    fifofull.setWhen(fifocach.io.occupancy === fifocach.io.occupancy.maxValue)
    when(fifofull){
      when(count.value<=datavec.length-1){
        validdata.payload(count.value,8 bits):=fifocach.io.pop
        count.increment()
      }.elsewhen(count.willOverflow){
        fifofull.clear()
        count.clear()
      }
    }

  }
  validdata.valid:= widthcov.fifofull//全满时数据有效
  io.dmaWrapper<-/<validdata
}


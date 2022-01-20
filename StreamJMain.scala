package myLib

import spinal.core.SpinalConfig

object StreamJMain {
  def main(args: Array[String]) {
    SpinalConfig(targetDirectory = "rtl").generateVerilog(StreamJ())
  }
}

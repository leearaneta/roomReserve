object Flash {
  import AppConfig._
  import sys.process._
  def main(args: Array[String]): Unit = {
    def flash(x: DeviceConfig): Unit = {s"particle flash ${x.deviceID} ../particle_binaries/roomReserve.bin" !}
    deviceList.foreach(flash)
  }
}

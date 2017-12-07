object Flash extends App {
  import AppConfig._
  import sys.process._
  def flash(x: DeviceConfig): Unit = {s"particle flash ${x.deviceID} ../particle_binaries/roomReserve.bin" !}
  deviceList.foreach(flash)
}
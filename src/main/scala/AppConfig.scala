object AppConfig {
  import java.io.FileReader
  import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
  import com.fasterxml.jackson.databind.ObjectMapper
  import com.fasterxml.jackson.module.scala.DefaultScalaModule

  case class DeviceConfig(username: String, password: String, deviceID: String)
  case class Configuration(accessToken: String, deviceList: List[DeviceConfig])

  private val configuration: Configuration = {
    val reader = new FileReader("../yaml_stuff/configuration.yaml") // maybe have this as argument
    val mapper = new ObjectMapper(new YAMLFactory())
    mapper.registerModule(DefaultScalaModule)
    mapper.readValue(reader, classOf[Configuration])
  }

  val deviceList: List[DeviceConfig] = configuration.deviceList
  val accessToken: String = configuration.accessToken
}
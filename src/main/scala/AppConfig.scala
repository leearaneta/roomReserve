object AppConfig {
  import java.io.FileReader
  import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
  import com.fasterxml.jackson.databind.ObjectMapper
  import com.fasterxml.jackson.module.scala.DefaultScalaModule

  case class DeviceConfig(username: String, password: String, deviceID: String)
  case class Configuration(accessToken: String, deviceList: List[DeviceConfig])

  private def getConfigFromYAML(filepath: String) = {
    val reader = new FileReader(filepath)
    val mapper = new ObjectMapper(new YAMLFactory())
    mapper.registerModule(DefaultScalaModule)
    mapper.readValue(reader, classOf[Configuration])
  }

  private val yamlPath = "../yaml_stuff/configuration.yaml" // maybe input this as argument
  private val configuration: Configuration = getConfigFromYAML(yamlPath)

  val deviceList: List[DeviceConfig] = configuration.deviceList
  val accessToken: String = configuration.accessToken
}
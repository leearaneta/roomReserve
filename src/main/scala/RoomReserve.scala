import akka.actor._
import akka.util.ByteString
import java.net.InetSocketAddress
import java.text.SimpleDateFormat
import AppConfig._

// microsoft imports
import microsoft.exchange.webservices.data.{core, credential, search}
import core.service.item.Appointment
import core.ExchangeService
import core.enumeration.misc.ExchangeVersion
import core.enumeration.property.WellKnownFolderName
import core.service.folder.CalendarFolder
import credential.WebCredentials
import search.CalendarView

object AppointmentLoader {
  type AppointmentList = java.util.ArrayList[Appointment]
  case object Load
  case class LoadedAppointments(a: AppointmentList)
  def props(username: String, password: String): Props = Props(new AppointmentLoader(username, password))
}

// loads appointments
class AppointmentLoader(username: String, password: String) extends Actor {
  import AppointmentLoader._

  val exchange: ExchangeService = {
    val service = new ExchangeService(ExchangeVersion.Exchange2010_SP2)
    val credentials = new WebCredentials(username, password)
    service.setCredentials(credentials)
    service.setUrl(new java.net.URI("https://east.exch026.serverdata.net/ews/Exchange.asmx"))
    service
  }

  def loadAppointments: LoadedAppointments = {
    val view: CalendarView = {
      val formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
      val startDate = formatter.parse("2017-11-06 09:00:00")
      val endDate = formatter.parse("2017-11-06 18:00:00")
      new CalendarView(startDate, endDate)
    }
    val appointments = CalendarFolder.bind(exchange, WellKnownFolderName.Calendar).findAppointments(view).getItems
    LoadedAppointments(appointments)
  }

  def receive = {
    case Load =>
      println("loading !!")
      context.actorSelection("../AppointmentTracker") ! loadAppointments
    case _ => None
  }
}

// keeps track of current appointments
// processes appointments and sends them to tcpClient
class AppointmentTracker extends Actor {
  import collection.JavaConverters._
  import AppointmentLoader._

  var currentAppointments: AppointmentList = new AppointmentList

  def extractAppointmentDetails(a: Appointment): String = {
    val timeFormat = new SimpleDateFormat("HH:mm")
    // refactor this later
    timeFormat.format(a.getStart) + "|" + timeFormat.format(a.getEnd) + "|" + a.getOrganizer.getName + "|" + a.getSubject
  }

  def compareAppointmentLists(a: AppointmentList, b: AppointmentList): Boolean = {
    def compare(index: Int): Boolean = {
      if (index >= a.size) true
      else if (a.get(index).getSubject == b.get(index).getSubject) compare(index+1)
      else false
    }
    compare(0)
  }

  def receive = {
    case LoadedAppointments(a) if a.size != this.currentAppointments.size || !compareAppointmentLists(a, this.currentAppointments) => {
      val totalComponents: Int = 24
      val appointmentString = a.asScala.map(extractAppointmentDetails).mkString("|") + "|"
      // add dummy components
      val dummyComponents: Int = totalComponents - appointmentString.count(_ == '|')
      val dummyString = " |" * dummyComponents
      val formattedString = appointmentString + dummyString
      println(formattedString)
      context.actorSelection("../TCPClient") ! ByteString(formattedString)
      this.currentAppointments = a
    }
    case _ => None
  }
}

object TCPClient {
  def props(remote: InetSocketAddress, listener: ActorRef): Props = Props(new TCPClient(remote, listener))
}

class TCPClient(remote: InetSocketAddress, listener: ActorRef) extends Actor {
  import akka.io.{IO, Tcp}; import Tcp._
  import context.system

  IO(Tcp) ! Connect(remote)

  def receive: Receive = {
    case CommandFailed(_: Connect) =>
      listener ! "connect failed"
      context stop self
    case c @ Connected(remote, local) =>
      listener ! c
      val connection = sender()
      connection ! Register(self)
      println("tcp client is connected")
      context become {
        case data: ByteString =>
          connection ! Write(data)
        case CommandFailed(w: Write) =>
          // O/S buffer was full
          listener ! "write failed"
        case Received(data) =>
          listener ! data
        case "close" =>
          connection ! Close
        case _: ConnectionClosed =>
          listener ! "connection closed"
          context stop self
      }
  }
}

class DeviceManager(config: DeviceConfig) extends Actor {
  import scala.concurrent.{Future, ExecutionContext, duration}; import duration._
  import scala.util.{Success, Failure}
  import akka.stream.ActorMaterializer

  import play.api.libs.ws.{ahc, JsonBodyReadables}; import ahc.StandaloneAhcWSClient; import JsonBodyReadables._
  import play.api.libs.json.JsValue
  import AppointmentLoader._
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val dispatcher: ExecutionContext = context.system.dispatcher

  val wsClient = StandaloneAhcWSClient()
  val url = s"https://api.particle.io/v1/devices/${config.deviceID}/ipAddress?access_token=$accessToken"
  val f: Future[String] = wsClient.url(url).get().map { response => (response.body[JsValue] \ "result").as[String]}
  f.onComplete {
    case Success(hostname) => {
      wsClient.close()
      val address: InetSocketAddress = new InetSocketAddress(hostname, 23)
      val loader: ActorRef = context.actorOf(AppointmentLoader.props(config.username, config.password), "AppointmentLoader")
      val tracker: ActorRef = context.actorOf(Props[AppointmentTracker], "AppointmentTracker")
      context.actorOf(TCPClient.props(address, tracker), "TCPClient")
      context.system.scheduler.schedule(0 seconds, 5 seconds, loader, Load)
    }
    case Failure(t) => {
      // shut down actor and try again?
      println("An error has occurred: " + t.getMessage)
      wsClient.close()
    }
  }

  def receive = {
    case _ => None
  }
}

object RoomReserve {
  def main(args: Array[String]) {
    import scala.io.StdIn
    val system: ActorSystem = ActorSystem("RoomReserve")

    try {
      def createDeviceManager(x: DeviceConfig): Unit = system.actorOf(Props(new DeviceManager(x)))

      deviceList.foreach(createDeviceManager)
      println(">>> Press ENTER to exit <<<")
      StdIn.readLine()

    } finally {
      system.terminate()

    }
  }
}
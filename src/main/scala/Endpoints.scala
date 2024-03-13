import Fruit.{Apple, Potato}
import cats.effect.IO
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredCodec
import sttp.capabilities.WebSockets
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter

sealed trait Fruit

object Fruit {
  case class Apple(color: String) extends Fruit

  case class Potato(weight: Double) extends Fruit

  private implicit val config: Configuration = Configuration.default
  implicit val fruitCodec: io.circe.Codec[Fruit] = deriveConfiguredCodec
}

object Endpoints {
  val ws = endpoint.get
    .in("ws")
    .out(
      webSocketBody[Fruit, CodecFormat.Json, Fruit, CodecFormat.Json](Fs2Streams[IO])
        .requestsExample(Apple("red"))
        .requestsExample(Potato(1.0))
    )

  val wsServerEndpoint = ws.serverLogicSuccess[IO](_ => IO(identity))

  val apiEndpoints = List(wsServerEndpoint)

  val docEndpoints: Seq[ServerEndpoint[Any, IO]] = SwaggerInterpreter()
    .fromServerEndpoints[IO](apiEndpoints, "tapir-union-types-schema", "1.0.0")

  val all: List[ServerEndpoint[Fs2Streams[IO] with WebSockets, IO]] = apiEndpoints ++ docEndpoints
}

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

  private implicit val circeConfig: Configuration = Configuration.default
  implicit val fruitCodec: io.circe.Codec[Fruit] = deriveConfiguredCodec

  def examples: List[Fruit] = List(Apple("red"), Potato(1.0))
}


sealed trait Fruit2

object Fruit2 {
  case class Apple(color: String) extends Fruit2

  case class Potato(weight: Double) extends Fruit2

  private implicit val circeConfig: Configuration = Configuration.default.withDiscriminator("fruit")
  implicit val fruitCodec: io.circe.Codec[Fruit2] = deriveConfiguredCodec

  private implicit val tapirConfig = sttp.tapir.generic.Configuration.default.withDiscriminator("fruit")
  implicit val fruitSchema: sttp.tapir.Schema[Fruit2] = sttp.tapir.Schema.derived

  def examples: List[Fruit2] = List(Apple("red"), Potato(1.0))
}

object Endpoints {
  val ws = endpoint.get
    .in("ws")
    .out(
      webSocketBody[Fruit2, CodecFormat.Json, Fruit2, CodecFormat.Json](Fs2Streams[IO])
        .responsesExamples(Fruit2.examples)
    )

  val wsServerEndpoint = ws.serverLogicSuccess[IO](_ => IO(identity))

  val apiEndpoints = List(wsServerEndpoint)

  val docEndpoints: Seq[ServerEndpoint[Any, IO]] = SwaggerInterpreter()
    .fromServerEndpoints[IO](apiEndpoints, "tapir-union-types-schema", "1.0.0")

  val all: List[ServerEndpoint[Fs2Streams[IO] with WebSockets, IO]] = apiEndpoints ++ docEndpoints
}

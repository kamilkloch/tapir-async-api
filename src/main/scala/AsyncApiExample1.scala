import cats.effect.IO
import cats.implicits.toFunctorOps
import io.circe.syntax.EncoderOps
import sttp.apispec.asyncapi.circe.yaml.RichAsyncAPI
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir._
import sttp.tapir.docs.asyncapi.AsyncAPIInterpreter
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

/** Invalid encoding of examples.
  *
  * Result:
  * ```
  * messages:
  *  Fruit:
  *    payload:
  *      $ref: '#/components/schemas/Fruit'
  *    contentType: application/json
  *    examples:
  *    - payload:
  *      - color: red
  *      - weight: 1.0
  * ```
  *
  * Expected
  * ```
  * messages:
  *  Fruit:
  *    payload:
  *      $ref: '#/components/schemas/Fruit'
  *    contentType: application/json
  *    examples:
  *    - payload:
  *        color: red
  *    - payload:
  *        weight: 1.0
  * ```
  *
  * Note circe codec correctly encodes `Apple("red")` as `{"color":"red"}` (no discriminant needed - schemas for coproduct elements are
  * incompatible between each other).
  */
object AsyncApiExample1 {

  sealed trait Fruit

  object Fruit {

    case class Apple(color: String) extends Fruit
    implicit val appleCodec = io.circe.generic.semiauto.deriveCodec[Apple]

    case class Potato(weight: Double) extends Fruit
    implicit val potatoCodec = io.circe.generic.semiauto.deriveCodec[Potato]

    implicit val encodeFruit: io.circe.Encoder[Fruit] = io.circe.Encoder.instance {
      case v: Apple  => v.asJson
      case v: Potato => v.asJson
    }

    implicit val decodeFruit: io.circe.Decoder[Fruit] =
      List[io.circe.Decoder[Fruit]](
        io.circe.Decoder[Apple].widen,
        io.circe.Decoder[Potato].widen
      ).reduceLeft(_ or _)
  }

  val ws = endpoint.get
    .in("ws")
    .out(
      webSocketBody[Fruit, CodecFormat.Json, Fruit, CodecFormat.Json](Fs2Streams[IO])
        .responsesExample(Fruit.Apple("red"))
        .responsesExample(Fruit.Potato(1.0))
    )

  val asyncApiYaml = AsyncAPIInterpreter()
    .toAsyncAPI(ws, "web socket", "1.0")
    .toYaml

  println(Fruit.Apple("red").asJson.noSpaces)
  def main(args: Array[String]): Unit = println(asyncApiYaml)
}

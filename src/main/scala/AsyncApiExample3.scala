import cats.effect.IO
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredCodec
import sttp.apispec.asyncapi.circe.yaml.RichAsyncAPI
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir._
import sttp.tapir.docs.asyncapi.AsyncAPIInterpreter
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

/** Broken rendering of examples.
 *
 * Result:
 * ```
 *   messages:
 *   Apple:
 *     examples:
 *     - payload:
 *       - color: red
 *       - color: green
 * ```
 *
 * Expected:
 * ```
 *   messages:
 *   Apple:
 *     examples:
 *     - payload:
 *       color: red
 *     - payload:
 *       color: green
 * ```
 */
object AsyncApiExample3 {

  case class Apple(color: String)

  private implicit val circeConfig: Configuration = Configuration.default
  implicit val fruitCodec: io.circe.Codec[Apple] = deriveConfiguredCodec

  val ws = endpoint.get
    .in("ws")
    .out(
      webSocketBody[Apple, CodecFormat.Json, Apple, CodecFormat.Json](Fs2Streams[IO])
        .responsesExample(Apple("red"))
        .responsesExample(Apple("green"))
    )

  val asyncApiYaml = AsyncAPIInterpreter()
    .toAsyncAPI(ws, "web socket", "1.0")
    .toYaml

  def main(args: Array[String]): Unit = println(asyncApiYaml)
}

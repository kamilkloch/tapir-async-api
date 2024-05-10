import cats.effect.IO
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredCodec
import io.circe.syntax.EncoderOps
import sttp.apispec.asyncapi.SingleMessage
import sttp.apispec.asyncapi.circe._
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.EndpointIO.Example
import sttp.tapir._
import sttp.tapir.docs.asyncapi.AsyncAPIInterpreter
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

/** Broken rendering of examples.
  *
  * ```
  * List(Map(payload -> List(ExampleSingleValue({"color":"red"}), ExampleSingleValue({"color":"green"}))))
  * [
  *   {
  *     "payload" : [
  *       {
  *         "color" : "red"
  *       },
  *       {
  *         "color" : "green"
  *       }
  *     ]
  *   }
  * ]
  * ```
  */
object AsyncApiExample3b {

  case class Apple(color: String)
  case class Apple2(color: String)

  private implicit val circeConfig: Configuration = Configuration.default
  implicit val fruitCodec: io.circe.Codec[Apple] = deriveConfiguredCodec
  implicit val fruitCodec2: io.circe.Codec[Apple2] = deriveConfiguredCodec

  implicit class ExampleEx[PIPE_REQ_RESP, REQ, RESP, T, S](a: WebSocketBodyOutput[PIPE_REQ_RESP, REQ, RESP, T, S]) {
    def responsesExample(e: Example[RESP]): WebSocketBodyOutput[PIPE_REQ_RESP, REQ, RESP, T, S] = {
      a.copy(responsesInfo = a.responsesInfo.example(e))
    }
  }
  val wsBody = webSocketBody[Apple, CodecFormat.Json, Apple2, CodecFormat.Json](Fs2Streams[IO])
    .responsesExample(Example(Apple2("red"), Some("Red apple"), Some("Red apple example"), Some("Red apple served response")))
    .responsesExample(Apple2("green"))

  val wsExamples = wsBody.responsesInfo.examples

  val ws = endpoint.get
    .in("ws")
    .out(wsBody)

  val asyncApi = AsyncAPIInterpreter()
    .toAsyncAPI(ws, "web socket", "1.0")

  val message = asyncApi.components.get.messages("Apple2").toOption.get.asInstanceOf[SingleMessage].examples

  def main(args: Array[String]): Unit = {
    println(s"Examples: $wsExamples")
    println(s"Message: $message")
    println(s"Message: ${message.asJson.deepDropNullValues}")
  }
}

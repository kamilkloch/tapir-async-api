import cats.effect.IO
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredCodec
import io.circe.syntax.EncoderOps
import sttp.apispec.asyncapi.circe.yaml.RichAsyncAPI
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir._
import sttp.tapir.docs.apispec.schema.{MetaSchemaDraft04, TapirSchemaToJsonSchema}
import sttp.tapir.docs.asyncapi.AsyncAPIInterpreter
import sttp.tapir.json.circe._

/** Use of discriminator, resulting yaml does not validate. Problematic part:
  * ```
  * discriminator:
  *   propertyName: fruit
  *   mapping:
  *     Apple: '#/components/schemas/Apple'
  *     Potato: '#/components/schemas/Potato'
  * ```
  *
  * It should explicitly provide discriminator values using `const` override.
  * ```
  * Fruit:
  *   title: Fruit
  *   oneOf:
  *   - $ref: '#/components/schemas/Apple'
  *   - $ref: '#/components/schemas/Potato'
  *   discriminator: fruit
  * Apple:
  *   title: Apple
  *   type: object
  *   required:
  *   - color
  *   - fruit
  *   properties:
  *     color:
  *       type: string
  *     fruit:
  *       type: string
  *       const: Apple
  * ```
  *
  * Also note `sttp.tapir.Schema` lacks `const: Option[T]` property which would be needed to represent such schemas.
  */
object AsyncApiExample2 {

  sttp.tapir.Schema

  sealed trait Fruit

  object Fruit {
    case class Apple(color: String) extends Fruit

    case class Potato(weight: Double) extends Fruit

    private implicit val circeConfig: Configuration = Configuration.default.withDiscriminator("fruit")
    implicit val fruitCodec: io.circe.Codec[Fruit] = deriveConfiguredCodec

    private implicit val tapirConfig = sttp.tapir.generic.Configuration.default.withDiscriminator("fruit")
    implicit val fruitSchema: sttp.tapir.Schema[Fruit] = sttp.tapir.Schema.derived
  }

  val ws = endpoint.get
    .in("ws")
    .out(
      webSocketBody[Fruit, CodecFormat.Json, Fruit, CodecFormat.Json](Fs2Streams[IO])
        .responsesExample(Fruit.Apple("red"))
        .responsesExample(Fruit.Potato(1.0))
    )

  // print raw jsonschema for Fruit
  val jsonSchema = TapirSchemaToJsonSchema(Fruit.fruitSchema, markOptionsAsNullable = true, metaSchema = MetaSchemaDraft04)
  import sttp.apispec.circe._
  println(jsonSchema.asJson.deepDropNullValues)

  // print async api
  val asyncApiYaml = AsyncAPIInterpreter()
    .toAsyncAPI(ws, "web socket", "1.0")
    .toYaml

  def main(args: Array[String]): Unit = println(asyncApiYaml)
}

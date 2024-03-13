import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s.{Host, IpLiteralSyntax, Port}
import org.http4s.ember.server.EmberServerBuilder
import sttp.apispec.asyncapi.circe.yaml.RichAsyncAPI
import sttp.tapir.docs.asyncapi.AsyncAPIInterpreter
import sttp.tapir.server.http4s.Http4sServerInterpreter

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    val routes = Http4sServerInterpreter[IO]().toWebSocketRoutes(Endpoints.all)

    val asyncApiYaml = AsyncAPIInterpreter()
      .toAsyncAPI(Endpoints.ws, "web socket", "1.0")
      .toYaml

    val port = sys.env
      .get("HTTP_PORT")
      .flatMap(_.toIntOption)
      .flatMap(Port.fromInt)
      .getOrElse(port"8080")

    EmberServerBuilder
      .default[IO]
      .withHost(Host.fromString("localhost").get)
      .withPort(port)
      .withHttpWebSocketApp(wsb2 => routes(wsb2).orNotFound)
      .build
      .use { server =>
        for {
          _ <- IO.println(s"Go to http://localhost:${server.address.getPort}/docs to open SwaggerUI. Press ENTER key to exit.")
          _ <- IO.println(asyncApiYaml)
          _ <- IO.readLine
        } yield ()
      }
      .as(ExitCode.Success)
  }
}

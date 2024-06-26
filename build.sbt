val tapirVersion = "1.10.6"
val sttpApiSpecVersion = "0.10.0"

lazy val rootProject = (project in file(".")).settings(
  Seq(
    name := "tapir-async-api",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "2.13.14",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
      "org.http4s" %% "http4s-ember-server" % "0.23.25",
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-asyncapi-docs" % tapirVersion,
      "com.softwaremill.sttp.apispec" %% "asyncapi-circe-yaml" % sttpApiSpecVersion,
      "io.circe" %% "circe-generic-extras" % "0.14.3",
      "ch.qos.logback" % "logback-classic" % "1.5.6",
      "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % tapirVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.18" % Test,
      "com.softwaremill.sttp.client3" %% "circe" % "3.9.5" % Test
    )
  )
)

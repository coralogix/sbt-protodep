logLevel := Level.Debug

Global / protodepUseHttps := true
enablePlugins(Protodep)

ThisBuild / scalaVersion := "2.13.3"

lazy val protodeps = Protodep.generateProject("grpc-deps")

lazy val root = (project in file("."))
  .settings(
    scalaVersion := "2.13.3",
    scalacOptions ++= Seq("-verbose")
  )
  .dependsOn(protodeps)

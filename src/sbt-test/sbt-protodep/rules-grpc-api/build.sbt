logLevel := Level.Debug

Global / protodepUseHttps := false
enablePlugins(Protodep)

ThisBuild / scalaVersion := "2.13.12"

lazy val protodeps = Protodep.generateProject("grpc-deps")

lazy val root = (project in file("."))
  .settings(
    scalacOptions ++= Seq("-verbose")
  )
  .dependsOn(protodeps)

ThisBuild / organization := "com.coralogix"
ThisBuild / scalaVersion := "2.12.12"

ThisBuild / licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

ThisBuild / dynverSonatypeSnapshots := true

lazy val root = (project in file("."))
  .settings(
    name      := "sbt-protodep",
    sbtPlugin := true,
    addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.0-RC4"),
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb.zio-grpc" %% "zio-grpc-codegen" % "0.4.2",
      "org.apache.commons"             % "commons-compress" % "1.20",
      "dev.zio"                       %% "zio-test"         % "1.0.3" % Test,
      "dev.zio"                       %% "zio-test-sbt"     % "1.0.3" % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false
  )
  .enablePlugins(SbtPlugin)

inThisBuild(
  List(
    scalaVersion            := "2.12.18",
    dynverSonatypeSnapshots := true,
    organization            := "com.coralogix",
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
    homepage := Some(url("https://github.com/coralogix/sbt-protodep")),
    developers := List(
      Developer(
        "vigoo",
        "Daniel Vigovszky",
        "daniel.vigovszky@gmail.com",
        url("https://www.coralogix.com")
      ),
      Developer(
        "zhrebicek",
        "Zdenek Hrebicek",
        "zdenek.hrebicek@gmail.com",
        url("https://www.coralogix.com")
      )
    )
  )
)

lazy val root = (project in file("."))
  .settings(
    name      := "sbt-protodep",
    sbtPlugin := true,
    addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.6"),
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb.zio-grpc" %% "zio-grpc-codegen" % "0.6.1",
      "org.apache.commons"             % "commons-compress" % "1.24.0",
      "dev.zio"                       %% "zio-test"         % "2.0.19" % Test,
      "dev.zio"                       %% "zio-test-sbt"     % "2.0.19" % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false,
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.6.2"
      }
    }
  )
  .enablePlugins(SbtPlugin)

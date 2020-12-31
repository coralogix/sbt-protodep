import sbt._
import Keys._
import sbtprotoc.ProtocPlugin.autoImport.PB
import sbtprotoc.ProtocPlugin

import scala.sys.process.Process

/** Plugin to be applied to a subproject responsible for compiling the imported protobuf definitions.
  *
  * Instead of enabling this plugin manually, use the 'Protodep' plugin that configures a subproject
  * using this plugin.
  */
object GrpcDependencies extends AutoPlugin {
  object autoImport {
    val protodepRoot = settingKey[File]("Directory where the protodep.toml file is")
    val protodepUp = taskKey[Unit]("Runs 'protodep up -f' but only if protodep.toml has changed")
    val forcedProtodepUp = taskKey[Unit]("Runs 'protodep up -f'")
  }

  import autoImport._

  override val requires = ProtocPlugin

  override lazy val projectSettings = Seq(
    libraryDependencies ++= Seq(
      // gRPC
      "com.thesamet.scalapb"               %% "scalapb-runtime-grpc"                    % scalapb.compiler.Version.scalapbVersion,
      "io.grpc"                             % "grpc-netty"                              % "1.34.0",
      "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.10" % "1.17.0-0" % "protobuf",
      "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.10" % "1.17.0-0",
      "io.github.scalapb-json"             %% "scalapb-circe"                           % "0.7.1"
    ),
    PB.targets in Compile := Seq(
      scalapb.gen(grpc = true)          -> (sourceManaged in Compile).value / "scalapb",
      scalapb.zio_grpc.ZioCodeGenerator -> (sourceManaged in Compile).value / "scalapb"
    ),
    (compile in Compile) := ((compile in Compile) dependsOn protodepUp).value,
    protodepRoot         := (ThisBuild / baseDirectory).value,
    protodepUp           := protodepUpTask.value,
    forcedProtodepUp     := forcedProtodepUpTask.value
  )

  private lazy val protodepUpTask = Def.task {
    import sbt.util.CacheImplicits._

    val s = streams.value
    val previous = protodepUp.previous
    val root = protodepRoot.value
    val protodepBinary = Protodep.autoImport.protodepBinary.value

    def run(): Unit =
      protodepBinary.up(root, forced = true)

    val cachedProtodepUp = Tracked.inputChanged[HashModifiedFileInfo, Unit](
      s.cacheStoreFactory.make("protodep.toml")
    ) { (changed: Boolean, in: HashModifiedFileInfo) =>
      previous match {
        case None =>
          run()
        case Some(_) =>
          if (changed)
            run()
      }
    }

    cachedProtodepUp(FileInfo.full(root / "protodep.toml"))
  }

  private lazy val forcedProtodepUpTask = Def.task {
    val protodepBinary = Protodep.autoImport.protodepBinary.value
    val root = protodepRoot.value
    protodepBinary.up(root, forced = true)
  }
}

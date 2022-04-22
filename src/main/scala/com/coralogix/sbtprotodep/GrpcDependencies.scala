package com.coralogix.sbtprotodep

import sbt.Keys._
import sbt._
import sbtprotoc.ProtocPlugin
import sbtprotoc.ProtocPlugin.autoImport.PB
import scalapb.GeneratorOption

/** Plugin to be applied to a subproject responsible for compiling the imported protobuf definitions.
  *
  * Instead of enabling this plugin manually, use the 'Protodep' plugin that configures a subproject
  * using this plugin.
  */
object GrpcDependencies extends AutoPlugin {
  object autoImport {
    val protodepRoot =
      settingKey[File]("Directory where the protodep.toml / protofetch.toml file is")
    val protodepUseHttps = settingKey[Boolean]("If true, backend will use HTTPS instead of SSH")
    val protodepFetchProtoFiles = taskKey[Unit](
      "Runs 'protodep up -f' or 'protofetch fetch -f' but only if protodep.toml has changed"
    )
    val forcedProtodepFetchProtoFiles =
      taskKey[Unit]("Runs 'protodep up -f' or 'protofetch fetch -f'")
    val scalapbGeneratorOptions =
      settingKey[Seq[GeneratorOption]]("Generator options to be used with scalapb")
  }

  import autoImport._

  override val requires = ProtocPlugin

  override lazy val globalSettings = Seq(
    protodepUseHttps := false
  )

  override lazy val projectSettings = Seq(
    libraryDependencies ++= Seq(
      // gRPC
      "com.thesamet.scalapb"               %% "scalapb-runtime-grpc"                    % scalapb.compiler.Version.scalapbVersion,
      "io.grpc"                             % "grpc-netty"                              % "1.40.1",
      "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.11" % "2.5.0-2" % "protobuf",
      "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.11" % "2.5.0-2",
      "io.github.scalapb-json"             %% "scalapb-circe"                           % "0.11.1"
    ),
    scalapbGeneratorOptions := Seq(GeneratorOption.Grpc),
    Compile / PB.targets := Seq(
      scalapb.gen(
        scalapbGeneratorOptions.value.toSet
      )                                 -> (Compile / sourceManaged).value / "scalapb",
      scalapb.zio_grpc.ZioCodeGenerator -> (Compile / sourceManaged).value / "scalapb"
    ),
    (Compile / PB.generate)       := ((Compile / PB.generate) dependsOn protodepFetchProtoFiles).value,
    protodepRoot                  := (ThisBuild / baseDirectory).value,
    protodepFetchProtoFiles       := protodepUpTask.value,
    forcedProtodepFetchProtoFiles := forcedProtodepUpTask.value
  )

  private lazy val protodepUpTask = Def.task {
    import sbt.util.CacheImplicits._

    val s = streams.value
    val previous = protodepFetchProtoFiles.previous
    val root = protodepRoot.value
    val protodepBinary = Protodep.autoImport.backendBinary.value
    val https = protodepUseHttps.value

    def run(): Unit =
      protodepBinary.fetchProtoFiles(root, forced = true, cleanup = true, https)

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
    val protodepBinary = Protodep.autoImport.backendBinary.value
    val root = protodepRoot.value
    val https = protodepUseHttps.value
    protodepBinary.fetchProtoFiles(root, forced = true, cleanup = true, https)
  }
}

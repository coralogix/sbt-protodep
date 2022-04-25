package com.coralogix.sbtprotodep

import com.coralogix.sbtprotodep.backends.{ BackendBinary, BackendType }
import sbt.Keys._
import sbt._

/** Plugin that carries setting keys for Protodep. */
object Protodep extends AutoPlugin {
  object autoImport {
    val protodepBackendVersion = taskKey[String]("Protodep version to use")
    val protodepBackendRepo = taskKey[String]("Protodep repository to use")
    val protodepBackendBinary = taskKey[BackendBinary]("Downloads and unpacks backend")
  }

  import autoImport._

  // This can be useful for projects using `sbt-projectmatrix`
  //   you need to do there .enablePlugins(GrpcDependencies).settings(Protodep.protodepSettings)
  lazy val protodepSettings = Seq(
    protodepBackendVersion := "v0.1.6",
    protodepBackendRepo    := "stormcat24",
    protodepBackendBinary := {
      BackendBinary(
        streams.value.log,
        protodepBackendRepo.value,
        protodepBackendVersion.value,
        targetRoot = Some(target.value),
        backendType = BackendType.Protodep
      )
    }
  )

  // This can be usefully for projects using `sbt-projectmatrix`
  //   you need to do there .enablePlugins(GrpcDependencies).settings(Protodep.protofetchSettings)
  lazy val protofetchSettings = Seq(
    protodepBackendVersion := "v0.0.5",
    protodepBackendRepo    := "coralogix",
    protodepBackendBinary := {
      BackendBinary(
        streams.value.log,
        protodepBackendRepo.value,
        protodepBackendVersion.value,
        targetRoot = Some(target.value),
        forceDownload = true,
        backendType = BackendType.Protofetch
      )
    }
  )

  def generateProject(
    name: String,
    path: Option[String] = None,
    backend: BackendType = BackendType.Protodep
  ): Project =
    Project(name, file(path.getOrElse(name)))
      .enablePlugins(GrpcDependencies)
      .settings(
        backend match {
          case BackendType.Protodep   => protodepSettings
          case BackendType.Protofetch => protofetchSettings
        }
      )
}

package com.coralogix.sbtprotodep

import com.coralogix.sbtprotodep.backends.{ BackendBinary, BackendType }
import sbt.Keys._
import sbt._

/** Plugin that carries setting keys for Protodep. */
object Protodep extends AutoPlugin {
  object autoImport {
    val backendVersion = taskKey[String]("Protodep version to use")
    val backendRepo = taskKey[String]("Protodep repository to use")
    val backendBinary = taskKey[BackendBinary]("Downloads and unpacks backend")
  }

  import autoImport._

  // TODO add support for project matrix
  // def generateProjectMatrix(state: State): Seq[(String, String)] = {

  def generateProject(
    name: String,
    path: Option[String] = None,
    backend: BackendType = BackendType.Protodep
  ): Project =
    Project(name, file(path.getOrElse(name)))
      .enablePlugins(GrpcDependencies)
      .settings(
        backend match {
          case BackendType.Protodep =>
            Seq(
              backendVersion := "v0.1.6",
              backendRepo    := "stormcat24",
              backendBinary := {
                BackendBinary(
                  streams.value.log,
                  backendRepo.value,
                  backendVersion.value,
                  targetRoot = Some(target.value),
                  backendType = BackendType.Protodep
                )
              }
            )
          case BackendType.Protofetch =>
            Seq(
              backendVersion := "v0.0.3",
              backendRepo    := "coralogix",
              backendBinary := {
                BackendBinary(
                  streams.value.log,
                  backendRepo.value,
                  backendVersion.value,
                  targetRoot = Some(target.value),
                  forceDownload = true,
                  backendType = BackendType.Protofetch
                )
              }
            )
        }
      )
}

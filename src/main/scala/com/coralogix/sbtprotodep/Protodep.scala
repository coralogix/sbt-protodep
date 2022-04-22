package com.coralogix.sbtprotodep

import com.coralogix.sbtprotodep.backends.{ BackendBinary, BackendType }
import sbt.Keys._
import sbt._

/** Plugin that carries setting keys for Protodep. */
object Protodep extends AutoPlugin {
  object autoImport {
    val protodepVersion = taskKey[String]("Protodep version to use")
    val protodepRepo = taskKey[String]("Protodep repository to use")
    val protodepBinary = taskKey[BackendBinary]("Downloads and unpacks backend")
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
              // v prefix is just an inconsistency added in 0.1.6
              protodepVersion := "v0.1.6",
              protodepRepo    := "stormcat24",
              protodepBinary := {
                BackendBinary(
                  streams.value.log,
                  protodepRepo.value,
                  protodepVersion.value,
                  targetRoot = Some(target.value),
                  backend = BackendType.Protodep
                )
              }
            )
          case BackendType.Protofetch =>
            Seq(
              protodepVersion := "v0.0.3",
              protodepRepo    := "coralogix",
              protodepBinary := {
                BackendBinary(
                  streams.value.log,
                  protodepRepo.value,
                  protodepVersion.value,
                  targetRoot = Some(target.value),
                  backend = BackendType.Protofetch
                )
              }
            )
        }
      )
}

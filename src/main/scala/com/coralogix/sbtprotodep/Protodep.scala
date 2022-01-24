package com.coralogix.sbtprotodep

import sbt._
import Keys._
import com.coralogix.sbtprotodep.protodep.ProtodepBinary

/** Plugin that carries setting keys for Protodep. */
object Protodep extends AutoPlugin {
  object autoImport {
    val protodepVersion = taskKey[String]("Protodep version to use")
    val protodepRepo = taskKey[String]("Protodep repository to use")
    val protodepBinary = taskKey[ProtodepBinary]("Downloads and unpacks protodep")
  }

  import autoImport._

  def generateProject(path: String, name: Option[String] = None) =
    Project(name.getOrElse(path), file(path))
      .enablePlugins(GrpcDependencies)
      .settings(
        protodepVersion := "0.1.2-1-ge811cd8",
        protodepRepo    := "vigoo",
        protodepBinary := {
          ProtodepBinary(
            streams.value.log,
            protodepRepo.value,
            protodepVersion.value,
            targetRoot = Some(target.value)
          )
        }
      )
}

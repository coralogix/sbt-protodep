package com.coralogix.sbtprotodep.backends

import sbt.util.Logger

import java.io.File
import scala.language.postfixOps
import scala.sys.process.Process
import scala.util.{ Success, Try }

class ProtodepBinary(
  log: Logger,
  val binary: File
) extends BackendBinary {
  val backend: BackendType = BackendType.Protodep

  def isVersion(desiredVersion: String): Boolean =
    version().exists(_.endsWith("-" + desiredVersion))

  private[backends] def version(): Option[String] = {
    val versionLine = Try(Process(binary.toString :: "version" :: Nil).lineStream(log).last)
    log.info(s"$binary version returned $versionLine")
    versionLine match {
      case Success(utils.protodepVersionMatcher(version, gitCommit, gitTag, buildDate)) =>
        Some(version)
      case _ => None
    }
  }

  def fetchProtoFiles(root: File, ci: Boolean, https: Boolean): Unit = {
    val args =
      List(
        Some("--cleanup"),
        if (https) Some("--use-https") else None
      ).flatten
    Process(binary.toString :: "up" :: args, root) ! log
  }

  def updateProtoFiles(root: File, https: Boolean): Unit = {
    val args =
      List(
        Some("-f"),
        Some("--cleanup"),
        if (https) Some("--use-https") else None
      ).flatten
    Process(binary.toString :: "up" :: args, root) ! log
  }
}

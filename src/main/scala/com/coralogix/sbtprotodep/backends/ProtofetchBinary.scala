package com.coralogix.sbtprotodep.backends

import sbt.util.Logger

import java.io.File
import scala.language.postfixOps
import scala.sys.process.Process
import scala.util.{ Success, Try }

class ProtofetchBinary(
  log: Logger,
  val binary: File
) extends BackendBinary {
  val backend: BackendType = BackendType.Protofetch
  def isVersion(desiredVersion: String): Boolean =
    version().exists(_.endsWith(desiredVersion.stripPrefix("v")))

  private[backends] def version(): Option[String] = {
    val versionLine = Try(Process(binary.toString :: "--version" :: Nil).lineStream(log).last)
    versionLine match {
      case Success(utils.protofetchVersionMatcher(version)) => Some(version)
      case _                                                => None
    }
  }

  def fetchProtoFiles(root: File, locked: Boolean, https: Boolean): Unit = {
    val args = if (locked) List("--locked") else Nil
    log.debug(s"Using binary: ${binary.toString}")
    Process(binary.toString :: "fetch" :: args, root) ! log
  }

  def updateProtoFiles(root: File, https: Boolean): Unit = {
    log.debug(s"Using binary: ${binary.toString}")
    Process(binary.toString :: "update" :: Nil, root) ! log
    Process(binary.toString :: "fetch" :: Nil, root) ! log
  }

}

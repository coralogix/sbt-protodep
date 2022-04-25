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
    val versionLine = Try(Process(binary.toString :: "version" :: Nil).lineStream(log).last)
    versionLine match {
      case Success(utils.versionMatcher(version, gitCommit, gitTag, buildDate)) => Some(version)
      case _                                                                    => None
    }
  }

  def fetchProtoFiles(root: File, forced: Boolean, cleanup: Boolean, https: Boolean): Unit = {
    println("Ignoring force, cleanup, https flags as protofetch does not support them.")
    Process(binary.toString :: "fetch" :: List(), root) ! log
  }
}

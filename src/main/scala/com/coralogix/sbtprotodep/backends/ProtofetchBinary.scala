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

  //TODO: Protofetch does not support some of these flags. As protofetch evolves, if none of these make sense consider changing the base trait
  def fetchProtoFiles(root: File, forced: Boolean, cleanup: Boolean, https: Boolean): Unit = {
    println("Ignoring any force, cleanup, https flags as protofetch does not support them")
    val args: List[String] =
      List(
        //        if (forced) Some("-f") else None,
        //        if (cleanup) Some("-c") else None,
        //        if (https) Some("-u") else None
        None
      ).flatten
    println(s"Using binary:  ${binary.toString}")
    Process(binary.toString :: "fetch" :: args, root) ! log
  }
}

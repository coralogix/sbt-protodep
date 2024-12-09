package com.coralogix.sbtprotodep.backends

import sbt.util.{ Level, Logger }

import java.io.File
import scala.language.postfixOps
import scala.sys.env
import scala.sys.process.Process
import scala.util.{ Success, Try }

object ProtofetchBinary {
  private val envLogVar = "RUST_LOG"
}

class ProtofetchBinary(
  log: Logger,
  val binary: File
) extends BackendBinary {
  // foo

  import ProtofetchBinary._

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

  /** Maps `Level.Value` to the equivalent level in `env_logger`. */
  private def envLogLevel(level: Level.Value): String = {
    import Level._

    def value(level: Level.Value): String =
      level match {
        case Info  => "info"
        case Debug => "debug"
        case Warn  => "warn"
        case Error => "error"
      }
    env.getOrElse(envLogVar, value(level))
  }

  private def proc(level: Level.Value)(args: List[String], root: File) = {
    val bin = binary.toString
    log.debug(s"Using binary: $bin")
    Process(bin :: args, root, envLogVar -> envLogLevel(level))
  }

  def fetchProtoFiles(level: Level.Value)(root: File, ci: Boolean, https: Boolean): Int = {
    val args = if (ci) List("--locked") else Nil
    proc(level)("fetch" :: args, root) ! log
  }

  def updateProtoFiles(level: Level.Value)(root: File, https: Boolean): Int = {
    val p = proc(level)("update" :: Nil, root) #&& proc(level)("fetch" :: Nil, root)
    p ! log
  }

}

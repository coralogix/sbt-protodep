package com.coralogix.sbtprotodep.backends

import org.apache.commons.compress.archivers.tar.{ TarArchiveEntry, TarArchiveInputStream }
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.utils.IOUtils
import sbt.util.{ Level, Logger }

import java.io.{ BufferedOutputStream, File, FileOutputStream }
import java.net.URL
import java.nio.file.{ FileSystems, Files }
import java.nio.file.attribute.PosixFilePermission
import java.util
import scala.annotation.tailrec
import scala.language.postfixOps

trait BackendBinary {
  def isVersion(desiredVersion: String): Boolean
  def fetchProtoFiles(level: Level.Value)(root: File, ci: Boolean, https: Boolean): Int
  def updateProtoFiles(level: Level.Value)(root: File, https: Boolean): Int
  val binary: File
  private[backends] def version(): Option[String]
}

object BackendBinary {
  def apply(
    log: Logger,
    repo: String,
    desiredVersion: String,
    targetRoot: Option[File] = None,
    forceDownload: Boolean = false,
    backendType: BackendType
  ): BackendBinary = {
    val makeBinary = (log: Logger, file: File) =>
      backendType match {
        case BackendType.Protodep   => new ProtodepBinary(log, file)
        case BackendType.Protofetch => new ProtofetchBinary(log, file)
      }
    val backendName = backendType.toString.toLowerCase
    val target = targetRoot.getOrElse(Files.createTempDirectory("sbt-protodep").toFile)
    val providedBinary = makeBinary(log, new File(backendName))

    if (!forceDownload && providedBinary.isVersion(desiredVersion)) {
      log.info(s"Using the $backendName binary provided by the system")
      providedBinary
    } else {
      val existingDownloaded = new File(downloadTarget(target, backendType), backendName)
      val existingBinary = makeBinary(log, existingDownloaded)
      if (existingDownloaded.exists() && existingBinary.isVersion(desiredVersion)) {
        log.info(s"Using the previously downloaded $backendName binary $existingDownloaded")
        existingBinary
      } else {
        val downloaded = download(log, target, repo, desiredVersion, backendType)
        val downloadedBinary = makeBinary(log, downloaded)
        log.info(
          s"Expected version is: ${desiredVersion}, binary version is: ${downloadedBinary.version()}"
        )
        assert(downloadedBinary.isVersion(desiredVersion))

        log.info(s"Using the downloaded $backendName binary $downloaded")
        downloadedBinary
      }
    }
  }

  private def downloadTarget(targetRoot: File, backend: BackendType): File =
    new File(targetRoot, backend.toString.toLowerCase)

  private def download(
    log: Logger,
    targetRoot: File,
    repo: String,
    version: String,
    backend: BackendType
  ): File = {
    val downloadUrl = new URL(backend match {
      case BackendType.Protofetch =>
        s"https://github.com/$repo/protofetch/releases/download/$version/protofetch_${platformProtofetch()}.tar.gz"
      case BackendType.Protodep =>
        s"https://github.com/$repo/protodep/releases/download/$version/protodep_${platformProtodep()}.tar.gz"
    })
    val targetDir = downloadTarget(targetRoot, backend)
    log.info(s"Downloading ${backend.toString.toLowerCase} from $downloadUrl to $targetDir")
    targetDir.mkdir()
    downloadAndUnpack(log, backend, downloadUrl, targetDir)
    new File(targetDir, backend.toString.toLowerCase)
  }

  private val permissions = PosixFilePermission.values.reverse
  private def toPermissions(mode: Int): java.util.Set[PosixFilePermission] = {
    val result = util.EnumSet.noneOf[PosixFilePermission](classOf[PosixFilePermission])

    var currentMode = mode
    for (permission <- permissions) {
      if ((currentMode & 1) == 1)
        result.add(permission)
      currentMode = currentMode >> 1
    }

    result
  }

  lazy val isPosix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix")

  @tailrec
  private def unpackEntry(
    log: Logger,
    backend: BackendType,
    stream: TarArchiveInputStream,
    target: File
  ): Unit =
    Option(stream.getNextEntry) match {
      case Some(entry: TarArchiveEntry) =>
        if (entry.isDirectory) {
          val dir = new File(target, entry.getName)
          dir.mkdir()
          log.info(s"Created directory $dir")
        } else {
          val file = backend match {
            case BackendType.Protofetch =>
              new File(target, entry.getName.stripPrefix("bin/"))
            case BackendType.Protodep =>
              new File(target, entry.getName)
          }

          val outputStream = new BufferedOutputStream(new FileOutputStream(file, false))
          try {
            val len = IOUtils.copy(stream, outputStream)
            log.info(s"Unpacked $len bytes to $file")
          } finally outputStream.close()

          if (isPosix) {
            val permissions = toPermissions(entry.getMode)
            Files.setPosixFilePermissions(file.toPath, permissions)
          }
        }
        unpackEntry(log, backend, stream, target)
      case Some(_) =>
        throw new RuntimeException(s"Entry in tar is not a TarAcrhiveEntry")
      case None =>
    }

  private def downloadAndUnpack(
    log: Logger,
    backend: BackendType,
    url: URL,
    targetDir: File
  ): Unit = {
    val stream = new TarArchiveInputStream(new GzipCompressorInputStream(url.openStream()))
    try unpackEntry(log, backend, stream, targetDir)
    finally stream.close()
  }

  private def platformProtodep(): String =
    System.getProperty("os.name").toLowerCase match {
      case mac if mac.contains("mac") =>
        System.getProperty("os.arch").toLowerCase match {
          case arm if arm.contains("aarch64") => "darwin_arm64"
          case _                              => "darwin_amd64"
        }
      case win if win.contains("win") => "windows_amd64"
      case linux if linux.contains("linux") =>
        System.getProperty("os.arch").toLowerCase match {
          case "aarch64"          => "linux_arm64"
          case "x86_64" | "amd64" => "linux_amd64"
        }
      case osName => throw new RuntimeException(s"Unknown operating system $osName")
    }

  private def platformProtofetch(): String =
    System.getProperty("os.name").toLowerCase match {
      case mac if mac.contains("mac") =>
        System.getProperty("os.arch").toLowerCase match {
          case arm if arm.contains("aarch64") => "aarch64-apple-darwin"
          case _                              => "x86_64-apple-darwin"
        }
      case win if win.contains("win") => "x86_64-pc-windows-msvc"
      case linux if linux.contains("linux") =>
        System.getProperty("os.arch").toLowerCase match {
          case "aarch64"          => "aarch64-unknown-linux-musl"
          case "x86_64" | "amd64" => "x86_64-unknown-linux-musl"
        }
      case osName => throw new RuntimeException(s"Unknown operating system $osName")
    }
}

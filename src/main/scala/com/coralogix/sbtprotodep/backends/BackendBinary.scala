package com.coralogix.sbtprotodep.backends

import org.apache.commons.compress.archivers.tar.{ TarArchiveEntry, TarArchiveInputStream }
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.utils.IOUtils
import sbt.util.Logger

import java.io.{ BufferedOutputStream, File, FileOutputStream }
import java.net.{ HttpURLConnection, URL }
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.util
import scala.annotation.tailrec

trait BackendBinary {
  def isVersion(desiredVersion: String): Boolean
  def fetchProtoFiles(root: File, forced: Boolean, cleanup: Boolean, https: Boolean): Unit
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
    backend: BackendType
  ): BackendBinary = {
    val target = targetRoot.getOrElse(Files.createTempDirectory("sbt-protodep").toFile)
    val providedBinary = new ProtodepBinary(log, new File("protodep"))
    if (!forceDownload && providedBinary.isVersion(desiredVersion)) {
      log.info(s"Using the protodep binary provided by the system")
      providedBinary
    } else {
      val existingDownloaded =
        new File(downloadTarget(target, backend), backend.toString.toLowerCase)
      val existingBinary = new ProtodepBinary(log, existingDownloaded)
      if (existingDownloaded.exists() && existingBinary.isVersion(desiredVersion)) {
        log.info(s"Using the previously downloaded protodep binary $existingDownloaded")
        existingBinary
      } else {
        val downloaded = download(log, target, repo, desiredVersion, backend)
        val downloadedBinary = new ProtodepBinary(log, downloaded)
        assert(downloadedBinary.isVersion(desiredVersion))

        log.info(s"Using the downloaded protodep binary $downloaded")
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
        s"https://github.com/$repo/protofetch/releases/download/$version/protofetch_${platform_with_arch()}"
      case BackendType.Protodep =>
        s"https://github.com/$repo/protodep/releases/download/$version/protodep_${platform_with_arch()}.tar.gz"
    })
    val targetDir = downloadTarget(targetRoot, backend)
    log.info(s"Downloading ${backend.toString.toLowerCase} from $downloadUrl to $targetDir")
    targetDir.mkdir()
    downloadAndUnpackIfNeeded(log, downloadUrl, targetDir, backend)
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

  @tailrec
  private def unpackEntry(log: Logger, stream: TarArchiveInputStream, target: File): Unit =
    Option(stream.getNextEntry) match {
      case Some(entry: TarArchiveEntry) =>
        if (entry.isDirectory) {
          val dir = new File(target, entry.getName)
          dir.mkdir()
          log.info(s"Created directory $dir")
        } else {
          val file = new File(target, entry.getName)
          val outputStream = new BufferedOutputStream(new FileOutputStream(file, false))
          try {
            val len = IOUtils.copy(stream, outputStream)
            log.info(s"Unpacked $len bytes to $file")
          } finally outputStream.close()

          val permissions = toPermissions(entry.getMode)
          Files.setPosixFilePermissions(file.toPath, permissions)
        }
        unpackEntry(log, stream, target)
      case Some(_) =>
        throw new RuntimeException(s"Entry in tar is not a TarAcrhiveEntry")
      case None =>
    }

  private def downloadAndUnpackIfNeeded(
    log: Logger,
    url: URL,
    target: File,
    backend: BackendType
  ): Unit =
    backend match {
      case BackendType.Protodep =>
        val stream = new TarArchiveInputStream(new GzipCompressorInputStream(url.openStream()))
        try unpackEntry(log, stream, target)
        finally stream.close()
      case BackendType.Protofetch =>
        val connection = url.openConnection().asInstanceOf[HttpURLConnection]
        try {
          import sys.process._
          connection.setConnectTimeout(5000)
          connection.setReadTimeout(5000)
          connection.connect()
          if (connection.getResponseCode >= 400)
            println(s"Error downloading ${backend.toString.toLowerCase} from $url")
          else
            url #> target !!
        } finally connection.disconnect()
    }

  private def platform_with_arch(): String =
    System.getProperty("os.name").toLowerCase match {
      case mac if mac.contains("mac") =>
        System.getProperty("os.arch").toLowerCase match {
          case arm if arm.contains("aarch64") => "darwin_arm64"
          case _                              => "darwin_amd64"
        }
      case win if win.contains("win")       => "windows_amd64"
      case linux if linux.contains("linux") => "linux_amd64"
      case osName                           => throw new RuntimeException(s"Unknown operating system $osName")
    }
}

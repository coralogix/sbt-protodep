package com.coralogix.sbtprotodep.protodep

import org.apache.commons.compress.archivers.tar.{ TarArchiveEntry, TarArchiveInputStream }
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.utils.IOUtils
import sbt.util.Logger

import java.io.{ BufferedOutputStream, File, FileOutputStream }
import java.net.URL
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.util
import scala.annotation.tailrec
import scala.sys.process.Process
import scala.util.{ Success, Try }

class ProtodepBinary(log: Logger, val binary: File) {

  def isVersion(desiredVersion: String): Boolean =
    version().exists(_.endsWith("-" + desiredVersion))

  private val versionMatcher =
    """\{"Version": "([a-zA-Z0-9\-.]+)", "GitCommit": "([a-zA-Z0-9\-.]+)", "GitCommitFull": "([a-z0-9]+)", "BuildDate": "([a-zA-Z0-9\-.]+)"}""".r
  def version(): Option[String] = {
    val versionLine = Try(Process(binary.toString :: "version" :: Nil).lineStream(log).last)
    log.info(s"$binary version returned $versionLine")
    versionLine match {
      case Success(versionMatcher(version, gitCommit, gitTag, buildDate)) =>
        Some(version)
      case _ =>
        None
    }
  }

  def up(root: File, forced: Boolean, cleanup: Boolean, https: Boolean): Unit = {
    val args =
      List(
        if (forced) Some("-f") else None,
        if (cleanup) Some("-c") else None,
        if (https) Some("-u") else None
      ).flatten
    Process(binary.toString :: "up" :: args, root) ! log
  }
}

object ProtodepBinary {
  def apply(
    log: Logger,
    repo: String,
    desiredVersion: String,
    targetRoot: Option[File] = None,
    forceDownload: Boolean = false
  ): ProtodepBinary = {
    val target = targetRoot.getOrElse(Files.createTempDirectory("sbt-protodep").toFile)
    val providedBinary = new ProtodepBinary(log, new File("protodep"))
    if (!forceDownload && providedBinary.isVersion(desiredVersion)) {
      log.info(s"Using the protodep binary provided by the system")
      providedBinary
    } else {
      val existingDownloaded = new File(downloadTarget(target), "protodep")
      val existingBinary = new ProtodepBinary(log, existingDownloaded)
      if (existingDownloaded.exists() && existingBinary.isVersion(desiredVersion)) {
        log.info(s"Using the previously downloaded protodep binary $existingDownloaded")
        existingBinary
      } else {
        val downloaded = download(log, target, repo, desiredVersion)
        val downloadedBinary = new ProtodepBinary(log, downloaded)
        assert(downloadedBinary.isVersion(desiredVersion))

        log.info(s"Using the downloaded protodep binary $downloaded")
        downloadedBinary
      }
    }
  }

  private def downloadTarget(targetRoot: File): File =
    new File(targetRoot, "protodep")

  private def download(log: Logger, targetRoot: File, repo: String, version: String): File = {
    val downloadUrl = new URL(
      s"https://github.com/$repo/protodep/releases/download/$version/protodep_${platform()}_${arch()}.tar.gz"
    )
    val targetDir = downloadTarget(targetRoot)

    log.info(s"Downloading protodep from $downloadUrl to $targetDir")

    targetDir.mkdir()
    downloadAndUnpack(log, downloadUrl, targetDir)
    new File(targetDir, "protodep")
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

  private def downloadAndUnpack(log: Logger, url: URL, target: File): Unit = {
    val stream = new TarArchiveInputStream(new GzipCompressorInputStream(url.openStream()))
    try unpackEntry(log, stream, target)
    finally stream.close()
  }

  private def arch(): String = "amd64"

  private def platform(): String =
    System.getProperty("os.name").toLowerCase match {
      case mac if mac.contains("mac")       => "darwin"
      case win if win.contains("win")       => "windows"
      case linux if linux.contains("linux") => "linux"
      case osName                           => throw new RuntimeException(s"Unknown operating system $osName")
    }
}

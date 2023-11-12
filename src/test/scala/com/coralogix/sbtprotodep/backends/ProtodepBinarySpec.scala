package com.coralogix.sbtprotodep.backends

import zio._
import zio.test._

import java.nio.file.Files
import com.coralogix.sbtprotodep._

object ProtodepBinarySpec extends ZIOSpecDefault {
  override def spec =
    suite("ProtodepBinary")(
      test("can download and unpack protodep")(
        for {
          tempDir <- ZIO.attempt(Files.createTempDirectory("sbtprotodep"))
          protodepBinary <- ZIO.attempt(
                              BackendBinary(
                                _root_.sbt.util.Logger.Null,
                                "stormcat24",
                                protodepVersion,
                                Some(tempDir.toFile),
                                forceDownload = true,
                                backendType = BackendType.Protodep
                              )
                            )
          path = protodepBinary.binary
          pathExists <- ZIO.attempt(path.exists())
          _          <- Console.printLine(s"Downloaded protodep to $path")
          version    <- ZIO.attempt(protodepBinary.version())
        } yield assertTrue(
          pathExists,
          path.toString.endsWith("/protodep"),
          version.get.endsWith(protodepVersion),
          protodepBinary.isVersion(protodepVersion)
        )
      ),
      test("can download protofetch")(
        for {
          tempDir <- ZIO.attempt(Files.createTempDirectory("sbtprotofetch"))
          protofetchBinary <- ZIO.attempt(
                                BackendBinary(
                                  _root_.sbt.util.Logger.Null,
                                  "coralogix",
                                  protofetchVersion,
                                  Some(tempDir.toFile),
                                  forceDownload = true,
                                  backendType = BackendType.Protofetch
                                )
                              )
          path = protofetchBinary.binary
          pathExists <- ZIO.attempt(path.exists())
          _          <- Console.printLine(s"Downloaded protofetch to $path")
          version    <- ZIO.attempt(protofetchBinary.version())
        } yield assertTrue(
          pathExists,
          path.toString.endsWith("/protofetch"),
          version.get == protofetchVersion.stripPrefix("v"),
          protofetchBinary.isVersion(protofetchVersion)
        )
      )
    )
}

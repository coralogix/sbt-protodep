package com.coralogix.sbtprotodep.backends

import zio.test._
import zio.test.environment.TestEnvironment
import zio.{ console, ZIO }

import java.nio.file.Files
import com.coralogix.sbtprotodep._

object ProtodepBinarySpec extends DefaultRunnableSpec {
  override def spec: ZSpec[TestEnvironment, Any] =
    suite("ProtodepBinary")(
      testM("can download and unpack protodep 0.1.2")(
        for {
          tempDir <- ZIO.effect(Files.createTempDirectory("sbtprotodep"))
          protodepBinary <- ZIO.effect(
                              BackendBinary(
                                _root_.sbt.util.Logger.Null,
                                "stormcat24",
                                "v0.1.7",
                                Some(tempDir.toFile),
                                forceDownload = true,
                                backendType = BackendType.Protodep
                              )
                            )
          path = protodepBinary.binary
          pathExists <- ZIO.effect(path.exists())
          _          <- console.putStrLn(s"Downloaded protodep to $path")
          version    <- ZIO.effect(protodepBinary.version())
        } yield assertTrue(pathExists) &&
          assertTrue(path.toString.endsWith("/protodep")) &&
          assertTrue(version.get == "20220211-v0.1.7")
      ),
      testM("can download protofetch")(
        for {
          tempDir <- ZIO.effect(Files.createTempDirectory("sbtprotofetch"))
          protofetchBinary <- ZIO.effect(
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

          pathExists <- ZIO.effect(path.exists())
          _          <- console.putStrLn(s"Downloaded protofetch to $path")
          version    <- ZIO.effect(protofetchBinary.version())
        } yield assertTrue(pathExists) &&
          assertTrue(path.toString.endsWith("/protofetch")) &&
          assertTrue(version.get == protofetchVersion.stripPrefix("v")) &&
          assertTrue(protofetchBinary.isVersion(protofetchVersion))
      )
    )
}

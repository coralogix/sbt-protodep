package com.coralogix.sbtprotodep.backends

import zio.test._
import zio.test.environment.TestEnvironment
import zio.{ console, ZIO }

import java.nio.file.Files

object ProtodepBinarySpec extends DefaultRunnableSpec {
  override def spec: ZSpec[TestEnvironment, Any] =
    suite("ProtodepBinary")(
      testM("can download and unpack protodep 0.1.2")(
        for {
          tempDir <- ZIO.effect(Files.createTempDirectory("sbtprotodep"))
          protodepBinary <- ZIO.effect(
                              BackendBinary(
                                _root_.sbt.util.Logger.Null,
                                "vigoo",
                                "0.1.2-1-ge811cd8",
                                Some(tempDir.toFile),
                                forceDownload = true,
                                backend = BackendType.Protodep
                              )
                            )
          path = protodepBinary.binary
          pathExists <- ZIO.effect(path.exists())
          _          <- console.putStrLn(s"Downloaded protodep to $path")
          version    <- ZIO.effect(protodepBinary.version())
        } yield assertTrue(pathExists) &&
          assertTrue(path.toString.startsWith("/protodep")) &&
          assertTrue(version.get == "20210105-0.1.2-1-ge811cd8")
      )
    )
}

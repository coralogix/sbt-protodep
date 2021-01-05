package com.coralogix.sbtprotodep.protodep

import zio.console
import zio.ZIO
import zio.test.environment.TestEnvironment
import zio.test._
import zio.test.Assertion._

import java.io.File
import java.nio.file.Files

object ProtodepBinarySpec extends DefaultRunnableSpec {
  override def spec: ZSpec[TestEnvironment, Any] =
    suite("ProtodepBinary")(
      testM("can download and unpack protodep 0.1.2")(
        for {
          tempDir <- ZIO.effect(Files.createTempDirectory("sbtprotodep"))
          protodepBinary <- ZIO.effect(
                              ProtodepBinary(
                                _root_.sbt.util.Logger.Null,
                                "vigoo",
                                "0.1.2-1-ge811cd8",
                                Some(tempDir.toFile),
                                forceDownload = true
                              )
                            )
          path = protodepBinary.binary
          pathExists <- ZIO.effect(path.exists())
          _          <- console.putStrLn(s"Downloaded protodep to $path")
          version    <- ZIO.effect(protodepBinary.version())
        } yield assert(pathExists)(isTrue) &&
          assert(path.toString)(endsWithString("/protodep")) &&
          assert(version)(isSome(equalTo("20210105-0.1.2-1-ge811cd8")))
      )
    )
}

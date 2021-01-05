import sbt._
import Keys._
import com.coralogix.sbtprotodep.protodep.ProtodepBinary

/** Plugin to be enabled top-level that adds a dynamic grpc-deps subproject to the build */
object Protodep extends AutoPlugin {
  object autoImport {
    val protodepVersion = taskKey[String]("Protodep version to use")
    val protodepRepo = taskKey[String]("Protodep repository to use")
    val protodepBinary = taskKey[ProtodepBinary]("Downloads and unpacks protodep")
  }

  import autoImport._

  override lazy val extraProjects: Seq[Project] = Seq(
    Project("grpc-deps", file("grpc-deps"))
      .enablePlugins(GrpcDependencies)
      .settings(
        protodepVersion := "0.1.2-1-ge811cd8",
        protodepRepo := "vigoo",
        protodepBinary := {
          ProtodepBinary(streams.value.log, protodepRepo.value, protodepVersion.value, targetRoot = Some(target.value))
        }
      )
  )
}

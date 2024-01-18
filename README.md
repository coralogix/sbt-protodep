# sbt-protodep
SBT plugin for importing protobuf definitions with these backends:
* [protodep](https://github.com/stormcat24/protodep)
* [protofetch](https://github.com/coralogix/protofetch)

## Usage

In `plugins.sbt`:

```scala
addSbtPlugin("com.thesamet"  % "sbt-protoc" % "1.0.6")
libraryDependencies += "com.thesamet.scalapb.zio-grpc" %% "zio-grpc-codegen" % "0.6.0"
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
addSbtPlugin("com.coralogix"  % "sbt-protodep" % "0.0.14")
```

In `build.sbt`:

```scala
// This applies scalaVersion for the dynamic grpc-deps project
ThisBuild / scalaVersion := "2.13.3"

// You can also pick `BackendType.Protofetch` for backend
lazy val grpcDeps = Protodep
  .generateProject(
    "grpc-deps",
     backendType = BackendType.Protodep
  )

lazy val root = (project in file("."))
  // ...
  .dependsOn(grpcDeps) // Depend on the generated client code
```
To use https instead of ssh set this in your build.sbt
```
Global / protodepUseHttps := true
```

In `protodep.toml`:

```toml
proto_outdir = "./grpc-deps/target/protobuf_external_src"
```

To update protobuf to the latest commit:

```shell
sbt grpc-deps/forcedProtodepFetchProtoFiles
```

### Trick to use it with `sbt-projectmatrix`

```
lazy val grpcDeps = (projectMatrix in file("grpc-deps"))
  .enablePlugins(GrpcDependencies)
  .defaultAxes(VirtualAxis.jvm)
  .settings(Protodep.protodepSettings)
```
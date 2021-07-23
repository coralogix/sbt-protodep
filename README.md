# sbt-protodep
SBT plugin for importing protobuf definitions with protodep

## Usage

In `plugins.sbt`:

```scala
addSbtPlugin("com.thesamet"  % "sbt-protoc" % "1.0.0-RC4")
libraryDependencies += "com.thesamet.scalapb.zio-grpc" %% "zio-grpc-codegen" % "0.4.2"
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
addSbtPlugin("com.coralogix"  % "sbt-protodep" % "0.0.8")
```

In `build.sbt`:

```scala
// This applies scalaVersion for the dynamic grpc-deps project
ThisBuild / scalaVersion := "2.13.3"

lazy val root = (project in file("."))
  // ...
  .dependsOn(LocalProject("grpc-deps")) // Depend on the generated client code
```
To use https instead of ssh set this in your build.sbt
```
Global / protodepUseHttps := true
```
You can specify settings for `grpc-deps` like:

```scala
lazy val grpcDeps = LocalProject("grpc-deps")
grpcDeps / Compile / scalacOptions --= Seq("-Wunused:imports", "-Xfatal-warnings")
```

In `protodep.toml`:

```toml
proto_outdir = "./grpc-deps/target/protobuf_external_src"
```

To update protobuf to the latest commit:

```shell
sbt grpc-deps/forcedProtodepUp
```

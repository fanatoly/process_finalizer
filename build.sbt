name := "Process Finalizer"
version := "0.0.1"

description := "Treasure Data Project"

// Enables publishing to maven repo
publishMavenStyle := true

// Do not append Scala versions to the generated artifacts
crossPaths := false

// This forbids including Scala related libraries into the dependency
autoScalaLibrary := false

libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test"

fork := true

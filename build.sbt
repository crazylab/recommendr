updateOptions := updateOptions.value.withCachedResolution(true)

name := """play-scala"""
version := "1.0-SNAPSHOT"

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"


lazy val root = (project in file(".")).enablePlugins(PlayScala)


scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  jdbc,
//  anorm,
  cache,
  ws,
  specs2 % Test,
  //"com.typesafe.slick" %% "slick" % "3.0.0",
  "com.typesafe.play" %% "play-slick" % "1.0.0",
  "com.zaxxer" % "HikariCP" % "2.3.3",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "org.apache.spark"%"spark-core_2.10"%"1.3.1",
  "org.apache.spark"%"spark-mllib_2.10"%"1.3.1",
  "org.apache.spark"%"spark-yarn_2.10"%"1.3.1",
  "org.spark-project.akka" % "akka-actor_2.10" % "2.3.4-spark",
  "org.spark-project.akka" % "akka-remote_2.10" % "2.3.4-spark",
  "org.apache.hadoop" % "hadoop-common" % "2.7.0",
  "org.apache.hadoop" % "hadoop-client" % "2.7.0",
  "org.scalatestplus" % "play_2.10" % "1.4.0-M2",

  //  "org.apache.hadoop"%"hadoop-yarn-api"%"2.2.0",
  //  "org.apache.hadoop"%"hadoop-mapreduce-client-app"%"2.2.0",
  //  "org.apache.hadoop"%"hadoop-mapreduce-client-core"%"2.2.0",
  //  "com.google.inject.extensions"%"guice-assistedinject"%"4.0",
  //  "org.apache.hadoop"%"hadoop-mapreduce-client-jobclient"%"2.2.0",
  //  "javax.servlet" % "javax.servlet-api" % "3.0.1",
  "org.scala-lang" % "scala-compiler" %"2.10.4"
)

doc in Compile <<= target.map(_ / "none")

javaOptions in Test ++= Seq("-Dconfig.file=conf/test.conf")

fork in run := true

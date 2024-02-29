import sbt._

object Dependencies {

  object Versions {
    val logback   = "1.5.1"
    val pekko     = "1.0.2"
    val pekkoHttp = "1.0.0"
    val scalaTest = "3.2.17"
    val thrift    = "0.19.0"
  }

  val pekkoHttp = "org.apache.pekko" %% "pekko-http" % Versions.pekkoHttp
  val thrift    = "org.apache.thrift" % "libthrift"  % Versions.thrift

  object Provided {
    val logback     = "ch.qos.logback"    % "logback-classic" % Versions.logback % "provided"
    val pekkoStream = "org.apache.pekko" %% "pekko-stream"    % Versions.pekko   % "provided"
  }

  object Test {
    val pekkoHttpTestkit = "org.apache.pekko" %% "pekko-http-testkit" % Versions.pekkoHttp % "test"
    val pekkoTestkit     = "org.apache.pekko" %% "pekko-testkit"      % Versions.pekko     % "test"
    val scalaTest        = "org.scalatest"    %% "scalatest"          % Versions.scalaTest % "test"
  }
}

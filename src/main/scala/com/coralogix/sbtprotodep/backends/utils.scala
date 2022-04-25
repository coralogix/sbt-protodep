package com.coralogix.sbtprotodep.backends

import scala.util.matching.Regex

object utils {
  val protodepVersionMatcher: Regex =
    """\{"Version": "([a-zA-Z\d\-.]+)", "GitCommit": "([a-zA-Z\d\-.]+)", "GitCommitFull": "([a-z\d]+)", "BuildDate": "([a-zA-Z\d\-.]+)"}""".r

  val protofetchVersionMatcher: Regex =
    """.*\s(\d+\.\d+\.\d+)""".r
}

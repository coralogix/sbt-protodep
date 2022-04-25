package com.coralogix.sbtprotodep.backends

sealed trait BackendType
object BackendType {
  case object Protodep extends BackendType
  case object Protofetch extends BackendType
}

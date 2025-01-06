package tech.rocksavage.chiselware.addressable

import scala.annotation.StaticAnnotation
import scala.annotation.meta._
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

/** Annotation to mark a register as addressable via APB. */
@field
class AddressableRegister extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro AddressableRegisterMacro.impl
}


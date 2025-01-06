package tech.rocksavage.chiselware.addressable

import scala.annotation.StaticAnnotation
import scala.annotation.meta._
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

object AddressableRegisterMacro {
  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    // Extract the annotated variable and its properties
    val (registerName, registerType, registerWidth) = annottees.map(_.tree) match {
      case List(q"val $name = $init") =>
        c.abort(c.enclosingPosition, "Must specify type for register")
      case List(q"val $name: $tpe = RegInit($init)") =>
        val width = tpe match {
          case tq"UInt" => {
            init match {
              case q"$default.U($width.W)" => c.eval(c.Expr[Int](width))
            }
          }
          case tq"Bool" => 1
          case _ => c.abort(c.enclosingPosition, s"Unsupported type for register $name: $tpe")
        }
        (name, tpe, width)
      case _ => c.abort(c.enclosingPosition, "Invalid annotation target")
    }

    // Generate the read and write functions
    val readFunction = q"""
      () => $registerName
    """
    val writeFunction = q"""
      (value: $registerType) => $registerName := value
    """

    // Generate the code to add the register to the RegisterMap
    val result = q"""
      val $registerName = RegInit(${annottees.head.tree.asInstanceOf[ValDef].rhs})
      registerMap.addRegister(
        ${registerName.toString},
        $registerWidth,
        $readFunction,
        $writeFunction
      )
    """

    c.Expr[Any](result)
  }
}
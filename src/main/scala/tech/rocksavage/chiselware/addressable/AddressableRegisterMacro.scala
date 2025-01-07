package tech.rocksavage.chiselware.addressable

import scala.annotation.StaticAnnotation
import scala.annotation.meta._
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

object AddressableRegisterMacro {
  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    // Extract the annotated variable and its properties
    val (registerName, registerType, registerWidth, writeCallback) = annottees.map(_.tree) match {
      case List(q"val $name = $init") =>
        c.abort(c.enclosingPosition, "Must specify type for register")
      case List(q"val $name: $tpe = RegInit($init)") =>
        val (width, writeCallback) = tpe match {
          case tq"UInt" =>
            // Extract the width from the initialization expression
            val width = init match {
              case q"$default.U($width.W)" => width
              case _ => c.abort(c.enclosingPosition, s"Unsupported initialization for UInt register $name")
            }
            val writeCallback = q"""
                (offset: UInt, width: Int, value: UInt) => {
                  val blankMask = (1.U << width) - 1.U
                  val mask = blankMask << (offset * width.U)
                  $name := ($name & ~mask) | ((value & blankMask) << offset)
                }
            """
            (width, writeCallback)
          case tq"Bool" =>
            (q"1", q"(offset: UInt, width: Int, value: UInt) => $name := value(0)")
          case _ => c.abort(c.enclosingPosition, s"Unsupported type for register $name: $tpe")
        }
        (name, tpe, width, writeCallback)
      case _ => c.abort(c.enclosingPosition, "Invalid annotation target")
    }

    // Generate the read function to read N bits from the register at some offset
    val readFunction = q"""
        (offset: UInt, width: Int) => {
          val blankMask = (1.U << width) - 1.U
          val mask = blankMask << offset
          ((mask & $registerName) >> offset) & blankMask
        }
    """

    // Generate the code to define the register and add it to the RegisterMap
    val result = q"""
      val $registerName = {
        val $registerName = RegInit(${annottees.head.tree.asInstanceOf[ValDef].rhs.asInstanceOf[Apply].args.head})
        registerMap.addRegister(
          ${registerName.toString},
          $registerWidth,
          $readFunction,
          $writeCallback
        )
        $registerName
      }
    """

    // Print the generated code for debugging
    c.info(c.enclosingPosition, s"Generated code: ${showCode(result)}", force = true)

    c.Expr[Any](result)
  }
}

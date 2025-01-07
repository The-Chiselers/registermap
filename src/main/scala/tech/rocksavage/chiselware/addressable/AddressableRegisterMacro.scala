package tech.rocksavage.chiselware.addressable

import scala.annotation.StaticAnnotation
import scala.annotation.meta._
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
import chisel3._
import chisel3.util._

object AddressableRegisterMacro {
  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    // Extract the annotated variable and its properties
    val (registerName, registerType, registerWidth, widthPerWord) = annottees.map(_.tree) match {
      case List(valDef @ ValDef(_, name, tpt, rhs)) =>
        val (width, widthPerWord) = tpt match {
          case tq"UInt" =>
            val width = rhs match {
              case q"RegInit($init)" =>
                init match {
                  case q"$value.U($w.W)" =>
                    val Literal(Constant(width: Int)) = w
                    width
                  case _ =>
                    c.abort(c.enclosingPosition, s"Unsupported initialization for UInt register $name")
                }
              case _ =>
                c.abort(c.enclosingPosition, s"Unsupported initializer for UInt register $name")
            }
            val widthPerWord = 32 // Adjust as per your requirement
            (width, widthPerWord)
          case _ =>
            c.abort(c.enclosingPosition, s"Unsupported type for register $name: $tpt")
        }
        (name, tpt, width, widthPerWord)
      case _ =>
        c.abort(c.enclosingPosition, "Invalid annotation target")
    }

    // Calculate the number of words
    val numWords = registerWidth / widthPerWord

    // Generate the read function
    val readFunction = {
      val cases = (0 until numWords).map { i =>
        val high = (i + 1) * widthPerWord - 1
        val low = i * widthPerWord
        cq"""
          $i.U => $registerName($high, $low)
        """
      }
      q"""
        (offset: UInt, width: Int) => {
          chisel3.util.switch(offset) {
            case ..$cases
            case _ => 0.U
          }
        }
      """
    }

    // Generate the write callback
    val writeCallback = {
      val assignments = (0 until numWords).map { i =>
        val high = (i + 1) * widthPerWord - 1
        val low = i * widthPerWord
        q"""
          segments($i) := Mux(offset === $i.U, value, $registerName($high, $low))
        """
      }
      q"""
        (offset: UInt, width: Int, value: UInt) => {
          val segments = Wire(Vec($numWords, UInt($widthPerWord.W)))
          ..$assignments
          $registerName := segments.asUInt
        }
      """
    }

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

    // Optionally, print the generated code for debugging
    c.info(c.enclosingPosition, s"Generated code: ${showCode(result)}", force = true)

    c.Expr[Any](result)
  }
}

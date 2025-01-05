
package tech.rocksavage.chiselware.addressable

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
import chisel3._

object APBInterfaceGenerator {
  // Macro to generate APB interface and memorySizes
  def generateAPBInterface[T](module: T): (T, Seq[Int]) = macro generateAPBInterfaceImpl[T]

  def generateAPBInterfaceImpl[T: c.WeakTypeTag](c: Context)(module: c.Expr[T]): c.Expr[(T, Seq[Int])] = {
    import c.universe._

    // Collect all annotated registers and their bit sizes
    val fields = module.tree.tpe.decls.collect {
      case m: MethodSymbol if m.isVal && m.annotations.exists(_.tree.tpe =:= typeOf[AddressableRegister]) =>
        val bitSize = m.returnType match {
          case t if t =:= typeOf[Bool] => 1
          case t if t =:= typeOf[UInt] =>
            // Extract the width from the UInt type (e.g., UInt(32.W) => 32)
            t.typeArgs.head match {
              case ConstantType(Constant(width: Int)) => width
              case _ => c.abort(c.enclosingPosition, s"Unsupported UInt width for register ${m.name}")
            }
          case _ => c.abort(c.enclosingPosition, s"Unsupported register type for ${m.name}")
        }
        (m.name.toString, bitSize)
    }

    // Generate memorySizes sequence
    val memorySizes = fields.map(_._2)

    // Generate APB interface logic
    val apbLogic = fields.map { case (name, bitSize) =>
      q"""
      val $name = RegInit(false.B)
      // Connect to APB interface
      apbInterface.io.mem.addr := addrDecode.io.addrOut
      apbInterface.io.mem.wdata := io.apb.PWDATA
      apbInterface.io.mem.read := !io.apb.PWRITE
      apbInterface.io.mem.write := io.apb.PWRITE
    """
    }

    // Return the modified module and memorySizes
    c.Expr[(T, Seq[Int])](q"""
    (new {
      ..$apbLogic
    }, Seq(..$memorySizes))
  """)
  }
}
package tech.rocksavage.chiselware.addressable

import scala.reflect.macros.blackbox.Context
import scala.language.experimental.macros

import chisel3.{Bool, UInt}
import tech.rocksavage.chiselware.apb.ApbInterface

object APBInterfaceGenerator {
  def generateAPBInterface(module: Any): (ApbInterface, Seq[Int]) = macro generateAPBInterfaceImpl

  def generateAPBInterfaceImpl(c: Context)(module: c.Expr[Any]): c.Expr[(ApbInterface, Seq[Int])] = {
    import c.universe._

    // Import the AddressableRegister annotation
    val addressableRegisterType = c.mirror.staticClass("tech.rocksavage.chiselware.addressable.AddressableRegister").toType

    // Extract the module's fields annotated with @AddressableRegister
    val moduleTree = module.tree
    val fields = moduleTree.tpe.decls.collect {
      case m: MethodSymbol if m.isVal && m.annotations.exists(_.tree.tpe =:= addressableRegisterType) =>
        m
    }

    // Calculate memory sizes based on the bit widths of the registers
    val memorySizes = fields.map { field =>
      val fieldType = field.returnType
      val bitWidth = fieldType match {
        case t if t =:= typeOf[Bool] => 1
        case t if t <:< typeOf[UInt] =>
          t.typeArgs.head match {
            case TypeRef(_, _, List(Literal(Constant(width: Int)))) => width
            case _ => c.abort(c.enclosingPosition, s"Unsupported UInt width in field ${field.name}")
          }
        case _ => c.abort(c.enclosingPosition, s"Unsupported type for field ${field.name}")
      }
      bitWidth
    }

    // Convert memorySizes to a List for splicing
    val memorySizesList = memorySizes.toList

    // Generate the APB interface and memory sizes
    val apbInterface = q"""
      val apbInterface = Module(new ApbInterface(apbParams))
      apbInterface.io.apb <> io.apb
      apbInterface
    """

    // Splice the memorySizesList into the result
    val result = q"""
      ($apbInterface, $memorySizesList)
    """

    c.Expr[(ApbInterface, Seq[Int])](result)
  }
}
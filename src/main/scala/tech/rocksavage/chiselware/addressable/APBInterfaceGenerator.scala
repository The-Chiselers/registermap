package tech.rocksavage.chiselware.addressable

import scala.reflect.macros.blackbox.Context
import scala.language.experimental.macros
import chisel3._
import tech.rocksavage.chiselware.addrdecode.{AddrDecode, AddrDecodeParams}
import tech.rocksavage.chiselware.apb.{ApbBundle, ApbInterface, ApbParams}

object APBInterfaceGenerator {
  def generateAPBInterface(module: Any, dataWidth: Int, addrWidth: Int): (ApbBundle, ApbInterface, AddrDecode) =
  macro generateAPBInterfaceImpl

  def generateAPBInterfaceImpl(c: Context)(module: c.Expr[Any], dataWidth: c.Expr[Int], addrWidth: c.Expr[Int]): c.Expr[(ApbBundle, ApbInterface, AddrDecode)] = {
    import c.universe._

    // Extract the values of dataWidth and addrWidth
    val dataWidthValue = dataWidth.tree match {
      case Literal(Constant(width: Int)) => width
      case _ => c.abort(c.enclosingPosition, "dataWidth must be a constant Int")
    }
    val addrWidthValue = addrWidth.tree match {
      case Literal(Constant(width: Int)) => width
      case _ => c.abort(c.enclosingPosition, "addrWidth must be a constant Int")
    }

    // Define a Liftable instance for ApbBundle
    implicit val liftApbBundle: Liftable[ApbBundle] = new Liftable[ApbBundle] {
      def apply(bundle: ApbBundle): Tree = {
        q"new ApbBundle(ApbParams(${dataWidthValue}, ${addrWidthValue}))"
      }
    }

    implicit val liftAddrDecodeParams: Liftable[AddrDecodeParams] = new Liftable[AddrDecodeParams] {
      def apply(params: AddrDecodeParams): Tree = {
        q"AddrDecodeParams(dataWidth = ${params.dataWidth}, addressWidth = ${params.addressWidth}, memorySizes = ${params.memorySizes})"
      }
    }

    implicit val liftApbParams: Liftable[ApbParams] = new Liftable[ApbParams] {
      def apply(params: ApbParams): Tree = {
        q"ApbParams(PDATA_WIDTH = ${params.PDATA_WIDTH}, PADDR_WIDTH = ${params.PADDR_WIDTH})"
      }
    }

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

    // Generate the AddrDecodeParams
    val addrDecodeParams = AddrDecodeParams(
      dataWidth = dataWidthValue,
      addressWidth = addrWidthValue,
      memorySizes = memorySizesList
    )

    // Generate the APB params
    val apbParams = ApbParams(
      PDATA_WIDTH = dataWidthValue,
      PADDR_WIDTH = addrWidthValue
    )

    // Generate the APB bundle
    val apbBundle = new ApbBundle(apbParams)

    // Generate the APB interface and memory sizes
    val apbInterface = q"""
      val apbInterface = Module(new ApbInterface(ApbParams($dataWidthValue, $addrWidthValue)))
      apbInterface.io.apb <> ${liftApbBundle(apbBundle)}
      apbInterface
    """

    // Generate the AddrDecode module
    val addrDecode = q"""
      val addrDecode = Module(new AddrDecode(${liftAddrDecodeParams(addrDecodeParams)}))
      addrDecode.io.addr := ${liftApbBundle(apbBundle)}.PADDR
      addrDecode.io.addrOffset := 0.U
      addrDecode.io.en := true.B
      addrDecode.io.selInput := true.B
      addrDecode
    """

    // Splice the memorySizesList into the result
    val result = q"""
      (${liftApbBundle(apbBundle)}, $apbInterface, $addrDecode)
    """

    c.Expr[(ApbBundle, ApbInterface, AddrDecode)](result)
  }
}
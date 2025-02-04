package tech.rocksavage.chiselware.addressable

import chisel3._
import chisel3.util._
import tech.rocksavage.chiselware.addrdecode.AddrDecodeParams

class RegisterMap(val dataWidth: Int, val addressWidth: Int) {
    private var registers: List[RegisterDescription] = List.empty
    private var currentOffset: Int                   = 0
    private var currentId: Int                       = 0

    def createAddressableRegister[T <: Data](
        register: T,
        regName: String,
        verbose: Boolean = false
    ): Unit = {
        val registerWidth = register.getWidth
        val numWords      = (registerWidth + dataWidth - 1) / dataWidth

        // Generate the read function
        def readFunction(offset: UInt): UInt = {
            val out = Wire(UInt(dataWidth.W))
            out := 0.U

            val regUInt   = register.asUInt
            val totalBits = regUInt.getWidth

            for (i <- 0 until numWords) {
                when(offset === i.U) {
                    val startBit = i * dataWidth
                    val endBit =
                        math.min((i + 1) * dataWidth - 1, totalBits - 1)
                    val bitsToRead = endBit - startBit + 1

                    if (bitsToRead > 0) {
                        val bits = regUInt(endBit, startBit)
                        if (bitsToRead < dataWidth) {
                            out := Cat(0.U((dataWidth - bitsToRead).W), bits)
                        } else {
                            out := bits
                        }
                    } else {
                        out := 0.U
                    }
                }
            }
            if (verbose) {
                printf(
                  s"Register ${regName} read with value %x\n",
                  out
                )
            }
            out
        }

        // Generate the write callback
        def writeCallback(offset: UInt, value: UInt): Unit = {
            val regUInt    = register.asUInt
            val totalBits  = regUInt.getWidth
            val segments   = Wire(Vec(numWords, UInt(dataWidth.W)))
            val newRegUInt = Wire(UInt(totalBits.W))

            for (i <- 0 until numWords) {
                val startBit = i * dataWidth
                val endBit   = math.min((i + 1) * dataWidth - 1, totalBits - 1)
                val bitsToWrite = endBit - startBit + 1

                val currentBits = regUInt(endBit, startBit)
                val valueBits   = value(bitsToWrite - 1, 0)

                val newBits = Wire(UInt(bitsToWrite.W))
                newBits := Mux(offset === i.U, valueBits, currentBits)

                segments(i) := newBits
            }

            newRegUInt := segments.asUInt
            register   := newRegUInt.asTypeOf(register)
            if (verbose) {
                printf(
                  s"Register ${regName} written with value %x\n",
                  newRegUInt
                )
            }
        }

        // Add the register to the RegisterMap
        addRegister(
          regName,
          registerWidth,
          readFunction,
          writeCallback
        )
    }

    def addRegister(
        name: String,
        width: Int,
        readCallback: UInt => UInt,
        writeCallback: (UInt, UInt) => Unit
    ): RegisterDescription = {
        val reg = RegisterDescription(
          name,
          width,
          currentOffset,
          currentId,
          readCallback,
          writeCallback
        )
        registers = registers :+ reg
        currentOffset += (width + dataWidth - 1) / dataWidth
        currentId += 1
        reg
    }

    def getAddrDecodeParams: AddrDecodeParams = {
        AddrDecodeParams(dataWidth, addressWidth, getMemorySizes)
    }

    def getMemorySizes: List[Int] =
        registers.map(r => (r.width + dataWidth - 1) / dataWidth)

    def getRegisters: List[RegisterDescription] = registers

    def getAddressOfRegister(name: String): Option[Int] = {
        val reg = registers.find(_.name == name)
        reg.map(_.offset)
    }
}

package tech.rocksavage.chiselware.addressable

import chisel3._
import chisel3.util._
import tech.rocksavage.chiselware.addrdecode.AddrDecodeParams

class RegisterMap(
    val dataWidth: Int,
    val addressWidth: Int,
    val wordWidthOption: Option[Int] = None
) {
    val wordWidth: Int = wordWidthOption.getOrElse(dataWidth)
    private var registers: List[RegisterDescription] = List.empty
    private var currentOffset: Int                   = 0
    private var currentId: Int                       = 0

    // Ensure that the data width is a multiple of the word width
    require(dataWidth % wordWidth == 0)

    // require the ratio of dataWidth to wordWidth is a power of 2
    require(
      dataWidth / wordWidth == 1 || dataWidth / wordWidth == 2 || dataWidth / wordWidth == 4 || dataWidth / wordWidth == 8
    )

    def createAddressableRegister[T <: Data](
        register: T,
        regName: String,
        readOnly: Boolean = false,
        verbose: Boolean = false
    ): Unit = {
        val registerWidth = register.getWidth
        val numDatas      = ((registerWidth + dataWidth - 1) / dataWidth)
        val ratio         = dataWidth / wordWidth
        val numWords      = numDatas * ratio

        // Generate the read function
        def readFunction(offsetRaw: UInt): UInt = {

//            printf("RegisterMap: readFunction\n")
//            printf("offsetRaw: %x\n", offsetRaw)
            // shift the offset by the ratio of dataWidth to wordWidth log2
//            printf("ratio: %x\n", ratio.U)
            val offset = offsetRaw >> log2Ceil(ratio).U
//            printf("offset: %x\n", offset)

            val out = Wire(UInt(dataWidth.W))
            out := 0.U

            val regUInt   = register.asUInt
            val totalBits = regUInt.getWidth

            for (i <- 0 until numDatas) {
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
        def writeCallback(offsetRaw: UInt, value: UInt): Unit = {
            val regUInt    = register.asUInt
            val totalBits  = regUInt.getWidth
            val segments   = Wire(Vec(numDatas, UInt(dataWidth.W)))
            val newRegUInt = Wire(UInt(totalBits.W))

            // shift the offset by the ratio of dataWidth to wordWidth log2
            val offset = offsetRaw >> log2Ceil(ratio).U

            for (i <- 0 until numDatas) {
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

        def readOnlyAttemptWrite(offset: UInt, value: UInt): Unit = {
            if (verbose) {
                printf(
                  s"Attempted write to read-only register ${regName} with value %x\n",
                  value
                )
            }
        }

        // Add the register to the RegisterMap
        if (readOnly)
            addRegister(
              regName,
              registerWidth,
              readFunction,
              readOnlyAttemptWrite
            )
        else
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
        currentOffset += ((width + dataWidth - 1) / dataWidth) * (dataWidth / wordWidth)
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

    def prettyPrint(): Unit = {
        println("Register Map:")
        registers.foreach(r => {
            println(s"Name: ${r.name}, Width: ${r.width}, Offset: ${r.offset}")
        })
    }

    def printHeaderFile(): Unit = {
        println("#ifndef REGISTER_MAP_H")
        println("#define REGISTER_MAP_H")
        println(s"// Word width in bits: ${(dataWidth / wordWidth)}")

        for (r <- registers) {
            val capitalizedName = r.name.toUpperCase
            val offsetHex       = f"0x${r.offset}%X"
            println(
              s"#define ${capitalizedName}_OFFSET ${offsetHex}"
            )
        }
        println()
        println("#endif REGISTER_MAP_H")
    }

}

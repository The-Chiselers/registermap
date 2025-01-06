package tech.rocksavage.chiselware.addressable

import chisel3._
import tech.rocksavage.chiselware.addrdecode.AddrDecodeParams

class RegisterMap(val dataWidth: Int, val addressWidth: Int) {
  private var registers: List[RegisterDescription] = List.empty
  private var currentOffset: Int = 0
  private var currentId: Int = 0

  def addRegister(name: String, width: Int, readCallback: () => UInt, writeCallback: UInt => Unit): RegisterDescription = {
    val reg = RegisterDescription(name, width, currentOffset, currentId, readCallback, writeCallback)
    registers = registers :+ reg
    currentOffset += (width + dataWidth - 1) / dataWidth
    currentId += 1
    reg
  }

  def getMemorySizes: List[Int] = registers.map(_.width)

  def getAddrDecodeParams: AddrDecodeParams = {
    AddrDecodeParams(dataWidth, addressWidth, getMemorySizes)
  }

  def getRegisters: List[RegisterDescription] = registers
}
package tech.rocksavage.chiselware.addressable

import chisel3._

case class RegisterDescription(
                                name: String,
                                width: Int,
                                offset: Int,
                                id: Int,
                                readCallback: () => UInt,
                                writeCallback: UInt => Unit
                              )

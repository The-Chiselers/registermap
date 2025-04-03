# Register Map Library

## Overview

The `RegisterMap` library is a Chisel-based utility designed to simplify the management of addressable registers in
hardware designs. It provides a flexible and configurable way to define, read, and write registers, making it
particularly useful for systems with memory-mapped I/O or complex register configurations. The library is built on top
of Chisel, a hardware design language, and integrates seamlessly with other Chisel components.

## Features

- **Addressable Registers**: Easily define and manage addressable registers with customizable widths and offsets.
- **Automatic Offset Management**: Automatically calculate register offsets based on their widths.
- **Integration with APB**: Seamlessly integrate with APB (Advanced Peripheral Bus) interfaces for memory-mapped I/O.
- **Scalable and Modular**: Designed to be scalable and modular, allowing for easy integration into larger designs.
- **Debug Information**: Provides detailed debug information for each register, including name, address, and width.
- **Header File Generation**: Generate header files with register information for software development.

## Usage

### Defining Registers

To define a register, use the `createAddressableRegister` method provided by the `RegisterMap` class. This method takes
the register and its name as parameters and automatically handles the creation of read and write functionality.

```scala
val registerMap = new RegisterMap(dataWidth = 32, addressWidth = 32)

val almostFullLevel = RegInit(
  (params.bufferSize - 1).U((log2Ceil(params.bufferSize) + 1).W)
)
registerMap.createAddressableRegister(
  almostFullLevel,
  "almostFullLevel",
  readOnly = false,
  verbose = params.verbose
)

val fifoAlmostFull = WireInit(false.B)
registerMap.createAddressableRegister(
  fifoAlmostFull,
  "fifoAlmostFull",
  readOnly = true,
  verbose = params.verbose
)


```

### Integrating with APB

The `RegisterMap` library can be easily integrated with APB interfaces. Use the `getAddrDecodeParams` method to generate
address decoder parameters and connect them to an `AddrDecode` module.

```scala
val addrDecodeParams = registerMap.getAddrDecodeParams
val addrDecode = Module(new AddrDecode(addrDecodeParams))

addrDecode.io.addr := io.apb.PADDR
addrDecode.io.addrOffset := 0.U
addrDecode.io.en := true.B
addrDecode.io.selInput := true.B

io.apb.PREADY := (io.apb.PENABLE && io.apb.PSEL)
io.apb.PSLVERR := addrDecode.io.errorCode === AddrDecodeError.AddressOutOfRange

io.apb.PRDATA := 0.U
when(io.apb.PSEL && io.apb.PENABLE) {
  when(io.apb.PWRITE) {
    for (reg <- registerMap.getRegisters) {
      when(addrDecode.io.sel(reg.id)) {
        reg.writeCallback(addrDecode.io.addrOffset, io.apb.PWDATA)
      }
    }
  }.otherwise {
    for (reg <- registerMap.getRegisters) {
      when(addrDecode.io.sel(reg.id)) {
        io.apb.PRDATA := reg.readCallback(addrDecode.io.addrOffset)
      }
    }
  }
}
```

If you want additional functionality like automatically checking the bounds of registers being programmed, that is simple as seen [here](https://github.com/The-Chiselers/uart/blob/75516efe70c4a93c155d41343f3badf3a01ac8a0/src/main/scala/tech/rocksavage/chiselware/uart/hw/Uart.scala#L493):
```scala
// Error Checks
// Add error checking for invalid register values
when(io.apb.PSEL && io.apb.PENABLE) {
    for (reg <- registerMap.getRegisters) {
        when(addrDecode.io.sel(reg.id)) {
            // Check for invalid values in configuration registers
            when(reg.name.contains("numOutputBitsDb").B) {
                val maxBits = uartParams.maxOutputBits.U
                when(io.apb.PWDATA > maxBits) {
                    error.topError := UartTopError.InvalidRegisterProgramming
                }
            }.elsewhen(reg.name.contains("_baudRate").B) {
                val maxBaud = uartParams.maxBaudRate.U
                when(io.apb.PWDATA > maxBaud) {
                    error.topError := UartTopError.InvalidRegisterProgramming
                }
            }.elsewhen(reg.name.contains("clockFreq").B) {
                val maxFreq = uartParams.maxClockFrequency.U
                when(io.apb.PWDATA > maxFreq) {
                    error.topError := UartTopError.InvalidRegisterProgramming
                }
            }
        }
    }
}
```

### Generating Header Files

The `RegisterMap` library can generate header files with register information for software development.

```scala
val load = RegInit(false.B)
registerMap.createAddressableRegister(
  load,
  "tx_load",
  readOnly = false,
  verbose = uartParams.verbose
)

val dataIn = RegInit(0.U(uartParams.maxOutputBits.W))
registerMap.createAddressableRegister(
  dataIn,
  "tx_dataIn",
  readOnly = false,
  verbose = uartParams.verbose
)

// Transmitter configuration registers
val tx_baud = RegInit(115200.U(32.W))
registerMap.createAddressableRegister(
  tx_baud,
  "tx_baudRate",
  readOnly = false,
  verbose = uartParams.verbose
)

...

val headerFile: String = registerMap.printHeaderFile()
```

Which will generate the following header file:

```c
#ifndef REGISTER_MAP_H
#define REGISTER_MAP_H
// Word width in bits: 4
#define TX_LOAD_OFFSET 0x0
#define TX_DATAIN_OFFSET 0x4
#define TX_BAUDRATE_OFFSET 0x8
#define TX_CLOCKFREQ_OFFSET 0xC
#define TX_UPDATEBAUD_OFFSET 0x10
#define TX_NUMOUTPUTBITSDB_OFFSET 0x14
#define TX_USEPARITYDB_OFFSET 0x18
#define TX_PARITYODDDB_OFFSET 0x1C
#define RX_DATA_OFFSET 0x20
#define RX_DATAAVAILABLE_OFFSET 0x24
#define ERROR_OFFSET 0x28
#define CLEARERROR_OFFSET 0x2C
#define RX_BAUDRATE_OFFSET 0x30
#define RX_CLOCKFREQ_OFFSET 0x34
#define RX_UPDATEBAUD_OFFSET 0x38
#define RX_NUMOUTPUTBITSDB_OFFSET 0x3C
#define RX_USEPARITYDB_OFFSET 0x40
#define RX_PARITYODDDB_OFFSET 0x44
#define RX_CLOCKSPERBIT_OFFSET 0x48
#define TX_CLOCKSPERBIT_OFFSET 0x4C

#endif REGISTER_MAP_H
```

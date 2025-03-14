# RegisterMap Library

## Setup

### Git

```bash
git clone [url].git
git submodule update --init --recursive
touch .git-blame-ignore-revs
git config blame.ignoreRevsFile .git-blame-ignore-revs
``` 

## Overview

The `RegisterMap` library is a Chisel-based utility designed to simplify the management of addressable registers in
hardware designs. It provides a flexible and configurable way to define, read, and write registers, making it
particularly useful for systems with memory-mapped I/O or complex register configurations. The library is built on top
of Chisel, a hardware design language, and integrates seamlessly with other Chisel components.

## Features

- **Addressable Registers**: Easily define and manage addressable registers with customizable widths and offsets.
- **Read/Write Callbacks**: Implement custom read and write logic for each register using callback functions.
- **Automatic Offset Management**: Automatically calculate register offsets based on their widths.
- **Integration with APB**: Seamlessly integrate with APB (Advanced Peripheral Bus) interfaces for memory-mapped I/O.
- **Scalable and Modular**: Designed to be scalable and modular, allowing for easy integration into larger designs.
- **Debug Information**: Provides detailed debug information for each register, including name, address, and width.
- **Header File Generation**: Generate header files with register information for software development.

## Usage

### Defining Registers

To define a register, use the `createAddressableRegister` method provided by the `RegisterMap` class. This method takes
the register and its name as parameters and automatically handles the creation of read and write callbacks.

```scala
val registerMap = new RegisterMap(dataWidth = 32, addressWidth = 32)

val en: Bool = RegInit(false.B)
registerMap.createAddressableRegister(en, "en")

val prescaler: UInt = RegInit(0.U(32.W))
registerMap.createAddressableRegister(prescaler, "prescaler")
```

### Integrating with APB

The `RegisterMap` library can be easily integrated with APB interfaces. Use the `getAddrDecodeParams` method to generate
address decode parameters and connect them to an `AddrDecode` module.

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

val tx_clockFreq = RegInit(25_000_000.U(32.W))
registerMap.createAddressableRegister(
  tx_clockFreq,
  "tx_clockFreq",
  readOnly = false,
  verbose = uartParams.verbose
)

val tx_updateBaud = RegInit(false.B)
registerMap.createAddressableRegister(
  tx_updateBaud,
  "tx_updateBaud",
  readOnly = false,
  verbose = uartParams.verbose
)

val tx_numOutputBitsDb = RegInit(
  8.U((log2Ceil(uartParams.maxOutputBits) + 1).W)
)
registerMap.createAddressableRegister(
  tx_numOutputBitsDb,
  "tx_numOutputBitsDb",
  readOnly = false,
  verbose = uartParams.verbose
)

val tx_useParityDb = RegInit(uartParams.parity.B)
registerMap.createAddressableRegister(
  tx_useParityDb,
  "tx_useParityDb",
  readOnly = false,
  verbose = uartParams.verbose
)

val tx_parityOddDb = RegInit(uartParams.parity.B)
registerMap.createAddressableRegister(
  tx_parityOddDb,
  "tx_parityOddDb",
  readOnly = false,
  verbose = uartParams.verbose
)

// -------------------------------------------------------
// RX registers (for the RX control bundle and RX data/status)
// -------------------------------------------------------
val rxData = WireInit(0.U(uartParams.maxOutputBits.W))
val rxDataAvailable = !fifoStatusRx.empty
registerMap.createAddressableRegister(
  rxData,
  "rx_data",
  readOnly = true,
  verbose = uartParams.verbose
)
registerMap.createAddressableRegister(
  rxDataAvailable,
  "rx_dataAvailable",
  readOnly = true,
  verbose = uartParams.verbose
)

// Create a wire for the error output
val error = RegInit(0.U.asTypeOf(new UartErrorBundle()))
registerMap.createAddressableRegister(
  error,
  "error",
  readOnly = true,
  verbose = uartParams.verbose
)

val clearError = RegInit(false.B)
registerMap.createAddressableRegister(
  clearError,
  "clearError",
  readOnly = false,
  verbose = uartParams.verbose
)

// Receiver configuration registers
val rx_baud = RegInit(115200.U(32.W))
registerMap.createAddressableRegister(
  rx_baud,
  "rx_baudRate",
  readOnly = false,
  verbose = uartParams.verbose
)

val rx_clockFreq = RegInit(25_000_000.U(32.W))
registerMap.createAddressableRegister(
  rx_clockFreq,
  "rx_clockFreq",
  readOnly = false,
  verbose = uartParams.verbose
)

val rx_updateBaud = RegInit(false.B)
registerMap.createAddressableRegister(
  rx_updateBaud,
  "rx_updateBaud",
  readOnly = false,
  verbose = uartParams.verbose
)

val rx_numOutputBitsDb = RegInit(
  8.U((log2Ceil(uartParams.maxOutputBits) + 1).W)
)
registerMap.createAddressableRegister(
  rx_numOutputBitsDb,
  "rx_numOutputBitsDb",
  readOnly = false,
  verbose = uartParams.verbose
)

val rx_useParityDb = RegInit(uartParams.parity.B)
registerMap.createAddressableRegister(
  rx_useParityDb,
  "rx_useParityDb",
  readOnly = false,
  verbose = uartParams.verbose
)

val rx_parityOddDb = RegInit(uartParams.parity.B)
registerMap.createAddressableRegister(
  rx_parityOddDb,
  "rx_parityOddDb",
  readOnly = false,
  verbose = uartParams.verbose
)

// -------------------------------------------------------
// Clocks-per-bit registers (read-only outputs)
// -------------------------------------------------------
val rxClocksPerBit = WireInit(
  0.U((log2Ceil(uartParams.maxClockFrequency) + 1).W)
)
val txClocksPerBit = WireInit(
  0.U((log2Ceil(uartParams.maxClockFrequency) + 1).W)
)
registerMap.createAddressableRegister(
  rxClocksPerBit,
  "rx_clocksPerBit",
  readOnly = true,
  verbose = uartParams.verbose
)
registerMap.createAddressableRegister(
  txClocksPerBit,
  "tx_clocksPerBit",
  readOnly = true,
  verbose = uartParams.verbose
)

val headerFile: String = registerMap.getHeaderFile()
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

### Example: Timer Design

The following example demonstrates how to use the `RegisterMap` library in a timer design with an APB interface.

```scala
class Timer(val timerParams: TimerParams) extends Module {
  val dataWidth = timerParams.dataWidth
  val addressWidth = timerParams.addressWidth

  val io = IO(new Bundle {
    val apb = new ApbBundle(ApbParams(dataWidth, addressWidth))
    val timerOutput = new TimerOutputBundle(timerParams)
    val interrupt = new TimerInterruptBundle
  })

  val registerMap = new RegisterMap(dataWidth, addressWidth)

  val en: Bool = RegInit(false.B)
  registerMap.createAddressableRegister(en, "en")

  val prescaler: UInt = RegInit(0.U(timerParams.countWidth.W))
  registerMap.createAddressableRegister(prescaler, "prescaler")

  val maxCount: UInt = RegInit(0.U(timerParams.countWidth.W))
  registerMap.createAddressableRegister(maxCount, "maxCount")

  val pwmCeiling: UInt = RegInit(0.U(timerParams.countWidth.W))
  registerMap.createAddressableRegister(pwmCeiling, "pwmCeiling")

  val setCountValue: UInt = RegInit(0.U(timerParams.countWidth.W))
  registerMap.createAddressableRegister(setCountValue, "setCountValue")

  val setCount: Bool = RegInit(false.B)
  registerMap.createAddressableRegister(setCount, "setCount")

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

  val timerInner = Module(new TimerInner(timerParams))
  timerInner.io.timerInputBundle.en := en
  timerInner.io.timerInputBundle.setCount := setCount
  timerInner.io.timerInputBundle.prescaler := prescaler
  timerInner.io.timerInputBundle.maxCount := maxCount
  timerInner.io.timerInputBundle.pwmCeiling := pwmCeiling
  timerInner.io.timerInputBundle.setCountValue := setCountValue

  io.timerOutput <> timerInner.io.timerOutputBundle

  io.interrupt.interrupt := TimerInterruptEnum.None
  when(timerInner.io.timerOutputBundle.maxReached) {
    io.interrupt.interrupt := TimerInterruptEnum.MaxReached
  }
}
```

## Conclusion

The `RegisterMap` library is a powerful tool for managing addressable registers in Chisel-based hardware designs. It
simplifies the process of defining, reading, and writing registers, provides robust offset management, and integrates
seamlessly with APB interfaces. The library is designed to be scalable and modular, making it an essential component for
complex systems with memory-mapped I/O.
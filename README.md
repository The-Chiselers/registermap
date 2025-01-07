# RegisterMap Library

## Overview

The `RegisterMap` library is a Chisel-based utility designed to simplify the management of addressable registers in hardware designs. It provides a flexible and configurable way to define, read, and write registers, making it particularly useful for systems with memory-mapped I/O or complex register configurations. The library is built on top of Chisel, a hardware design language, and integrates seamlessly with other Chisel components.

## Features

- **Addressable Registers**: Easily define and manage addressable registers with customizable widths and offsets.
- **Read/Write Callbacks**: Implement custom read and write logic for each register using callback functions.
- **Automatic Offset Management**: Automatically calculate register offsets based on their widths.
- **Integration with APB**: Seamlessly integrate with APB (Advanced Peripheral Bus) interfaces for memory-mapped I/O.
- **Scalable and Modular**: Designed to be scalable and modular, allowing for easy integration into larger designs.

## Usage

### Defining Registers

To define a register, use the `createAddressableRegister` method provided by the `RegisterMap` class. This method takes the register and its name as parameters and automatically handles the creation of read and write callbacks.

```scala
val registerMap = new RegisterMap(dataWidth = 32, addressWidth = 32)

val en: Bool = RegInit(false.B)
registerMap.createAddressableRegister(en, "en")

val prescaler: UInt = RegInit(0.U(32.W))
registerMap.createAddressableRegister(prescaler, "prescaler")
```

### Integrating with APB

The `RegisterMap` library can be easily integrated with APB interfaces. Use the `getAddrDecodeParams` method to generate address decode parameters and connect them to an `AddrDecode` module.

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

The `RegisterMap` library is a powerful tool for managing addressable registers in Chisel-based hardware designs. It simplifies the process of defining, reading, and writing registers, provides robust offset management, and integrates seamlessly with APB interfaces. The library is designed to be scalable and modular, making it an essential component for complex systems with memory-mapped I/O.
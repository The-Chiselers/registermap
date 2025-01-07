# RegisterMap Module

## Overview

The `RegisterMap` module is a utility designed to simplify the management of addressable registers in hardware designs using Chisel. It provides a structured way to define, read, and write registers, making it easier to handle complex memory-mapped I/O operations.

## Features

- **Register Creation**: Easily create addressable registers with specified data and address widths.
- **Read/Write Callbacks**: Define custom read and write functions for each register.
- **Memory Management**: Automatically manage memory offsets and sizes for registers.
- **Integration with APB**: Seamlessly integrate with APB (Advanced Peripheral Bus) interfaces for memory-mapped I/O.

## Usage

### Defining Registers

To define a register, use the `createAddressableRegister` method. This method takes the register and its name as parameters and automatically handles the read and write operations.

```scala
val registerMap = new RegisterMap(dataWidth, addressWidth)

val en: Bool = RegInit(false.B)
registerMap.createAddressableRegister(en, "en")

val prescaler: UInt = RegInit(0.U(timerParams.countWidth.W))
registerMap.createAddressableRegister(prescaler, "prescaler")
```

### Reading and Writing Registers

The `RegisterMap` module provides read and write callbacks that can be used to handle memory operations. These callbacks are automatically generated when a register is created.

```scala
// Handle writes to the registers
when(apbInterface.io.mem.write) {
  for (reg <- registerMap.getRegisters) {
    when(addrDecode.io.sel(reg.id)) {
      reg.writeCallback(addrDecode.io.addrOffset, apbInterface.io.mem.wdata)
    }
  }
}

// Handle reads from the registers
when(apbInterface.io.mem.read) {
  apbInterface.io.mem.rdata := 0.U
  for (reg <- registerMap.getRegisters) {
    when(addrDecode.io.sel(reg.id)) {
      apbInterface.io.mem.rdata := reg.readCallback(addrDecode.io.addrOffset)
    }
  }
}
```

### Integration with APB

The `RegisterMap` module can be integrated with an APB interface to handle memory-mapped I/O operations. This involves connecting the APB interface to the `RegisterMap` and using the address decode module to manage memory accesses.

```scala
val apbInterface = Module(new ApbInterface(ApbParams(dataWidth, addressWidth)))
apbInterface.io.apb <> io.apb

val addrDecodeParams = registerMap.getAddrDecodeParams
val addrDecode = Module(new AddrDecode(addrDecodeParams))
addrDecode.io.addr := apbInterface.io.mem.addr
addrDecode.io.addrOffset := 0.U
addrDecode.io.en := true.B
addrDecode.io.selInput := true.B

apbInterface.io.mem.error := addrDecode.io.errorCode === AddrDecodeError.AddressOutOfRange
apbInterface.io.mem.rdata := 0.U
```

## Example

The following example demonstrates how to use the `RegisterMap` module in a timer design.

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

  val apbInterface = Module(new ApbInterface(ApbParams(dataWidth, addressWidth)))
  apbInterface.io.apb <> io.apb

  val addrDecodeParams = registerMap.getAddrDecodeParams
  val addrDecode = Module(new AddrDecode(addrDecodeParams))
  addrDecode.io.addr := apbInterface.io.mem.addr
  addrDecode.io.addrOffset := 0.U
  addrDecode.io.en := true.B
  addrDecode.io.selInput := true.B

  apbInterface.io.mem.error := addrDecode.io.errorCode === AddrDecodeError.AddressOutOfRange
  apbInterface.io.mem.rdata := 0.U

  when(apbInterface.io.mem.write) {
    for (reg <- registerMap.getRegisters) {
      when(addrDecode.io.sel(reg.id)) {
        reg.writeCallback(addrDecode.io.addrOffset, apbInterface.io.mem.wdata)
      }
    }
  }

  when(apbInterface.io.mem.read) {
    apbInterface.io.mem.rdata := 0.U
    for (reg <- registerMap.getRegisters) {
      when(addrDecode.io.sel(reg.id)) {
        apbInterface.io.mem.rdata := reg.readCallback(addrDecode.io.addrOffset)
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

The `RegisterMap` module is a powerful tool for managing addressable registers in Chisel-based hardware designs. It simplifies the process of defining, reading, and writing registers, and integrates seamlessly with APB interfaces for memory-mapped I/O operations.
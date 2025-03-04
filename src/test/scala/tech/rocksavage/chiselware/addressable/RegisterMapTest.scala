package tech.rocksavage.chiselware.addressable

import chiseltest._
import chisel3._
import chiseltest.formal.BoundedCheck
import chiseltest.simulator._
import chiseltest.RawTester.verify
import firrtl2.annotations.Annotation
import firrtl2.options.TargetDirAnnotation
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import tech.rocksavage.chiselware.addrdecode.AddrDecode

/** Highly randomized test suite driven by configuration parameters. Includes code coverage for all
 * top-level ports. Inspired by the DynamicFifo
 */

class RegisterMapTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {

    class I2cBundle(size: Int) extends Bundle {
        val mCtrl = UInt(size.W)
        val mStatus = UInt(size.W)
        val mBaud = UInt(size.W)
        val mAddr = UInt(size.W)
        val mData = UInt(size.W)
        val sCtrl = UInt(size.W)
        val sStatus = UInt(size.W)
        val sAddr = UInt(size.W)
        val sData = UInt(size.W)
    }

    class I2cStub(size: Int, dataWidth: Int, addrWidth: Int, wordWidth: Int) extends Module {
        val i2cStubRegMap = new RegisterMap(dataWidth, addrWidth, Some(wordWidth))
        def getRegMap = i2cStubRegMap
        val io = IO(
            new Bundle {
                val setManually = Input(Bool())
                val addr = Input(UInt(addrWidth.W))
                val dataIn = Input(UInt(dataWidth.W))
                val dataOut = Output(UInt(dataWidth.W))
                val read = Input(Bool())
                val write = Input(Bool())

                val inputBundle = Input(new I2cBundle(size))
                val outputBundle = Output(new I2cBundle(size))
            }
        )

        val regBundle = Reg(new I2cBundle(size))

        when(io.setManually) {
            regBundle := io.inputBundle
        }

        io.outputBundle := regBundle

        i2cStubRegMap.createAddressableRegister(regBundle.mCtrl, "mctrl")
        i2cStubRegMap.createAddressableRegister(regBundle.mStatus, "mstatus")
        i2cStubRegMap.createAddressableRegister(regBundle.mBaud, "mbaud")
        i2cStubRegMap.createAddressableRegister(regBundle.mAddr, "maddr")
        i2cStubRegMap.createAddressableRegister(regBundle.mData, "mdata")
        i2cStubRegMap.createAddressableRegister(regBundle.sCtrl, "sctrl")
        i2cStubRegMap.createAddressableRegister(regBundle.sStatus, "sstatus")
        i2cStubRegMap.createAddressableRegister(regBundle.sAddr, "saddr")
        i2cStubRegMap.createAddressableRegister(regBundle.sData, "sdata")

        val addrDecodeParams = i2cStubRegMap.getAddrDecodeParams
        val addrDecode       = Module(new AddrDecode(addrDecodeParams))
        addrDecode.io.addrRaw    := io.addr
        addrDecode.io.en         := true.B
        addrDecode.io.selInput   := true.B

        io.dataOut := 0.U
        when(io.write) {
            for (reg <- i2cStubRegMap.getRegisters) {
                when(addrDecode.io.sel(reg.id)) {
                    reg.writeCallback(addrDecode.io.addrOut, io.dataIn)
                }
            }
        }
        when(io.read) {
            for (reg <- i2cStubRegMap.getRegisters) {
                when(addrDecode.io.sel(reg.id)) {
                    io.dataOut := reg.readCallback(addrDecode.io.addrOut)
                }
            }
        }
    }
    val backendAnnotations = Seq(chiseltest.WriteVcdAnnotation, TargetDirAnnotation("out/test"))

    it should "Intantiate the correct I2C Registermap at 32 data size" in {
        test(new I2cStub(32, 32, 32, 8)) { dut =>

            val expectedMctrl = 0
            val expectedMstatus = 4
            val expectedMbaud = 8
            val expectedMaddr = 12
            val expectedMdata = 16
            val expectedSctrl = 20
            val expectedSstatus = 24
            val expectedSaddr = 28
            val expectedSdata = 32

            val i2cStubRegMap = dut.getRegMap

            val actualMctrl = i2cStubRegMap.getAddressOfRegister("mctrl").get
            val actualMstatus = i2cStubRegMap.getAddressOfRegister("mstatus").get
            val actualMbaud = i2cStubRegMap.getAddressOfRegister("mbaud").get
            val actualMaddr = i2cStubRegMap.getAddressOfRegister("maddr").get
            val actualMdata = i2cStubRegMap.getAddressOfRegister("mdata").get
            val actualSctrl = i2cStubRegMap.getAddressOfRegister("sctrl").get
            val actualSstatus = i2cStubRegMap.getAddressOfRegister("sstatus").get
            val actualSaddr = i2cStubRegMap.getAddressOfRegister("saddr").get
            val actualSdata = i2cStubRegMap.getAddressOfRegister("sdata").get

            assert(actualMctrl == expectedMctrl)
            assert(actualMstatus == expectedMstatus)
            assert(actualMbaud == expectedMbaud)
            assert(actualMaddr == expectedMaddr)
            assert(actualMdata == expectedMdata)
            assert(actualSctrl == expectedSctrl)
            assert(actualSstatus == expectedSstatus)
            assert(actualSaddr == expectedSaddr)
            assert(actualSdata == expectedSdata)
        }
    }

    it should "correctly read regs at 32 bit" in {
        test(new I2cStub(32, 32, 32, 8))
          .withAnnotations(backendAnnotations)
          { dut =>

            val i2cStubRegMap = dut.getRegMap

            val actualMctrl = i2cStubRegMap.getAddressOfRegister("mctrl").get
            val actualMstatus = i2cStubRegMap.getAddressOfRegister("mstatus").get
            val actualMbaud = i2cStubRegMap.getAddressOfRegister("mbaud").get
            val actualMaddr = i2cStubRegMap.getAddressOfRegister("maddr").get
            val actualMdata = i2cStubRegMap.getAddressOfRegister("mdata").get
            val actualSctrl = i2cStubRegMap.getAddressOfRegister("sctrl").get
            val actualSstatus = i2cStubRegMap.getAddressOfRegister("sstatus").get
            val actualSaddr = i2cStubRegMap.getAddressOfRegister("saddr").get
            val actualSdata = i2cStubRegMap.getAddressOfRegister("sdata").get


            // #### Test MCTRL ####
            val randData = randInt(32)
            dut.io.setManually.poke(false.B)
            dut.clock.step()
            dut.io.outputBundle.mAddr.expect(0.U)
            dut.io.inputBundle.mAddr.poke(randData.U)
            dut.io.setManually.poke(true.B)
            dut.clock.step()

            // Read the value back
            dut.io.addr.poke(actualMaddr.U)
            dut.io.read.poke(true.B)
            dut.clock.step()
            dut.io.dataOut.expect(randData.U)
            dut.io.addr.poke(0.U)
            dut.io.read.poke(false.B)
        }
    }

  it should "correctly write regs at 32 bits" in {
    test(new I2cStub(32, 32, 32, 8))
      .withAnnotations(backendAnnotations)
      { dut =>

        val i2cStubRegMap = dut.getRegMap

        val actualMctrl = i2cStubRegMap.getAddressOfRegister("mctrl").get
        val actualMstatus = i2cStubRegMap.getAddressOfRegister("mstatus").get
        val actualMbaud = i2cStubRegMap.getAddressOfRegister("mbaud").get
        val actualMaddr = i2cStubRegMap.getAddressOfRegister("maddr").get
        val actualMdata = i2cStubRegMap.getAddressOfRegister("mdata").get
        val actualSctrl = i2cStubRegMap.getAddressOfRegister("sctrl").get
        val actualSstatus = i2cStubRegMap.getAddressOfRegister("sstatus").get
        val actualSaddr = i2cStubRegMap.getAddressOfRegister("saddr").get
        val actualSdata = i2cStubRegMap.getAddressOfRegister("sdata").get


        // #### Test MCTRL ####
        val randData = randInt(32)
        dut.io.setManually.poke(false.B)
        dut.clock.step()

        dut.io.outputBundle.mAddr.expect(0.U)
        dut.io.addr.poke(actualMaddr.U)
        dut.io.dataIn.poke(randData.U)
        dut.io.write.poke(true.B)
        dut.clock.step()
        dut.io.write.poke(false.B)
        dut.io.outputBundle.mAddr.expect(randData.U)
      }
  }

    it should "Intantiate the correct I2C Registermap at 8 data size" in {
        test(new I2cStub(8,8,8,8))
//          .withAnnotations(backendAnnotations)
          { dut =>

            val expectedMctrl = 0
            val expectedMstatus = 1
            val expectedMbaud = 2
            val expectedMaddr = 3
            val expectedMdata = 4
            val expectedSctrl = 5
            val expectedSstatus = 6
            val expectedSaddr = 7
            val expectedSdata = 8

            val i2cStubRegMap = dut.getRegMap

            val actualMctrl = i2cStubRegMap.getAddressOfRegister("mctrl").get
            val actualMstatus = i2cStubRegMap.getAddressOfRegister("mstatus").get
            val actualMbaud = i2cStubRegMap.getAddressOfRegister("mbaud").get
            val actualMaddr = i2cStubRegMap.getAddressOfRegister("maddr").get
            val actualMdata = i2cStubRegMap.getAddressOfRegister("mdata").get
            val actualSctrl = i2cStubRegMap.getAddressOfRegister("sctrl").get
            val actualSstatus = i2cStubRegMap.getAddressOfRegister("sstatus").get
            val actualSaddr = i2cStubRegMap.getAddressOfRegister("saddr").get
            val actualSdata = i2cStubRegMap.getAddressOfRegister("sdata").get

            assert(actualMctrl == expectedMctrl)
            assert(actualMstatus == expectedMstatus)
            assert(actualMbaud == expectedMbaud)
            assert(actualMaddr == expectedMaddr)
            assert(actualMdata == expectedMdata)
            assert(actualSctrl == expectedSctrl)
            assert(actualSstatus == expectedSstatus)
            assert(actualSaddr == expectedSaddr)
            assert(actualSdata == expectedSdata)
        }
    }

    it should "correctly read regs at 8 bit" in {
        test(new I2cStub(8, 8, 8, 8))
//        .withAnnotations(backendAnnotations)
        { dut =>

            val i2cStubRegMap = dut.getRegMap

            val actualMctrl = i2cStubRegMap.getAddressOfRegister("mctrl").get
            val actualMstatus = i2cStubRegMap.getAddressOfRegister("mstatus").get
            val actualMbaud = i2cStubRegMap.getAddressOfRegister("mbaud").get
            val actualMaddr = i2cStubRegMap.getAddressOfRegister("maddr").get
            val actualMdata = i2cStubRegMap.getAddressOfRegister("mdata").get
            val actualSctrl = i2cStubRegMap.getAddressOfRegister("sctrl").get
            val actualSstatus = i2cStubRegMap.getAddressOfRegister("sstatus").get
            val actualSaddr = i2cStubRegMap.getAddressOfRegister("saddr").get
            val actualSdata = i2cStubRegMap.getAddressOfRegister("sdata").get


            // #### Test MCTRL ####
            val randData = randInt(32)
            dut.io.setManually.poke(false.B)
            dut.clock.step()
            dut.io.outputBundle.mAddr.expect(0.U)
            dut.io.inputBundle.mAddr.poke(randData.U)
            dut.io.setManually.poke(true.B)
            dut.clock.step()
             dut.io.setManually.poke(false.B)

            // Read the value back
            dut.io.addr.poke(actualMaddr.U)
            dut.io.read.poke(true.B)
            dut.clock.step()
            dut.io.dataOut.expect(randData.U)
            dut.io.addr.poke(0.U)
            dut.io.read.poke(false.B)
        }
    }

  it should "correctly write regs at 8 bits" in {
    test(new I2cStub(8,8,8,8))
      .withAnnotations(backendAnnotations)
      { dut =>

        val i2cStubRegMap = dut.getRegMap

        val actualMctrl = i2cStubRegMap.getAddressOfRegister("mctrl").get
        val actualMstatus = i2cStubRegMap.getAddressOfRegister("mstatus").get
        val actualMbaud = i2cStubRegMap.getAddressOfRegister("mbaud").get
        val actualMaddr = i2cStubRegMap.getAddressOfRegister("maddr").get
        val actualMdata = i2cStubRegMap.getAddressOfRegister("mdata").get
        val actualSctrl = i2cStubRegMap.getAddressOfRegister("sctrl").get
        val actualSstatus = i2cStubRegMap.getAddressOfRegister("sstatus").get
        val actualSaddr = i2cStubRegMap.getAddressOfRegister("saddr").get
        val actualSdata = i2cStubRegMap.getAddressOfRegister("sdata").get


        // #### Test MCTRL ####
        val randData = randInt(32)
        dut.io.setManually.poke(false.B)
        dut.clock.step()

        dut.io.outputBundle.mAddr.expect(0.U)
        dut.io.addr.poke(actualMaddr.U)
        dut.io.dataIn.poke(randData.U)
        dut.io.write.poke(true.B)
        dut.clock.step()
        dut.io.write.poke(false.B)
        dut.io.outputBundle.mAddr.expect(randData.U)
      }
  }

    def randInt(max: Int): Int = scala.util.Random.nextInt(max)

}
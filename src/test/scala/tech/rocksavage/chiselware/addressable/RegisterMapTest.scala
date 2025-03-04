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
    class I2cStub32 extends Module {
        val i2cStubRegMap = new RegisterMap(32,8,Some(8))
        def getRegMap = i2cStubRegMap
        val io = IO(
            new Bundle {
                val i = Input(UInt(1.W))
                val o = Output(UInt(1.W))
            }
        )
        io.o := io.i

        val mctrl  = RegInit(0.U(32.W))
        i2cStubRegMap.createAddressableRegister(mctrl, "mctrl")
        val mstatus = RegInit(0.U(32.W))
        i2cStubRegMap.createAddressableRegister(mstatus, "mstatus")
        val mbaud   = RegInit(10.U(32.W))
        i2cStubRegMap.createAddressableRegister(mbaud, "mbaud")
        val maddr   = RegInit(0.U(32.W))
        i2cStubRegMap.createAddressableRegister(maddr, "maddr")
        val mdata   = RegInit(0.U(32.W))
        i2cStubRegMap.createAddressableRegister(mdata, "mdata")
        // SLAVE REGISTERS
        val sctrl     = RegInit(0.U(32.W))
        i2cStubRegMap.createAddressableRegister(sctrl, "sctrl")
        val sstatus   = RegInit(0.U(32.W))
        i2cStubRegMap.createAddressableRegister(sstatus, "sstatus")
        val saddr     = RegInit(0.U(32.W))
        i2cStubRegMap.createAddressableRegister(saddr, "saddr")
        val sdata     = RegInit(0.U(32.W))
        i2cStubRegMap.createAddressableRegister(sdata, "sdata")
    }

    class I2cStub8 extends Module {
        val i2cStubRegMap = new RegisterMap(8,8,Some(8))
        def getRegMap = i2cStubRegMap
        val io = IO(
            new Bundle {
                val i = Input(UInt(1.W))
                val o = Output(UInt(1.W))
            }
        )
        io.o := io.i

        val mctrl  = RegInit(0.U(8.W))
        i2cStubRegMap.createAddressableRegister(mctrl, "mctrl")
        val mstatus = RegInit(0.U(8.W))
        i2cStubRegMap.createAddressableRegister(mstatus, "mstatus")
        val mbaud   = RegInit(10.U(8.W))
        i2cStubRegMap.createAddressableRegister(mbaud, "mbaud")
        val maddr   = RegInit(0.U(8.W))
        i2cStubRegMap.createAddressableRegister(maddr, "maddr")
        val mdata   = RegInit(0.U(8.W))
        i2cStubRegMap.createAddressableRegister(mdata, "mdata")
        // SLAVE REGISTERS
        val sctrl     = RegInit(0.U(8.W))
        i2cStubRegMap.createAddressableRegister(sctrl, "sctrl")
        val sstatus   = RegInit(0.U(8.W))
        i2cStubRegMap.createAddressableRegister(sstatus, "sstatus")
        val saddr     = RegInit(0.U(8.W))
        i2cStubRegMap.createAddressableRegister(saddr, "saddr")
        val sdata     = RegInit(0.U(8.W))
        i2cStubRegMap.createAddressableRegister(sdata, "sdata")
    }

    it should "Intantiate the correct I2C Registermap at 32 data size" in {
        test(new I2cStub32) { dut =>

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

    it should "Intantiate the correct I2C Registermap at 8 data size" in {
        test(new I2cStub8) { dut =>

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

}
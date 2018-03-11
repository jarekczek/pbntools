package jc.pbntools

import org.junit.After
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.util.*

class PropertiesTest {
  companion object {
    val outFile = File.createTempFile("PbnTools", "properties")

    @AfterClass
    fun close() {
      outFile.delete()
    }
  }

  @Test
  fun storeProps() {
    val props = Properties()
    props["bbo.user"] = "USER"
    props["bbo.pass"] = "PASS"
    PbnToolsKt.saveProperties(props, outFile.toString())
    println(outFile.readText())

    val propsMasked = Properties()
    val instr = outFile.inputStream()
    propsMasked.load(instr)
    instr.close()
    Assert.assertTrue(propsMasked["bbo.user"]!!.equals("USER"))
    Assert.assertNull(propsMasked["bbo.pass"])
  }
}
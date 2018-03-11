package jc.pbntools

import jc.f
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.lang.RuntimeException
import java.util.*

object PbnToolsKt {
  val pbnTools = PbnTools.getInstance()

  @Throws(IOException::class)
  fun saveProperties(props: Properties, fileName: String)
  {
    f.trace(1, "Properties stored in file " + fileName)
    val propsMasked = Properties()
    props.propertyNames().asSequence().forEach { key ->
      if (!"bbo.pass".equals(key))
        propsMasked[key] = props[key]
    }
    var ou = FileOutputStream(fileName)
    propsMasked.store(
      OutputStreamWriter(ou, "ISO-8859-1"),
      null)
    ou.close()
  }
}
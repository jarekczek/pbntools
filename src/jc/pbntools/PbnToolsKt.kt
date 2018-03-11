package jc.pbntools

import jc.f
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.lang.RuntimeException
import java.util.*

object PbnToolsKt {
  @Throws(IOException::class)
  fun saveProperties(props: Properties, fileName: String)
  {
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
    f.trace(1, "Properties stored in file " + fileName)
  }
}
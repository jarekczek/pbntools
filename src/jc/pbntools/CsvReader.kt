package jc.pbntools

import java.io.InputStream

class CsvReader(val sep: String = ";") {
  fun read(istr: InputStream): Array<Map<String, String>> {
    val rd = istr.bufferedReader(Charsets.UTF_8)
    val header = rd.readLine()
    val cols: Array<String> = header.split(sep).toTypedArray()
    val rows = ArrayList<Map<String, String>>()
    while (true) {
      val line = rd.readLine()
      if (line == null)
        break
      val row = HashMap<String, String>()
      val fields = line.split(sep)
      fields.mapIndexed { i: Int, value: String ->
        row[cols[i]] = value
      }
      rows.add(row)
    }
    return rows.toTypedArray()
  }
}

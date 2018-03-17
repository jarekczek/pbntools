package jc.pbntools

import org.assertj.core.api.Assertions
import org.junit.Test
import java.io.File

class CsvReaderTest {
  @Test
  fun readResults() {
    val csvFile = File("test/test_bbo_postprocess", "results.csv")
    val rows = CsvReader().read(csvFile.inputStream())
    Assertions.assertThat(rows).hasSize(32)
    Assertions.assertThat(rows[0].size).isEqualTo(3)
    println(rows[5].keys.joinToString("\n"))
    Assertions.assertThat(rows[5]["Result"]).isEqualTo("42.26")
  }
}
package jc.outputwindow

class TestPrinter : SimplePrinter {
  private val lines: ArrayList<String> = ArrayList<String>()

  private var partialLine = false

  override fun addLine(s: String?) {
    println("test printer: " + s)
    lines.add(s!!)
    partialLine = false
  }

  override fun addText(s: String?) {
    print(s)
    if (partialLine)
      lines.set(lines.size - 1, lines.last() + s)
    else {
      lines.add(s!!)
    }
    partialLine = true
  }

  override fun setTitle(sTitle: String?) {
  }

  public fun getLines(): ArrayList<String> = lines
}
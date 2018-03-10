package jc

import java.io.File

fun main(args: Array<String>) {
  val dir = File(args[0])
  if (dir.exists())
    dir.deleteRecursively()
}
package jc.pbntools

import org.assertj.core.api.Assertions
import org.junit.Assert
import org.junit.Test
import java.math.BigDecimal

class SimpleResultsRankerTest {

  fun PairResult(name: String, points: Int) = listOf(
    UserResult(name + "1", BigDecimal(points)),
    UserResult(name + "1", BigDecimal(points))
  )

  @Test
  fun test2() {
    val results = listOf(
      PairResult("a", 1),
      PairResult("b", 2)
    ).flatMap { listOf(it.first(), it.last()) }
    val ranked = SimpleResultsRanker().rank(results)
    ranked.forEach { println(it) }
    Assert.assertEquals("b1", ranked.first().res.name)
    Assert.assertEquals(2, ranked.last().rank)
  }

  @Test
  fun test3() {
    val results = listOf(
      PairResult("a", 2),
      PairResult("b", 2),
      PairResult("c", 1)
    ).flatMap { listOf(it.first(), it.last()) }
    val ranked = SimpleResultsRanker().rank(results)
    ranked.forEach { println(it) }
    Assert.assertEquals(3, ranked.last().rank)
  }

  @Test
  fun testTie() {
    val results = listOf(
      PairResult("a", 1),
      PairResult("b", 1)
    ).flatMap { listOf(it.first(), it.last()) }
    val ranked = SimpleResultsRanker().rank(results)
    ranked
      .sortedByDescending { it.rank }
      .forEach {
        println(it)
      }
    Assertions.assertThat(ranked).allMatch { it.rank == 1 }
  }

  @Test
  fun testAlphabetic() {
    val results = listOf(
      PairResult("a", 1),
      PairResult("b", 1)
    ).flatMap { listOf(it.first(), it.last()) }
    var ranked = SimpleResultsRanker().rank(results)
    ranked.forEach { println(it) }
    Assertions.assertThat(ranked.first().res.name).isEqualTo("a1")
    ranked = SimpleResultsRanker().rank(results.reversed())
    Assertions.assertThat(ranked.first().res.name).isEqualTo("a1")
  }


}

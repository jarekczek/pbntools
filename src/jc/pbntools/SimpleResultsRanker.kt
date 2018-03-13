package jc.pbntools

import java.math.BigDecimal

/**
 * Gives pdf points from 1 to n, based on ranks.
 */
class SimpleResultsRanker: ResultsRanker {
  override fun rank(userResults: Iterable<UserResult>): Iterable<UserResultRanked> {
    val users = userResults.count()
    fun calcPdf(rank: Int) = BigDecimal(users - rank + 1)
    return userResults
      .sortedBy { it.points }
      .reversed()
      .mapIndexed { i: Int, res: UserResult ->
        val rank = i + 1
        UserResultRanked(res, rank, calcPdf(rank))
      }
  }
}
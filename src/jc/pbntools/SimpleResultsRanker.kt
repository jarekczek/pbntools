package jc.pbntools

import java.math.BigDecimal

/**
 * Gives pdf points from 1 to n, based on ranks.
 */
class SimpleResultsRanker: ResultsRanker {
  override fun rank(userResults: Iterable<UserResult>): Iterable<UserResultRanked> {
    val pairs = userResults.count() / 2
    fun calcPdf(rank: Int) = BigDecimal(pairs - rank + 1)
    var prev: UserResultRanked? = null
    return userResults
      .sortedWith(object : Comparator<UserResult> {
        override fun compare(r1: UserResult, r2: UserResult) =
          if (r1.points.equals(r2.points))
            r1.name.compareTo(r2.name)
          else
            r2.points.compareTo(r1.points)
      })
      .mapIndexed { i: Int, res: UserResult ->
        val rank = if (res.points.equals(prev?.res?.points))
          prev!!.rank
        else
          // These are pair tournaments, so only every second should increment rank.
          i / 2 + 1
        prev = UserResultRanked(res, rank, calcPdf(rank))
        prev!!
      }
  }
}

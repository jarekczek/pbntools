package jc.pbntools

interface ResultsRanker {
  /**
   * Returned collection is sorted by rank desc, name asc.
   */
  fun rank(userResults: Iterable<UserResult>): Iterable<UserResultRanked>
}
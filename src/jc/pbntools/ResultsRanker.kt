package jc.pbntools

interface ResultsRanker {
  fun rank(userResults: Iterable<UserResult>): Iterable<UserResultRanked>
}
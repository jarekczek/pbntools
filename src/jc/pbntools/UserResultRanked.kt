package jc.pbntools

import java.math.BigDecimal

data class UserResultRanked(
  val res: UserResult,
  val rank: Int,
  val pdf: BigDecimal // long term points
) {}
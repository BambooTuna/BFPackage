package com.github.BambooTuna.BFPackage

import enumeratum.values._

object Protocol {

  sealed abstract class StreamChannel(val value: String) extends StringEnumEntry
  case object StreamChannel extends StringEnum[StreamChannel] with StringCirceEnum[StreamChannel] {

    case object Executions_FX extends StreamChannel("lightning_executions_FX_BTC_JPY")
    case object Executions_Spot extends StreamChannel("lightning_executions_BTC_JPY")

    case object Ticker_FX extends StreamChannel("lightning_ticker_FX_BTC_JPY")
    case object Ticker_Spot extends StreamChannel("lightning_ticker_BTC_JPY")

    case object Board_FX extends StreamChannel("lightning_board_FX_BTC_JPY")
    case object Board_Spot extends StreamChannel("lightning_board_BTC_JPY")

    case object Board_snapshot_FX extends StreamChannel("lightning_board_snapshot_FX_BTC_JPY")
    case object Board_snapshot_Spot extends StreamChannel("lightning_board_snapshot_BTC_JPY")

    val values = findValues
  }

}

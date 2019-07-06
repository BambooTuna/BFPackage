# BFPackage

## 概要
Bitflyerで自動取引Botを作る際に必要になってくる、
- 注文周り（リアルタイムポジション管理）
- Websocket購読
をやってくれるものです。

## 使い方
### 依存
```
    resolvers += "Maven Repo on github" at "https://BambooTuna.github.io/CryptoLib",
    resolvers += "Maven Repo on github" at "https://BambooTuna.github.io/WebSocketManager",
    resolvers += "Maven Repo on github" at "https://BambooTuna.github.io/BFPackage",
    libraryDependencies ++= Seq(
          "com.github.BambooTuna" %% "bfpackage" % "2.0.1-SNAPSHOT",
        )
```


### 注文周り
詳細は[こちら]()を見てください。

**主な機能**
- 新規注文
- 個別注文キャンセル
- 全注文キャンセル
- 約定などで保持ポジション数量が変動した際にすぐに通知される
    - その際はオープンな注文も通知されます。
    - 一定時間ごとにRESTAPIでズレの修正
    - 任意のタイミングで最新情報の取得
    

### Websocket購読
詳細は[こちら]()を見てください。


**主な機能**
- 再起動、再接続
    - 一定時間データ受信できない時
    - コネクションエラー時
    - その他エラーが起きた時
    
**購読可能チャンネル**
```scala
  sealed abstract class StreamChannel(val value: String) extends StringEnumEntry
  case object StreamChannel extends StringEnum[StreamChannel] with StringCirceEnum[StreamChannel] {
    case object Executions_FX       extends StreamChannel("lightning_executions_FX_BTC_JPY")
    case object Executions_Spot     extends StreamChannel("lightning_executions_BTC_JPY")
    case object Ticker_FX           extends StreamChannel("lightning_ticker_FX_BTC_JPY")
    case object Ticker_Spot         extends StreamChannel("lightning_ticker_BTC_JPY")
    case object Board_FX            extends StreamChannel("lightning_board_FX_BTC_JPY")
    case object Board_Spot          extends StreamChannel("lightning_board_BTC_JPY")
    case object Board_snapshot_FX   extends StreamChannel("lightning_board_snapshot_FX_BTC_JPY")
    case object Board_snapshot_Spot extends StreamChannel("lightning_board_snapshot_BTC_JPY")

    val values = findValues
  }
```
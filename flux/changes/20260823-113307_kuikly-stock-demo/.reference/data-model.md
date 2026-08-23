# Data Model: Kuikly AI 股票客户端 Demo

## StockQuote

Fields: `symbol`, `name`, `exchange`, `price`, `change`, `changePercent`, `open`, `high`, `low`, `previousClose`, `volume`, `trendPoints`. Symbol is the stable identity. Trend points must be non-empty for chart display.

## StockInsight

Fields: `symbol`, `trendLabel`, `summary`, `signals`, `risks`, `updatedAt`. It belongs to one quote and always includes a disclaimer at presentation time.

## LoadState

States: `Loading`, `Content<T>`, `Empty`, `Error(message)`. Retry transitions Error → Loading → Content/Empty/Error.

## ChatMessage

Fields: `id`, `role`, `blocks`, `status`, `retryQuestion`. Roles are USER and ASSISTANT. Status is COMPLETE, GENERATING or FAILED.

## ChatContentBlock

- `Markdown(text)` contains only visible Markdown after metadata removal.
- `StockCard(symbol)` resolves data through `StockRepository`.
- `Trend(symbol)` resolves normalized trend points through `StockRepository`.

## ChatState

Fields: `messages`, `draft`, `isSending`, `error`. Empty input is ignored. Sending is idempotent while `isSending` is true. Retry reuses `retryQuestion`.

<!-- cli_version: 0.2.14 -->

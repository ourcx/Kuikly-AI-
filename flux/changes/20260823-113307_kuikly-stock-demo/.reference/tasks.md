# Tasks: Kuikly AI 股票客户端 Demo

## Phase 1: Project foundation

- [x] T001 Import official Kuikly DSL 2.16.0 / Kotlin 2.1.21 scaffold.
- [x] T002 Limit production configuration to Android and add KuiklyMarkdown dependencies.
- [x] T003 Configure Android host to launch the `StockHome` Kuikly page.

## Phase 2: Domain and data

- [x] T004 Add stock, insight, chat and load-state domain models.
- [x] T005 Add price formatting, trend normalization and AI metadata parsing.
- [x] T006 Add repository contracts and deterministic stock/chat fixtures.
- [x] T007 Add market and chat controllers with retry and send de-duplication.

## Phase 3: Kuikly UI

- [x] T008 Implement app shell and market/AI navigation with Kuikly DSL.
- [x] T009 Implement market list and loading/empty/error panels.
- [x] T010 Implement reusable stock card, metric grid, sparkline and detail view.
- [x] T011 Implement chat history, input, Markdown rendering and structured content blocks.
- [x] T012 Wire market and AI cards to the shared detail view.

## Phase 4: Quality and delivery

- [x] T013 Add common tests for formatting, normalization, metadata parsing, fixture and controllers.
- [x] T014 Add README, architecture notes and local build instructions.
- [x] T015 Run source/test/build checks available in the Worker and document environment blockers.

<!-- cli_version: 0.2.14 -->

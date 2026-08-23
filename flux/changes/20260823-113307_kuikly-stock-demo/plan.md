# Implementation Plan: Kuikly AI 股票客户端 Demo

## Technical Context

- Language: Kotlin 2.1.21, JVM target 1.8
- UI: Kuikly DSL 2.16.0
- Platform: Android first, minSdk 21, compileSdk 34
- Shared module: Kotlin Multiplatform `shared`, production targets limited to Android for this iteration
- Content rendering: KuiklyMarkdown 1.0.6-2.1.21
- State: immutable screen state coordinated by presentation controllers
- Data: repository interfaces with deterministic in-memory fixtures
- Testing: commonTest for pure domain, parsing, formatting, repository and controller logic
- Environment: source tree can be completed here; Android APK build requires JDK 17 and Android SDK unavailable in this Worker

## Constitution Check

- No repository constitution exists; project and organization rules are applied directly.
- Package names are lowercase; Kotlin declarations use standard camel casing.
- Android host contains no stock state, prompts or fixture behavior.
- No credential is stored in source or configuration.
- Fixture-first flow avoids network and cleartext dependencies.

## Architecture

1. `domain`: immutable quote, insight, chat and load-state models plus formatting/parsing utilities.
2. `data`: repository contracts and fixture implementations.
3. `presentation`: market/chat controllers owning screen state and user intents.
4. `ui`: Kuikly pages and reusable cards, charts and state panels.
5. `androidApp`: lifecycle, Kuikly render registration and route host only.

Dependencies point inward: UI → presentation → domain/repository contract; fixture implementations satisfy contracts without leaking into Android.

## Navigation Contract

- `StockHome`: starts the main experience.
- In-page tab state switches between market and AI assistant.
- Selecting a quote or AI stock card pushes an in-page detail state using a stock symbol.
- Back from detail restores the prior tab and list state.

## Data Flow

- Market controller requests quotes from `StockRepository`, producing Loading → Content/Empty/Error.
- Chat controller validates input, appends one user message, requests a fixture response, parses optional stock metadata, then emits Markdown and structured blocks.
- Retry retains the failed request identity and replaces the failed assistant result without duplicating user content.

## Key Decisions

- Use Kuikly DSL rather than Kuikly Compose to align with the technical design and Markdown reference sample.
- Keep all primary navigation inside one Kuikly page to preserve state and minimize Android host responsibilities.
- Render the sparkline as Kuikly primitives derived from normalized segments; no chart dependency.
- Keep remote providers as contracts only; offline acceptance cannot depend on keys or services.
- Centralize visual rules in Design Tokens. Use a deep navy background, elevated slate cards, a restrained blue accent, red for gains and green for losses, while always pairing movement color with an explicit sign. Typography, spacing, corner radius and Markdown styling must reference the same token set.

## Verification Plan

- Compile common Kotlin and execute commonTest when toolchain permits.
- Verify dependency graph and Gradle configuration without exposing credentials.
- Statically check that every acceptance flow has a page/controller path.
- Record Android SDK/JDK blockers separately from source correctness.

<!-- cli_version: 0.2.14 -->

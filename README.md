# EZCryptoExchange ![Build](https://github.com/your-org/EZCryptoExchange/actions/workflows/build.yml/badge.svg)

A simulated cryptocurrency exchange plugin for Paper 1.21.x servers.

## Features
- GUI-first exchange flow for BTC/ETH/SOL
- Vault economy integration for USD simulation
- Limit orders with automatic async checker
- SQLite-backed wallets, balances, orders, and trades
- Chat-based custom and limit input handling

<!-- TODO: add screenshots -->

## Requirements
- Java 21
- Paper 1.21.x
- Vault + economy plugin

## Installation
1. Build/download jar.
2. Place `EZCryptoExchange-1.0.0.jar` in `plugins/`.
3. Install Vault and an economy provider.
4. Start server.

## Commands
| Command | Description | Permission |
|---|---|---|
| `/crypto` | Open main exchange GUI | `ezcrypto.use` |
| `/crypto wallet` | Show wallet and balances | `ezcrypto.use` |
| `/crypto orders` | Open open-orders GUI | `ezcrypto.use` |
| `/crypto reload` | Reload config/messages | `ezcrypto.admin` |

## Permissions
| Permission | Default | Description |
|---|---|---|
| `ezcrypto.use` | true | Use plugin |
| `ezcrypto.admin` | op | Reload plugin |

## Configuration
See `src/main/resources/config.yml` and `messages.yml` for full options.

## Build
```bash
mvn clean package
```

## License
MIT

## Credits
EZInnovations, CoinGecko API

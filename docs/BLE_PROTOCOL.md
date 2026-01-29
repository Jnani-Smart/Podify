# Apple BLE Proximity Pairing Protocol (Reverse Engineered)

This document details the reverse-engineered protocol used by Podify to detect AirPods status.

## Manufacturer Specific Data

AirPods broadcast status via Bluetooth Low Energy (BLE) using Manufacturer Specific Data (0xFF).
- **Company ID**: `0x004C` (Apple Inc.)
- **Length**: 27 Bytes (AirPods Pro / Gen 2 / Gen 3)

### Structure

| Byte Index | Length | Description | Notes |
|------------|--------|-------------|-------|
| 0 | 1 | Message Type | `0x07` = Proximity Pairing |
| 1 | 1 | Length | `0x19` (25 bytes follow) |
| 2 | 1 | Status Byte? | Usually `0x01` |
| 3-4 | 2 | Device Model | e.g. `0x0220` (AirPods 1) |
| 5 | 1 | **Wearing State (UTP)** | See State Table below |
| 6 | 1 | **Battery 1** | Left + Right levels |
| 7 | 1 | **Battery 2** | Case level + Charging flags |
| 8 | 1 | Lid Open Count | Increments on open |
| 9 | 1 | Device Color | `0x01`=White, `0x06`=Silver, etc. |
| 10-26 | 16 | Encrypted Payload | Keys required (not used) |

### Wearing State (Byte 5)

Based on hexway/CAPods research:

| Value (Hex) | Meaning |
|-------------|---------|
| `0x00` | Case Closed |
| `0x01` | All Out of Case |
| `0x03` | Left Out |
| `0x05` | Right Out |
| `0x0B` | Both In Ear |
| `0x13` | Right In Ear |
| `0x53` | Left In Ear |
| `0x50` | Case Open |

### Battery Encoding (Byte 6 & 7)

**Byte 6 (Left/Right):**
- **Upper Nibble (Bits 4-7)**: Left Bud Battery (0-10 scale)
- **Lower Nibble (Bits 0-3)**: Right Bud Battery (0-10 scale)

**Byte 7 (Case/Charging):**
- **Upper Nibble (Bits 4-7)**: Case Battery (0-10 scale)
- **Lower Nibble (Bits 0-3)**: Charging Flags`

**Charging Flags (Byte 7 Lower):**
- `Bit 2`: Case Charging
- `Bit 1`: Right Charging
- `Bit 0`: Left Charging

**Calculating Percentage:**
Value `0-10` maps to `0-100%`.
Value `15 (0xF)` means "Disconnected" or "Unknown".

## References

- [hexway/apple_bleee](https://github.com/hexway/apple_bleee)
- [OpenPods](https://github.com/OpenPods/OpenPods)

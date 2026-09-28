# OraLite — Oracle Database Mobile Editor

Lightweight, mobile-first Oracle Database client for Android. Built with 100% Kotlin, Jetpack Compose, Room, and dual-mode database engine (Direct JDBC & REST Middleware).

## Features
- **Dual Connection Engine:**
  - **Direct TCP 1521:** Native Oracle thin driver (`ojdbc6:11.2.0.4`) over local Wi-Fi / VPN without Java 8+ `DriverAction` crash on Android ART.
  - **REST Middleware:** HTTP JSON query execution (`http://192.168.1.1:8192/api/query`) for proxy/middleware setups.
- **Mobile-Optimized SQL Input:**
  - Quick SQL Chips bar (`SELECT`, `FROM`, `WHERE`, `*`, `AND`, `ORDER BY`, `ROWNUM`, `;`, `'`, `,`).
  - Realtime SQL syntax highlighting (keywords, functions, strings, numbers, comments).
  - SQL uppercase formatter & monospace editor font.
  - Collapsible query editor to maximize data reading area.
  - Query auto-save & bookmarked query manager.
- **Persistent State:**
  - Saves last connected database profile and active query across restarts.
- **Responsive 2D Data Grid:**
  - Sticky column headers with horizontal & vertical scrolling.
  - Auto-calculated column widths with max-width bounding.
  - **Tap-to-Inspect:** Tap any row to open the vertical Key-Value Bottom Sheet (ideal for wide columns).
- **Safety Guard:**
  - Injects `ROWNUM <= 100` automatically to prevent out-of-memory crashes on large tables.
- **Theming:**
  - Default Light theme with instant Dark theme toggle.

## Tech Stack
- Kotlin 2.0.21
- Jetpack Compose & Material 3
- Room 2.6.1
- Oracle JDBC Driver (`com.oracle.database.jdbc:ojdbc6:11.2.0.4`)
- OkHttp 4.12.0 & Gson 2.11.0

## Build & Test
```bash
# Run unit tests
./gradlew testDebugUnitTest

# Assemble Debug APK
./gradlew assembleDebug

# Assemble Signed Release APK
./gradlew assembleRelease
```

## License
MIT License. See [LICENSE](LICENSE) for details.

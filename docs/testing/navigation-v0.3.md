# Navigation v0.3 validation

Executed on 2026-11-06 using Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.165.0+26.3, Temurin Java 25 and the Gradle 9.8.1 wrapper on Windows.

## Diagnosis

The previous implementation used greedy best-first search without accumulated path costs. It required full-cube support, picked a single stand position before checking reachability, mixed planning into the keyboard callback and abandoned routes without bounded replanning. Drops were treated as exact block-position goals, even when their actual pickup surface was elsewhere. The `onlyVisible` check rejected hidden mining targets before a route could be attempted.

The 26.3 bytecode confirms that `KeyboardInput.tick()` first reads physical key mappings and then derives `ClientInput.moveVector`. The replacement injection runs at its tail, preserves physical takeover, and supplies matching `Input` flags and an actual movement vector. The acceptance test verifies both a nonzero vector and displacement of the real integrated-server player; this is not a position-only mock.

## Unit tests and build

Command: `gradlew.bat --offline build --console plain`

Result: **BUILD SUCCESSFUL**, **45 JUnit tests**, zero failures/errors. Existing filter, inventory, preset, metadata and transaction tests remain included. New tests cover A* accumulated costs, improved nodes, multiple goal positions, limits, no-path/start-at-goal, collision-free corner paths, input coordinate transforms, arrival height/tolerance, navigation transitions, priorities and mining costs.

## Actual client acceptance run

Command: `gradlew.bat runClientGameTest --console plain`

Result: **BUILD SUCCESSFUL**, all **11 acceptance scenarios** in `NavigationClientTest` completed. The framework launched a real client with its integrated Survival server. No production server or normal save was modified. Startup had offline-development-account service warnings; these did not prevent local test execution.

| Scenario | Observed assertion |
| --- | --- |
| Open corridor | Nonzero normal `ClientInput` vector and actual server-player displacement, followed by arrival |
| Corner/detour | Collision-free arrival around a wall using a longer route with right-angle turns |
| Height and shapes | Actual ascent over a path-block/stair fixture to the higher goal |
| Hidden ore | With `onlyVisible=true`, walked around the wall, mined normally and received a diamond |
| Stone tunnel | At least two obstacles removed after authoritative server block confirmations; reached goal |
| Bedrock | Failed safely before the wall; no removed obstacles |
| Lava | Refused route without entering lava or catching fire |
| Moving drop | Followed relocated item, received pickup packet for own player and updated inventory |
| Chest Auto Sorter | Walked to chest and deposited exactly 16 diamonds verified in server chest contents |
| Manual takeover | Physical movement paused navigation and synthetic forward input was released |
| Server denial | Test-only break callback denied all wall mining; no confirmed removal or passage through wall |

The local log contains these markers (Europe/Amsterdam):

```text
00:46:01 NAV_SMOKE_PASS: open corridor: real client input and server-validated displacement
00:46:06 NAV_SMOKE_PASS: corner and obstacle detour
00:46:10 NAV_SMOKE_PASS: path block, stairs and jump/height difference
00:46:17 NAV_SMOKE_PASS: hidden target detected despite onlyVisible; normal mining and pickup
00:46:20 NAV_SMOKE_PASS: stone tunnel and authoritative server block confirmations
00:46:21 NAV_SMOKE_PASS: unmineable barrier
00:46:21 NAV_SMOKE_PASS: lava route refused
00:46:25 NAV_SMOKE_PASS: moving drop: reachable pickup position and actual inventory/pickup acknowledgement
00:46:28 NAV_SMOKE_PASS: chest walk-only route and existing sorter transaction workflow
00:46:29 NAV_SMOKE_PASS: manual movement cancels synthetic input
00:46:38 NAV_SMOKE_PASS: server mining refusal prevents movement and is not bypassed
```

Local outputs: `build/reports/tests/test/index.html`, `build/run/clientGameTest/logs/latest.log`, and `build/run/clientGameTest/screenshots/0000_yunaget-smart-navigation-v0.3.png`. These generated files are deliberately not committed. The final screenshot is the end-of-suite view, not evidence that every route was visually inspected.

## Limits of this evidence

- Tested against the integrated vanilla/Fabric server, including a test-only mining denial callback. External servers, claim plugins and Lunar Client were not separately tested.
- The automated suite is finite; it does not prove arbitrary terrain, all collision shapes, all latency conditions or every possible moving obstruction.
- Live route validation/replanning is implemented; the corner fixture tests a static detour, not an exhaustive dynamic-obstacle scenario matrix.
- Server block updates are required for route excavation. Local prediction is not accepted as permission. A server that withholds confirmation causes a stop/replan, never a bypass.
- Debugging and configuration are available, but no claim is made that every UI page was manually inspected.

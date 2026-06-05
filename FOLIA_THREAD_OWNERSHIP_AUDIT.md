# Folia thread ownership audit

Scope: core plugin/API paths plus bundled PerWorldInventory integration and current 1.21.11 platform modules. Ownership terms follow Folia's scheduler model: global region for global server/plugin state, entity scheduler for player/entity state, region scheduler for world/chunk/location state, and async only for non-Bukkit blocking work.

## Bukkit/NMS ownership table

| Area | Representative files / calls | Current owner decision | Required follow-up |
| --- | --- | --- | --- |
| Command entry and tab permissions | `InvseeCommandExecutor`, `EnderseeCommandExecutor`, `ReloadCommandExecutor`, `InvseeTabCompleter`; `hasPermission`, command sender checks | Command sender entity for `Player`; console/global for non-player sender | Keep permission checks before completions; audit async tab event separately before enabling more async Bukkit reads. |
| Command feedback | Command `sendMessage` calls | Player entity scheduler for players; direct for console | Implemented for main command responses; keep addon command feedback in task 12. |
| Online player name completion | `InvseeTabCompleter#getOnlinePlayers`, `Player#canSee` | Viewer entity/global snapshot; not async-only | Move online player snapshots behind scheduler/cache in task 10. |
| Player lookup by UUID/name | `Server#getPlayer`, `getPlayerExact` in `InvseeAPI`, `FoliaScheduler`, PWI integration | Entity lookup gate only; all player state mutation must move to entity scheduler | Remaining lookup+immediate inventory creation in `mainSpectatorInventory`/`enderSpectatorInventory` is follow-up for tasks 7-9. |
| Opening spectator inventories | `platform.openMainSpectatorInventory`, `platform.openEnderSpectatorInventory`, `HumanEntity#openInventory` | Viewer entity scheduler | Async open futures and PWI command opens now schedule final open on viewer entity. Direct deprecated sync APIs remain compatibility-only and should be migrated/removed later. |
| Closing/reopening spectator viewers | `HumanEntity#closeInventory`, `HumanEntity#openInventory` in join/PWI profile transitions | Viewer entity scheduler | Core join transfer and PWI profile transitions now schedule close/open per viewer entity; remaining container transaction safety is task 7. |
| Inventory mutation / live inventory writes | `setContents`, NMS `clicked`, wrapper inventories, PWI live transfer | Target entity scheduler for online targets; async file/NBT flow for offline targets | Not safe yet: convert to snapshot/diff/commit in task 7 and split live/offline flows in task 8. |
| Inventory close save | `InventoryCloseEvent`, `saveInventory`, `saveEnderChest` | Event owner for view close; async/file owner for save; player entity for error feedback | Error feedback now schedules on event player entity; save flow thread ownership remains task 8/9. |
| Event registration / unregistration | `registerEvents`, `HandlerList.unregisterAll` | Global/plugin lifecycle | Keep in enable/disable/global context. |
| Event dispatch | `EventHelper#callEvent` | Global or event-specific owner before call | Audit platform event firing after scheduler transaction refactor. |
| NMS container access | `MainNmsContainer#clicked`, `EnderNmsContainer#clicked`, fake Craft/NMS players in 1.21.11 modules | Viewer entity currently; target entity required for live target writes | Explicitly unsafe for Folia until task 7 snapshot/diff/commit is implemented. |
| Player data save/load | platform `createOfflineInventory`, `saveInventory`, PWI data source, save-file strategies | Async for disk/NBT; region/entity only for Bukkit touchpoints | Catalog exact file IO call sites in task 8. |
| World/location data | PWI logout location/world/group lookups, region tasks in scheduler | Region scheduler for world/chunk/location-owned operations | Convert direct world access in PWI save/load logic in tasks 8 and 13. |

## Third-party API ownership

| API | Current call sites | Allowed owner decision | Follow-up |
| --- | --- | --- | --- |
| LuckPerms / permission plugins | `Exempt`, name/UUID permission strategies, command bypass permissions | Prefer async for LuckPerms-style lookups already documented in code; legacy permission plugins need sync/entity wrapper unless documented safe | Task 11 must split LuckPerms from legacy permission plugins and document each supported API. |
| Vault | Common permission/economy integration through permission strategies | Unknown; assume not async-safe unless Vault provider documents otherwise | Task 11: wrap provider calls on global/entity scheduler or disable unsafe async usage. |
| PerWorldInventory | `PerWorldInventorySeeApi`, `PerWorldInventoryHook`, PWI events/data source | PWI events on triggering player/entity; data source/file access async unless API documents main-thread requirement | Task 13: verify PWI API thread guarantees; current open/close viewer actions are entity-scheduled. |
| Multiverse-Inventories | Disabled/commented integration module | Unknown | Task 13: keep disabled on Folia until API thread guarantees are known. |
| GroupManager / BungeePerms / UltraPermissions | Common permission resolve strategies | Unknown legacy APIs; assume not async-safe | Task 11: isolate behind scheduler adapters or mark unsupported on Folia. |

## Follow-up tasks created from unknown/unsafe ownership

1. Task 7: replace direct NMS/live inventory mutation with snapshot/diff/commit on target entity scheduler.
2. Task 8: separate online target entity flow from offline async file/NBT flow, including retired callbacks.
3. Task 9: make caches and pending futures explicitly thread-safe under Folia concurrency.
4. Task 10: move tab-completion online-player reads to safe snapshots and preserve permission-first filtering.
5. Task 11: document and adapt every permission provider by actual thread guarantee.
6. Task 13: verify PerWorldInventory and Multiverse-Inventories APIs; disable or adapt unsafe integrations on Folia.

## Documentation references

- Folia 1.21.11 scheduler Javadocs: https://jd.papermc.io/folia/1.21.11/io/papermc/paper/threadedregions/scheduler/package-summary.html
- Paper plugin development docs: https://docs.papermc.io/paper/dev/
- Bukkit API Javadocs: https://hub.spigotmc.org/javadocs/bukkit/

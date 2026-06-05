# Folia thread ownership audit

Scope: core plugin/API paths plus bundled PerWorldInventory integration and current 1.21.11 platform modules. Ownership terms follow Folia's scheduler model: global region for global server/plugin state, entity scheduler for player/entity state, region scheduler for world/chunk/location state, and async only for non-Bukkit blocking work.

## Bukkit/NMS ownership table

| Area | Representative files / calls | Current owner decision | Required follow-up |
| --- | --- | --- | --- |
| Command entry and tab permissions | `InvseeCommandExecutor`, `EnderseeCommandExecutor`, `ReloadCommandExecutor`, `command.CommandCompletionService`; `hasPermission`, command sender checks | Command sender entity for `Player`; async tab reads permission UUID snapshot only | Permission checks happen before completion generation; async tab no longer calls Bukkit online-player APIs. |
| Command feedback | Command `sendMessage` calls | Player entity scheduler for players; direct for console | Implemented for main command responses; keep addon command feedback in task 12. |
| Online player name completion | `CommandCompletionService#onlineNames`, player join/quit snapshots | Global plugin snapshot updated from scheduled server work/events; async readers use the snapshot only | Implemented for core `/invsee` and `/endersee` completions. |
| Player lookup by UUID/name | `Server#getPlayer`, `getPlayerExact` in `InvseeAPI`, `FoliaScheduler`, PWI integration | Entity lookup gate only; all player state mutation must move to entity scheduler | Pending offline creations are now deduplicated by global concurrent request registry; remaining direct online lookup state mutation is tracked under live/offline flow cleanup. |
| Opening spectator inventories | `platform.openMainSpectatorInventory`, `platform.openEnderSpectatorInventory`, `HumanEntity#openInventory` | Viewer entity scheduler | Async open futures and PWI command opens now schedule final open on viewer entity. Direct deprecated sync APIs remain compatibility-only and should be migrated/removed later. |
| Closing/reopening spectator viewers | `HumanEntity#closeInventory`, `HumanEntity#openInventory` in join/PWI profile transitions | Viewer entity scheduler | Core join transfer and PWI profile transitions now schedule close/open per viewer entity; remaining container transaction safety is task 7. |
| Inventory mutation / live inventory writes | `setContents`, NMS `clicked`, wrapper inventories, PWI live transfer | Target entity scheduler for online targets; async file/NBT flow for offline targets | 1.21.11 main/ender NMS containers now mutate a snapshot on the viewer thread and commit only on the target entity owner; remaining non-1.21.11 and save-flow work stays in tasks 8-9. |
| Inventory close save | `InventoryCloseEvent`, `saveInventory`, `saveEnderChest` | Event owner for view close; async/file owner for save; player entity for error feedback | Error feedback now schedules on event player entity; save flow thread ownership remains task 8/9. |
| Event registration / unregistration | `registerEvents`, `HandlerList.unregisterAll` | Global/plugin lifecycle | Keep in enable/disable/global context. |
| Event dispatch | `EventHelper#callEvent` | Global or event-specific owner before call | Audit platform event firing after scheduler transaction refactor. |
| NMS container access | `MainNmsContainer#clicked`, `EnderNmsContainer#clicked`, fake Craft/NMS players in 1.21.11 modules | Viewer entity for UI click calculation; target entity for live player commit | Implemented for Paper/Folia and CraftBukkit 1.21.11 with a shared transaction service: conflicts abort/resync and retired targets go through an async offline-flow hook. |
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

1. Task 7: replace direct NMS/live inventory mutation with snapshot/diff/commit on target entity scheduler. **Implemented for 1.21.11 main/ender NMS containers; extend the same service to older NMS modules before setting global Folia support.**
2. Task 8: separate online target entity flow from offline async file/NBT flow, including retired callbacks. **Partially implemented for retired 1.21.11 container commits; offline save/load still needs full async service extraction.**
3. Task 9: make caches and pending futures explicitly thread-safe under Folia concurrency. **Implemented for open spectator cache, UUID/name snapshots and pending request registries.**
4. Task 10: move tab-completion online-player reads to safe snapshots and preserve permission-first filtering. **Implemented for core command/tab-completion module.**
5. Task 11: document and adapt every permission provider by actual thread guarantee.
6. Task 13: verify PerWorldInventory and Multiverse-Inventories APIs; disable or adapt unsafe integrations on Folia.

## Documentation references

- Folia 1.21.11 scheduler Javadocs: https://jd.papermc.io/folia/1.21.11/io/papermc/paper/threadedregions/scheduler/package-summary.html
- Paper plugin development docs: https://docs.papermc.io/paper/dev/
- Bukkit API Javadocs: https://hub.spigotmc.org/javadocs/bukkit/


## Manual Folia scenario for spectator container transactions

1. Start Folia 1.21.11 with two players in different regions: `viewer` and `target`.
2. Run `/invsee target` and move at least one item in the target main inventory from the viewer GUI while the target also moves an item locally. Expected: the viewer-side snapshot either commits on the target entity owner or aborts/resyncs; no region thread violation or concurrent modification is logged.
3. Repeat with `/endersee target` and ender chest slots. Expected: same commit/abort behavior.
4. While the viewer GUI is open, make the target teleport between distant regions. Expected: commits follow the target entity scheduler and do not fall back to global region execution.
5. While the viewer GUI is open, make the target log out. Expected: the retired callback runs, the live entity is no longer touched, and pending snapshot state is handed to the async offline-flow hook without blocking a region tick.

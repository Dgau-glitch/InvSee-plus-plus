# Folia 1.21.11 QA plan

This checklist is the release gate for publishing InvSee++ with `folia-supported: true`. It covers the single-jar strategy used by this repository: the normal InvSee++ jar is installed on Folia 1.21.11, and runtime detection must select the Folia scheduler plus the compatible 1.21.11 platform implementation.

## 1. Test environment

- Server: Folia `1.21.11-R0.1-SNAPSHOT` or the exact release build used for the target publication.
- Java: Java 21 or newer for Minecraft 1.21.11 runtime. If the full Maven reactor includes 26.x modules, compile them with the Java version required by that module line before release.
- Plugins to install for the baseline pass:
  - `InvSee++.jar` from `InvSee++_Plugin/target/`.
  - Migrated bundled addons: Give, Clear and Clone.
  - No PerWorldInventory or Multiverse-Inventories during the baseline pass.
- Plugins to install for integration passes:
  - LuckPerms, Vault and one legacy permission provider at a time.
  - PerWorldInventory only for the disabled-on-Folia warning check.
  - Multiverse-Inventories only for the disabled/unsupported integration check.

## 2. Automated checks before starting Folia

Run these checks from the repository root and record the command, Java version and result in the release notes:

| Check | Command | Expected result |
| --- | --- | --- |
| Patch hygiene | `git diff --check` | No whitespace or conflict-marker errors. |
| Common API compile | `mvn -pl InvSee++_Common -am -DskipTests compile` | Compiles with scheduler/API abstractions. |
| Plugin compile/package | `mvn -pl InvSee++_Plugin -am -DskipTests package` | Produces the main jar and resolves Folia API as `provided`. |
| Addon compile | `mvn -pl InvSee++_Give_Plugin,InvSee++_Clear_Plugin,InvSee++_Clone_Plugin -am -DskipTests compile` | Addons compile against the shared scheduler/API services. |
| Integration compile | `mvn -pl InvSee++_PerWorldInventory -am -DskipTests compile` | Integration compiles; runtime remains disabled on Folia until audited. |

## 3. Startup and shutdown checklist

| Step | Action | Expected result |
| --- | --- | --- |
| FOLIA-START-001 | Start a clean Folia 1.21.11 server with InvSee++ only. | Plugin enables without unsupported-version errors. Logs identify `Folia version 1.21.11`. |
| FOLIA-START-002 | Confirm scheduler selection in logs/debug output. | Folia path uses entity/region/global/async schedulers, not the Bukkit scheduler fallback. |
| FOLIA-START-003 | Run `/plugins` and inspect plugin metadata. | InvSee++ and migrated addons are loaded; Folia does not reject them for missing `folia-supported`. |
| FOLIA-STOP-001 | Stop the server with no active viewers. | Shutdown completes; pending futures/tasks are cancelled or completed without hanging. |
| FOLIA-STOP-002 | Stop the server while viewers have main and ender inventories open. | Viewers are closed safely, target commits are either completed or aborted deterministically, and no task keeps the JVM alive. |

## 4. Command and tab-completion checklist

Run each command once from console and once from an operator player, then repeat with a player that has no InvSee++ permissions.

| Step | Action | Expected result |
| --- | --- | --- |
| FOLIA-CMD-001 | Tab-complete `/invsee`, `/endersee`, `/invseeplusplusreload`. | Suggestions are fast, contextual and permission-filtered. |
| FOLIA-CMD-002 | Tab-complete addon commands: `/invgive`, `/endergive`, `/invclear`, `/enderclear`, `/invclone`, `/enderclone`. | Suggestions come from cached snapshots and do not expose arguments to unauthorized players. |
| FOLIA-CMD-003 | Execute `/invsee <online-target>` and `/endersee <online-target>` from a player. | Final `openInventory` runs on the viewer entity scheduler; no Folia thread-check warning appears. |
| FOLIA-CMD-004 | Execute the same commands from console. | Console receives deterministic success/error responses through the safe command/global path. |
| FOLIA-CMD-005 | Execute `/invseeplusplusreload`. | Reload feedback is delivered safely; config changes apply without cross-thread warnings. |

## 5. Online target inventory scenarios

Use two real players, `viewer` and `target`. Repeat all checks for main inventory and ender chest.

| Step | Scenario | Expected result |
| --- | --- | --- |
| FOLIA-ONLINE-001 | Viewer and target stand near each other in the same region; viewer opens and edits target inventory. | Changes commit to target inventory; click cancellation/edit permissions are immediate. |
| FOLIA-ONLINE-002 | Viewer and target stand far apart so they are owned by different region threads; viewer opens and edits target inventory. | Snapshot/diff/commit schedules the commit on the target entity scheduler; no concurrent modification or thread violation occurs. |
| FOLIA-ONLINE-003 | Target moves items while viewer edits the spectator inventory. | Conflict handling aborts/resyncs or deterministically merges according to the transaction service; no duplicated or lost items. |
| FOLIA-ONLINE-004 | Target teleports to another region while viewer is editing. | Follow-up commits use the target entity scheduler after teleport; no stale region/global fallback is used. |
| FOLIA-ONLINE-005 | Target logs out while viewer is editing. | Retired entity callback switches to the offline flow; no further player-entity access occurs after retirement. |
| FOLIA-ONLINE-006 | Viewer logs out while a target commit is pending. | Pending UI work is cancelled/retired cleanly and target inventory remains consistent. |

## 6. Offline target scenarios

| Step | Scenario | Expected result |
| --- | --- | --- |
| FOLIA-OFFLINE-001 | Open an existing offline player's main inventory and ender chest. | File/NBT work is asynchronous; Bukkit touchpoints are scheduled safely. |
| FOLIA-OFFLINE-002 | Two viewers open the same offline target at the same time. | Pending request registry deduplicates loading and both viewers observe a consistent inventory state. |
| FOLIA-OFFLINE-003 | Edit and close an offline target inventory. | Save happens without blocking a region tick thread; pending futures are removed on success/failure. |
| FOLIA-OFFLINE-004 | Try an unknown player with offline/unknown support disabled and enabled. | Existing configuration semantics are preserved and responses are delivered safely. |
| FOLIA-OFFLINE-005 | Reload/disable while offline inventory IO is pending. | Pending work is cancelled or completed without hanging plugin shutdown. |

## 7. Addon smoke tests

| Step | Scenario | Expected result |
| --- | --- | --- |
| FOLIA-ADDON-001 | Give to online and offline targets with `/invgive` and `/endergive`. | Item creation and command feedback remain safe; offline writes do not block region threads. |
| FOLIA-ADDON-002 | Clear online and offline targets with `/invclear` and `/enderclear`. | Target mutations run through shared API/scheduler services. |
| FOLIA-ADDON-003 | Clone online-to-online, online-to-offline, offline-to-online and offline-to-offline with `/invclone` and `/enderclone`. | Clone addon uses the core API without duplicating scheduler logic and has no thread-check errors. |

## 8. Integration smoke tests

| Step | Scenario | Expected result |
| --- | --- | --- |
| FOLIA-INT-001 | Start Folia with PerWorldInventory installed. | InvSee++ logs that PerWorldInventory integration is disabled on Folia; core functionality remains enabled. |
| FOLIA-INT-002 | Start Folia with Multiverse-Inventories installed. | Integration remains disabled/unsupported; core functionality remains enabled. |
| FOLIA-INT-003 | Run edit-permission checks with LuckPerms. | Async LuckPerms lookups do not block entity threads and click cancellation remains immediate. |
| FOLIA-INT-004 | Run edit-permission checks with Vault or a legacy permission provider. | Provider calls are not made from arbitrary async tab threads; unsafe providers are wrapped or documented as unsupported. |

## 9. Regression scenario registry

Every Folia-specific bug must add a row here before release. The scenario ID should also be referenced from the fixing commit or pull request.

| Regression ID | Bug summary | Reproduction steps | Expected fixed behavior | Status |
| --- | --- | --- | --- | --- |
| FOLIA-REG-001 | Spectator click mutates online target inventory from viewer thread. | Run `FOLIA-ONLINE-002` while both players move items. | Commit happens on target entity scheduler or aborts/resyncs. | Covered by transaction smoke test. |
| FOLIA-REG-002 | Target logs out while spectator transaction is pending. | Run `FOLIA-ONLINE-005`. | Retired callback switches to offline flow without player-entity access. | Covered by retired-flow smoke test. |
| FOLIA-REG-003 | Async tab completion reads Bukkit online players directly. | Run `FOLIA-CMD-001` and `FOLIA-CMD-002` under async tab events. | Completion uses cached snapshots and permission prechecks. | Covered by command smoke test. |

## 10. Release sign-off

A Folia release candidate is ready only when all of these are checked:

- [ ] Automated checks in section 2 passed or every warning has an environment-specific explanation.
- [ ] Startup/shutdown checklist passed on a clean Folia 1.21.11 server.
- [ ] Command/tab-completion checklist passed for operator, console and no-permission players.
- [ ] Online target scenarios passed for main inventory and ender chest.
- [ ] Offline target scenarios passed for main inventory and ender chest.
- [ ] Give, Clear and Clone addon smoke tests passed.
- [ ] Unsafe integrations are disabled with clear warnings or covered by an adapter smoke test.
- [ ] No known Folia thread-check warning remains untracked.
- [ ] Any new Folia-specific bug has a regression row in section 9.

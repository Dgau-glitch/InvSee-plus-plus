# InvSee++

A bukkit plugin for manipulating player inventories.

![Logo](https://github.com/Jannyboy11/InvSee-plus-plus/blob/master/img/invsee6.png?raw=true)

This plugin will still work when target players are offline, even when they have never been on the server.

Do you like this plugin? Then please leave a rating and a review on [SpigotMC](https://www.spigotmc.org/resources/invsee.82342/)!

### Running the plugin

Just drop the InvSee++.jar file in your server's /plugins folder.

See also: [What Server Software does InvSee++ support?](#supported-server-software)

### Commands
- `/invsee <userName>|<uniqueId>`
- `/endersee <userName>|<uniqueId>`
- `/invseeplusplusreload`

### Permissions

###### Base permissions:
- `invseeplusplus.invsee.view` allows access to `/invsee`. By default only for server operators.
- `invseeplusplus.invsee.edit` allows the player to manipulate the target player's inventory. By default only for server operators.
- `invseeplusplus.endersee.view` allows access to `/endersee`. By default only for server operators.
- `invseeplusplus.endersee.edit` allows the player to manipulate the target player's enderchest. By default only for server operators.
- `invseeplusplus.exempt.invsee` makes it impossible to spectate the inventory of the owner of this permission.
- `invseeplusplus.exempt.endersee` makes it impossible to spectate the enderchest of the owner of this permission.
- `invseeplusplus.bypass-exempt.invsee` ignore whether target players are exempted from having their inventory spectated.
- `invseeplusplus.bypass-exempt.endersee` ignore whether target players are exempted from having their enderchest spectated.
- `invseeplusplus.tabcomplete` allows username tabcompletion in /invsee or /endersee commands. This permission is automatically provided by `invseeplusplus.invsee.view` and `invseeplusplus.endersee.view`.
- `invseeplusplus.reload` reloads the configuration.

###### Aggregate permissions:
- `invseeplusplus.view` provides `invseeplusplus.invsee.view` and `invseeplusplus.endersee.view`.
- `invseeplusplus.edit` provides `invseeplusplus.invsee.edit` and `invseeplusplus.endersee.edit`.
- `invseeplusplus.exempt` provides `invseeplusplus.exempt.invsee` and `invseeplusplus.exempt.endersee`.
- `invseeplusplus.bypass-exempt` provides `invseeplusplus.bypass-exempt.invsee` and `invseeplusplus.bypass-exempt.endersee`.
- `invseeplusplus.*` provides all ten of the base permissions as well as all of the addon permissions.

## Addons

#### InvSee++_Give

This Folia-only Maven reactor does not build bundled addons by default. Re-add addon modules separately if you need them.

##### Commands:
- `/invgive <target player> <item type> [<amount>] [<nbt tag>]`
- `/endergive <target player> <item type> [<amount>] [<nbt tag>]`
###### Examples:
On 1.20.4 and earlier:
- `/invgive Notch diamond 1 {"foo":"bar"}`
- `/endergive Jannyboy11 wool:14`

On 1.20.5 and later:
- `/invgive Jannyboy11 minecraft:emerald[minecraft:max_stack_size=99] 65`
##### Permissions:
- `invseeplusplus.give.*` provides `invseeplusplus.give.inventory` and `invseeplusplus.give.enderchest`.
- `invseeplusplus.give.inventory` allows access to `/invgive`.
- `invseeplusplus.give.enderchest` allows access to `/endergive`.


#### InvSee++_Clear
##### Commands:
- `/invclear <player> <item type>? <amount>?`
- `/enderclear <player> <item type>? <amount>?`
###### Examples:
- `/invclear Notch diamond 1`
- `/enderclear Jannyboy11 wool:14`
##### Permissions:
- `invseeplusplus.clear.*` provides `invseeplusplus.clear.inventory` and `invseeplusplus.clear.enderchest`.
- `invseeplusplus.clear.inventory` allows access to `/invclear`.
- `invseeplusplus.clear.enderchest` allows access to `/enderclear`.


#### InvSee++_Clone
##### Commands:
- `/invclone <source player> <target player>?`
- `/enderclone <source player> <target player>?`
###### Examples:
- `/invclone Notch` (copies Notch's inventory to yourself)
- `/enderclone Jannyboy11 Notch` (copies Jannyboy11's enderchest to Notch's enderchest)
##### Permissions:
- `invseeplusplus.clone.*` provides `invseeplusplus.clone.inventory` and `invseeplusplus.clone.enderchest`.
- `invseeplusplus.clone.inventory` allows access to `/invclone`.
- `invseeplusplus.clone.enderchest` allows access to `/enderclone`.

### Statistics

InvSee++ by default sends statistics to [bStats](https://bstats.org/plugin/bukkit/InvseePlusPlus/9309) and [FastStats](https://faststats.dev/project/invsee-plus-plus/invsee%2B%2B).
The statistics are meant to be anonymous, and they help me with future development.
If you wish to opt-out of metrics collection, you can edit their config files in /plugins/bStats and /plugin/fastStats.

[![Servers & Players](https://faststats.dev/embed/2cfe5112-d8dc-4b71-8759-c0b0601d0076?w=800&h=300)](https://faststats.dev/project/invsee-plus-plus/invsee++)

### Contact

Bugs & Feature requests: [GitHub issues](https://github.com/Jannyboy11/InvSee-plus-plus/issues)
Anything else can be discussed via the [discussion thread on SpigotMC](https://www.spigotmc.org/threads/invsee.456148/) or via
[Discord](https://discord.gg/Z8WCDHHcdJ).

### Compiling

###### Prerequisites: [JDK-21](https://jdk.java.net/) or newer and [Maven](https://maven.apache.org).

This branch is a Folia 1.21.11-only Maven build. It does not build legacy CraftBukkit/Paper, 26.x, Glowstone, PerWorldInventory, Multiverse-Inventories or bundled addon modules.

1. From the root directory of this project, initialize the Paper 1.21.11 NMS dependency used by the Folia implementation. This generates `ca.bkaw:paper-nms:1.21.11-SNAPSHOT` in your local Maven repository; it is not downloaded from PaperMC.
   - `mvn -U ca.bkaw:paper-nms-maven-plugin:1.4.10:init --pl :impl_paper_1_21_11`
2. Build the Folia 1.21.11 plugin jar from the root reactor:
   - `mvn -pl InvSee++_Plugin -am -DskipTests package`

If Maven says `Could not find artifact ca.bkaw:paper-nms:jar:1.21.11-SNAPSHOT in papermc` or `at specified path .../.m2/repository/ca/bkaw/paper-nms/...`, the init command in step 1 has not completed successfully for the same local Maven repository that step 2 uses.

You can find the plugin jar at InvSee++_Plugin/target/InvSee++.jar.

### Developers API
Documentation available on the [wiki](https://github.com/Jannyboy11/InvSee-plus-plus/wiki)!

### License
LGPLv2.1. See the LICENSE.txt file.

### Credits
Special thanks to Icodak ([Discord](https://discordapp.com/users/345308025331908619)) ([SpigotMC](https://www.spigotmc.org/members/icodak.473813/)) for creating the logo!

### Supported server software

InvSee++ supports servers implementing the [Bukkit](https://dev.bukkit.org) api which is currently maintained by [SpigotMC](https://spigotmc.org).
There are two types of support, Tier 1 support and Tier 2 support.
- Tier 1 support: I regularly test new versions of InvSee++ on this server software to make sure that it runs smooth.
- Tier 2 support: I don't test InvSee++ regularly on this server software, but will make an effort to fix bugs encountered when running on this server software when [an issue](https://github.com/Jannyboy11/InvSee-plus-plus/issues) is reported.

In general I support the latest patch release of popularly used Minecraft version, as well as multiple recent versions of the latest major release.

#### Folia 1.21.11 artifact strategy

This branch now publishes a Folia 1.21.11-only `InvSee++.jar`. Install the jar from `InvSee++_Plugin/target/InvSee++.jar` on Folia 1.21.11; legacy CraftBukkit/Paper, 26.x, Glowstone and third-party inventory integrations are not part of this Maven reactor.

The `folia-supported: true` flag in `plugin.yml` is only a compatibility declaration. It is not sufficient by itself: every Bukkit/NMS touchpoint must still use the scheduler, transaction and command services documented in `FOLIA_MIGRATION_PLAN.md`, and the release checklist in `FOLIA_QA_PLAN.md` must pass before publishing a Folia build.

Known Folia integration limits:
- PerWorldInventory and Multiverse-Inventories are not included in this Folia-only Maven reactor.
- LuckPerms lookups use its asynchronous API where available; Vault and legacy permission providers are treated as sync/entity-owned integrations unless their own documentation proves async safety.

Folia API coordinates for developers:

```xml
<dependency>
  <groupId>dev.folia</groupId>
  <artifactId>folia-api</artifactId>
  <version>1.21.11-R0.1-SNAPSHOT</version>
  <scope>provided</scope>
</dependency>
```

```kotlin
compileOnly("dev.folia:folia-api:1.21.11-R0.1-SNAPSHOT")
```

Server support matrix for this Folia-only Maven reactor:
| Server Software | 1.21.11 |
|-----------------|---------|
| Folia           | Tier 2  |

Other server software and Minecraft versions were removed from this Maven reactor. Re-add the relevant modules explicitly if you need legacy CraftBukkit/Paper, 26.x, Glowstone or hybrid-server builds.

[![Historic Minecraft Version Usage](https://faststats.dev/embed/010faaef-face-4f9d-8288-61621c708031?w=800&h=300)](https://faststats.dev/project/invsee-plus-plus/invsee++)

### Supported Java versions
| Minecraft version: | 1.21.11      |
|--------------------|--------------|
| Java version:      | 21 or newer |

[![Java Versions](https://faststats.dev/embed/dc2e7402-115d-457d-a230-c025ba101968?w=600&h=300)](https://faststats.dev/project/invsee-plus-plus/invsee++)

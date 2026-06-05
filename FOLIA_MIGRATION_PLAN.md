# План полного перехода InvSee++ на Folia 1.21.11

Цель: перевести основной плагин, API и встроенные аддоны на модель Folia без небезопасного доступа к Bukkit/NMS из чужого региона, с компиляционной зависимостью `dev.folia:folia-api:1.21.11-R0.1-SNAPSHOT` и без включения Folia API в итоговый jar.

> Для Gradle-веток использовать `compileOnly("dev.folia:folia-api:1.21.11-R0.1-SNAPSHOT")`. В текущем Maven-проекте эквивалентом является `scope=provided`.
> Перед каждой реализацией перепроверять актуальные API: Folia Javadocs 1.21.11, Paper docs, Bukkit Javadocs.

## Текущее состояние и риски

- В основном plugin.yml сейчас указано `folia-supported: false`, поэтому главный плагин не объявлен совместимым с Folia.
- Модуль `InvSee++_Plugin` уже содержит `FoliaScheduler`, но зависимость Folia API устарела (`1.19.4-R0.1-SNAPSHOT`), а абстракция `Scheduler` покрывает только global/entity/async сценарии и не моделирует регионные, cancellable и player-pair операции.
- Платформа определяется как `CRAFTBUKKIT`, `PAPER` или `GLOWSTONE`; отдельного `FOLIA` типа нет, поэтому Folia сейчас фактически маскируется под Paper/CraftBukkit по версии сервера.
- В NMS-контейнерах 1.21.11 уже отмечены TODO о data race между tick-потоком зрителя и tick-потоком цели при кликах по инвентарю.
- В командах, tab-completion, listeners, integrations и save/load логике есть Bukkit-вызовы, которые нужно классифицировать по владельцу данных: global, async, entity scheduler или region scheduler.
- Аддоны имеют разный статус: `InvSee++_Clear_Plugin` уже помечен Folia-compatible, а `InvSee++_Clone_Plugin` — нет; их команды используют API scheduler и должны мигрировать вместе с ядром.

## Правило декомпозиции

Каждый пункт ниже — отдельная задача, которую можно выдать одним сообщением. Не переходить к следующему пункту, пока предыдущий не собран и не проверен. После каждой задачи запускать минимально релевантную сборку/тесты и фиксировать регрессии в плане.

## Статус выполненных задач

### Задача 1 — build-конфигурация Folia 1.21.11

- Выполнено: `InvSee++_Plugin/pom.xml` использует `dev.folia:folia-api:1.21.11-R0.1-SNAPSHOT` со scope `provided`; это Maven-эквивалент Gradle-зависимости `compileOnly("dev.folia:folia-api:1.21.11-R0.1-SNAPSHOT")`.
- Проверено по конфигурации: Folia API не должен попадать в shaded jar, потому что зависимость объявлена как `provided`, а не `compile`.
- Решение по модулям: `InvSee++_Common` пока остается на `paper-api`/Bukkit API и не получает прямую зависимость на `folia-api`, потому что публичный common API не принимает Folia-специфичные типы; это снижает связанность и не протекает runtime scheduler-детали в API.
- Paper/Bukkit/NMS platform modules остаются на своих текущих API до задач 3–14, где будет спроектирован общий scheduler/service layer и отдельный Folia-safe runtime path.

### Задача 2 — явное определение Folia как платформы выполнения

- Выполнено: в модель платформ добавлен `FOLIA`, а определение Folia вынесено в общий detector server software.
- Выполнено: локальная проверка `RegionizedServer` удалена из выбора scheduler; `InvseePlusPlus` теперь выбирает `FoliaScheduler` через `ServerSoftware.detect(...)`.
- Выполнено: Folia 1.21.11 зарегистрирована на базе paper-реализации 1.21.11 только как implementation provider, без утверждения о полной thread-safety до выполнения следующих задач плана.
- Ожидаемое поведение: на Folia 1.21.11 лог detection должен показывать `Folia version 1.21.11`, на Paper detection остается `Paper version ...`, а unsupported-version сообщения теперь имеют отдельную платформу `Folia`.

## Задачи миграции

### 1. Обновить build-конфигурацию под Folia 1.21.11

**Сделать:**
- Обновить Maven-зависимость `dev.folia:folia-api` в `InvSee++_Plugin/pom.xml` до `1.21.11-R0.1-SNAPSHOT` со scope `provided`.
- Для потенциальной Gradle-ветки зафиксировать эквивалент `compileOnly("dev.folia:folia-api:1.21.11-R0.1-SNAPSHOT")` в документации/комментарии миграции.
- Проверить, нужен ли перенос `paper-api`/`folia-api` в `InvSee++_Common`, если публичный API начнет принимать Folia-типы напрямую; предпочтительно не протекать Folia-классами в публичные интерфейсы без необходимости.

**Критерии приемки:**
- Сборка `InvSee++_Plugin` компилируется с Folia API 1.21.11.
- Folia API не попадает в shaded jar.
- В плане отмечено, какие модули остаются на Paper/Bukkit API и почему.

### 2. Ввести явное определение Folia как платформы выполнения

**Сделать:**
- Добавить `FOLIA` в модель платформы/серверного ПО.
- Перенести `Class.forName("io.papermc.paper.threadedregions.RegionizedServer")` из локальной проверки scheduler в общий detector.
- Сделать `Setup.setup(...)` логирующим именно Folia, а не только Paper/CraftBukkit.
- Зарегистрировать Folia 1.21.11 на базе paper-реализации 1.21.11, но не смешивать этот факт с безопасностью потоков.

**Критерии приемки:**
- На Folia сервер определяется как `Folia version 1.21.11`.
- На Paper поведение определения версии не меняется.
- Ошибки unsupported-version показывают Folia отдельно от Paper/CraftBukkit.

### 3. Спроектировать новую DRY-абстракцию планировщика

**Сделать:**
- Расширить или заменить `Scheduler` на модульную абстракцию с операциями: `global`, `async`, `entity(Player/UUID)`, `region(Location/World+chunk/block)`, delayed/repeating, cancellable task handles и retired-callback.
- Сохранить backward compatibility для публичного `InvseeAPI#getScheduler()` через adapter/deprecation слой.
- Убрать дублирование между `DefaultScheduler` и `FoliaScheduler`: общие контракты, проверки thread ownership и error handling вынести в отдельные компоненты.

**Критерии приемки:**
- Все существующие вызовы scheduler компилируются через adapter.
- Новые API не требуют от callers знать, Paper это или Folia.
- Есть тест/проверка компиляции для common + plugin.

### 4. Реализовать FoliaScheduler 1.21.11 поверх Global/Async/Region/Entity scheduler

**Сделать:**
- Переписать `FoliaScheduler` под Folia 1.21.11 Javadocs.
- Использовать `EntityScheduler` для операций над игроком/сущностью, `RegionScheduler` для location/world data, `GlobalRegionScheduler` только для global state, `AsyncScheduler` только для неблокирующих/Bukkit-free операций.
- Нормализовать retired-callback: если player entity retired/offline, задача должна уходить в безопасный offline-flow, а не silently fallback на global.

**Критерии приемки:**
- Нет fallback с player operation на global scheduler без явного решения вызывающего кода.
- Delayed/repeating tasks возвращают handles и отменяются на disable.
- Код не использует deprecated/неофициальные методы.

### 5. Провести аудит всех Bukkit/NMS вызовов по thread ownership

**Сделать:**
- Составить таблицу всех мест, где вызываются `getOnlinePlayers`, `getPlayer`, permissions, `openInventory`, `closeInventory`, inventory mutation, world/player data save/load, event registration и NMS container access.
- Для каждого места назначить owner: global, viewer entity, target entity, region или async-only.
- Отдельно отметить API сторонних плагинов (`Vault`, `LuckPerms`, `PerWorldInventory`, `Multiverse-Inventories`, permission plugins) и допустимые потоки для каждого.

**Критерии приемки:**
- Таблица добавлена в план или отдельный audit markdown.
- Все места с неопределенным owner превращены в отдельные последующие задачи.
- Нет реализации “наугад” без ссылки на официальный API/документацию.

### 6. Исправить открытие/закрытие инвентарей через entity scheduler зрителя

**Сделать:**
- Все вызовы `viewer.openInventory(...)`, `viewer.closeInventory()`, `player.openInventory(...)`, `player.closeInventory()` выполнять на scheduler владельца viewer/player entity.
- Для цепочек `CompletableFuture` гарантировать, что final UI action исполняется на entity scheduler зрителя, а не на global scheduler.
- Ответы команд (`sendMessage`) также выполнять на scheduler отправителя, если отправитель — player.

**Критерии приемки:**
- Команды `/invsee` и `/endersee` открывают GUI на Folia без thread-check ошибок.
- Console sender продолжает работать через безопасный global/command path.
- Поведение Paper не ломается.

### 7. Устранить data race в NMS-контейнерах spectator inventory

**Сделать:**
- Заменить прямую мутацию live inventory цели из tick-потока зрителя на безопасную модель: snapshot/diff/commit, command queue или transaction service.
- Коммит изменений в инвентарь цели выполнять на `EntityScheduler` цели.
- Для конфликтов реализовать abort/rollback или deterministic merge; логирование difference делать после успешного commit.
- Вынести общую логику для main/ender containers в reusable service, чтобы не дублировать алгоритм по версиям.

**Критерии приемки:**
- TODO о Folia data race в 1.21.11 контейнерах закрыты реализацией.
- При одновременных кликах зрителя и действиях цели нет concurrent modification/thread violation.
- Есть ручной сценарий проверки: online target, viewer edits, target moves/teleports/logs out.

### 8. Разделить live-player и offline-player flows

**Сделать:**
- Для online target использовать только entity-owned операции.
- Для offline target: file IO/NBT parse/save выполнять async, а любые Bukkit API touchpoints выносить на корректный scheduler.
- Retired-callback из entity scheduler должен переводить задачу в offline-flow: load data, apply pending diff, save data.

**Критерии приемки:**
- Нет операций с player entity после retirement/offline.
- Offline inventory creation/save не блокирует region tick thread.
- Существующие настройки offline/unknown player support сохраняются.

### 9. Мигрировать cache и pending futures на Folia-safe модель

**Сделать:**
- Проверить `OpenSpectatorsCache`, pending maps и UUID/name cache на thread-safety и ownership.
- Определить, какие структуры являются global plugin state, а какие привязаны к player entity.
- Для shared state использовать thread-safe коллекции или serial executor/service, а не случайные global scheduler вызовы.

**Критерии приемки:**
- Нет гонок при одновременном открытии одного offline inventory несколькими viewers.
- Pending futures удаляются корректно при success/failure/disable.
- `api.shutDown()` завершает/отменяет задачи без зависаний.

### 10. Перенести команды и tab-completion на Folia-safe command module

**Сделать:**
- Разделить commands/tab-completion в отдельный модуль/пакет с shared permission checks и контекстными completions.
- Проверку permission выполнять до формирования списка completions.
- Не показывать команды и аргументы игрокам без permission.
- Offline player completions строить из async-safe cache, а online players читать через безопасный scheduler/снимок.

**Критерии приемки:**
- `/invsee`, `/endersee`, `/invseeplusplusreload` имеют быстрые и контекстные completions.
- Игрок без permission не получает подсказки ни через sync tab, ни через async tab event.
- Нет Bukkit API access из async tab thread, кроме официально разрешенного event context.

### 11. Мигрировать listeners и permission checks

**Сделать:**
- Проверить `InventoryClickEvent`, `PlayerJoinEvent`, permission subscriptions и edit listener на Folia thread ownership.
- Все операции с конкретным игроком выполнять в его entity context; shared permission snapshots обновлять безопасно.
- Для сторонних permission APIs оставить async только там, где это официально допустимо; иначе делать sync/entity wrapper.

**Критерии приемки:**
- Edit permissions продолжают отменять клики без задержек.
- Join/tab cache обновляется без thread violations.
- LuckPerms/Vault/legacy permission plugins не вызываются из опасного потока.

### 12. Привести встроенные аддоны к Folia-safe API

**Сделать:**
- Проверить `InvSee++_Give_Plugin`, `InvSee++_Clear_Plugin`, `InvSee++_Clone_Plugin` и общие give/clear/clone modules.
- Убрать прямые Bukkit scheduler assumptions; все операции проводить через обновленный scheduler/API service.
- В `plugin.yml` каждого аддона ставить `folia-supported: true` только после фактической проверки.

**Критерии приемки:**
- Clear/Give/Clone команды работают с online и offline target без thread errors.
- `InvSee++_Clone_Plugin` больше не имеет `folia-supported: false`, если миграция завершена.
- Аддоны не дублируют scheduler logic ядра.

### 13. Проверить integrations: PerWorldInventory, Multiverse-Inventories и permission plugins

**Сделать:**
- Для каждого integration определить потокобезопасность API.
- Если API не Folia-safe, добавить adapter с ограничениями или отключать integration на Folia с понятным warning.
- Для PWI/MVI arguments и completions исключить вызовы стороннего API из async tab thread, если это не разрешено.

**Критерии приемки:**
- На Folia unsafe integration не ломает ядро.
- Пользователь видит понятное сообщение, если integration недоступна.
- Безопасные integrations покрыты smoke-test сценариями.

### 14. Обновить platform modules под Folia 1.21.11 как основной target

**Сделать:**
- Решить, остается ли поддержка legacy CraftBukkit/Paper в этом репозитории или Folia становится отдельным артефактом/профилем.
- Если “полностью на Folia” означает отдельный Folia-only артефакт — сократить runtime selection до Folia/Paper 1.21.11 совместимых модулей.
- Если legacy остается — Folia code path должен быть отдельным и не ухудшать старые версии.

**Критерии приемки:**
- Сформулирована стратегия артефактов: single jar с legacy или Folia-only jar.
- 1.21.11 Folia implementation выбирается предсказуемо.
- Нет accidental classloading несовместимых NMS классов.

### 15. Обновить `plugin.yml`, документацию и compatibility matrix

**Сделать:**
- После завершения технических задач поменять `folia-supported: true` для основного плагина и мигрированных аддонов.
- Обновить README/wiki: минимальная версия Folia, поддерживаемые Minecraft версии, известные ограничения integrations.
- Документировать dependency coordinates для Maven и Gradle.

**Критерии приемки:**
- `folia-supported: true` выставлен только после успешных smoke-tests.
- Документация явно говорит, что simple flag недостаточен без scheduler/thread-safety миграции.
- Пользователь понимает, какой jar ставить на Folia 1.21.11.

### 16. Добавить тестовый и ручной Folia QA-план

**Сделать:**
- Добавить checklist запуска на Folia 1.21.11: enable, commands, tab completion, online target, offline target, edit, save, reload, disable.
- Добавить сценарии с разными регионами: viewer и target далеко друг от друга, target teleport, target logout во время просмотра.
- Автоматизировать то, что можно проверить компиляцией/unit tests; остальное оформить как smoke-test protocol.

**Критерии приемки:**
- Есть воспроизводимый QA markdown.
- Каждый Folia-specific bug фиксируется отдельным регрессионным сценарием.
- Перед релизом выполняется полный checklist.

### 17. Финальная стабилизация и cleanup

**Сделать:**
- Удалить устаревшие TODO/adapter временного периода или превратить их в tracked issues.
- Проверить DRY: общие transaction/scheduler/command utilities не продублированы по main/ender/give/clear/clone.
- Провести финальную сборку всех релевантных modules и smoke-test на Folia.

**Критерии приемки:**
- Нет известных Folia thread violations.
- Нет несогласованных `folia-supported` flags.
- Код расширяемый: новая inventory feature подключается через service/API без переписывания ядра.

## Рекомендуемый порядок выполнения

1. Задачи 1–4: фундамент build/platform/scheduler.
2. Задачи 5–9: core inventory safety и state management.
3. Задачи 10–13: user-facing commands, addons, integrations.
4. Задачи 14–17: packaging, docs, QA и cleanup.

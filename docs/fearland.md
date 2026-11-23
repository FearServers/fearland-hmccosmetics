# FearLand fork of HMCCosmetics

Base: upstream `remapped` (2.9.3). Version `2.9.3-fearland.1`. Paper 26.x, HibiscusCommons 0.9.3, Nexo 1.30 items via `nexo:<id>`.

## Changes
- `database-settings.type: postgresql` (HikariCP, `INSERT ... ON CONFLICT DO UPDATE`). Table `cosmeticdatabase(uuid, cosmetics, updated_at)`; the `cosmetics` text keeps the upstream serialization (slot=id[&color]), so formats are unchanged.
- Writes run on one writer thread (ordered per player; retries stay on that thread). Reads retry and fail the load instead of returning an empty user, so a failed read never overwrites stored equipment.
- Saves after equip, remove and dye (one tick later, merged), and on quit.
- Optional `category-permission` per cosmetic; when `permission` is also set both are required. Both nodes are registered as Bukkit permissions.
- Password: `postgresql.password` or the `FEARLAND_API_DB_PASSWORD` env var.

## Tests
- `test/scenarios/visual.yml` (`fl test`, real Fabric client, no LuckPerms/Nexo on that server): denied without permission, equip, menu, reconnect, immediate leave after equip, restart, crash. Report and screenshots: `docs/evidence/`.
- `test/network/network-check.js` (mineflayer through the local Velocity, lobby + smp, LuckPerms, PostgreSQL): item-only / category-only / both permissions, row in PostgreSQL, lobby<->smp transfers right after an equip, PostgreSQL stopped while changing equipment.

Last run 2026-11-22 (final jar, sha256 `1a7519cb…1441`, `FEARLAND_SERVER_DIR` = checkout with HibiscusCommons declared): `fl test` PASSOU; `network-check.js` 13/13 (reads the bot's exact row by UUID, confirms the bot is on the destination server).

## Staging (2026-11-23)
Installed on `lobby` and `smp` by `fl deploy hibiscuscommons hmccosmetics` (deploy run FearServers/fearland#74; fork merge `f366299`, server merge `d646294`). Config rendered with the live `FEARLAND_API_DB_*` values and copied under `plugins/HMCCosmetics/`; Pl-Hide-Pro live config got only the `cosmetic`, `cosmetics`, `hmccosmetics` labels (the file differs in other live settings, so it was not replaced). Backups: `~fearland/backups/hmc-20261122/` on the host. Both servers log `Enabling HibiscusCommons v0.9.3`, `Enabling HMCCosmetics v2.9.3-fearland.1`, `Data storage is set to postgresql`, no error. The pack published by `login` has the five Nexo items and their models/textures.
`test/network/staging-check.js` (scripted player through the staging Velocity, LuckPerms set from the Panel console): 11/11 PASS, output in `docs/evidence/staging-check-2026-11-23.txt`. Not done on staging: server restart persistence (covered by the `fl test` restart step), visual render (see Limits). Rollback: delete `plugins/HMCCosmetics/`, the two jars, and restore `Pl-Hide-Pro/config.yml` from the backup.
`network-check.js` (local) also checks the dye color in the row (`dyed_cap&16711680`) across lobby -> smp -> lobby.

## Limits
- The Fabric client cannot join the local network (it accepts only 1.21.11/26.1/26.2), and `fl test` has neither Nexo nor LuckPerms: Nexo models were not seen rendered in a screenshot; only the vanilla item and the menu were.
- Dye colors: the saved value and its persistence are tested (`network-check.js`); how the tint looks in the client was not seen.
- Staging bot tests need nLogin `show-dialogs` off (the login dialog blocks a scripted client); another session also toggles that setting, so coordinate before repeating.
- Velocity is not needed: the shared PostgreSQL table was enough for equipment to follow the player between servers, including a transfer right after an equip.

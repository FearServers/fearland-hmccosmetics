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

## Limits
- The Fabric client cannot join the local network (it accepts only 1.21.11/26.1/26.2), and `fl test` has neither Nexo nor LuckPerms: Nexo models were not seen rendered in a screenshot; only the vanilla item and the menu were.
- Dye colors: stored by the unchanged upstream serializer, not exercised in game.
- Velocity is not needed: the shared PostgreSQL table was enough for equipment to follow the player between servers, including a transfer right after an equip.

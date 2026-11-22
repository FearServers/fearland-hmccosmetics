# Sistema: HMCCosmetics — PASSOU

Executado em 2026-11-22 23:54 UTC

## Pedido e o que foi feito

**Pedido:** HMCCosmetics final jar, HibiscusCommons declared

**O que foi feito:**

- Comando /hmccosmetics: Base Cosmetic Command
- Permissão hmccosmetics.cmd.apply
- Permissão hmccosmetics.cmd.apply.other
- Permissão hmccosmetics.cmd.clearhiddenreasons
- Permissão hmccosmetics.cmd.clearhiddenreasons.other
- Permissão hmccosmetics.cmd.dataclear
- Permissão hmccosmetics.cmd.debug
- Permissão hmccosmetics.cmd.default
- Permissão hmccosmetics.cmd.disableall
- Permissão hmccosmetics.cmd.dye
- Permissão hmccosmetics.cmd.emote
- Permissão hmccosmetics.cmd.emote.other
- Permissão hmccosmetics.cmd.hiddenreasons
- Permissão hmccosmetics.cmd.hiddenreasons.other
- Permissão hmccosmetics.cmd.hide
- Permissão hmccosmetics.cmd.hide.other
- Permissão hmccosmetics.cmd.menu
- Permissão hmccosmetics.cmd.menu.other
- Permissão hmccosmetics.cmd.playemote
- Permissão hmccosmetics.cmd.playemote.other
- Permissão hmccosmetics.cmd.reload
- Permissão hmccosmetics.cmd.setwardrobesetting
- Permissão hmccosmetics.cmd.show
- Permissão hmccosmetics.cmd.show.other
- Permissão hmccosmetics.cmd.toggle
- Permissão hmccosmetics.cmd.toggle.other
- Permissão hmccosmetics.cmd.unapply
- Permissão hmccosmetics.cmd.unapply.other
- Permissão hmccosmetics.cmd.wardrobe
- Permissão hmccosmetics.cmd.wardrobe.other
- Permissão hmccosmetics.emote.shiftrun
- Permissão hmccosmetics.unapplydeath.bypass

## Compila e liga

- Build: ok
- Servidor liga: ok
- Cliente (bot): ok
- Log: limpo

## Sem permissao

| # | Passo | Status | Esperado | Visto | Tempo | Print |
|---|---|---|---|---|---|---|
| 1 | console tp @bot 0.5 -60 0.5 | ok | console: tp @bot 0.5 -60 0.5 | System chat: Teleported @bot to 0.500000, -60.000000, 0.500000 | 56 ms | — |
| 2 | sleep 1500 | ok | esperar 1500 ms | ok | 1.5 s | — |
| 3 | command cosmetic apply top_hat | ok | enviar /cosmetic apply top_hat | enviado | 91 ms | — |
| 4 | expectChat do not have permission | ok | chat com "do not have permission" | HMCCosmetics » You do not have permission for this cosmetic! | 64 ms | — |
| 5 | command cosmetic unapply HELMET | ok | enviar /cosmetic unapply HELMET | enviado | 116 ms | — |
| 6 | expectChat no cosmetics in that slot | ok | chat com "no cosmetics in that slot" | HMCCosmetics » There are no cosmetics in that slot! | 57 ms | — |
| 7 | screenshot 00-sem-permissao | ok | print da tela | 07-00-sem-permissao.png | 546 ms | [ver](07-00-sem-permissao.png) |

## Equipar

| # | Passo | Status | Esperado | Visto | Tempo | Print |
|---|---|---|---|---|---|---|
| 8 | op | ok | @bot vira operador | System chat: Made @bot a server operator | 54 ms | — |
| 9 | command cosmetic apply top_hat | ok | enviar /cosmetic apply top_hat | enviado | 167 ms | — |
| 10 | expectChat equipped top_hat | ok | chat com "equipped top_hat" | HMCCosmetics » You have equipped top_hat! | 57 ms | — |
| 11 | command cosmetic apply dragon_hat | ok | enviar /cosmetic apply dragon_hat | enviado | 111 ms | — |
| 12 | expectChat equipped dragon_hat | ok | chat com "equipped dragon_hat" | HMCCosmetics » You have equipped dragon_hat! | 56 ms | — |
| 13 | key F5 | ok | tecla F5 | pressionada | 58 ms | — |
| 14 | sleep 1200 | ok | esperar 1200 ms | ok | 1.2 s | — |
| 15 | screenshot 01-dragao-3a-pessoa | ok | print da tela | 15-01-dragao-3a-pessoa.png | 528 ms | [ver](15-01-dragao-3a-pessoa.png) |
| 16 | key F5 | ok | tecla F5 | pressionada | 54 ms | — |
| 17 | key F5 | ok | tecla F5 | pressionada | 55 ms | — |

## Menu

| # | Passo | Status | Esperado | Visto | Tempo | Print |
|---|---|---|---|---|---|---|
| 18 | command cosmetic | ok | enviar /cosmetic | enviado | 105 ms | — |
| 19 | sleep 1200 | ok | esperar 1200 ms | ok | 1.2 s | — |
| 20 | screenshot 02-menu | ok | print da tela | 20-02-menu.png | 450 ms | [ver](20-02-menu.png) |
| 21 | key ESCAPE | ok | tecla ESCAPE | pressionada | 67 ms | — |

## Persistencia depois de sair e voltar

| # | Passo | Status | Esperado | Visto | Tempo | Print |
|---|---|---|---|---|---|---|
| 22 | desconectar | ok | o bot sai do servidor | saiu; o servidor não o vê mais | 199 ms | — |
| 23 | sleep 1500 | ok | esperar 1500 ms | ok | 1.5 s | — |
| 24 | reconectar | ok | o bot volta ao servidor | voltou em 12 s | 12.1 s | — |
| 25 | sleep 3000 | ok | esperar 3000 ms | ok | 3.0 s | — |
| 26 | command cosmetic unapply HELMET | ok | enviar /cosmetic unapply HELMET | enviado | 86 ms | — |
| 27 | expectChat unequipped dragon_hat | ok | chat com "unequipped dragon_hat" | HMCCosmetics » You have unequipped dragon_hat! | 54 ms | — |
| 28 | command cosmetic apply dragon_hat | ok | enviar /cosmetic apply dragon_hat | enviado | 110 ms | — |
| 29 | expectChat equipped dragon_hat | ok | chat com "equipped dragon_hat" | HMCCosmetics » You have equipped dragon_hat! | 56 ms | — |

## Saida imediata depois de equipar

| # | Passo | Status | Esperado | Visto | Tempo | Print |
|---|---|---|---|---|---|---|
| 30 | command cosmetic apply top_hat | ok | enviar /cosmetic apply top_hat | enviado | 100 ms | — |
| 31 | desconectar | ok | o bot sai do servidor | saiu; o servidor não o vê mais | 198 ms | — |
| 32 | reconectar | ok | o bot volta ao servidor | voltou em 11 s | 12.0 s | — |
| 33 | sleep 3000 | ok | esperar 3000 ms | ok | 3.0 s | — |
| 34 | command cosmetic unapply HELMET | ok | enviar /cosmetic unapply HELMET | enviado | 70 ms | — |
| 35 | expectChat unequipped top_hat | ok | chat com "unequipped top_hat" | HMCCosmetics » You have unequipped top_hat! | 57 ms | — |
| 36 | command cosmetic apply dragon_hat | ok | enviar /cosmetic apply dragon_hat | enviado | 109 ms | — |
| 37 | expectChat equipped dragon_hat | ok | chat com "equipped dragon_hat" | HMCCosmetics » You have equipped dragon_hat! | 55 ms | — |

## Restart

| # | Passo | Status | Esperado | Visto | Tempo | Print |
|---|---|---|---|---|---|---|
| 38 | restart | ok | servidor reinicia e o bot volta | voltou em 22 s | 22.7 s | — |
| 39 | sleep 3000 | ok | esperar 3000 ms | ok | 3.0 s | — |
| 40 | command cosmetic unapply HELMET | ok | enviar /cosmetic unapply HELMET | enviado | 91 ms | — |
| 41 | expectChat unequipped dragon_hat | ok | chat com "unequipped dragon_hat" | HMCCosmetics » You have unequipped dragon_hat! | 54 ms | — |
| 42 | command cosmetic apply dragon_hat | ok | enviar /cosmetic apply dragon_hat | enviado | 107 ms | — |
| 43 | expectChat equipped dragon_hat | ok | chat com "equipped dragon_hat" | HMCCosmetics » You have equipped dragon_hat! | 54 ms | — |

## Crash

| # | Passo | Status | Esperado | Visto | Tempo | Print |
|---|---|---|---|---|---|---|
| 44 | sleep 800 | ok | esperar 800 ms | ok | 800 ms | — |
| 45 | cair | ok | o servidor cai sem onDisable, volta e o bot reconecta | voltou em 21 s | 21.5 s | — |
| 46 | sleep 3000 | ok | esperar 3000 ms | ok | 3.0 s | — |
| 47 | command cosmetic unapply HELMET | ok | enviar /cosmetic unapply HELMET | enviado | 79 ms | — |
| 48 | expectChat unequipped dragon_hat | ok | chat com "unequipped dragon_hat" | HMCCosmetics » You have unequipped dragon_hat! | 54 ms | — |
| 49 | screenshot 03-removida | ok | print da tela | 49-03-removida.png | 485 ms | [ver](49-03-removida.png) |

## Estado no servidor

Nenhuma checagem de estado no roteiro.

## Lag

- MSPT médio: 3.4 ms
- MSPT p95: 10 ms
- MSPT máximo: 458 ms
- Veredito: ok
- Tempo do plugin na thread principal: 68 ms no perfil inteiro

| Função do plugin | Tempo total | Tempo próprio | Chama |
|---|---|---|---|
| com.hibiscusmc.hmccosmetics.command.CosmeticCommand.onCommand | 48 ms | 0 ms | — |
| com.hibiscusmc.hmccosmetics.gui.Menu.openMenu | 16 ms | 0 ms | java.lang.ClassLoader.loadClass |
| com.hibiscusmc.hmccosmetics.user.CosmeticUser.updateCosmetic | 16 ms | 0 ms | — |
| com.hibiscusmc.hmccosmetics.gui.Menu$$Lambda.0x0000000016cdc8b8.run | 12 ms | 0 ms | — |
| com.hibiscusmc.hmccosmetics.gui.Menu.lambda$openMenu$7 | 12 ms | 0 ms | — |

Notas:

- API cara no tick: world entity scan (org.bukkit.craftbukkit.CraftWorld.getNearbyEntities) dentro de com.hibiscusmc.hmccosmetics.util.search.BukkitPlayerSearchEngine.getPlayersInRange (4 ms no perfil).
- O p95 de 10 ms inclui a partida do servidor (ligado havia 18 s) e não conta no veredito; este trecho é julgado pela mediana das janelas e pelos picos com culpado.
- Pico de 458 ms sem culpado no perfil; no mesmo minuto o pacote com.hibiscusmc.hmccosmetics gastou 8 ms (o mais pesado: com.hibiscusmc.hmccosmetics.cosmetic.types.CosmeticArmorType.dispatchUpdate). O tempo por seção diz em que trecho ele caiu.
- O p95 de 9.2 ms inclui a partida do servidor (ligado havia 17 s) e não conta no veredito; este trecho é julgado pela mediana das janelas e pelos picos com culpado.
- Pico de 419.5 ms sem culpado no perfil; no mesmo minuto o pacote com.hibiscusmc.hmccosmetics gastou 4 ms (o mais pesado: com.hibiscusmc.hmccosmetics.util.MessagesUtil.processString). O tempo por seção diz em que trecho ele caiu.

## Escala

Medido com 1 jogador (o bot). Projeção para 100 jogadores (padrão: o cenário não declara a lotação (lotacao:)): 3.6 ms por tick no plugin, teto de 5 ms.

- Veredito: ok

| Função | Evento | Chamadas | Média | p99 | Máximo | Ritmo por jogador | Com a lotação |
|---|---|---|---|---|---|---|---|
| /hmccosmetics | comando | 13 | 3.1 ms | 16.4 ms | 13.7 ms | 0.2/s | 3.13 ms |
| CosmeticSaveListener.onRemove | PlayerCosmeticRemoveEvent | 6 | 58 µs | 512 µs | 296 µs | 1/s | 0.29 ms |
| PlayerGameListener.onPlayerDamaged | EntityDamageEvent | 1 | 14 µs | 16 µs | 14 µs | 3/s | 0.22 ms (1 chamada, fora da soma) |
| PlayerGameListener.onPlayerTeleport | PlayerTeleportEvent | 1 | 197 µs | 256 µs | 197 µs | 0.1/s | 0.1 ms (1 chamada, fora da soma) |
| CosmeticSaveListener.onEquip | PlayerCosmeticPostEquipEvent | 8 | 15 µs | 512 µs | 306 µs | 1/s | 0.08 ms |
| PlayerGameListener.onPlayerCosmeticEquip | PlayerCosmeticPostEquipEvent | 8 | 11 µs | 64 µs | 62 µs | 1/s | 0.05 ms |
| PlayerGameListener | EntityDamageEvent | 1 | 2 µs | 2 µs | 2 µs | 3/s | 0.03 ms (1 chamada, fora da soma) |
| GuiListener.onGuiClose | InventoryCloseEvent | 3 | 16 µs | 1 ms | 698 µs | 0.3/s | 0.02 ms |
| PlayerMovementListener.onPlayerQuit | PlayerQuitEvent | 2 | 10 µs | 64 µs | 52 µs | 0/s | 0 ms |
| PlayerConnectionListener.onPlayerQuit | PlayerQuitEvent | 2 | 63 µs | 1 ms | 713 µs | 0/s | 0 ms |
| UpdateCheckListener.notifyOnJoin | PlayerJoinEvent | 2 | 17 µs | 32 µs | 20 µs | 0/s | 0 ms |
| PlayerConnectionListener.notifyAdminUpdate/onPlayerJoin | PlayerJoinEvent | 4 | 36 µs | 64 µs | 60 µs | 0/s | 0 ms |

Tempo por seção:

| Seção | Ticks | MSPT médio | MSPT máximo | Ticks de 50 ms ou mais | Plugin |
|---|---|---|---|---|---|
| Sem permissao | 53 | 3.3 ms | 25.6 ms | 0 | 5.3 ms |
| Equipar | 47 | 2.5 ms | 6.2 ms | 0 | 14.8 ms |
| Menu | 37 | 2.5 ms | 17.2 ms | 0 | 13.7 ms |
| Persistencia depois de sair e voltar | 342 | 2.3 ms | 47.2 ms | 0 | 4.7 ms |
| Saida imediata depois de equipar | 312 | 2.1 ms | 30 ms | 0 | 6.1 ms |
| Restart | 69 | 2.4 ms | 5.4 ms | 0 | 8.2 ms |
| Crash | 91 | 2.5 ms | 4.7 ms | 0 | 6.2 ms |

Notas:

- Com 1 chamada só, fora da soma (a primeira chamada paga a JVM fria): PlayerGameListener.onPlayerDamaged, PlayerGameListener.onPlayerTeleport, PlayerGameListener.

## Revisão de padrão

Não houve revisão.

## Não testado

- dependências opcionais não carregadas no servidor de teste: BetterHud, CraftEngine, Denizen, Eco, GSit, Geary, HMCColor, ItemsAdder, LibsDisguises, MMOItems, ModelEngine, MythicMobs, Nexo, Oraxen, PlaceholderAPI, PremiumVanish, SuperVanish, WorldGuard

## Prints

![Passo 7](07-00-sem-permissao.png)

Passo 7: screenshot 00-sem-permissao

![Passo 15](15-01-dragao-3a-pessoa.png)

Passo 15: screenshot 01-dragao-3a-pessoa

![Passo 20](20-02-menu.png)

Passo 20: screenshot 02-menu

![Passo 49](49-03-removida.png)

Passo 49: screenshot 03-removida

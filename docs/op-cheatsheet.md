# OP User Cheatsheet

Quick reference for server operators on starbase.minecraft.party.

## Giving Yourself Items

### Basic Syntax
```
/give <player> <item> [amount]
/give @s <item> [amount]        # @s = yourself
```

### Examples
```
/give @s diamond 64
/give @s netherite_pickaxe
/give @s enchanted_golden_apple 16
```

### Give with Enchantments
```
/give @s diamond_sword[enchantments={levels:{sharpness:5,unbreaking:3,mending:1}}]
/give @s netherite_pickaxe[enchantments={levels:{efficiency:5,fortune:3,unbreaking:3,mending:1}}]
/give @s elytra[enchantments={levels:{unbreaking:3,mending:1}}]
```

### Give with Custom Names
```
/give @s diamond_sword[custom_name='"Excalibur"']
```

---

## Useful Items Quick Reference

### Building Blocks
| Item | ID |
|------|-----|
| Stone | `stone` |
| Cobblestone | `cobblestone` |
| Oak Planks | `oak_planks` |
| Glass | `glass` |
| Bricks | `bricks` |
| Quartz Block | `quartz_block` |
| Concrete (White) | `white_concrete` |
| Deepslate | `deepslate` |
| Copper Block | `copper_block` |

### Tools & Weapons
| Item | ID |
|------|-----|
| Netherite Sword | `netherite_sword` |
| Netherite Pickaxe | `netherite_pickaxe` |
| Netherite Axe | `netherite_axe` |
| Netherite Shovel | `netherite_shovel` |
| Netherite Hoe | `netherite_hoe` |
| Bow | `bow` |
| Crossbow | `crossbow` |
| Trident | `trident` |
| Mace | `mace` |
| Shield | `shield` |

### Armor
| Item | ID |
|------|-----|
| Netherite Helmet | `netherite_helmet` |
| Netherite Chestplate | `netherite_chestplate` |
| Netherite Leggings | `netherite_leggings` |
| Netherite Boots | `netherite_boots` |
| Elytra | `elytra` |
| Turtle Helmet | `turtle_helmet` |

### Valuable Resources
| Item | ID |
|------|-----|
| Diamond | `diamond` |
| Netherite Ingot | `netherite_ingot` |
| Emerald | `emerald` |
| Ancient Debris | `ancient_debris` |
| Nether Star | `nether_star` |
| Shulker Shell | `shulker_shell` |
| Totem of Undying | `totem_of_undying` |
| Heart of the Sea | `heart_of_the_sea` |

### Food
| Item | ID |
|------|-----|
| Golden Apple | `golden_apple` |
| Enchanted Golden Apple | `enchanted_golden_apple` |
| Cooked Steak | `cooked_beef` |
| Golden Carrot | `golden_carrot` |
| Suspicious Stew | `suspicious_stew` |

### Transportation
| Item | ID |
|------|-----|
| Elytra | `elytra` |
| Firework Rocket | `firework_rocket` |
| Minecart | `minecart` |
| Saddle | `saddle` |
| Ender Pearl | `ender_pearl` |

### Redstone & Tech
| Item | ID |
|------|-----|
| Redstone | `redstone` |
| Redstone Block | `redstone_block` |
| Repeater | `repeater` |
| Comparator | `comparator` |
| Observer | `observer` |
| Piston | `piston` |
| Sticky Piston | `sticky_piston` |
| Hopper | `hopper` |
| Dropper | `dropper` |
| Dispenser | `dispenser` |

### Utility Blocks
| Item | ID |
|------|-----|
| Crafting Table | `crafting_table` |
| Enchanting Table | `enchanting_table` |
| Anvil | `anvil` |
| Brewing Stand | `brewing_stand` |
| Beacon | `beacon` |
| Ender Chest | `ender_chest` |
| Shulker Box | `shulker_box` |
| Barrel | `barrel` |
| Smithing Table | `smithing_table` |
| Crafter | `crafter` |

### Potions
| Item | ID |
|------|-----|
| Healing Potion | `potion[potion_contents={potion:"strong_healing"}]` |
| Strength Potion | `potion[potion_contents={potion:"long_strength"}]` |
| Speed Potion | `potion[potion_contents={potion:"long_swiftness"}]` |
| Night Vision | `potion[potion_contents={potion:"long_night_vision"}]` |
| Fire Resistance | `potion[potion_contents={potion:"long_fire_resistance"}]` |
| Invisibility | `potion[potion_contents={potion:"long_invisibility"}]` |

### Spawn Eggs (Creative/Testing)
| Item | ID |
|------|-----|
| Villager | `villager_spawn_egg` |
| Iron Golem | `iron_golem_spawn_egg` |
| Horse | `horse_spawn_egg` |
| Wolf | `wolf_spawn_egg` |
| Cat | `cat_spawn_egg` |
| Allay | `allay_spawn_egg` |
| Warden | `warden_spawn_egg` |
| Breeze | `breeze_spawn_egg` |

---

## OP Commands Cheatsheet

### Player Management
```
/op <player>                    # Grant operator status
/deop <player>                  # Remove operator status
/kick <player> [reason]         # Kick player from server
/ban <player> [reason]          # Ban player
/pardon <player>                # Unban player
/whitelist add <player>         # Add to whitelist
/whitelist remove <player>      # Remove from whitelist
```

### Gamemode
```
/gamemode creative              # Switch to creative
/gamemode survival              # Switch to survival
/gamemode spectator             # Switch to spectator
/gamemode adventure             # Switch to adventure
/gamemode creative <player>     # Change another player's mode
```

### Teleportation
```
/tp <player> <x> <y> <z>        # Teleport to coordinates
/tp <player1> <player2>         # Teleport player1 to player2
/tp @s ~ ~100 ~                 # Teleport 100 blocks up
/tp @s 0 64 0                   # Teleport to world origin
```

### Time & Weather
```
/time set day                   # Set to daytime (1000)
/time set night                 # Set to night (13000)
/time set noon                  # Set to noon (6000)
/time set midnight              # Set to midnight (18000)
/weather clear                  # Clear weather
/weather rain                   # Make it rain
/weather thunder                # Thunderstorm
/weather clear 1000000          # Clear for a long time
```

### World Management
```
/gamerule keepInventory true    # Keep items on death
/gamerule doDaylightCycle false # Stop day/night cycle
/gamerule doWeatherCycle false  # Stop weather changes
/gamerule mobGriefing false     # Prevent mob damage to blocks
/gamerule doFireTick false      # Prevent fire spread
/gamerule fallDamage false      # Disable fall damage
/gamerule doMobSpawning false   # Disable mob spawning
/difficulty peaceful            # Set difficulty
/difficulty hard
```

### Effects
```
/effect give @s speed 600 2           # Speed III for 10 min
/effect give @s night_vision 99999    # Permanent night vision
/effect give @s strength 600 1        # Strength II for 10 min
/effect give @s resistance 600 3      # Resistance IV for 10 min
/effect give @s jump_boost 600 1      # Jump boost II for 10 min
/effect give @s haste 600 2           # Haste III for 10 min
/effect give @s regeneration 600 1    # Regen II for 10 min
/effect give @s saturation 600        # Never hungry
/effect clear @s                      # Remove all effects
```

### World Editing
```
/fill <x1> <y1> <z1> <x2> <y2> <z2> <block>           # Fill area with block
/fill <x1> <y1> <z1> <x2> <y2> <z2> air               # Clear area
/fill ~-5 ~-1 ~-5 ~5 ~-1 ~5 stone                     # Platform under you
/setblock <x> <y> <z> <block>                         # Place single block
/clone <x1> <y1> <z1> <x2> <y2> <z2> <x> <y> <z>     # Clone structure
```

### Locating Structures
```
/locate structure village             # Find nearest village
/locate structure fortress            # Find nether fortress
/locate structure bastion_remnant     # Find bastion
/locate structure ancient_city        # Find ancient city
/locate structure trial_chambers      # Find trial chambers
/locate structure end_city            # Find end city
/locate biome cherry_grove            # Find biome
```

### Kill & Summon
```
/kill @e[type=!player]          # Kill all mobs (not players)
/kill @e[type=zombie]           # Kill all zombies
/summon lightning_bolt          # Strike lightning at your position
/summon villager ~ ~ ~          # Spawn villager
/summon iron_golem ~ ~ ~        # Spawn iron golem
```

---

## Server Console Commands

Access the console with `make console` (Ctrl+B, D to detach).

### Common Console Commands
```
op <player>                     # Grant op from console
whitelist add <player>          # Add to whitelist
say <message>                   # Broadcast message to all players
stop                            # Gracefully stop the server
save-all                        # Force save the world
save-off                        # Disable auto-save (for backups)
save-on                         # Re-enable auto-save
list                            # Show online players
kick <player>                   # Kick a player
ban <player>                    # Ban a player
```

### Useful One-Liners
```
# Give full netherite gear to yourself
/give @s netherite_helmet[enchantments={levels:{protection:4,unbreaking:3,mending:1}}]
/give @s netherite_chestplate[enchantments={levels:{protection:4,unbreaking:3,mending:1}}]
/give @s netherite_leggings[enchantments={levels:{protection:4,unbreaking:3,mending:1}}]
/give @s netherite_boots[enchantments={levels:{protection:4,unbreaking:3,mending:1,feather_falling:4}}]

# God mode setup
/effect give @s resistance 99999 4
/effect give @s regeneration 99999 4
/effect give @s saturation 99999

# Building kit
/give @s scaffolding 64
/give @s torch 64
/give @s water_bucket
/give @s lava_bucket

# Flight without elytra
/effect give @s levitation 99999 0
# (use levitation 255 to fall slowly, 0 to rise slowly)
```

---

## Target Selectors

| Selector | Description |
|----------|-------------|
| `@s` | Yourself |
| `@p` | Nearest player |
| `@a` | All players |
| `@e` | All entities |
| `@r` | Random player |
| `@e[type=zombie]` | All zombies |
| `@e[distance=..10]` | Entities within 10 blocks |
| `@a[gamemode=survival]` | All survival players |

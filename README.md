# Level Up

A Fabric mod for Minecraft 26.3 with leveling, skill abilities, and a separate skill-tree screen.

## Progression and controls

- Defeating an entity grants **10 XP**.
- Level 1 takes 20 XP; each subsequent level costs 10 more XP than the previous one.
- Click **LEVEL UP** in the top-left HUD for the separate level-up screen.
- Click **SKILL TREE** or press **F8** to open the skill tree (not the inventory screen).

## Skills and flexible loadouts

Unlocks and loadout slots are saved on the world scoreboard. Unlock each skill once at its required level; then re-equip it to any of four slots, clear slots, or reset the loadout at no cost and as often as you like. Resetting the loadout keeps unlocks.

| Ability | Required level | Effect |
| --- | ---: | --- |
| Invisibility | 2 | Invisibility for 30 seconds |
| Night Vision | 3 | Night vision for 5 minutes |
| Gojo's Mukagen (Infinity) | 4 | For 30 seconds, pushes nearby living entities, including players and mobs, away from the user. |
| Sukuna's Kai (Cleave / Dismantle) | 5 | A forward-facing slash up to 8 blocks away that damages and knocks back targets in a narrow cone. |

Choose one of four loadout slots at the top of the skill-tree screen, then use **Unlock** or **Equip →** on a skill. **Use Selected** casts the equipped ability in the selected slot. **Clear Slot** clears one slot; **Reset Loadout** clears all four. An ability can be equipped in multiple slots.

For command use, `/skill unlock <ability>`, `/skill equip <ability> <slot>`, `/skill cast <slot>`, `/skill unequip <slot>`, `/skill reset`, and `/skill stop_infinity` are available to players.

## Build

Use Java 25 and the included Gradle wrapper:

```sh
./gradlew build
```

The mod jar is created in `build/libs/`.

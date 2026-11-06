# Level Up

A Fabric mod for Minecraft 26.3 with a server-side leveling system, clickable HUD shortcuts, and a separate skill tree.

## Progression

- Defeating an entity grants **10 XP**.
- Level 1 takes 20 XP; each subsequent level costs 10 more XP than the previous one.
- XP carries over when you level up, and every level-up restores your health.
- The top-left **LEVEL UP** button opens a separate player progression screen.
- The top-left **SKILL TREE** button or **F8** opens the ability tree. It is separate from the inventory and the level-up screen.

## Skills

Unlocks are saved to the world scoreboard and are available in multiplayer. Open the skill tree to unlock an ability once you reach its required level; use its button to activate it.

| Ability | Required level | Effect |
| --- | ---: | --- |
| Invisibility | 2 | Invisibility for 30 seconds |
| Night Vision | 3 | Night vision for 5 minutes |
| Gojo's Mukagen (Infinity) | 4 | Resistance V for 30 seconds |
| Sukuna's Kai (Dismantle) | 5 | A close-range slash that damages living targets within 6 blocks |

## Build

Use Java 25 and the included Gradle wrapper:

```sh
./gradlew build
```

The mod jar is created in `build/libs/`.

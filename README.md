# Level Up

A small Fabric mod for Minecraft 26.3 that adds a server-side player leveling system and a custom in-game progression card.

## How it works

- Defeating an entity grants **10 XP**.
- Level 1 takes 20 XP; each subsequent level costs 10 more XP than the previous one.
- XP carries over when you level up, and every level-up restores your health.
- The HUD card shows your level and progress to the next level.
- Progress is stored in the world's scoreboard, so it is saved with the world and works in multiplayer.

## Build

Use Java 25 and the included Gradle wrapper:

```sh
./gradlew build
```

The mod jar is created in `build/libs/`.

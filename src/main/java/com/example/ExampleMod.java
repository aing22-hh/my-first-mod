package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardScore;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Server-side leveling progression, earned by defeating mobs. */
public class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "modid";
    public static final String XP_OBJECTIVE = "mod_xp";
    public static final String LEVEL_OBJECTIVE = "mod_level";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** The next level costs 20 XP at level 1, then 10 more XP per level. */
    public static int xpToNextLevel(int level) {
        return 20 + Math.max(0, level - 1) * 10;
    }

    @Override
    public void onInitialize() {
        LOGGER.info("{} leveling system initialized", MOD_ID);

        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {
            if (!(entity instanceof PlayerEntity player)) return;

            Scoreboard scoreboard = world.getScoreboard();
            ScoreboardObjective xpObjective = getOrCreateObjective(scoreboard, XP_OBJECTIVE, "Adventurer XP");
            ScoreboardObjective levelObjective = getOrCreateObjective(scoreboard, LEVEL_OBJECTIVE, "Adventurer Level");
            String entry = player.getScoreboardEntryName();
            ScoreboardScore xpScore = scoreboard.getOrCreateScore(entry, xpObjective);
            ScoreboardScore levelScore = scoreboard.getOrCreateScore(entry, levelObjective);

            int xp = xpScore.getScore() + 10;
            int level = levelScore.getScore();
            player.sendMessage(Text.literal("✦ +10 XP").formatted(Formatting.AQUA), true);

            while (xp >= xpToNextLevel(level)) {
                xp -= xpToNextLevel(level);
                level++;
                player.sendMessage(Text.literal("LEVEL UP!  You reached level " + level + "!")
                        .formatted(Formatting.GOLD, Formatting.BOLD), false);
                player.setHealth(player.getMaxHealth());
            }

            xpScore.setScore(xp);
            levelScore.setScore(level);
        });
    }

    private static ScoreboardObjective getOrCreateObjective(Scoreboard scoreboard, String name, String displayName) {
        ScoreboardObjective objective = scoreboard.getNullableObjective(name);
        if (objective == null) {
            objective = scoreboard.addObjective(name, ScoreboardCriterion.DUMMY,
                    Text.literal(displayName), ScoreboardCriterion.RenderType.INTEGER);
        }
        return objective;
    }
}

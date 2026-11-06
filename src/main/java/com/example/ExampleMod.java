package com.example;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardScore;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Server-side leveling, skill unlocks, and ability effects. */
public class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "modid";
    public static final String XP_OBJECTIVE = "mod_xp";
    public static final String LEVEL_OBJECTIVE = "mod_level";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final String[] ABILITIES = {"invisibility", "night_vision", "infinity", "dismantle"};
    private static final int[] REQUIRED_LEVELS = {2, 3, 4, 5};
    private static final String[] UNLOCK_OBJECTIVES = {"mod_unlock_inv", "mod_unlock_night", "mod_unlock_inf", "mod_unlock_dis"};

    /** The next level costs 20 XP at level 1, then 10 more XP per level. */
    public static int xpToNextLevel(int level) {
        return 20 + Math.max(0, level - 1) * 10;
    }

    public static int abilityIndex(String ability) {
        for (int i = 0; i < ABILITIES.length; i++) {
            if (ABILITIES[i].equalsIgnoreCase(ability)) return i;
        }
        return -1;
    }

    public static boolean isAbilityUnlocked(Scoreboard scoreboard, String playerEntry, int abilityIndex) {
        if (abilityIndex < 0 || abilityIndex >= UNLOCK_OBJECTIVES.length) return false;
        ScoreboardObjective objective = scoreboard.getNullableObjective(UNLOCK_OBJECTIVES[abilityIndex]);
        return objective != null && scoreboard.getOrCreateScore(playerEntry, objective).getScore() > 0;
    }

    @Override
    public void onInitialize() {
        LOGGER.info("{} leveling and skills initialized", MOD_ID);

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
                player.sendMessage(Text.literal("LEVEL UP! You reached level " + level + "!")
                        .formatted(Formatting.GOLD, Formatting.BOLD), false);
                player.setHealth(player.getMaxHealth());
            }
            xpScore.setScore(xp);
            levelScore.setScore(level);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("skill")
                        .then(CommandManager.literal("unlock")
                                .then(CommandManager.argument("ability", StringArgumentType.word())
                                        .executes(context -> unlockAbility(context.getSource().getPlayerOrThrow(),
                                                StringArgumentType.getString(context, "ability")))))
                        .then(CommandManager.literal("cast")
                                .then(CommandManager.argument("ability", StringArgumentType.word())
                                        .executes(context -> castAbility(context.getSource().getPlayerOrThrow(),
                                                StringArgumentType.getString(context, "ability")))))));
    }

    private static int unlockAbility(ServerPlayerEntity player, String name) {
        int index = abilityIndex(name);
        if (index < 0) {
            player.sendMessage(Text.literal("Unknown skill. Choose invisibility, night_vision, infinity, or dismantle."), false);
            return 0;
        }
        Scoreboard scoreboard = player.getWorld().getScoreboard();
        String entry = player.getScoreboardEntryName();
        if (isAbilityUnlocked(scoreboard, entry, index)) {
            player.sendMessage(Text.literal("That ability is already unlocked."), false);
            return 0;
        }
        ScoreboardObjective levels = getOrCreateObjective(scoreboard, LEVEL_OBJECTIVE, "Adventurer Level");
        int level = scoreboard.getOrCreateScore(entry, levels).getScore();
        if (level < REQUIRED_LEVELS[index]) {
            player.sendMessage(Text.literal("Reach level " + REQUIRED_LEVELS[index] + " to unlock " + displayName(index) + ".")
                    .formatted(Formatting.RED), false);
            return 0;
        }
        ScoreboardObjective unlocked = getOrCreateObjective(scoreboard, UNLOCK_OBJECTIVES[index], "Skill unlocked");
        scoreboard.getOrCreateScore(entry, unlocked).setScore(1);
        player.sendMessage(Text.literal(displayName(index) + " unlocked!").formatted(Formatting.GREEN, Formatting.BOLD), false);
        return 1;
    }

    private static int castAbility(ServerPlayerEntity player, String name) {
        int index = abilityIndex(name);
        if (index < 0) {
            player.sendMessage(Text.literal("Unknown skill. Choose invisibility, night_vision, infinity, or dismantle."), false);
            return 0;
        }
        Scoreboard scoreboard = player.getWorld().getScoreboard();
        if (!isAbilityUnlocked(scoreboard, player.getScoreboardEntryName(), index)) {
            player.sendMessage(Text.literal("Unlock this skill in the skill tree first.").formatted(Formatting.RED), false);
            return 0;
        }

        switch (index) {
            case 0 -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 20 * 30, 0));
            case 1 -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 20 * 60 * 5, 0));
            case 2 -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20 * 30, 4));
            case 3 -> {
                int hitCount = 0;
                for (LivingEntity target : player.getWorld().getEntitiesByClass(LivingEntity.class,
                        player.getBoundingBox().expand(6.0), target -> target != player && target.isAlive())) {
                    target.damage(player.getDamageSources().playerAttack(player), 8.0F);
                    hitCount++;
                }
                player.sendMessage(Text.literal("Kai: Dismantle! " + hitCount + " target(s) struck.")
                        .formatted(Formatting.DARK_RED, Formatting.BOLD), false);
                return hitCount;
            }
            default -> { return 0; }
        }
        player.sendMessage(Text.literal("Ability used: " + displayName(index)).formatted(Formatting.AQUA), false);
        return 1;
    }

    private static String displayName(int index) {
        return switch (index) {
            case 0 -> "Invisibility";
            case 1 -> "Night Vision";
            case 2 -> "Gojo's Mukagen (Infinity)";
            case 3 -> "Sukuna's Kai (Dismantle)";
            default -> "Unknown";
        };
    }

    public static ScoreboardObjective getOrCreateObjective(Scoreboard scoreboard, String name, String displayName) {
        ScoreboardObjective objective = scoreboard.getNullableObjective(name);
        if (objective == null) {
            objective = scoreboard.addObjective(name, ScoreboardCriterion.DUMMY,
                    Text.literal(displayName), ScoreboardCriterion.RenderType.INTEGER);
        }
        return objective;
    }
}

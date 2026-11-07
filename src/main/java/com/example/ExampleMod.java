package com.example;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardScore;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Server-side leveling, skill unlocks, freely configurable loadouts, and ability effects. */
public class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "modid";
    public static final String XP_OBJECTIVE = "mod_xp";
    public static final String LEVEL_OBJECTIVE = "mod_level";
    public static final String INFINITY_TICKS_OBJECTIVE = "mod_infinity";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final String[] ABILITIES = {"invisibility", "night_vision", "infinity", "dismantle"};
    private static final int[] REQUIRED_LEVELS = {2, 3, 4, 5};
    private static final String[] UNLOCK_OBJECTIVES = {"mod_unlock_inv", "mod_unlock_night", "mod_unlock_inf", "mod_unlock_dis"};

    public static int xpToNextLevel(int level) {
        return 20 + Math.max(0, level - 1) * 10;
    }

    public static int abilityIndex(String ability) {
        for (int i = 0; i < ABILITIES.length; i++) {
            if (ABILITIES[i].equalsIgnoreCase(ability)) return i;
        }
        return -1;
    }

    public static String abilityName(int index) {
        return switch (index) {
            case 0 -> "Invisibility";
            case 1 -> "Night Vision";
            case 2 -> "Gojo's Mukagen (Infinity)";
            case 3 -> "Sukuna's Kai (Cleave / Dismantle)";
            default -> "Empty";
        };
    }

    private static String loadoutObjectiveName(int slot) {
        return "mod_skill_" + slot;
    }

    public static int equippedAbility(Scoreboard scoreboard, String playerEntry, int slot) {
        if (slot < 1 || slot > 4) return -1;
        ScoreboardObjective objective = scoreboard.getNullableObjective(loadoutObjectiveName(slot));
        if (objective == null) return -1;
        int value = scoreboard.getOrCreateScore(playerEntry, objective).getScore();
        return value >= 1 && value <= ABILITIES.length ? value - 1 : -1;
    }

    public static boolean isAbilityUnlocked(Scoreboard scoreboard, String playerEntry, int ability) {
        if (ability < 0 || ability >= UNLOCK_OBJECTIVES.length) return false;
        ScoreboardObjective objective = scoreboard.getNullableObjective(UNLOCK_OBJECTIVES[ability]);
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
                        .then(CommandManager.literal("equip")
                                .then(CommandManager.argument("ability", StringArgumentType.word())
                                        .then(CommandManager.argument("slot", IntegerArgumentType.integer(1, 4))
                                                .executes(context -> equipAbility(context.getSource().getPlayerOrThrow(),
                                                        StringArgumentType.getString(context, "ability"),
                                                        IntegerArgumentType.getInteger(context, "slot"))))))
                        .then(CommandManager.literal("unequip")
                                .then(CommandManager.argument("slot", IntegerArgumentType.integer(1, 4))
                                        .executes(context -> unequipAbility(context.getSource().getPlayerOrThrow(),
                                                IntegerArgumentType.getInteger(context, "slot")))))
                        .then(CommandManager.literal("reset")
                                .executes(context -> resetLoadout(context.getSource().getPlayerOrThrow())))
                        .then(CommandManager.literal("cast")
                                .then(CommandManager.argument("slot", IntegerArgumentType.integer(1, 4))
                                        .executes(context -> castSlot(context.getSource().getPlayerOrThrow(),
                                                IntegerArgumentType.getInteger(context, "slot")))))
                        .then(CommandManager.literal("stop_infinity")
                                .executes(context -> stopInfinity(context.getSource().getPlayerOrThrow())))));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) tickInfinity(player);
        });
    }

    private static int unlockAbility(ServerPlayerEntity player, String name) {
        int index = abilityIndex(name);
        if (index < 0) {
            player.sendMessage(Text.literal("Unknown skill. Use invisibility, night_vision, infinity, or dismantle."), false);
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
            player.sendMessage(Text.literal("Reach level " + REQUIRED_LEVELS[index] + " to unlock " + abilityName(index) + ".")
                    .formatted(Formatting.RED), false);
            return 0;
        }
        ScoreboardObjective unlocked = getOrCreateObjective(scoreboard, UNLOCK_OBJECTIVES[index], "Skill unlocked");
        scoreboard.getOrCreateScore(entry, unlocked).setScore(1);
        player.sendMessage(Text.literal(abilityName(index) + " unlocked!").formatted(Formatting.GREEN, Formatting.BOLD), false);
        return 1;
    }

    private static int equipAbility(ServerPlayerEntity player, String name, int slot) {
        int ability = abilityIndex(name);
        if (ability < 0) {
            player.sendMessage(Text.literal("Unknown skill."), false);
            return 0;
        }
        Scoreboard scoreboard = player.getWorld().getScoreboard();
        String entry = player.getScoreboardEntryName();
        if (!isAbilityUnlocked(scoreboard, entry, ability)) {
            player.sendMessage(Text.literal("Unlock " + abilityName(ability) + " before equipping it.").formatted(Formatting.RED), false);
            return 0;
        }
        ScoreboardObjective objective = getOrCreateObjective(scoreboard, loadoutObjectiveName(slot), "Skill loadout slot " + slot);
        scoreboard.getOrCreateScore(entry, objective).setScore(ability + 1);
        player.sendMessage(Text.literal(abilityName(ability) + " equipped in slot " + slot + ".")
                .formatted(Formatting.GREEN), false);
        return 1;
    }

    private static int unequipAbility(ServerPlayerEntity player, int slot) {
        Scoreboard scoreboard = player.getWorld().getScoreboard();
        ScoreboardObjective objective = getOrCreateObjective(scoreboard, loadoutObjectiveName(slot), "Skill loadout slot " + slot);
        scoreboard.getOrCreateScore(player.getScoreboardEntryName(), objective).setScore(0);
        player.sendMessage(Text.literal("Skill slot " + slot + " cleared."), false);
        return 1;
    }

    private static int resetLoadout(ServerPlayerEntity player) {
        Scoreboard scoreboard = player.getWorld().getScoreboard();
        String entry = player.getScoreboardEntryName();
        for (int slot = 1; slot <= 4; slot++) {
            ScoreboardObjective objective = getOrCreateObjective(scoreboard, loadoutObjectiveName(slot), "Skill loadout slot " + slot);
            scoreboard.getOrCreateScore(entry, objective).setScore(0);
        }
        player.sendMessage(Text.literal("Loadout cleared. Your unlocked skills are kept; reconfigure any time."), false);
        return 1;
    }

    private static int castSlot(ServerPlayerEntity player, int slot) {
        Scoreboard scoreboard = player.getWorld().getScoreboard();
        int ability = equippedAbility(scoreboard, player.getScoreboardEntryName(), slot);
        if (ability < 0) {
            player.sendMessage(Text.literal("That loadout slot is empty."), false);
            return 0;
        }
        return castAbility(player, ability);
    }

    private static int castAbility(ServerPlayerEntity player, int ability) {
        Scoreboard scoreboard = player.getWorld().getScoreboard();
        if (!isAbilityUnlocked(scoreboard, player.getScoreboardEntryName(), ability)) {
            player.sendMessage(Text.literal("Unlock this skill in the skill tree first.").formatted(Formatting.RED), false);
            return 0;
        }
        switch (ability) {
            case 0 -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 20 * 30, 0));
            case 1 -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 20 * 60 * 5, 0));
            case 2 -> {
                ScoreboardObjective timer = getOrCreateObjective(scoreboard, INFINITY_TICKS_OBJECTIVE, "Infinity duration");
                scoreboard.getOrCreateScore(player.getScoreboardEntryName(), timer).setScore(20 * 30);
                player.sendMessage(Text.literal("Mukagen: Infinity is active for 30 seconds.").formatted(Formatting.AQUA), false);
                return 1;
            }
            case 3 -> {
                int hits = 0;
                Vec3d look = player.getRotationVec(1.0F).normalize();
                for (LivingEntity target : player.getWorld().getEntitiesByClass(LivingEntity.class,
                        player.getBoundingBox().expand(8.0), target -> target != player && target.isAlive())) {
                    Vec3d offset = target.getPos().subtract(player.getPos());
                    double distance = offset.length();
                    if (distance > 8.0 || distance < 0.001) continue;
                    Vec3d direction = offset.normalize();
                    if (look.dotProduct(direction) < 0.78) continue;
                    // Kai is an entity-only attack: instant kill and no changes to blocks in the world.
                    if (target instanceof PlayerEntity) continue;
                    target.kill(player.getWorld());
                    hits++;
                }
                player.sendMessage(Text.literal("Kai: Cleave! " + hits + " monster(s) eliminated.")
                        .formatted(Formatting.DARK_RED, Formatting.BOLD), false);
                return hits;
            }
            default -> { return 0; }
        }
        player.sendMessage(Text.literal("Ability used: " + abilityName(ability)).formatted(Formatting.AQUA), false);
        return 1;
    }

    /** Infinity continuously creates space around its user, rather than only granting damage resistance. */
    private static void tickInfinity(ServerPlayerEntity player) {
        Scoreboard scoreboard = player.getWorld().getScoreboard();
        ScoreboardObjective timerObjective = scoreboard.getNullableObjective(INFINITY_TICKS_OBJECTIVE);
        if (timerObjective == null) return;
        ScoreboardScore timer = scoreboard.getOrCreateScore(player.getScoreboardEntryName(), timerObjective);
        int ticksLeft = timer.getScore();
        if (ticksLeft <= 0) return;

        double radius = 3.25;
        for (LivingEntity entity : player.getWorld().getEntitiesByClass(LivingEntity.class,
                player.getBoundingBox().expand(radius), target -> target != player && target.isAlive())) {
            double dx = entity.getX() - player.getX();
            double dz = entity.getZ() - player.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance < 0.001 || distance >= radius) continue;
            double force = 0.22 + (radius - distance) * 0.10;
            entity.addVelocity(dx / distance * force, 0.08, dz / distance * force);
        }
        timer.setScore(ticksLeft - 1);
        if (ticksLeft == 1) player.sendMessage(Text.literal("Mukagen: Infinity faded.").formatted(Formatting.GRAY), false);
    }

    private static int stopInfinity(ServerPlayerEntity player) {
        Scoreboard scoreboard = player.getWorld().getScoreboard();
        ScoreboardObjective timer = scoreboard.getNullableObjective(INFINITY_TICKS_OBJECTIVE);
        if (timer != null) scoreboard.getOrCreateScore(player.getScoreboardEntryName(), timer).setScore(0);
        player.sendMessage(Text.literal("Mukagen: Infinity deactivated."), false);
        return 1;
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

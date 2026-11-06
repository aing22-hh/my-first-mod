package com.example.client;

import com.example.ExampleMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardScore;
import net.minecraft.text.Text;

/** Draws a compact, custom progression card over the normal game HUD. */
public class ExampleModClient implements ClientModInitializer {
    private static final int PANEL_WIDTH = 164;
    private static final int PANEL_HEIGHT = 48;

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> renderLevelCard(drawContext));
    }

    private static void renderLevelCard(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden) return;

        Scoreboard scoreboard = client.world.getScoreboard();
        ScoreboardObjective xpObjective = scoreboard.getNullableObjective(ExampleMod.XP_OBJECTIVE);
        ScoreboardObjective levelObjective = scoreboard.getNullableObjective(ExampleMod.LEVEL_OBJECTIVE);
        if (xpObjective == null || levelObjective == null) return;

        String entry = client.player.getScoreboardEntryName();
        ScoreboardScore xpScore = scoreboard.getOrCreateScore(entry, xpObjective);
        ScoreboardScore levelScore = scoreboard.getOrCreateScore(entry, levelObjective);
        int xp = Math.max(0, xpScore.getScore());
        int level = Math.max(0, levelScore.getScore());
        int needed = ExampleMod.xpToNextLevel(level);

        int x = 10;
        int y = 10;
        int barX = x + 10;
        int barY = y + 34;
        int barWidth = PANEL_WIDTH - 20;
        int filled = Math.min(barWidth, xp * barWidth / needed);

        // Dark glass-like card, jewel accent, and a crisp two-tone progress meter.
        context.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xD91B2030);
        context.fill(x, y, x + 3, y + PANEL_HEIGHT, 0xFF55E6C1);
        context.fill(x + 3, y, x + PANEL_WIDTH, y + 1, 0x6655E6C1);
        context.drawTextWithShadow(client.textRenderer, Text.literal("ADVENTURER") , x + 10, y + 7, 0xFF9FB0C8);
        context.drawTextWithShadow(client.textRenderer, Text.literal("LVL " + level), x + 10, y + 19, 0xFFFFD166);
        context.drawTextWithShadow(client.textRenderer, Text.literal(xp + " / " + needed + " XP"),
                x + PANEL_WIDTH - 10 - client.textRenderer.getWidth(xp + " / " + needed + " XP"), y + 19, 0xFFEAF2FF);
        context.fill(barX, barY, barX + barWidth, barY + 5, 0xFF394357);
        if (filled > 0) {
            context.fill(barX, barY, barX + filled, barY + 5, 0xFF55E6C1);
            if (filled > 2) context.fill(barX + 2, barY + 1, barX + filled, barY + 2, 0xFFB2FFE8);
        }
    }
}

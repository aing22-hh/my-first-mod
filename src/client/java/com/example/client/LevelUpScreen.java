package com.example.client;

import com.example.ExampleMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardScore;
import net.minecraft.text.Text;

/** Level details are intentionally separate from the skill-tree screen. */
public class LevelUpScreen extends Screen {
    public LevelUpScreen() {
        super(Text.literal("Level Up"));
    }

    @Override
    protected void init() {
        int panelWidth = 250;
        int x = (width - panelWidth) / 2;
        int y = height / 2 - 62;
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close())
                .dimensions(x + 70, y + 108, 110, 22).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        MinecraftClient client = MinecraftClient.getInstance();
        int panelWidth = 250;
        int x = (width - panelWidth) / 2;
        int y = height / 2 - 62;
        context.fill(x, y, x + panelWidth, y + 140, 0xF01B2030);
        context.fill(x, y, x + panelWidth, y + 2, 0xFFB77CFF);
        context.drawCenteredTextWithShadow(textRenderer, "ADVENTURER PROFILE", width / 2, y + 13, 0xFFE9D7FF);
        int level = 0, xp = 0;
        if (client.player != null && client.world != null) {
            Scoreboard board = client.world.getScoreboard();
            String entry = client.player.getScoreboardEntryName();
            ScoreboardObjective levels = board.getNullableObjective(ExampleMod.LEVEL_OBJECTIVE);
            ScoreboardObjective experience = board.getNullableObjective(ExampleMod.XP_OBJECTIVE);
            if (levels != null) level = board.getOrCreateScore(entry, levels).getScore();
            if (experience != null) xp = board.getOrCreateScore(entry, experience).getScore();
        }
        int needed = ExampleMod.xpToNextLevel(level);
        context.drawCenteredTextWithShadow(textRenderer, "Level " + level, width / 2, y + 40, 0xFFFFD166);
        context.drawCenteredTextWithShadow(textRenderer, xp + " / " + needed + " XP to next level", width / 2, y + 58, 0xFFEAF2FF);
        int barX = x + 25, barY = y + 81, barWidth = panelWidth - 50;
        context.fill(barX, barY, barX + barWidth, barY + 8, 0xFF394357);
        int filled = Math.min(barWidth, xp * barWidth / needed);
        if (filled > 0) context.fill(barX, barY, barX + filled, barY + 8, 0xFFB77CFF);
        context.drawCenteredTextWithShadow(textRenderer, "Earn XP by defeating enemies", width / 2, y + 95, 0xFF9FB0C8);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

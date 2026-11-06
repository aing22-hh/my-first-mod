package com.example.client;

import com.example.ExampleMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.text.Text;

/** Separate unlock-and-cast interface for the four progression abilities. */
public class SkillTreeScreen extends Screen {
    private static final String[] NAMES = {"Invisibility", "Night Vision", "Mukagen (Infinity)", "Kai (Dismantle)"};
    private static final String[] COMMAND_NAMES = {"invisibility", "night_vision", "infinity", "dismantle"};
    private static final int[] REQUIRED_LEVELS = {2, 3, 4, 5};

    public SkillTreeScreen() {
        super(Text.literal("Skill Tree"));
    }

    @Override
    protected void init() {
        int cardWidth = 310;
        int x = (width - cardWidth) / 2;
        int top = Math.max(28, (height - 240) / 2);
        for (int i = 0; i < NAMES.length; i++) {
            final int ability = i;
            int rowY = top + 44 + i * 42;
            addDrawableChild(ButtonWidget.builder(Text.literal("Unlock"), button -> send("unlock", ability))
                    .dimensions(x + 165, rowY, 62, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Use"), button -> send("cast", ability))
                    .dimensions(x + 232, rowY, 62, 20).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> close())
                .dimensions(x + 100, top + 224, 110, 20).build());
    }

    private static void send(String action, int ability) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) client.player.networkHandler.sendChatCommand("skill " + action + " " + COMMAND_NAMES[ability]);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        MinecraftClient client = MinecraftClient.getInstance();
        int cardWidth = 310;
        int x = (width - cardWidth) / 2;
        int top = Math.max(28, (height - 240) / 2);
        context.fill(x, top, x + cardWidth, top + 252, 0xF01B2030);
        context.fill(x, top, x + cardWidth, top + 2, 0xFF55E6C1);
        context.drawCenteredTextWithShadow(textRenderer, "SKILL TREE", width / 2, top + 12, 0xFFB2FFE8);
        Scoreboard board = client.world == null ? null : client.world.getScoreboard();
        int level = 0;
        if (board != null && client.player != null) {
            var objective = board.getNullableObjective(ExampleMod.LEVEL_OBJECTIVE);
            if (objective != null) level = board.getOrCreateScore(client.player.getScoreboardEntryName(), objective).getScore();
        }
        context.drawCenteredTextWithShadow(textRenderer, "Current level: " + level, width / 2, top + 28, 0xFF9FB0C8);
        for (int i = 0; i < NAMES.length; i++) {
            int rowY = top + 47 + i * 42;
            boolean unlocked = board != null && client.player != null
                    && ExampleMod.isAbilityUnlocked(board, client.player.getScoreboardEntryName(), i);
            context.fill(x + 10, rowY - 5, x + 300, rowY + 28, 0xAA283044);
            context.drawTextWithShadow(textRenderer, NAMES[i], x + 17, rowY + 2, unlocked ? 0xFF55E6C1 : 0xFFEAF2FF);
            context.drawTextWithShadow(textRenderer, "Level " + REQUIRED_LEVELS[i], x + 17, rowY + 14, 0xFF9FB0C8);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

package com.example.client;

import com.example.ExampleMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.text.Text;

/** Separate UI for skill unlocks and freely reconfigurable four-slot loadouts. */
public class SkillTreeScreen extends Screen {
    private static final String[] NAMES = {"Invisibility", "Night Vision", "Mukagen (Infinity)", "Kai (Cleave / Dismantle)"};
    private static final String[] COMMAND_NAMES = {"invisibility", "night_vision", "infinity", "dismantle"};
    private static final int[] REQUIRED_LEVELS = {2, 3, 4, 5};
    private int selectedSlot = 1;

    public SkillTreeScreen() {
        super(Text.literal("Skill Tree"));
    }

    @Override
    protected void init() {
        clearChildren();
        int panelWidth = Math.min(430, width - 16);
        int x = (width - panelWidth) / 2;
        int top = Math.max(12, (height - 310) / 2);
        int slotWidth = (panelWidth - 28) / 4;
        for (int slot = 1; slot <= 4; slot++) {
            final int selected = slot;
            addDrawableChild(ButtonWidget.builder(Text.literal("Slot " + slot), button -> {
                selectedSlot = selected;
                clearAndInit();
            }).dimensions(x + 10 + (slot - 1) * (slotWidth + 2), top + 48, slotWidth, 20).build());
        }

        for (int i = 0; i < NAMES.length; i++) {
            final int ability = i;
            int rowY = top + 112 + i * 34;
            addDrawableChild(ButtonWidget.builder(Text.literal("Unlock"), button -> send("unlock " + COMMAND_NAMES[ability]))
                    .dimensions(x + panelWidth - 151, rowY, 62, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Equip →"), button -> send("equip " + COMMAND_NAMES[ability] + " " + selectedSlot))
                    .dimensions(x + panelWidth - 84, rowY, 74, 20).build());
        }

        int footerY = top + 260;
        addDrawableChild(ButtonWidget.builder(Text.literal("Use Selected"), button -> send("cast " + selectedSlot))
                .dimensions(x + 10, footerY, 98, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear Slot"), button -> send("unequip " + selectedSlot))
                .dimensions(x + 114, footerY, 82, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset Loadout"), button -> send("reset"))
                .dimensions(x + 202, footerY, 105, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> close())
                .dimensions(x + panelWidth - 93, footerY, 83, 20).build());
    }

    private static void send(String command) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) client.player.networkHandler.sendChatCommand("skill " + command);
    }

    private Scoreboard scoreboard() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.world == null ? null : client.world.getScoreboard();
    }

    private int playerLevel(Scoreboard board) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (board == null || client.player == null) return 0;
        var objective = board.getNullableObjective(ExampleMod.LEVEL_OBJECTIVE);
        return objective == null ? 0 : board.getOrCreateScore(client.player.getScoreboardEntryName(), objective).getScore();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        MinecraftClient client = MinecraftClient.getInstance();
        int panelWidth = Math.min(430, width - 16);
        int x = (width - panelWidth) / 2;
        int top = Math.max(12, (height - 310) / 2);
        context.fill(x, top, x + panelWidth, top + 294, 0xF01B2030);
        context.fill(x, top, x + panelWidth, top + 2, 0xFF55E6C1);
        context.drawCenteredTextWithShadow(textRenderer, "SKILL TREE  •  FLEXIBLE LOADOUT", width / 2, top + 10, 0xFFB2FFE8);
        context.drawCenteredTextWithShadow(textRenderer, "Level " + playerLevel(scoreboard()) + "   |   Select a slot, then equip any unlocked skill",
                width / 2, top + 27, 0xFF9FB0C8);

        Scoreboard board = scoreboard();
        String entry = client.player == null ? "" : client.player.getScoreboardEntryName();
        int equipped = board == null || client.player == null ? -1 : ExampleMod.equippedAbility(board, entry, selectedSlot);
        String equippedName = equipped < 0 ? "Empty" : NAMES[equipped];
        context.drawCenteredTextWithShadow(textRenderer, "Selected slot " + selectedSlot + ": " + equippedName,
                width / 2, top + 75, 0xFFFFD166);

        for (int i = 0; i < NAMES.length; i++) {
            int rowY = top + 108 + i * 34;
            boolean unlocked = board != null && client.player != null
                    && ExampleMod.isAbilityUnlocked(board, entry, i);
            context.fill(x + 8, rowY - 5, x + panelWidth - 8, rowY + 27, 0xAA283044);
            context.drawTextWithShadow(textRenderer, NAMES[i], x + 15, rowY, unlocked ? 0xFF55E6C1 : 0xFFEAF2FF);
            context.drawTextWithShadow(textRenderer, unlocked ? "Unlocked" : "Unlock at level " + REQUIRED_LEVELS[i],
                    x + 15, rowY + 13, 0xFF9FB0C8);
        }
        context.drawCenteredTextWithShadow(textRenderer, "Re-equip or reset your loadout any time; unlocks are retained.",
                width / 2, top + 244, 0xFF9FB0C8);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

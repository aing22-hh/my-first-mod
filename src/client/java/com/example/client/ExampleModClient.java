package com.example.client;

import com.example.ExampleMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardScore;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Renders HUD launch buttons and registers the skill-tree hotkey. */
public class ExampleModClient implements ClientModInitializer {
    private static final int BUTTON_X = 10;
    private static final int BUTTON_WIDTH = 142;
    private static final int BUTTON_HEIGHT = 22;
    private static boolean leftMouseDown;
    private static KeyBinding skillTreeKey;

    @Override
    public void onInitializeClient() {
        skillTreeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.modid.skill_tree", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8, "category.modid"));
        HudRenderCallback.EVENT.register((context, tickCounter) -> renderHud(context));
        ClientTickEvents.END_CLIENT_TICK.register(ExampleModClient::handleInput);
    }

    private static void handleInput(MinecraftClient client) {
        while (skillTreeKey.wasPressed()) {
            if (client.currentScreen == null) client.setScreen(new SkillTreeScreen());
        }
        if (client.player == null || client.currentScreen != null || !client.isWindowFocused()) {
            leftMouseDown = false;
            return;
        }
        boolean down = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;
        if (down && !leftMouseDown) {
            double scaleX = (double) client.getWindow().getScaledWidth() / client.getWindow().getWidth();
            double scaleY = (double) client.getWindow().getScaledHeight() / client.getWindow().getHeight();
            int mouseX = (int) (client.mouse.getX() * scaleX);
            int mouseY = (int) (client.mouse.getY() * scaleY);
            if (inside(mouseX, mouseY, BUTTON_X, 10)) {
                client.setScreen(new LevelUpScreen());
            } else if (inside(mouseX, mouseY, BUTTON_X, 36)) {
                client.setScreen(new SkillTreeScreen());
            }
        }
        leftMouseDown = down;
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX <= x + BUTTON_WIDTH && mouseY >= y && mouseY <= y + BUTTON_HEIGHT;
    }

    private static void renderHud(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden) return;
        drawHudButton(context, client, "✦  LEVEL UP", BUTTON_X, 10, 0xFFB77CFF);
        drawHudButton(context, client, "✧  SKILL TREE  [F8]", BUTTON_X, 36, 0xFF55E6C1);

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
        int x = 10, y = 64, width = 164, height = 42;
        int barWidth = width - 20;
        int filled = Math.min(barWidth, xp * barWidth / needed);
        context.fill(x, y, x + width, y + height, 0xD91B2030);
        context.fill(x, y, x + 3, y + height, 0xFF55E6C1);
        context.drawTextWithShadow(client.textRenderer, Text.literal("ADVENTURER"), x + 10, y + 6, 0xFF9FB0C8);
        context.drawTextWithShadow(client.textRenderer, Text.literal("LVL " + level), x + 10, y + 19, 0xFFFFD166);
        String progress = xp + " / " + needed + " XP";
        context.drawTextWithShadow(client.textRenderer, Text.literal(progress), x + width - 10 - client.textRenderer.getWidth(progress), y + 19, 0xFFEAF2FF);
        context.fill(x + 10, y + 32, x + 10 + barWidth, y + 37, 0xFF394357);
        if (filled > 0) context.fill(x + 10, y + 32, x + 10 + filled, y + 37, 0xFF55E6C1);
    }

    private static void drawHudButton(DrawContext context, MinecraftClient client, String label, int x, int y, int accent) {
        context.fill(x, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, 0xE51B2030);
        context.fill(x, y, x + 3, y + BUTTON_HEIGHT, accent);
        context.drawTextWithShadow(client.textRenderer, Text.literal(label), x + 10, y + 7, 0xFFF2F4FF);
    }
}

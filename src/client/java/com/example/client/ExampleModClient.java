package com.example.client; // แก้บรรทัดแรกให้เป็นแบบนี้ครับ

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardScore;
import net.minecraft.text.Text;

public class ExampleModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // ... โค้ดส่วนที่เหลือทั้งหมดด้านล่างเหมือนเดิมเลยครับ ...

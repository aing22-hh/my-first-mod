package net.fabricmc.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardScore;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "modid";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // กำหนดว่าต้องใช้ XP เท่าไหร่ในการเลเวลอัพแต่ละขั้น (ตัวอย่าง: ใช้ 5 XP)
    private static final int XP_PER_LEVEL = 5;

    @Override
    public void onInitialize() {
        LOGGER.info("ระบบ Level Up กำลังเปิดใช้งาน...");

        // ดักจับเหตุการณ์: เมื่อมี Entity ถูกฆ่าในสนามรบ
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {
            // ตรวจสอบว่า "ผู้ฆ่า" ต้องเป็นผู้เล่นเท่านั้น
            if (entity instanceof PlayerEntity player) {
                Scoreboard scoreboard = world.getScoreboard();
                
                // ค้นหาหรือสร้างคลังเก็บคะแนน XP และ Level ของผู้เล่น
                ScoreboardObjective xpObjective = getOrCreateObjective(scoreboard, "custom_xp", "Custom XP");
                ScoreboardObjective levelObjective = getOrCreateObjective(scoreboard, "custom_level", "Player Level");

                // ดึงคะแนนปัจจุบันของผู้เล่นออกมา
                ScoreboardScore xpScore = scoreboard.getOrCreateScore(player.getScoreboardEntryName(), xpObjective);
                ScoreboardScore levelScore = scoreboard.getOrCreateScore(player.getScoreboardEntryName(), levelObjective);

                // 1. เพิ่มค่า XP ให้ผู้เล่น 1 แต้มเมื่อฆ่ามอนสเตอร์ได้
                xpScore.setScore(xpScore.getScore() + 1);
                player.sendMessage(Text.literal("+1 Custom XP (" + xpScore.getScore() + "/" + XP_PER_LEVEL + ")").formatted(Formatting.GREEN), true);

                // 2. ตรวจสอบระบบ Level Up (ถ้า XP ถึงกำหนด)
                if (xpScore.getScore() >= XP_PER_LEVEL) {
                    // Reset ค่า XP กลับเป็น 0
                    xpScore.setScore(0);
                    
                    // เพิ่มเลเวลผู้เล่นขึ้น 1 เลเวล
                    int newLevel = levelScore.getScore() + 1;
                    levelScore.setScore(newLevel);

                    // แสดงข้อความประกาศความยินดีบนหน้าจอผู้เล่น (เป็นตัวหนาสีทอง)
                    player.sendMessage(Text.literal("🎉 LEVEL UP! ตอนนี้คุณเลเวล " + newLevel + " แล้ว! 🎉")
                            .formatted(Formatting.GOLD, Formatting.BOLD), false);
                    
                    // แถม: ฟื้นฟูเลือดให้เต็มหลอดเป็นรางวัลเมื่อเลเวลอัพ
                    player.setHealth(player.getMaxHealth());
                }
            }
        });
    }

    // ฟังก์ชันช่วยสร้างบอร์ดเก็บคะแนนในระบบของ Minecraft
    private ScoreboardObjective getOrCreateObjective(Scoreboard scoreboard, String name, String displayName) {
        ScoreboardObjective objective = scoreboard.getNullableObjective(name);
        if (objective == null) {
            objective = scoreboard.addObjective(name, net.minecraft.scoreboard.ScoreboardCriterion.DUMMY, 
                    Text.literal(displayName), net.minecraft.scoreboard.ScoreboardCriterion.RenderType.INTEGER);
        }
        return objective;
    }
}

package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DirectionArrowEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.GravityDirectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Keeps Kyogai's gravity indicator visible only to the player who used the drum. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, value = Dist.CLIENT)
public final class KyogaiGravityArrowRenderer {
    private static final Minecraft MINECRAFT = Minecraft.getInstance();
    private static DirectionArrowEntity activeArrow;
    private static Direction pendingGravity;
    private static int pendingDuration;
    private static int pendingColor;
    private static int nextClientEntityId = -1;

    private KyogaiGravityArrowRenderer() {
    }

    public static void show(Direction gravity, int durationTicks, int color) {
        pendingGravity = gravity;
        pendingDuration = durationTicks;
        pendingColor = color;
        spawnPendingArrow();
    }

    private static void spawnPendingArrow() {
        Level level = MINECRAFT.level;
        LocalPlayer player = MINECRAFT.player;
        if (level == null || player == null || pendingGravity == null) {
            return;
        }

        if (activeArrow != null && !activeArrow.isRemoved()) {
            activeArrow.discard();
        }

        Vec3 look = GravityDirectionHelper.getLookDirection(player);
        if (look.lengthSqr() < 1.0E-6D) {
            look = player.getLookAngle();
        }
        Vec3 center = player.getEyePosition().add(look.normalize().scale(3.0D));
        Vec3 direction = Vec3.atLowerCornerOf(pendingGravity.getNormal());
        activeArrow = DirectionArrowEntity.create(level, center, direction, pendingColor, pendingDuration);
        activeArrow.setOldPosAndRot();
        if (!(level instanceof ClientLevel clientLevel)) {
            activeArrow = null;
            return;
        }
        activeArrow.setId(nextClientEntityId--);
        clientLevel.putNonPlayerEntity(activeArrow.getId(), activeArrow);
        pendingGravity = null;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (activeArrow == null) {
            spawnPendingArrow();
        }

        if (activeArrow == null || activeArrow.isRemoved()) {
            activeArrow = null;
            return;
        }

        LocalPlayer player = MINECRAFT.player;
        Level level = MINECRAFT.level;
        if (player == null || level == null || activeArrow.level() != level) {
            activeArrow.discard();
            activeArrow = null;
            return;
        }

        Vec3 look = GravityDirectionHelper.getLookDirection(player);
        if (look.lengthSqr() < 1.0E-6D) {
            look = player.getLookAngle();
        }
        Vec3 center = player.getEyePosition().add(look.normalize().scale(3.0D));
        activeArrow.setPos(center.x, center.y, center.z);
    }
}

package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "kimetsunoyaibamultiplayer")
public final class SwordsmithVillageEscortHandler {

    private static final ResourceLocation KAKUSHI_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "kakushi");
    private static final ResourceLocation SWORDSMITH_VILLAGE_DIM_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "swordsmith_village");
    private static final ResourceKey<Level> SWORDSMITH_VILLAGE_KEY =
        ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, SWORDSMITH_VILLAGE_DIM_ID);

    private static final double VILLAGE_ENTRY_X = 3.0D;
    private static final double VILLAGE_ENTRY_Y = 70.0D;
    private static final double VILLAGE_ENTRY_Z = 280.0D;
    private static final float VILLAGE_ENTRY_YAW = 180.0F;
    private static final float VILLAGE_ENTRY_PITCH = 0.0F;
    private static final BlockPos VILLAGE_ENTRY_KAKUSHI_POS = new BlockPos(3, 70, 280);
    private static final double ENTRY_KAKUSHI_EXISTING_RADIUS = 20.0D;
    private static final double ENTRY_KAKUSHI_MAX_DRIFT = 3.0D;
    private static final double ENTRY_KAKUSHI_HOME_SPEED = 1.0D;
    private static final double ENTRY_KAKUSHI_PLAYER_RADIUS = 50.0D;

    private static final String RETURN_TAG = "SwordsmithVillageReturn";
    private static final String RETURN_X = "ReturnX";
    private static final String RETURN_Y = "ReturnY";
    private static final String RETURN_Z = "ReturnZ";
    private static final String RETURN_YAW = "ReturnYaw";
    private static final String RETURN_PITCH = "ReturnPitch";
    private static final String RETURN_DIM = "ReturnDim";

    private static final String ESCORT_PENDING_TAG = "SwordsmithVillageEscortPending";
    private static final String ENTRY_KAKUSHI_TAG = "SwordsmithVillageEntryKakushi";

    private static final int PLAYER_CHECK_INTERVAL = 10;
    private static final long PROMPT_COOLDOWN_MS = 5000L;
    private static final long CONFIRM_TIMEOUT_MS = 30000L;
    private static final double KAKUSHI_DETECTION_RANGE = 5.0D;
    private static final double PROMPT_RANGE = 6.0D;
    private static final double RETURN_PROMPT_RADIUS = 8.0D;
    private static final double LOOK_DOT_THRESHOLD = 0.92D;

    private static final Map<UUID, Long> PROMPT_COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingEscort> PENDING_ESCORTS = new ConcurrentHashMap<>();

    private SwordsmithVillageEscortHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % PLAYER_CHECK_INTERVAL != 0) {
            return;
        }

        expirePendingEscort(player);
        handlePendingBlindfoldTeleport(player);
        maybePromptEscort(player);
        maybeEnsureVillageEntryKakushi(player);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null || event.getServer().getTickCount() % PLAYER_CHECK_INTERVAL != 0) {
            return;
        }

        ServerLevel village = event.getServer().getLevel(SWORDSMITH_VILLAGE_KEY);
        if (village == null) {
            return;
        }

        maintainVillageEntryKakushi(village);
    }

    public static boolean confirmEscort(ServerPlayer player) {
        PendingEscort pending = PENDING_ESCORTS.get(player.getUUID());
        if (pending == null) {
            player.sendSystemMessage(Component.literal("§cNo pending swordsmith village escort."));
            return false;
        }
        if (System.currentTimeMillis() > pending.expiresAtMs()) {
            PENDING_ESCORTS.remove(player.getUUID());
            player.sendSystemMessage(Component.literal("§cEscort request has expired."));
            return false;
        }
        if (!pending.kakushi().isAlive()) {
            PENDING_ESCORTS.remove(player.getUUID());
            player.sendSystemMessage(Component.literal("§cThe Kakushi is no longer nearby."));
            return false;
        }
        if (!player.level().dimension().location().equals(pending.originDimension())) {
            PENDING_ESCORTS.remove(player.getUUID());
            player.sendSystemMessage(Component.literal("§cYou moved too far away from the Kakushi."));
            return false;
        }

        if (pending.mode() == EscortMode.TO_VILLAGE) {
            sendKakushiMessage(player, "You must put on this blindfold");
        } else {
            sendKakushiMessage(player, "Put on your blindfold and I will take you back.");
        }

        player.getPersistentData().putString(ESCORT_PENDING_TAG, pending.mode().name());

        if (playerHasBlindfold(player)) {
            player.sendSystemMessage(Component.literal("§6[Kakushi] §ePut on your blindfold."));
        } else {
            dropBlindfoldForPlayer(player, pending.kakushi());
        }

        handlePendingBlindfoldTeleport(player);
        return true;
    }

    public static void cancelEscort(ServerPlayer player) {
        PENDING_ESCORTS.remove(player.getUUID());
        player.getPersistentData().remove(ESCORT_PENDING_TAG);
        player.sendSystemMessage(Component.literal("§eEscort cancelled."));
    }

    private static void maybePromptEscort(ServerPlayer player) {
        if (Damager.isDemon(player)) {
            return;
        }
        if (player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.BLINDFOLD.get())) {
            return;
        }

        PendingEscort pending = PENDING_ESCORTS.get(player.getUUID());
        if (pending != null && System.currentTimeMillis() <= pending.expiresAtMs()) {
            return;
        }

        if (isInSwordsmithVillage(player)) {
            if (!hasReturnPosition(player) || distanceToVillageEntry(player) > RETURN_PROMPT_RADIUS) {
                return;
            }

            Mob kakushi = findLookedAtKakushi(player, true, false);
            if (kakushi == null) {
                return;
            }

            maybeSendPrompt(player, kakushi, EscortMode.RETURN_TO_OVERWORLD,
                "Do you want to return to the overworld?");
            return;
        }

        if (!isHoldingNichirinOre(player)) {
            return;
        }

        Mob kakushi = findLookedAtKakushi(player, false, false);
        if (kakushi == null) {
            return;
        }

        maybeSendPrompt(player, kakushi, EscortMode.TO_VILLAGE,
            "do you want me to take you to the swordsmith village?");
    }

    private static void maybeSendPrompt(ServerPlayer player, Mob kakushi, EscortMode mode, String text) {
        long now = System.currentTimeMillis();
        Long lastPrompt = PROMPT_COOLDOWNS.get(player.getUUID());
        if (lastPrompt != null && now - lastPrompt < PROMPT_COOLDOWN_MS) {
            return;
        }

        PROMPT_COOLDOWNS.put(player.getUUID(), now);
        PENDING_ESCORTS.put(player.getUUID(), new PendingEscort(
            mode,
            kakushi,
            player.level().dimension().location(),
            now + CONFIRM_TIMEOUT_MS
        ));

        sendConfirmationMessage(player, text);
    }

    private static void handlePendingBlindfoldTeleport(ServerPlayer player) {
        String pendingMode = player.getPersistentData().getString(ESCORT_PENDING_TAG);
        if (pendingMode == null || pendingMode.isEmpty()) {
            return;
        }

        ItemStack headStack = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!headStack.is(ModItems.BLINDFOLD.get())) {
            return;
        }

        EscortMode mode;
        try {
            mode = EscortMode.valueOf(pendingMode);
        } catch (IllegalArgumentException ex) {
            player.getPersistentData().remove(ESCORT_PENDING_TAG);
            return;
        }

        if (mode == EscortMode.TO_VILLAGE) {
            teleportToVillage(player);
        } else {
            returnFromVillage(player);
        }

        PENDING_ESCORTS.remove(player.getUUID());
        player.getPersistentData().remove(ESCORT_PENDING_TAG);
    }

    private static void teleportToVillage(ServerPlayer player) {
        storeReturnPosition(player);
        ServerLevel village = player.getServer().getLevel(SWORDSMITH_VILLAGE_KEY);
        if (village == null) {
            player.sendSystemMessage(Component.literal("§cSwordsmith Village dimension not found."));
            return;
        }

        player.teleportTo(village, VILLAGE_ENTRY_X, VILLAGE_ENTRY_Y, VILLAGE_ENTRY_Z, VILLAGE_ENTRY_YAW, VILLAGE_ENTRY_PITCH);
        ensureVillageEntryKakushi(village);
        sendKakushiMessage(player, "You can remove your blindfold.");
    }

    private static void returnFromVillage(ServerPlayer player) {
        if (!hasReturnPosition(player)) {
            player.sendSystemMessage(Component.literal("§cNo return position stored."));
            return;
        }

        var returnData = player.getPersistentData().getCompound(RETURN_TAG);
        ResourceLocation dimId = ResourceLocation.tryParse(returnData.getString(RETURN_DIM));
        if (dimId == null) {
            player.sendSystemMessage(Component.literal("§cInvalid return destination."));
            player.getPersistentData().remove(RETURN_TAG);
            return;
        }

        ServerLevel target = player.getServer().getLevel(ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION, dimId));
        if (target == null) {
            player.sendSystemMessage(Component.literal("§cReturn dimension not found."));
            player.getPersistentData().remove(RETURN_TAG);
            return;
        }

        player.teleportTo(
            target,
            returnData.getDouble(RETURN_X),
            returnData.getDouble(RETURN_Y),
            returnData.getDouble(RETURN_Z),
            returnData.getFloat(RETURN_YAW),
            returnData.getFloat(RETURN_PITCH)
        );
        player.getPersistentData().remove(RETURN_TAG);
        sendKakushiMessage(player, "You can remove your blindfold.");
    }

    private static void maybeEnsureVillageEntryKakushi(ServerPlayer player) {
        if (!isInSwordsmithVillage(player)) {
            return;
        }
        ensureVillageEntryKakushi((ServerLevel) player.level());
    }

    private static void ensureVillageEntryKakushi(ServerLevel level) {
        Mob taggedKakushi = findTaggedEntryKakushi(level);
        if (taggedKakushi != null) {
            maintainEntryKakushiHome(taggedKakushi);
            return;
        }
        if (!level.isLoaded(VILLAGE_ENTRY_KAKUSHI_POS) || !hasNearbyVillageEntryPlayer(level)) {
            return;
        }

        List<Mob> nearbyKakushi = level.getEntitiesOfClass(
            Mob.class,
            new AABB(VILLAGE_ENTRY_KAKUSHI_POS).inflate(ENTRY_KAKUSHI_EXISTING_RADIUS),
            mob -> mob.isAlive() && KAKUSHI_ID.equals(EntityType.getKey(mob.getType()))
        );
        if (!nearbyKakushi.isEmpty()) {
            Mob adoptedKakushi = nearbyKakushi.stream()
                .min(java.util.Comparator.comparingDouble(mob -> mob.distanceToSqr(
                    VILLAGE_ENTRY_X + 0.5D,
                    VILLAGE_ENTRY_Y,
                    VILLAGE_ENTRY_Z + 0.5D
                )))
                .orElse(null);
            if (adoptedKakushi != null) {
                configureEntryKakushi(adoptedKakushi);
                maintainEntryKakushiHome(adoptedKakushi);
            }
            return;
        }

        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(KAKUSHI_ID).orElse(null);
        if (entityType == null) {
            return;
        }

        Entity created = entityType.create(level);
        if (!(created instanceof Mob kakushi)) {
            return;
        }

        kakushi.moveTo(VILLAGE_ENTRY_X + 0.5D, VILLAGE_ENTRY_Y, VILLAGE_ENTRY_Z + 0.5D, 180.0F, 0.0F);
        configureEntryKakushi(kakushi);
        kakushi.finalizeSpawn(level, level.getCurrentDifficultyAt(VILLAGE_ENTRY_KAKUSHI_POS), MobSpawnType.MOB_SUMMONED, null, null);
        level.addFreshEntity(kakushi);
    }

    private static void maintainVillageEntryKakushi(ServerLevel level) {
        if (!level.isLoaded(VILLAGE_ENTRY_KAKUSHI_POS)) {
            return;
        }
        Mob kakushi = findTaggedEntryKakushi(level);
        if (kakushi == null) {
            return;
        }
        maintainEntryKakushiHome(kakushi);
    }

    private static boolean hasNearbyVillageEntryPlayer(ServerLevel level) {
        double maxDistanceSqr = ENTRY_KAKUSHI_PLAYER_RADIUS * ENTRY_KAKUSHI_PLAYER_RADIUS;
        double centerX = VILLAGE_ENTRY_X + 0.5D;
        double centerY = VILLAGE_ENTRY_Y;
        double centerZ = VILLAGE_ENTRY_Z + 0.5D;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            if (player.distanceToSqr(centerX, centerY, centerZ) <= maxDistanceSqr) {
                return true;
            }
        }
        return false;
    }

    private static Mob findTaggedEntryKakushi(ServerLevel level) {
        for (Entity entity : level.getEntities().getAll()) {
            if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                continue;
            }
            if (!KAKUSHI_ID.equals(EntityType.getKey(mob.getType()))) {
                continue;
            }
            if (!mob.getPersistentData().getBoolean(ENTRY_KAKUSHI_TAG)) {
                continue;
            }
            return mob;
        }
        return null;
    }

    private static void configureEntryKakushi(Mob kakushi) {
        kakushi.setPersistenceRequired();
        kakushi.getPersistentData().putBoolean(ENTRY_KAKUSHI_TAG, true);
    }

    private static void maintainEntryKakushiHome(Mob kakushi) {
        double homeX = VILLAGE_ENTRY_X + 0.5D;
        double homeY = VILLAGE_ENTRY_Y;
        double homeZ = VILLAGE_ENTRY_Z + 0.5D;

        if (kakushi.distanceToSqr(homeX, homeY, homeZ) > ENTRY_KAKUSHI_MAX_DRIFT * ENTRY_KAKUSHI_MAX_DRIFT) {
            kakushi.getNavigation().moveTo(homeX, homeY, homeZ, ENTRY_KAKUSHI_HOME_SPEED);
            return;
        }

        kakushi.getNavigation().stop();
        kakushi.setDeltaMovement(Vec3.ZERO);
        kakushi.setYRot(VILLAGE_ENTRY_YAW);
        kakushi.setYHeadRot(VILLAGE_ENTRY_YAW);
        kakushi.setXRot(VILLAGE_ENTRY_PITCH);
    }

    private static Mob findLookedAtKakushi(ServerPlayer player, boolean requireEntryKakushi, boolean allowAnyInVillage) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        AABB searchBox = player.getBoundingBox().inflate(KAKUSHI_DETECTION_RANGE);
        List<Mob> kakushiList = player.level().getEntitiesOfClass(
            Mob.class,
            searchBox,
            mob -> mob.isAlive()
                && KAKUSHI_ID.equals(EntityType.getKey(mob.getType()))
                && (!requireEntryKakushi || mob.getPersistentData().getBoolean(ENTRY_KAKUSHI_TAG))
        );

        Mob best = null;
        double bestDot = LOOK_DOT_THRESHOLD;
        for (Mob kakushi : kakushiList) {
            if (!player.hasLineOfSight(kakushi)) {
                continue;
            }
            if (!allowAnyInVillage && isInSwordsmithVillage(player) && !kakushi.getPersistentData().getBoolean(ENTRY_KAKUSHI_TAG)) {
                continue;
            }
            Vec3 toKakushi = kakushi.getEyePosition().subtract(eyePos).normalize();
            double dot = look.dot(toKakushi);
            if (dot > bestDot && player.distanceToSqr(kakushi) <= PROMPT_RANGE * PROMPT_RANGE) {
                best = kakushi;
                bestDot = dot;
            }
        }
        return best;
    }

    private static void sendConfirmationMessage(ServerPlayer player, String question) {
        MutableComponent prefix = Component.literal("§6[Kakushi] §e" + question + " ");
        MutableComponent yes = Component.literal("§a§l[Yes]")
            .withStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/swordsmithvillage confirm"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Accept"))));
        MutableComponent no = Component.literal("§c§l[No]")
            .withStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/swordsmithvillage cancel"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Decline"))));

        player.sendSystemMessage(prefix.append(yes).append(Component.literal(" ")).append(no));
    }

    private static void sendKakushiMessage(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal("§6[Kakushi] §e" + message));
    }

    private static void dropBlindfoldForPlayer(ServerPlayer player, Mob kakushi) {
        ItemEntity blindfold = kakushi.spawnAtLocation(new ItemStack(ModItems.BLINDFOLD.get()));
        if (blindfold == null) {
            return;
        }

        Vec3 velocity = player.position().add(0.0D, 1.0D, 0.0D)
            .subtract(blindfold.position())
            .normalize()
            .scale(0.25D);
        blindfold.setDeltaMovement(velocity);
        blindfold.setPickUpDelay(0);
    }

    private static boolean playerHasBlindfold(Player player) {
        if (player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.BLINDFOLD.get())) {
            return true;
        }
        if (player.getOffhandItem().is(ModItems.BLINDFOLD.get())) {
            return true;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ModItems.BLINDFOLD.get())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHoldingNichirinOre(Player player) {
        return player.getMainHandItem().is(ModItems.NICHIRIN_ORE.get())
            || player.getOffhandItem().is(ModItems.NICHIRIN_ORE.get());
    }

    private static boolean isInSwordsmithVillage(Player player) {
        return player.level().dimension().location().equals(SWORDSMITH_VILLAGE_DIM_ID);
    }

    private static double distanceToVillageEntry(Player player) {
        return player.position().distanceTo(new Vec3(VILLAGE_ENTRY_X + 0.5D, VILLAGE_ENTRY_Y, VILLAGE_ENTRY_Z + 0.5D));
    }

    private static boolean hasReturnPosition(ServerPlayer player) {
        return player.getPersistentData().contains(RETURN_TAG);
    }

    private static void storeReturnPosition(ServerPlayer player) {
        var tag = player.getPersistentData().getCompound(RETURN_TAG);
        tag.putDouble(RETURN_X, player.getX());
        tag.putDouble(RETURN_Y, player.getY());
        tag.putDouble(RETURN_Z, player.getZ());
        tag.putFloat(RETURN_YAW, player.getYRot());
        tag.putFloat(RETURN_PITCH, player.getXRot());
        tag.putString(RETURN_DIM, player.level().dimension().location().toString());
        player.getPersistentData().put(RETURN_TAG, tag);
    }

    private static void expirePendingEscort(ServerPlayer player) {
        PendingEscort pending = PENDING_ESCORTS.get(player.getUUID());
        if (pending == null) {
            return;
        }
        if (System.currentTimeMillis() <= pending.expiresAtMs()) {
            return;
        }
        PENDING_ESCORTS.remove(player.getUUID());
        if (!player.getPersistentData().getString(ESCORT_PENDING_TAG).isEmpty()) {
            player.getPersistentData().remove(ESCORT_PENDING_TAG);
        }
    }

    private enum EscortMode {
        TO_VILLAGE,
        RETURN_TO_OVERWORLD
    }

    private record PendingEscort(EscortMode mode, Mob kakushi, ResourceLocation originDimension, long expiresAtMs) {
    }
}

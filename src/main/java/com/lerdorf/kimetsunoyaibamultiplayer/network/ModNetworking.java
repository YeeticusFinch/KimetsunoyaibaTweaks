package com.lerdorf.kimetsunoyaibamultiplayer.network;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.AnimationSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.AdjustPassiveSkillPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CloseDemonPropositionPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CombustibleBloodM1AttackPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CustomBdaPassiveAttackPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.DemonEyesSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BloodDemonArtBuilderActionPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.DemonPropositionResponsePacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.OrochiDismountPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.OpenDemonPropositionPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.OpenBloodDemonArtBuilderPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.OpenMeditationMenuPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.PuppetLineSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.RequestBloodDemonArtBuilderPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SetDemonPropositionStatePacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SetDemonEyesPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SelectMeditationTargetPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SetCrowQuestMarkerPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.WebTraversalInputPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetworking {
    private static SimpleChannel INSTANCE;

    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        // Register the packet for BOTH directions with the same ID
        // Use PLAY_TO_SERVER as primary registration
        int packetId = id();
        net.messageBuilder(AnimationSyncPacket.class, packetId)
                .decoder(AnimationSyncPacket::new)
                .encoder(AnimationSyncPacket::toBytes)
                .consumerMainThread(AnimationSyncPacket::handle)
                .add();

        // Register sword display sync packet
        int swordDisplayPacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwordDisplaySyncPacket.class, swordDisplayPacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwordDisplaySyncPacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwordDisplaySyncPacket::toBytes)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwordDisplaySyncPacket::handle)
                .add();

        // Register sword wielder capability sync packet (server -> client)
        int swordWielderPacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwordWielderSyncPacket.class, swordWielderPacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwordWielderSyncPacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwordWielderSyncPacket::toBytes)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwordWielderSyncPacket::handle)
                .add();

        // Register player rotation sync packet (server -> client)
        int rotationPacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.PlayerRotationSyncPacket.class, rotationPacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.PlayerRotationSyncPacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.PlayerRotationSyncPacket::toBytes)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.PlayerRotationSyncPacket::handle)
                .add();

        // Register breathing sword swing packet (client -> server)
        int breathingSwordSwingPacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BreathingSwordSwingPacket.class, breathingSwordSwingPacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BreathingSwordSwingPacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BreathingSwordSwingPacket::toBytes)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BreathingSwordSwingPacket::handle)
                .add();

        // Register breathing form cycle packet (client -> server)
        int cycleBreathingFormPacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleBreathingFormPacket.class, cycleBreathingFormPacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleBreathingFormPacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleBreathingFormPacket::encode)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleBreathingFormPacket::handle)
                .add();

        int cycleBloodDemonArtPacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleBloodDemonArtFormPacket.class, cycleBloodDemonArtPacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleBloodDemonArtFormPacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleBloodDemonArtFormPacket::encode)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleBloodDemonArtFormPacket::handle)
                .add();

        // Register sword model override packet (server -> client)
        int swordModelOverridePacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwordModelOverridePacket.class, swordModelOverridePacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwordModelOverridePacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwordModelOverridePacket::encode)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwordModelOverridePacket::handle)
                .add();

        // Register raw slash render packet (server -> client)
        int rawSlashRenderPacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.RawSlashRenderPacket.class, rawSlashRenderPacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.RawSlashRenderPacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.RawSlashRenderPacket::toBytes)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.RawSlashRenderPacket::handle)
                .add();

        // Register raw horizontal slash render packet (server -> client)
        int rawHorizontalSlashRenderPacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.RawHorizontalSlashRenderPacket.class, rawHorizontalSlashRenderPacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.RawHorizontalSlashRenderPacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.RawHorizontalSlashRenderPacket::toBytes)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.RawHorizontalSlashRenderPacket::handle)
                .add();

        // Register raw vertical slash render packet (server -> client)
        int rawVerticalSlashRenderPacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.RawVerticalSlashRenderPacket.class, rawVerticalSlashRenderPacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.RawVerticalSlashRenderPacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.RawVerticalSlashRenderPacket::toBytes)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.RawVerticalSlashRenderPacket::handle)
                .add();

        // Register mob animation sync packet (server -> client)
        int mobAnimationSyncPacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MobAnimationSyncPacket.class, mobAnimationSyncPacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MobAnimationSyncPacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MobAnimationSyncPacket::toBytes)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MobAnimationSyncPacket::handle)
                .add();

        // Register mob sword slash packet (server -> client)
        int mobSwordSlashPacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MobSwordSlashPacket.class, mobSwordSlashPacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MobSwordSlashPacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MobSwordSlashPacket::toBytes)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MobSwordSlashPacket::handle)
                .add();

        // Register whip animation packet (bidirectional)
        int whipAnimationPacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.WhipAnimationPacket.class, whipAnimationPacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.WhipAnimationPacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.WhipAnimationPacket::toBytes)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.WhipAnimationPacket::handle)
                .add();

        // Register form sync packet (server -> client)
        int formSyncPacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.FormSyncPacket.class, formSyncPacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.FormSyncPacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.FormSyncPacket::encode)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.FormSyncPacket::handle)
                .add();

        // Register cycle form variation packet (client -> server)
        int cycleFormVariationPacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleFormVariationPacket.class, cycleFormVariationPacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleFormVariationPacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleFormVariationPacket::encode)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CycleFormVariationPacket::handle)
                .add();

        // NOTE: VariationSyncPacket removed - variations are now encoded in breathes value

        // Register spawn love sword slashes packet (server -> client)
        int spawnLoveSwordSlashesPacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SpawnLoveSwordSlashesPacket.class, spawnLoveSwordSlashesPacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SpawnLoveSwordSlashesPacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SpawnLoveSwordSlashesPacket::toBytes)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SpawnLoveSwordSlashesPacket::handle)
                .add();

        int updateLoveSwordSlashesPacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.UpdateLoveSwordSlashesPacket.class, updateLoveSwordSlashesPacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.UpdateLoveSwordSlashesPacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.UpdateLoveSwordSlashesPacket::toBytes)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.UpdateLoveSwordSlashesPacket::handle)
                .add();

        // Register spawn love tornado packet (server -> client)
        int spawnLoveTornadoPacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SpawnLoveTornadoPacket.class, spawnLoveTornadoPacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SpawnLoveTornadoPacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SpawnLoveTornadoPacket::toBytes)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SpawnLoveTornadoPacket::handle)
                .add();

        // Register breathes value sync packet (server -> client)
        int breathesValueSyncPacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BreathesValueSyncPacket.class, breathesValueSyncPacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BreathesValueSyncPacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BreathesValueSyncPacket::toBytes)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BreathesValueSyncPacket::handle)
                .add();

        // Register variation index sync packet (server -> client)
        int variationIndexSyncPacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.VariationIndexSyncPacket.class, variationIndexSyncPacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.VariationIndexSyncPacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.VariationIndexSyncPacket::encode)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.VariationIndexSyncPacket::handle)
                .add();

        // Register survival raid boss arrow packet (server -> client)
        int bossArrowPacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BossArrowPacket.class, bossArrowPacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BossArrowPacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BossArrowPacket::toBytes)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BossArrowPacket::handle)
                .add();

        int orochiDismountPacketId = id();
        net.messageBuilder(OrochiDismountPacket.class, orochiDismountPacketId)
                .decoder(OrochiDismountPacket::new)
                .encoder(OrochiDismountPacket::toBytes)
                .consumerMainThread(OrochiDismountPacket::handle)
                .add();

        int bleedingFlashPacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BleedingFlashPacket.class, bleedingFlashPacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BleedingFlashPacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BleedingFlashPacket::toBytes)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BleedingFlashPacket::handle)
                .add();

        int meditationOpenPacketId = id();
        net.messageBuilder(OpenMeditationMenuPacket.class, meditationOpenPacketId)
                .decoder(OpenMeditationMenuPacket::new)
                .encoder(OpenMeditationMenuPacket::toBytes)
                .consumerMainThread(OpenMeditationMenuPacket::handle)
                .add();

        int bloodDemonArtBuilderOpenPacketId = id();
        net.messageBuilder(OpenBloodDemonArtBuilderPacket.class, bloodDemonArtBuilderOpenPacketId)
                .decoder(OpenBloodDemonArtBuilderPacket::new)
                .encoder(OpenBloodDemonArtBuilderPacket::toBytes)
                .consumerMainThread(OpenBloodDemonArtBuilderPacket::handle)
                .add();

        int requestBloodDemonArtBuilderPacketId = id();
        net.messageBuilder(RequestBloodDemonArtBuilderPacket.class, requestBloodDemonArtBuilderPacketId)
                .decoder(RequestBloodDemonArtBuilderPacket::new)
                .encoder(RequestBloodDemonArtBuilderPacket::encode)
                .consumerMainThread(RequestBloodDemonArtBuilderPacket::handle)
                .add();

        int bloodDemonArtBuilderActionPacketId = id();
        net.messageBuilder(BloodDemonArtBuilderActionPacket.class, bloodDemonArtBuilderActionPacketId)
                .decoder(BloodDemonArtBuilderActionPacket::new)
                .encoder(BloodDemonArtBuilderActionPacket::encode)
                .consumerMainThread(BloodDemonArtBuilderActionPacket::handle)
                .add();

        int meditationSelectionPacketId = id();
        net.messageBuilder(SelectMeditationTargetPacket.class, meditationSelectionPacketId)
                .decoder(SelectMeditationTargetPacket::new)
                .encoder(SelectMeditationTargetPacket::toBytes)
                .consumerMainThread(SelectMeditationTargetPacket::handle)
                .add();

        int adjustPassiveSkillPacketId = id();
        net.messageBuilder(AdjustPassiveSkillPacket.class, adjustPassiveSkillPacketId)
                .decoder(AdjustPassiveSkillPacket::new)
                .encoder(AdjustPassiveSkillPacket::toBytes)
                .consumerMainThread(AdjustPassiveSkillPacket::handle)
                .add();

        int customBdaPassiveAttackPacketId = id();
        net.messageBuilder(CustomBdaPassiveAttackPacket.class, customBdaPassiveAttackPacketId)
                .decoder(CustomBdaPassiveAttackPacket::new)
                .encoder(CustomBdaPassiveAttackPacket::toBytes)
                .consumerMainThread(CustomBdaPassiveAttackPacket::handle)
                .add();

        int combustibleBloodM1AttackPacketId = id();
        net.messageBuilder(CombustibleBloodM1AttackPacket.class, combustibleBloodM1AttackPacketId)
                .decoder(CombustibleBloodM1AttackPacket::new)
                .encoder(CombustibleBloodM1AttackPacket::toBytes)
                .consumerMainThread(CombustibleBloodM1AttackPacket::handle)
                .add();

        int swampPuddleStatePacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwampPuddleStatePacket.class, swampPuddleStatePacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwampPuddleStatePacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwampPuddleStatePacket::toBytes)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwampPuddleStatePacket::handle)
                .add();

        int nezukoBoxSyncPacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.NezukoBoxSyncPacket.class, nezukoBoxSyncPacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.NezukoBoxSyncPacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.NezukoBoxSyncPacket::toBytes)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.NezukoBoxSyncPacket::handle)
                .add();

        int debugPlayerDimensionsPacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.DebugPlayerDimensionsPacket.class, debugPlayerDimensionsPacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.DebugPlayerDimensionsPacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.DebugPlayerDimensionsPacket::toBytes)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.DebugPlayerDimensionsPacket::handle)
                .add();

        int crowQuestMarkerPacketId = id();
        net.messageBuilder(SetCrowQuestMarkerPacket.class, crowQuestMarkerPacketId)
                .decoder(SetCrowQuestMarkerPacket::new)
                .encoder(SetCrowQuestMarkerPacket::toBytes)
                .consumerMainThread(SetCrowQuestMarkerPacket::handle)
                .add();

        int openDemonPropositionPacketId = id();
        net.messageBuilder(OpenDemonPropositionPacket.class, openDemonPropositionPacketId)
                .decoder(OpenDemonPropositionPacket::new)
                .encoder(OpenDemonPropositionPacket::toBytes)
                .consumerMainThread(OpenDemonPropositionPacket::handle)
                .add();

        int demonPropositionResponsePacketId = id();
        net.messageBuilder(DemonPropositionResponsePacket.class, demonPropositionResponsePacketId)
                .decoder(DemonPropositionResponsePacket::new)
                .encoder(DemonPropositionResponsePacket::toBytes)
                .consumerMainThread(DemonPropositionResponsePacket::handle)
                .add();

        int closeDemonPropositionPacketId = id();
        net.messageBuilder(CloseDemonPropositionPacket.class, closeDemonPropositionPacketId)
                .decoder(CloseDemonPropositionPacket::new)
                .encoder(CloseDemonPropositionPacket::toBytes)
                .consumerMainThread(CloseDemonPropositionPacket::handle)
                .add();

        int setDemonPropositionStatePacketId = id();
        net.messageBuilder(SetDemonPropositionStatePacket.class, setDemonPropositionStatePacketId)
                .decoder(SetDemonPropositionStatePacket::new)
                .encoder(SetDemonPropositionStatePacket::toBytes)
                .consumerMainThread(SetDemonPropositionStatePacket::handle)
                .add();

        int demonEyesSyncPacketId = id();
        net.messageBuilder(DemonEyesSyncPacket.class, demonEyesSyncPacketId)
                .decoder(DemonEyesSyncPacket::new)
                .encoder(DemonEyesSyncPacket::toBytes)
                .consumerMainThread(DemonEyesSyncPacket::handle)
                .add();

        int setDemonEyesPacketId = id();
        net.messageBuilder(SetDemonEyesPacket.class, setDemonEyesPacketId)
                .decoder(SetDemonEyesPacket::new)
                .encoder(SetDemonEyesPacket::toBytes)
                .consumerMainThread(SetDemonEyesPacket::handle)
                .add();

        int updateBridgerBlockPacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.UpdateBridgerBlockPacket.class, updateBridgerBlockPacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.UpdateBridgerBlockPacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.UpdateBridgerBlockPacket::toBytes)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.UpdateBridgerBlockPacket::handle)
                .add();

        int updateGravityBlockPacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.UpdateGravityBlockPacket.class, updateGravityBlockPacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.UpdateGravityBlockPacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.UpdateGravityBlockPacket::toBytes)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.UpdateGravityBlockPacket::handle)
                .add();

        int demonRankSyncPacketId = id();
        net.messageBuilder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.DemonRankSyncPacket.class, demonRankSyncPacketId)
                .decoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.DemonRankSyncPacket::new)
                .encoder(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.DemonRankSyncPacket::toBytes)
                .consumerMainThread(com.lerdorf.kimetsunoyaibamultiplayer.network.packets.DemonRankSyncPacket::handle)
                .add();

        int puppetLineSyncPacketId = id();
        net.messageBuilder(PuppetLineSyncPacket.class, puppetLineSyncPacketId)
                .decoder(PuppetLineSyncPacket::new)
                .encoder(PuppetLineSyncPacket::toBytes)
                .consumerMainThread(PuppetLineSyncPacket::handle)
                .add();

        int webTraversalInputPacketId = id();
        net.messageBuilder(WebTraversalInputPacket.class, webTraversalInputPacketId)
                .decoder(WebTraversalInputPacket::new)
                .encoder(WebTraversalInputPacket::toBytes)
                .consumerMainThread(WebTraversalInputPacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToAllClients(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }

    public static <MSG> void sendToAllClientsExcept(MSG message, ServerPlayer excludePlayer) {
        for (ServerPlayer player : excludePlayer.server.getPlayerList().getPlayers()) {
            if (!player.equals(excludePlayer)) {
                sendToPlayer(message, player);
            }
        }
    }

    public static <MSG> void sendToNearby(MSG message, net.minecraft.server.level.ServerLevel level,
                                          double x, double y, double z, double radius) {
        INSTANCE.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(x, y, z, radius, level.dimension())), message);
    }

    public static <MSG> void sendToTrackingAndSelf(MSG message, net.minecraft.world.entity.Entity entity) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), message);
    }
}

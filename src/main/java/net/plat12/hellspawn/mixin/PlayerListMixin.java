package net.plat12.hellspawn.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.plat12.hellspawn.util.savedata.SpawnDimensionSaveData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerList.class)
public class PlayerListMixin {

    @Redirect(method = "respawn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;findRespawnPositionAndUseSpawnBlock(ZLnet/minecraft/world/level/portal/DimensionTransition$PostDimensionTransition;)Lnet/minecraft/world/level/portal/DimensionTransition;"))
    private DimensionTransition redirectRespawnPosition(ServerPlayer player, boolean keepInventory, DimensionTransition.PostDimensionTransition postTransition) {
        DimensionTransition originalTransition = player.findRespawnPositionAndUseSpawnBlock(keepInventory, postTransition);
        if (originalTransition.newLevel().dimension().equals(Level.OVERWORLD)
                && originalTransition.missingRespawnBlock()) {
            PlayerList playerList = (PlayerList) (Object) this;
            MinecraftServer server = playerList.getServer();
            ServerLevel overworldLevel = server.overworld();
            SpawnDimensionSaveData saveData = SpawnDimensionSaveData.getOrCreate(overworldLevel);

            ResourceKey<Level> customSpawnDimension = saveData.getSpawnDimension();
            ServerLevel customSpawnLevel = server.getLevel(customSpawnDimension);
            BlockPos customSpawnPos = saveData.getDimensionSpawnPos();
            if (customSpawnDimension != null &&
                    customSpawnLevel != null &&
                    customSpawnPos != null &&
                    saveData.hasPlayerJoined(player)) {

                return new DimensionTransition(
                        customSpawnLevel,
                        Vec3.atBottomCenterOf(customSpawnPos),
                        Vec3.ZERO,
                        0.0F,
                        0.0F,
                        postTransition
                );
            }
        }

        return originalTransition;
    }
}
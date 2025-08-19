package net.plat12.hellspawn.event;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.plat12.hellspawn.util.ModUtils;
import net.plat12.hellspawn.util.savedata.SpawnDimensionSaveData;


@EventBusSubscriber
public class ModEvents {
    @SubscribeEvent
    public static void sendPlayerToDimension(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.level() instanceof ServerLevel level
                && !SpawnDimensionSaveData.getOrCreate(level).hasPlayerJoined(player)) {
            SpawnDimensionSaveData data = SpawnDimensionSaveData.getOrCreate(level);
            boolean isFirstJoiner = !data.hasWorldBeenJoined();
            ResourceKey<Level> dimension = isFirstJoiner ? ModUtils.selectedDimension : data.getSpawnDimension();
            if (isFirstJoiner) {
                ModUtils.selectedDimension = null;
            }
            ModUtils.sendToDimension(level, player, dimension, isFirstJoiner, data);
        }
    }


}
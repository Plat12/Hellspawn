package net.plat12.hellspawn.util;

import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.LevelStem;
import net.plat12.hellspawn.Hellspawn;
import net.plat12.hellspawn.util.savedata.SpawnDimensionSaveData;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ModUtils {
    public static final List<ResourceKey<Level>> ORDERED_VANILLA_DIMENSIONS = List.of(Level.OVERWORLD, Level.NETHER, Level.END);
    public static final String DIMENSION_PREFIX = "dimension";
    public static final String DESCRIPTION_PREFIX = "description";
    public static final String FULL_DESCRIPTION_PREFIX =
            Hellspawn.MOD_ID + "." + DIMENSION_PREFIX + "." + DESCRIPTION_PREFIX;
    public static ResourceKey<Level> selectedDimension = null;

    public static List<ResourceKey<Level>> getAllDimensions(WorldCreationUiState uiState) {
        Set<ResourceKey<Level>> result = new HashSet<>();
        try {
            WorldCreationContext settings = uiState.getSettings();

            for (Map.Entry<ResourceKey<LevelStem>, LevelStem> entry : settings.selectedDimensions().dimensions().entrySet()) {
                result.add(dimensionLocationToResourceKey(entry.getKey().location()));
            }

            for (Map.Entry<ResourceKey<LevelStem>, LevelStem> entry : settings.datapackDimensions().entrySet()) {
                result.add(dimensionLocationToResourceKey(entry.getKey().location()));
            }
        } catch (Exception e) {
            System.err.println("Error loading dimensions");
            e.printStackTrace();
        }

        return sortDimensions(result);
    }


    public static List<ResourceKey<Level>> sortDimensions(Set<ResourceKey<Level>> dimensions) {

        Stream<ResourceKey<Level>> vanillaDimensions = ORDERED_VANILLA_DIMENSIONS.stream()
                .filter(dimensions::contains);

        Stream<ResourceKey<Level>> nonVanillaDimensions = dimensions.stream()
                .filter(d -> !ORDERED_VANILLA_DIMENSIONS.contains(d))
                .sorted(Comparator.comparing(d -> getDimensionName(d).getString()));

        return Stream.concat(vanillaDimensions, nonVanillaDimensions)
                .filter(Objects::nonNull).collect(Collectors.toList());
    }


    public static ResourceKey<Level> dimensionLocationToResourceKey(ResourceLocation location) {
        return ResourceKey.create(Registries.DIMENSION, location);
    }

    public static Component getDimensionName(ResourceKey<Level> dimension) {
        return Component.translatable(DIMENSION_PREFIX + "." + getBaseDimensionTranslationKey(dimension));
    }

    public static String getBaseDimensionTranslationKey(ResourceKey<Level> dimension) {
        return dimension.location().toString().replace(':', '.');
    }

    public static Component getDimensionDescription(ResourceKey<Level> dimension) {
        return Component.translatable(FULL_DESCRIPTION_PREFIX + "." + getBaseDimensionTranslationKey(dimension));
    }

    public static void sendToDimension(ServerLevel origin, ServerPlayer player, ResourceKey<Level> dimension,
                                       boolean isFirstJoiner, SpawnDimensionSaveData data) {
        ServerLevel targetLevel = origin.getServer().getLevel(dimension);

        if (targetLevel == null || player.level() == targetLevel) {
            return;
        }

        BlockPos spawnPos = isFirstJoiner ? getValidSpawnPosition(targetLevel) : data.getDimensionSpawnPos();

        player.teleportTo(targetLevel,
                spawnPos.getX() + 0.5,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5,
                player.getYRot(),
                player.getXRot()
        );
        player.setRespawnPosition(dimension, spawnPos, 0.0f, true, false);
        if (isFirstJoiner) {
            targetLevel.setDefaultSpawnPos(spawnPos, 0.0f);
            data.setSpawnDimension(dimension);
            data.setDimensionSpawnPos(spawnPos);
        }
        data.joinWorld(player);
    }

    private static @NotNull BlockPos getValidSpawnPosition(ServerLevel level) {
        int x = 0;
        int z = 0;
        int direction = 0; // 0=right, 1=down, 2=left, 3=up
        int stepsInDirection = 0;
        int maxStepsInDirection = 1;
        BlockPos spawnPos = null;
        LevelChunk spawnChunk;
        RandomSource random = level.random;
        while (true) {
            spawnChunk = level.getChunk(x, z);
            spawnChunk.setLoaded(true);
            spawnPos = findValidSpawnPositionInChunk(spawnChunk, random, level);

            if (spawnPos != null) {
                break;
            }

            // Move to next position in spiral
            switch (direction) {
                case 0 -> // right
                        x++;
                case 1 -> // down
                        z++;
                case 2 -> // left
                        x--;
                case 3 -> // up
                        z--;
            }

            stepsInDirection++;

            // Check if we need to turn
            if (stepsInDirection == maxStepsInDirection) {
                stepsInDirection = 0;
                direction = (direction + 1) % 4;

                // Increase the spiral radius every two direction changes
                if (direction == 0 || direction == 2) {
                    maxStepsInDirection++;
                }
            }
        }
        return spawnPos;
    }


    private static BlockPos findValidSpawnPositionInChunk(LevelChunk spawnChunk, RandomSource random, Level level) {
        List<BlockPos> validPositions = new ArrayList<>();
        List<BlockPos> skylitPositions = new ArrayList<>();
        boolean hasSkylightInChunk = false;

        // First pass: check if chunk has any skylight and collect all valid positions
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = spawnChunk.getMinBuildHeight(); y < spawnChunk.getMaxBuildHeight() - 1; y++) {
                    if (y <= spawnChunk.getMinBuildHeight()) {
                        continue; // Skip if we can't check below
                    }
                    BlockPos pos = new BlockPos(
                            spawnChunk.getPos().getMinBlockX() + x, y, spawnChunk.getPos().getMinBlockZ() + z);
                    BlockPos above = pos.above();
                    BlockPos below = pos.below();

                    // Check collision boxes
                    BlockState currentState = spawnChunk.getBlockState(pos);
                    BlockState aboveState = spawnChunk.getBlockState(above);
                    BlockState belowState = spawnChunk.getBlockState(below);


                    if (!currentState.getCollisionShape(spawnChunk, pos).isEmpty()
                            || !aboveState.getCollisionShape(spawnChunk, above).isEmpty()
                            || belowState.getCollisionShape(spawnChunk, below).isEmpty()
                            || !spawnChunk.getFluidState(pos).isEmpty()
                            || !spawnChunk.getFluidState(above).isEmpty()
                            || belowState.is(Blocks.BEDROCK)) {
                        continue;
                    }

                    int skyLight = level.getBrightness(LightLayer.SKY, pos);
                    if (skyLight > 0) {
                        hasSkylightInChunk = true;
                        skylitPositions.add(pos);
                    } else {
                        validPositions.add(pos);
                    }
                }
            }
        }
        if (hasSkylightInChunk && !skylitPositions.isEmpty()) {
            return skylitPositions.get(random.nextInt(skylitPositions.size()));
        }

        if (validPositions.isEmpty()) {
            return null; // No valid positions found
        }

        // No skylight in chunk OR no valid skylit positions found
        // Select semi-randomly from bottom up
        validPositions.sort(Comparator.comparingInt(Vec3i::getY));

        // Use weighted random selection favoring lower Y positions
        int totalWeight = validPositions.size() * (validPositions.size() + 1) / 2;
        int randomWeight = random.nextInt(totalWeight);

        int currentWeight = 0;
        for (int i = 0; i < validPositions.size(); i++) {
            currentWeight += validPositions.size() - i;
            if (randomWeight < currentWeight) {
                return validPositions.get(i);
            }
        }

        // Fallback (shouldn't reach here)
        return validPositions.getFirst();
    }

}

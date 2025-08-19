package net.plat12.hellspawn.util.savedata;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.plat12.hellspawn.Hellspawn;

import java.util.*;

public class SpawnDimensionSaveData extends SavedData {
    public static final String DATA_KEY = Hellspawn.MOD_ID + "_spawn_dimension";
    public static final SavedData.Factory<SpawnDimensionSaveData> FACTORY = new SavedData.Factory<>(
            SpawnDimensionSaveData::create,
            SpawnDimensionSaveData::load,
            null);

    private ResourceKey<Level> spawnDimension;
    private BlockPos dimensionSpawnPos;
    private final Set<UUID> joinedPlayers = new HashSet<>();

    public static SpawnDimensionSaveData getOrCreate(ServerLevel world) {
        return world.getServer().getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(FACTORY, DATA_KEY);
    }

    public static SpawnDimensionSaveData load(CompoundTag nbt, HolderLookup.Provider registries) {
        SpawnDimensionSaveData saveData = create();

        // Load spawn dimension if present
        if (nbt.contains("spawnDimension", Tag.TAG_STRING)) {
            ResourceLocation dimensionId = ResourceLocation.tryParse(nbt.getString("spawnDimension"));
            if (dimensionId != null) {
                saveData.spawnDimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
            }
        }

        // Load dimension spawn position if present
        if (nbt.contains("dimensionSpawnPos", Tag.TAG_COMPOUND)) {
            CompoundTag posTag = nbt.getCompound("dimensionSpawnPos");
            saveData.dimensionSpawnPos = new BlockPos(
                    posTag.getInt("x"),
                    posTag.getInt("y"),
                    posTag.getInt("z")
            );
        }

        // Load joined players
        ListTag playersTag = nbt.getList("joinedPlayers", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < playersTag.size(); i++) {
            int[] uuidArray = playersTag.getIntArray(i);
            if (uuidArray.length == 4) {
                UUID playerUUID = NbtUtils.loadUUID(new IntArrayTag(uuidArray));
                saveData.joinedPlayers.add(playerUUID);
            }
        }

        return saveData;
    }

    public static SpawnDimensionSaveData create() {
        return new SpawnDimensionSaveData();
    }

    @Override
    public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider) {
        // Save spawn dimension
        if (spawnDimension != null) {
            compoundTag.putString("spawnDimension", spawnDimension.location().toString());
        }

        // Save dimension spawn position
        if (dimensionSpawnPos != null) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", dimensionSpawnPos.getX());
            posTag.putInt("y", dimensionSpawnPos.getY());
            posTag.putInt("z", dimensionSpawnPos.getZ());
            compoundTag.put("dimensionSpawnPos", posTag);
        }

        // Save joined players
        ListTag playersTag = new ListTag();
        for (UUID playerUUID : joinedPlayers) {
            playersTag.add(NbtUtils.createUUID(playerUUID));
        }
        compoundTag.put("joinedPlayers", playersTag);

        return compoundTag;
    }

    /**
     * Sets the spawn dimension. Can only be set once - subsequent calls will be ignored.
     * @param dimension the dimension to set as spawn
     * @return true if the dimension was set, false if it was already set
     */
    public boolean setSpawnDimension(ResourceKey<Level> dimension) {
        if (spawnDimension == null && dimension != null) {
            spawnDimension = dimension;
            setDirty(true);
            return true;
        }
        return false;
    }

    /**
     * Sets the dimension spawn position. Can only be set once - subsequent calls will be ignored.
     * @param pos the position to set as spawn
     * @return true if the position was set, false if it was already set
     */
    public boolean setDimensionSpawnPos(BlockPos pos) {
        if (dimensionSpawnPos == null && pos != null) {
            dimensionSpawnPos = pos.immutable();
            setDirty(true);
            return true;
        }
        return false;
    }

    /**
     * Adds a player to the joined players list. Players can only be added, not removed.
     * @param playerUUID the UUID of the player to add
     * @return true if the player was added, false if they were already in the list
     */
    public boolean addJoinedPlayer(UUID playerUUID) {
        if (playerUUID != null && !joinedPlayers.contains(playerUUID)) {
            joinedPlayers.add(playerUUID);
            setDirty(true);
            return true;
        }
        return false;
    }

    public boolean joinWorld(Player joinedPlayer) {
        return this.addJoinedPlayer(joinedPlayer.getUUID());
    }

    /**
     * Gets the spawn dimension. May be null if not set.
     */
    public ResourceKey<Level> getSpawnDimension() {
        return spawnDimension;
    }

    /**
     * Gets the dimension spawn position. May be null if not set.
     */
    public BlockPos getDimensionSpawnPos() {
        return dimensionSpawnPos;
    }

    /**
     * Gets a copy of the joined players list to prevent external modification.
     */
    public List<UUID> getJoinedPlayers() {
        return new ArrayList<>(joinedPlayers);
    }

    /**
     * Checks if a player has joined before.
     */
    public boolean hasPlayerJoined(UUID playerUUID) {
        return joinedPlayers.contains(playerUUID);
    }

    public boolean hasPlayerJoined(Player player) {
        return joinedPlayers.contains(player.getUUID());
    }

    public boolean hasWorldBeenJoined() {
        return !this.joinedPlayers.isEmpty();
    }

    /**
     * Gets the number of players that have joined.
     */
    public int getJoinedPlayersCount() {
        return joinedPlayers.size();
    }
}
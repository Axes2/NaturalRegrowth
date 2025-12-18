package net.axes.naturalregrowth.block.entity;

import net.axes.naturalregrowth.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class RegrowingStumpBlockEntity extends BlockEntity {

    // DEFAULT VALUES: If we forget who we are, act like Oak.
    private BlockState mimicState = Blocks.STRIPPED_OAK_WOOD.defaultBlockState();
    private BlockState futureSapling = Blocks.OAK_SAPLING.defaultBlockState();
    private long creationTime = 0L;

    public RegrowingStumpBlockEntity(BlockPos pos, BlockState blockState) {
        // We reference the registry object here (created in Step 2)
        super(ModBlocks.REGROWING_STUMP_BE.get(), pos, blockState);
    }

    public void setCreationTime(long time) {
        this.creationTime = time;
        this.setChanged();
    }

    public long getCreationTime() {
        return creationTime;
    }

    // --- 1. DATA MANAGEMENT ---
    // This is how we tell the stump what to look like
    public void setMimic(BlockState strippedWood, BlockState sapling) {
        this.mimicState = strippedWood;
        this.futureSapling = sapling;
        this.setChanged(); // Mark "Dirty" so Minecraft knows to save this to disk

        // Force the client to update immediately (so the texture changes instantly)
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public BlockState getMimicState() {
        return mimicState;
    }

    public BlockState getFutureSapling() {
        return futureSapling;
    }

    // --- 2. SAVING & LOADING (NBT) ---
    // This runs when you quit the game. It saves our "Redwood" identity to the world file.
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("MimicState", NbtUtils.writeBlockState(mimicState));
        tag.put("FutureSapling", NbtUtils.writeBlockState(futureSapling));
        tag.putLong("CreationTime", creationTime);
    }

    // This runs when you load the world. It reads the identity back from the file.
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("MimicState")) {
            // Fix: Get the BLOCK registry specifically
            this.mimicState = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("MimicState"));
        }

        if (tag.contains("FutureSapling")) {
            // Fix: Same here
            this.futureSapling = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("FutureSapling"));
        }
        if (tag.contains("CreationTime")) {
            this.creationTime = tag.getLong("CreationTime"); // Load it!
        }
    }

    // --- 3. NETWORK SYNC (The "Chameleon" Effect) ---
    // These two methods ensure the Client (your screen) knows what the Server (the game logic) knows.
    // Without this, the stump would look like Oak to you, even if the server knew it was Redwood.

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
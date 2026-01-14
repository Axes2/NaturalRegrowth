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

public class HealingBlockEntity extends BlockEntity {

    private BlockState originalState = Blocks.AIR.defaultBlockState();
    private BlockState renderState = Blocks.AIR.defaultBlockState(); // <--- NEW

    public HealingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.HEALING_BE.get(), pos, state);
    }

    public void setStates(BlockState original, BlockState visual) {
        this.originalState = original;
        this.renderState = visual;
        this.setChanged();
        // Force chunk update so client sees the new texture immediately
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public BlockState getOriginalState() {
        return originalState;
    }

    public BlockState getRenderState() {
        return renderState;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("OriginalBlock", NbtUtils.writeBlockState(originalState));
        tag.put("RenderBlock", NbtUtils.writeBlockState(renderState)); // <--- SAVE
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("OriginalBlock")) {
            this.originalState = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("OriginalBlock"));
        }
        if (tag.contains("RenderBlock")) {
            this.renderState = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("RenderBlock")); // <--- LOAD
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        // OPTIMIZATION: Only send the Render State to the client.
        // The client renderer doesn't need to know the 'OriginalState' (regrowth logic).
        CompoundTag tag = new CompoundTag();
        tag.put("RenderBlock", NbtUtils.writeBlockState(renderState));
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
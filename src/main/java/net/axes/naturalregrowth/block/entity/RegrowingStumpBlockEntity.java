package net.axes.naturalregrowth.block.entity;

import net.axes.naturalregrowth.Config;
import net.axes.naturalregrowth.ModBlocks;
import net.axes.naturalregrowth.block.RegrowingStumpBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import javax.annotation.Nullable;

public class RegrowingStumpBlockEntity extends BlockEntity {

    private BlockState mimicState = Blocks.STRIPPED_OAK_WOOD.defaultBlockState();
    private BlockState futureSapling = Blocks.OAK_SAPLING.defaultBlockState();
    private long creationTime = 0L;

    public RegrowingStumpBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlocks.REGROWING_STUMP_BE.get(), pos, blockState);
    }

    public void setCreationTime(long time) {
        this.creationTime = time;
        this.setChanged();
    }

    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && Config.COMMON.catchUpGrowth.get()) {
            performCatchUpLogic();
        }
    }

    private void performCatchUpLogic() {
        long currentTime = level.getGameTime();
        long age = currentTime - this.creationTime;
        long delay = Config.COMMON.regrowthDelay.get();

        if (age < delay) return;

        long eligibleTicks = age - delay;
        if (eligibleTicks <= 0) return;

        int randomTickSpeed = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        if (randomTickSpeed <= 0) return;

        double chanceToBePicked = (double) randomTickSpeed / 4096.0;
        double chanceToGrow = Config.COMMON.regrowthChance.get();
        double p = chanceToBePicked * chanceToGrow;

        double probOfSuccess = 1.0 - Math.pow(1.0 - p, eligibleTicks);

        if (level.random.nextDouble() < probOfSuccess) {
            growIntoSapling();
        }
    }

    private void growIntoSapling() {
        if (level instanceof ServerLevel serverLevel) {
            BlockPos pos = this.getBlockPos();

            // 1. ALWAYS Clean up the old tree first (Flood Fill)
            // This runs for both modes to ensure no floating debris
            RegrowingStumpBlock.destroyTreeFloodFill(serverLevel, pos.above());

            // 2. Prepare the Sapling
            BlockState saplingState = this.getFutureSapling();

            // Set to Stage 1 (Ready to grow)
            if (saplingState.hasProperty(BlockStateProperties.STAGE)) {
                saplingState = saplingState.setValue(BlockStateProperties.STAGE, 1);
            }

            // 3. Place the sapling
            serverLevel.setBlock(pos, saplingState, 3);

            // 4. CHECK CONFIG: Do we force instant growth?
            if (Config.COMMON.instantCatchUp.get()) {
                Block block = saplingState.getBlock();

                if (block instanceof net.minecraft.world.level.block.BonemealableBlock growable) {
                    if (growable.isValidBonemealTarget(serverLevel, pos, saplingState)) {
                        try {
                            // The "Bonemeal Trick"
                            // Since we set STAGE=1 above, this will trigger the tree generation immediately.
                            growable.performBonemeal(serverLevel, serverLevel.random, pos, saplingState);
                        } catch (Exception e) {
                            // If a modded tree crashes during gen, catch it so we don't crash the chunk load
                            System.err.println("Natural Regrowth: Failed to instant-grow tree at " + pos + ": " + e.getMessage());
                        }
                    }
                }
            }
        }
    }

    // --- STANDARD METHODS (SetMimic, Save, Load, Packet) ---
    // (These remain exactly the same as your previous version)

    public void setMimic(BlockState strippedWood, BlockState sapling) {
        this.mimicState = strippedWood;
        this.futureSapling = sapling;
        this.setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public BlockState getMimicState() { return mimicState; }
    public BlockState getFutureSapling() { return futureSapling; }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("MimicState", NbtUtils.writeBlockState(mimicState));
        tag.put("FutureSapling", NbtUtils.writeBlockState(futureSapling));
        tag.putLong("CreationTime", creationTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("MimicState")) this.mimicState = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("MimicState"));
        if (tag.contains("FutureSapling")) this.futureSapling = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("FutureSapling"));
        if (tag.contains("CreationTime")) this.creationTime = tag.getLong("CreationTime");
    }

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
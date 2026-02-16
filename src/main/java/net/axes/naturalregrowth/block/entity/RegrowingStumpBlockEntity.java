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
import java.util.Set;
import java.util.Collections;

import javax.annotation.Nullable;

public class RegrowingStumpBlockEntity extends BlockEntity {

    private BlockState mimicState = Blocks.STRIPPED_OAK_WOOD.defaultBlockState();
    private BlockState futureSapling = Blocks.OAK_SAPLING.defaultBlockState();
    private long creationTime = 0L;
    private boolean isFireStump = false; // <--- NEW FLAG

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

    // --- NEW GETTER/SETTER ---
    public void setIsFireStump(boolean isFire) {
        this.isFireStump = isFire;
        this.setChanged();
    }

    public boolean isFireStump() {
        return isFireStump;
    }
    // -------------------------

    public void performRegrowth(ServerLevel level, BlockPos pos) {
        Block mimicBlock = this.mimicState.getBlock();
        Set<BlockPos> partners = find2x2Partners(level, pos, mimicBlock);

        for (BlockPos partnerPos : partners) {
            BlockEntity be = level.getBlockEntity(partnerPos);
            if (be instanceof RegrowingStumpBlockEntity partnerStump) {
                partnerStump.growIntoSapling();
            }
        }
        growIntoSapling();
    }

    private Set<BlockPos> find2x2Partners(ServerLevel level, BlockPos origin, Block mimicBlock) {
        int[][] quadrants = {
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };

        for (int[] q : quadrants) {
            BlockPos p1 = origin.offset(q[0], 0, 0);
            BlockPos p2 = origin.offset(0, 0, q[1]);
            BlockPos p3 = origin.offset(q[0], 0, q[1]);

            if (isValidPartner(level, p1, mimicBlock) &&
                    isValidPartner(level, p2, mimicBlock) &&
                    isValidPartner(level, p3, mimicBlock)) {

                return Set.of(p1, p2, p3);
            }
        }
        return Collections.emptySet();
    }

    private boolean isValidPartner(ServerLevel level, BlockPos pos, Block mimicBlock) {
        if (level.getBlockEntity(pos) instanceof RegrowingStumpBlockEntity stump) {
            return stump.getMimicState().is(mimicBlock);
        }
        return false;
    }

    private void growIntoSapling() {
        if (level instanceof ServerLevel serverLevel) {
            BlockPos pos = this.getBlockPos();
            RegrowingStumpBlock.destroyTreeFloodFill(serverLevel, pos.above());

            BlockState saplingState = this.getFutureSapling();
            if (saplingState.hasProperty(BlockStateProperties.STAGE)) {
                saplingState = saplingState.setValue(BlockStateProperties.STAGE, 1);
            }

            serverLevel.setBlock(pos, saplingState, 3);

            if (Config.COMMON.instantCatchUp.get()) {
                Block block = saplingState.getBlock();
                if (block instanceof net.minecraft.world.level.block.BonemealableBlock growable) {
                    if (growable.isValidBonemealTarget(serverLevel, pos, saplingState)) {
                        try {
                            growable.performBonemeal(serverLevel, serverLevel.random, pos, saplingState);
                        } catch (Exception e) {
                            System.err.println("Natural Regrowth: Failed to instant-grow tree at " + pos + ": " + e.getMessage());
                        }
                    }
                }
            }
        }
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

        // --- UPDATED DELAY LOGIC ---
        long delay = this.isFireStump
                ? Config.COMMON.fireRegrowthDelay.get()
                : Config.COMMON.regrowthDelay.get();
        // ---------------------------

        if (age < delay) return;

        long eligibleTicks = age - delay;
        if (eligibleTicks <= 0) return;

        int randomTickSpeed = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        if (randomTickSpeed <= 0) return;

        double chanceToBePicked = (double) randomTickSpeed / 4096.0;
        double chanceToGrow = this.isFireStump
                ? Config.COMMON.fireRegrowthChance.get()
                : Config.COMMON.regrowthChance.get();

        double p = chanceToBePicked * chanceToGrow;
        double probOfSuccess = 1.0 - Math.pow(1.0 - p, eligibleTicks);

        if (level.random.nextDouble() < probOfSuccess) {
            performRegrowth((ServerLevel) level, this.getBlockPos());
        }
    }

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
        tag.putBoolean("IsFireStump", isFireStump); // <--- SAVE
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("MimicState")) this.mimicState = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("MimicState"));
        if (tag.contains("FutureSapling")) this.futureSapling = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("FutureSapling"));
        if (tag.contains("CreationTime")) this.creationTime = tag.getLong("CreationTime");
        if (tag.contains("IsFireStump")) this.isFireStump = tag.getBoolean("IsFireStump"); // <--- LOAD
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
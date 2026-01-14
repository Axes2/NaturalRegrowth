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

    // --- REAL-TIME REGROWTH (Called by Block Random Tick) ---
    // This was the missing method!
    public void performRegrowth(ServerLevel level, BlockPos pos) {
        // 1. COORDINATION CHECK: Are we part of a 2x2 Mega Base?
        // If we are, we must wake up our neighbors so we all turn into saplings together.
        Block mimicBlock = this.mimicState.getBlock();
        Set<BlockPos> partners = find2x2Partners(level, pos, mimicBlock);

        for (BlockPos partnerPos : partners) {
            BlockEntity be = level.getBlockEntity(partnerPos);
            if (be instanceof RegrowingStumpBlockEntity partnerStump) {
                // Force the partner to grow NOW (Skipping their random lottery)
                // This ensures all 4 saplings appear on the exact same tick.
                partnerStump.growIntoSapling();
            }
        }

        // 2. Grow ourselves
        growIntoSapling();
    }

    // --- 2x2 COORDINATION LOGIC ---
    private Set<BlockPos> find2x2Partners(ServerLevel level, BlockPos origin, Block mimicBlock) {
        // We check the 4 directions that 'origin' could be a corner of.
        int[][] quadrants = {
                {1, 1},   // We are SW corner
                {1, -1},  // We are NW corner
                {-1, 1},  // We are SE corner
                {-1, -1}  // We are NE corner
        };

        for (int[] q : quadrants) {
            BlockPos p1 = origin.offset(q[0], 0, 0);      // Neighbor X
            BlockPos p2 = origin.offset(0, 0, q[1]);      // Neighbor Z
            BlockPos p3 = origin.offset(q[0], 0, q[1]);   // Diagonal Partner

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
            // It must be a stump, AND it must be the same wood type.
            return stump.getMimicState().is(mimicBlock);
        }
        return false;
    }

    // --- INTERNAL GROWTH HELPER ---
    private void growIntoSapling() {
        if (level instanceof ServerLevel serverLevel) {
            BlockPos pos = this.getBlockPos();

            // 1. Clean up old tree parts above us
            RegrowingStumpBlock.destroyTreeFloodFill(serverLevel, pos.above());

            // 2. Prepare the Sapling
            BlockState saplingState = this.getFutureSapling();

            // Set to Stage 1 (Ready to grow immediately if bonemealed/ticked)
            if (saplingState.hasProperty(BlockStateProperties.STAGE)) {
                saplingState = saplingState.setValue(BlockStateProperties.STAGE, 1);
            }

            // 3. Place the sapling
            serverLevel.setBlock(pos, saplingState, 3);

            // 4. Instant Catch-Up (Configurable)
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

    // --- CATCH UP LOGIC (On Load) ---
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
            // Instead of calling growIntoSapling directly, we call performRegrowth
            // This ensures 2x2 trees catch up together too!
            performRegrowth((ServerLevel) level, this.getBlockPos());
        }
    }

    // --- STANDARD METHODS ---
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
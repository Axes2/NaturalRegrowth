package net.axes.naturalregrowth.compat.dt;

import com.dtteam.dynamictrees.tree.species.Species;
import net.axes.naturalregrowth.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules; // Added Import
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class DoomedSoilBlockEntity extends BlockEntity {

    private ResourceLocation speciesName;
    private long creationTime = 0L;
    private boolean executed = false;

    public DoomedSoilBlockEntity(BlockPos pos, BlockState blockState) {
        super(DTRegistries.DOOMED_SOIL_BE.get(), pos, blockState);
    }

    // --- CATCH UP LOGIC (NEW) ---
    // This runs once every time the chunk loads (e.g., logging in, teleporting back)
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

        // 1. If we haven't waited long enough, do nothing.
        if (age < delay) return;

        // 2. Calculate how many ticks we "missed" while unloaded
        long eligibleTicks = age - delay;
        if (eligibleTicks <= 0) return;

        // 3. Get server tick speed to calculate probability
        int randomTickSpeed = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        if (randomTickSpeed <= 0) return;

        // 4. The Math: Calculate probability of success over N trials
        // (Copied 1:1 from RegrowingStumpBlockEntity for consistency)
        double chanceToBePicked = (double) randomTickSpeed / 4096.0;
        double chanceToGrow = Config.COMMON.regrowthChance.get();
        double p = chanceToBePicked * chanceToGrow;

        // probability = 1 - (chance_to_fail ^ number_of_tries)
        double probOfSuccess = 1.0 - Math.pow(1.0 - p, eligibleTicks);

        // 5. Roll the die
        if (level.random.nextDouble() < probOfSuccess) {
            if (level instanceof ServerLevel serverLevel) {
                performRegrowth(serverLevel, this.getBlockPos());
            }
        }
    }
    // ----------------------------

    public void setSpecies(ResourceLocation speciesName) {
        this.speciesName = speciesName;
        this.setChanged();
    }

    public void startTimer(long time) {
        this.creationTime = time;
        this.setChanged();
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void performRegrowth(ServerLevel level, BlockPos pos) {
        if (executed) return;
        this.executed = true;

        Species species = Species.REGISTRY.get(speciesName);
        if (species == null) species = Species.NULL_SPECIES;

        DTIntegration.fellTree(level, pos, species);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (speciesName != null) tag.putString("Species", speciesName.toString());
        tag.putLong("CreationTime", creationTime);
        tag.putBoolean("Executed", executed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Species")) speciesName = ResourceLocation.parse(tag.getString("Species"));
        creationTime = tag.getLong("CreationTime");
        executed = tag.getBoolean("Executed");
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
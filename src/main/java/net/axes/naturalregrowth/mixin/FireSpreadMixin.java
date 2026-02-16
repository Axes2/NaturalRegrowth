package net.axes.naturalregrowth.mixin;

import dev.protomanly.pmweather.data.ReinforcementManager;
import dev.protomanly.pmweather.event.GameBusEvents;
import net.axes.naturalregrowth.block.RegrowingStumpBlock;
import net.axes.naturalregrowth.block.entity.RegrowingStumpBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// FIX: Changed from 'class' to 'interface' because the target is an Interface.
@Mixin(targets = "dev.protomanly.pmweather.block.interfaces.BurningBlockInterface")
public interface FireSpreadMixin {

    // FIX: Method must be 'default' in an interface mixin
    @Inject(method = "setBurning", at = @At("HEAD"), remap = false)
    default void onSetBurning(Block newBlock, BlockState oldState, BlockPos pos, Level level, CallbackInfo ci) {

        Block saplingToSave = null;

        // Case A: Burning a normal Log
        if (oldState.is(BlockTags.LOGS)) {
            saplingToSave = getSaplingFromState(oldState);
        }

        // Case B: Burning an existing Regrowing Stump
        if (oldState.getBlock() instanceof RegrowingStumpBlock) {
            if (level.getBlockEntity(pos) instanceof RegrowingStumpBlockEntity stump) {
                saplingToSave = stump.getFutureSapling().getBlock();
            }
        }

        // Save data if found
        if (saplingToSave != null) {
            ReinforcementManager manager = (ReinforcementManager) GameBusEvents.REINFORCEMENTMANAGERS.get(level.dimension());
            if (manager != null) {
                manager.setSaplingData(pos, saplingToSave);
            }
        }
    }

    // FIX: Helper methods in interfaces should be static (Java 21 supports private static interface methods)
    private static Block getSaplingFromState(BlockState logState) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(logState.getBlock());
        String namespace = id.getNamespace();
        String path = id.getPath().replace("stripped_", "").replace("_log", "").replace("_wood", "");

        ResourceLocation saplingId = ResourceLocation.fromNamespaceAndPath(namespace, path + "_sapling");

        return BuiltInRegistries.BLOCK.getOptional(saplingId).orElse(Blocks.OAK_SAPLING);
    }
}
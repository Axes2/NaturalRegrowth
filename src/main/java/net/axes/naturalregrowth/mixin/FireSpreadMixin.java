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

import java.util.Optional;

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

    // FIX: Helper methods in interfaces should be static
    private static Block getSaplingFromState(BlockState logState) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(logState.getBlock());
        String namespace = id.getNamespace();
        String path = id.getPath();

        // 1. Clean Prefix (e.g. "stripped_oak_log" -> "oak_log")
        if (path.startsWith("stripped_")) {
            path = path.substring(9);
        }

        // 2. Identify and Clean Suffix
        // Use 'endsWith' to be precise, preventing accidental mid-string replacements
        String baseName = path;
        String[] logSuffixes = { "_log", "_wood", "_stem", "_hyphae", "_block" };

        for (String suffix : logSuffixes) {
            if (path.endsWith(suffix)) {
                baseName = path.substring(0, path.length() - suffix.length());
                break; // Stop after first match to avoid over-stripping
            }
        }

        // 3. Smart Guessing (Try multiple common sapling names)
        String[] saplingSuffixes = { "_sapling", "_fungus", "_propagule" };

        for (String suffix : saplingSuffixes) {
            ResourceLocation candidateId = ResourceLocation.fromNamespaceAndPath(namespace, baseName + suffix);

            // Check if this guess actually exists in the game registry
            Optional<Block> result = BuiltInRegistries.BLOCK.getOptional(candidateId);
            if (result.isPresent()) {
                return result.get();
            }
        }

        // 4. Last Resort: Default to Oak if nothing matched
        return Blocks.OAK_SAPLING;
    }
}
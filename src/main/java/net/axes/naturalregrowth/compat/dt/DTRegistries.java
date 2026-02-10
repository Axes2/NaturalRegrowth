package net.axes.naturalregrowth.compat.dt;

import net.axes.naturalregrowth.NaturalRegrowth;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class DTRegistries {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NaturalRegrowth.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NaturalRegrowth.MODID);

    // The Doomed Soil Block
    public static final DeferredBlock<Block> DOOMED_SOIL = BLOCKS.register("doomed_soil", DoomedSoilBlock::new);

    // The Block Entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DoomedSoilBlockEntity>> DOOMED_SOIL_BE =
            BLOCK_ENTITIES.register("doomed_soil_be",
                    () -> BlockEntityType.Builder.of(DoomedSoilBlockEntity::new, DOOMED_SOIL.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
package net.axes.naturalregrowth;

import net.axes.naturalregrowth.block.RegrowingStumpBlock;
import net.axes.naturalregrowth.block.entity.RegrowingStumpBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    // 1. Block Registry
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NaturalRegrowth.MODID);

    // 2. Block Entity Registry (NEW)
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NaturalRegrowth.MODID);

    // The Block
    public static final DeferredBlock<RegrowingStumpBlock> REGROWING_STUMP =
            BLOCKS.register("regrowing_stump", RegrowingStumpBlock::new);

    // The Block Entity (The "Brain")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RegrowingStumpBlockEntity>> REGROWING_STUMP_BE =
            BLOCK_ENTITIES.register("regrowing_stump", () ->
                    BlockEntityType.Builder.of(RegrowingStumpBlockEntity::new, REGROWING_STUMP.get()).build(null)
            );

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus); // Don't forget to register the new list!
    }
}
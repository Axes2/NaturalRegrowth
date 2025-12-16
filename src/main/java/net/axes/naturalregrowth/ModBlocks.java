package net.axes.naturalregrowth;

import net.axes.naturalregrowth.block.RegrowingStumpBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    // The Registry: Holds all our blocks
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("naturalregrowth");
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("naturalregrowth");

    // The Block: "regrowing_stump"
    public static final DeferredBlock<Block> REGROWING_STUMP = BLOCKS.register("regrowing_stump",
            RegrowingStumpBlock::new);

    // The Item: We need an item version so you can hold it (optional, but good for debugging)
    public static final DeferredItem<BlockItem> REGROWING_STUMP_ITEM = ITEMS.registerSimpleBlockItem("regrowing_stump", REGROWING_STUMP);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }
}
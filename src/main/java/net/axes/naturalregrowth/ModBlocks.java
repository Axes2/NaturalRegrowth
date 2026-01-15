package net.axes.naturalregrowth;

import net.axes.naturalregrowth.block.HealingBlock;
import net.axes.naturalregrowth.block.RegrowingStumpBlock;
import net.axes.naturalregrowth.block.entity.HealingBlockEntity;
import net.axes.naturalregrowth.block.entity.RegrowingStumpBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NaturalRegrowth.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NaturalRegrowth.MODID);

    // --- STUMP ---
    // Manually defined properties to avoid Rotation/Axis crashes
    public static final DeferredBlock<Block> REGROWING_STUMP = BLOCKS.register("regrowing_stump",
            () -> new RegrowingStumpBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .randomTicks()
                    .pushReaction(PushReaction.BLOCK)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RegrowingStumpBlockEntity>> REGROWING_STUMP_BE =
            BLOCK_ENTITIES.register("regrowing_stump_be",
                    () -> BlockEntityType.Builder.of(RegrowingStumpBlockEntity::new, REGROWING_STUMP.get()).build(null));

    // --- HEALING LOG ---
    // FIX: Do NOT use ofFullCopy(STRIPPED_OAK_LOG) because it forces an Axis check.
    // We define it manually as a simple wood block.
    public static final DeferredBlock<Block> HEALING_LOG = BLOCKS.register("healing_log",
            () -> new HealingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .randomTicks()
                    .pushReaction(PushReaction.BLOCK)));

    // --- HEALING LEAF ---
    // FIX: Do NOT use ofFullCopy(LEAVES) to avoid complex leaf logic conflicts.
    // We define it as a passable, transparent block.
    public static final DeferredBlock<Block> HEALING_LEAF = BLOCKS.register("healing_leaf",
            () -> new HealingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(0.2f)
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .noCollission()
                    .replaceable()
                    .randomTicks()
                    .pushReaction(PushReaction.DESTROY)));

    // --- BLOCK ENTITY ---
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HealingBlockEntity>> HEALING_BE =
            BLOCK_ENTITIES.register("healing_be",
                    () -> BlockEntityType.Builder.of(HealingBlockEntity::new,
                            HEALING_LOG.get(),
                            HEALING_LEAF.get()
                    ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
    }
}
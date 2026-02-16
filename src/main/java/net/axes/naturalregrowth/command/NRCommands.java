package net.axes.naturalregrowth.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.axes.naturalregrowth.Config; // <--- Import Config
import net.axes.naturalregrowth.block.entity.RegrowingStumpBlockEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

public class NRCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register /naturalregrowth and /nr alias
        registerAliases(dispatcher, "naturalregrowth");
        registerAliases(dispatcher, "nr");
    }

    private static void registerAliases(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(Commands.literal(commandName)
                // 1. FORCE GROW (Instant)
                .then(Commands.literal("grow")
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 256))
                                .executes(NRCommands::growStumps)
                        )
                )
                // 2. MAKE READY (Skip Delay) - NEW
                .then(Commands.literal("ready")
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 256))
                                .executes(NRCommands::makeStumpsReady)
                        )
                )
        );
    }

    // --- INSTANT GROWTH LOGIC ---
    private static int growStumps(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        BlockPos center = BlockPos.containing(context.getSource().getPosition());
        int blockRadius = IntegerArgumentType.getInteger(context, "radius");

        List<RegrowingStumpBlockEntity> stumps = collectStumps(level, center, blockRadius);
        int count = 0;

        for (RegrowingStumpBlockEntity stump : stumps) {
            if (!stump.isRemoved()) {
                stump.performRegrowth(level, stump.getBlockPos());
                count++;
            }
        }

        final int finalCount = count;
        context.getSource().sendSuccess(() -> Component.literal("§aInstantly grew " + finalCount + " stumps."), true);
        return 1;
    }

    // --- SKIP DELAY LOGIC (NEW) ---
    private static int makeStumpsReady(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        BlockPos center = BlockPos.containing(context.getSource().getPosition());
        int blockRadius = IntegerArgumentType.getInteger(context, "radius");

        List<RegrowingStumpBlockEntity> stumps = collectStumps(level, center, blockRadius);
        int count = 0;
        long currentTime = level.getGameTime();

        for (RegrowingStumpBlockEntity stump : stumps) {
            if (!stump.isRemoved()) {
                // 1. Check Config for how long it SHOULD have waited
                int requiredDelay = stump.isFireStump()
                        ? Config.COMMON.fireRegrowthDelay.get()
                        : Config.COMMON.regrowthDelay.get();

                // 2. Backdate the creation time
                // Setting it to (Now - Delay - 1) ensures Age > Delay
                stump.setCreationTime(currentTime - requiredDelay - 1);
                count++;
            }
        }

        final int finalCount = count;
        context.getSource().sendSuccess(() -> Component.literal("§eSkipped delay for " + finalCount + " stumps. They will now grow naturally."), true);
        return 1;
    }

    // --- SHARED HELPER ---
    private static List<RegrowingStumpBlockEntity> collectStumps(ServerLevel level, BlockPos center, int blockRadius) {
        int minChunkX = (center.getX() - blockRadius) >> 4;
        int maxChunkX = (center.getX() + blockRadius) >> 4;
        int minChunkZ = (center.getZ() - blockRadius) >> 4;
        int maxChunkZ = (center.getZ() + blockRadius) >> 4;

        List<RegrowingStumpBlockEntity> list = new ArrayList<>();

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (level.hasChunk(cx, cz)) {
                    LevelChunk chunk = level.getChunk(cx, cz);
                    if (chunk != null) {
                        for (BlockEntity be : chunk.getBlockEntities().values()) {
                            if (be instanceof RegrowingStumpBlockEntity stump) {
                                if (be.getBlockPos().distSqr(center) <= (blockRadius * blockRadius)) {
                                    list.add(stump);
                                }
                            }
                        }
                    }
                }
            }
        }
        return list;
    }
}
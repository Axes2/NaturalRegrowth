package net.axes.naturalregrowth.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.axes.naturalregrowth.Config;
import net.axes.naturalregrowth.block.entity.RegrowingStumpBlockEntity;
import net.axes.naturalregrowth.compat.dt.DTLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

public class NRCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerAliases(dispatcher, "naturalregrowth");
        registerAliases(dispatcher, "nr");
    }

    private static void registerAliases(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(Commands.literal(commandName)
                .then(Commands.literal("grow")
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 256))
                                .executes(NRCommands::growAll)
                        )
                )
                .then(Commands.literal("ready")
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 256))
                                .executes(NRCommands::makeAllReady)
                        )
                )
        );
    }

    private static int growAll(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        BlockPos center = BlockPos.containing(context.getSource().getPosition());
        int blockRadius = IntegerArgumentType.getInteger(context, "radius");

        int stumpCount = 0;
        for (RegrowingStumpBlockEntity stump : collectStumps(level, center, blockRadius)) {
            if (!stump.isRemoved()) {
                stump.performRegrowth(level, stump.getBlockPos());
                stumpCount++;
            }
        }

        int doomedCount = 0;
        if (DTLoader.isLoaded()) {
            doomedCount = net.axes.naturalregrowth.compat.dt.DTCommands.growInRadius(level, center, blockRadius);
        }

        final int finalStumps = stumpCount;
        final int finalDoomed = doomedCount;
        final boolean dt = DTLoader.isLoaded();
        context.getSource().sendSuccess(() -> Component.literal(
                "§aInstantly grew " + finalStumps + " stump(s)"
                        + (dt ? " and " + finalDoomed + " Dynamic Tree(s)." : ".")
        ), true);
        return 1;
    }

    private static int makeAllReady(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        BlockPos center = BlockPos.containing(context.getSource().getPosition());
        int blockRadius = IntegerArgumentType.getInteger(context, "radius");
        long currentTime = level.getGameTime();

        int stumpCount = 0;
        for (RegrowingStumpBlockEntity stump : collectStumps(level, center, blockRadius)) {
            if (!stump.isRemoved()) {
                int requiredDelay = stump.isFireStump()
                        ? Config.COMMON.fireRegrowthDelay.get()
                        : Config.COMMON.regrowthDelay.get();
                stump.setCreationTime(currentTime - requiredDelay - 1);
                stumpCount++;
            }
        }

        int doomedCount = 0;
        if (DTLoader.isLoaded()) {
            doomedCount = net.axes.naturalregrowth.compat.dt.DTCommands.makeReadyInRadius(level, center, blockRadius);
        }

        final int finalStumps = stumpCount;
        final int finalDoomed = doomedCount;
        final boolean dt = DTLoader.isLoaded();
        context.getSource().sendSuccess(() -> Component.literal(
                "§eSkipped delay for " + finalStumps + " stump(s)"
                        + (dt ? " and " + finalDoomed + " Dynamic Tree(s). They will now grow naturally."
                        : ". They will now grow naturally.")
        ), true);
        return 1;
    }

    private static List<RegrowingStumpBlockEntity> collectStumps(ServerLevel level, BlockPos center, int blockRadius) {
        List<RegrowingStumpBlockEntity> list = new ArrayList<>();
        int minChunkX = (center.getX() - blockRadius) >> 4;
        int maxChunkX = (center.getX() + blockRadius) >> 4;
        int minChunkZ = (center.getZ() - blockRadius) >> 4;
        int maxChunkZ = (center.getZ() + blockRadius) >> 4;
        long radiusSq = (long) blockRadius * blockRadius;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                LevelChunk chunk = level.getChunk(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof RegrowingStumpBlockEntity stump
                            && be.getBlockPos().distSqr(center) <= radiusSq) {
                        list.add(stump);
                    }
                }
            }
        }
        return list;
    }
}

package com.imyvm.economy.commands;

import com.imyvm.economy.EconomyMod;
import com.imyvm.economy.Translator;
import com.imyvm.economy.PlayerData;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.function.BiConsumer;

import static com.imyvm.economy.Translator.tr;

public class WalletCommand extends BaseCommand {
    @Override
    protected void registerCommand(CommandDispatcher<Object> dispatcher) {
        dispatcher.register(
            LiteralArgumentBuilder.literal("wallet")
                .requires(source -> Permissions.check((ServerCommandSource) source, EconomyMod.MOD_ID + ".wallet", 2))
                .then(LiteralArgumentBuilder.literal("add")
                    .then(RequiredArgumentBuilder.argument("player", EntityArgumentType.player())
                        .then(RequiredArgumentBuilder.argument("amount", DoubleArgumentType.doubleArg(0))
                            .executes(ctx -> this.updateOnesBalance(ctx, PlayerData::addMoney)))))
                .then(LiteralArgumentBuilder.literal("take")
                    .then(RequiredArgumentBuilder.argument("player", EntityArgumentType.player())
                        .then(RequiredArgumentBuilder.argument("amount", DoubleArgumentType.doubleArg(0))
                            .executes(ctx -> this.updateOnesBalance(ctx, (data, amount) -> data.addMoney(-amount))))))
                .then(LiteralArgumentBuilder.literal("set")
                    .then(RequiredArgumentBuilder.argument("player", EntityArgumentType.player())
                        .then(RequiredArgumentBuilder.argument("amount", DoubleArgumentType.doubleArg(0))
                            .executes(ctx -> this.updateOnesBalance(ctx, PlayerData::setMoney)))))
                .then(LiteralArgumentBuilder.literal("get")
                    .then(RequiredArgumentBuilder.argument("player", EntityArgumentType.player())
                        .executes(ctx -> {
                            CommandContext<ServerCommandSource> context = this.castCommandContext(ctx);

                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                            PlayerData data = EconomyMod.data.getOrCreate(player);

                            String formattedAmount = data.getMoneyFormatted();
                            context.getSource().sendFeedback(tr("commands.wallet.get", player.getName(), formattedAmount), true);

                            return Command.SINGLE_SUCCESS;
                        }))));
    }

    private int updateOnesBalance(CommandContext<Object> ctx, BiConsumer<PlayerData, Long> modifier) throws CommandSyntaxException {
        CommandContext<ServerCommandSource> context = this.castCommandContext(ctx);

        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        long amount = (long) (DoubleArgumentType.getDouble(context, "amount") * 100);

        PlayerData data = EconomyMod.data.getOrCreate(player);
        modifier.accept(data, amount);

        // make sure balance >= 0
        data.setMoney(Long.max(0, data.getMoney()));

        String formattedAmount = data.getMoneyFormatted();
        context.getSource().sendFeedback(tr("commands.wallet.set", player.getName(), formattedAmount), true);

        return Command.SINGLE_SUCCESS;
    }
}

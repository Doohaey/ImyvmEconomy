package com.imyvm.economy.commands;

import com.imyvm.economy.EconomyMod;
import com.imyvm.economy.PlayerData;
import com.imyvm.economy.RateList;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

import static com.imyvm.economy.Translator.tr;

public class TaxCommand extends BaseCommand{
    @Override
    public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        return runPlayerQuery(player, player);
    }

    public int runListStock(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        return runList(player, RateList.TaxRate.TaxType.STOCK_TAX,Long.MAX_VALUE);
    }

    public int runListTraffic(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        return runList(player, RateList.TaxRate.TaxType.TRAFFIC_TAX,Long.MAX_VALUE);
    }

    public int runPlayerQuery(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ServerPlayerEntity playerToQuery = EntityArgumentType.getPlayer(context, "player");
        return runPlayerQuery(player, playerToQuery);
    }

    public int runSetStock(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return runSet(RateList.TaxRate.TaxType.STOCK_TAX, context);
    }

    public int runSetTraffic(CommandContext<ServerCommandSource> context) throws CommandSyntaxException{
        return runSet(RateList.TaxRate.TaxType.TRAFFIC_TAX, context);
    }

    public int runDeleteStock(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return runDelete(RateList.TaxRate.TaxType.STOCK_TAX, context);
    }

    public int runDeleteTraffic(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return runDelete(RateList.TaxRate.TaxType.TRAFFIC_TAX, context);
    }


    private int runPlayerQuery(ServerPlayerEntity player, ServerPlayerEntity playerToQuery) throws CommandSyntaxException {
        PlayerData playerData = EconomyMod.data.getOrCreate(playerToQuery);

        Long playerStockCurrency = playerData.getMoney();
        Long playerTrafficCurrency = EconomyMod.rateList.getOrCreate(player).turnoverCount;

        player.sendMessage(tr("commands.rate.query.player"));
        runList(player, RateList.TaxRate.TaxType.STOCK_TAX, playerStockCurrency);
        runList(player, RateList.TaxRate.TaxType.TRAFFIC_TAX, playerTrafficCurrency);

        return Command.SINGLE_SUCCESS;
    }

    private int runList(ServerPlayerEntity player, RateList.TaxRate.TaxType taxType, Long amount) throws CommandSyntaxException{
        Map<Long, RateList.TaxRate> toDisplay = EconomyMod.rateList.getTaxRateList();
        PriorityQueue<Map.Entry<Long, RateList.TaxRate>> heap = new PriorityQueue<>(Map.Entry.comparingByKey());

        for (Map.Entry<Long, RateList.TaxRate> entry : toDisplay.entrySet()) {
            if (entry.getValue().getTaxType() == taxType && entry.getKey() <= amount) {
                heap.add(entry);
            }
        }

        Text text = ((taxType == RateList.TaxRate.TaxType.STOCK_TAX) ? tr("commands.rate.query.list.stock") :
                tr("commands.rate.query.list.traffic"));
        MutableText mutableText = (MutableText) tr("commands.rate.query.list", text);
        for (Map.Entry<Long, RateList.TaxRate> entry : heap) {
            mutableText.append("\n").append(entry.getKey().toString()).append("\t").append(entry.getValue().getTaxRate().toString());
        }

        player.sendMessage(mutableText);

        return Command.SINGLE_SUCCESS;
    }

    private int runSet(RateList.TaxRate.TaxType taxType, CommandContext<ServerCommandSource> context){
        long segmentation = LongArgumentType.getLong(context, "segmentation");
        double rate = DoubleArgumentType.getDouble(context, "rate");
        ServerPlayerEntity player = context.getSource().getPlayer();

        if (!EconomyMod.rateList.setNewRate(segmentation,rate,taxType)) {
            Objects.requireNonNull(player).sendMessage(tr("commands.rate.set.fail"));
        }
        Objects.requireNonNull(player).sendMessage(tr("commands.rate.set.success"));

        return Command.SINGLE_SUCCESS;
    }

    private int runDelete(RateList.TaxRate.TaxType taxType, CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        long segmentation = LongArgumentType.getLong(context, "segmentation");
        if (EconomyMod.rateList.deleteExistingRate(segmentation,taxType)){
            Objects.requireNonNull(player).sendMessage(tr("commands.delete.success"));
        }
        Objects.requireNonNull(player).sendMessage(tr("commands.delete.fail"));
        return Command.SINGLE_SUCCESS;
    }
}

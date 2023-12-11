package com.imyvm.economy;

import com.imyvm.economy.interfaces.Serializable;
import com.imyvm.economy.util.MoneyUtil;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.PriorityQueue;

import static com.imyvm.economy.EconomyMod.CONFIG;

public class PlayerData implements Serializable {
    private String name;
    private long money;

    public PlayerData(String name) {
        this.name = name;
        this.money = CONFIG.USER_DEFAULT_BALANCE.getValue();
    }

    @Override
    public void serialize(DataOutputStream stream) throws IOException {
        stream.writeUTF(name);
        stream.writeLong(money);
    }

    @Override
    public void deserialize(DataInputStream stream) throws IOException {
        this.name = stream.readUTF();
        this.money = stream.readLong();
    }

    public long addMoney(long amount) {
        this.money += amount;
        return this.money;
    }

    public long addMoney(RateList.TaxRate.TaxType taxType, long amount, ServerPlayerEntity player) {
        PriorityQueue<Map.Entry<Long, RateList.TaxRate>> heap = EconomyMod.rateList.getMatchTaxRate(amount, taxType);
        int size = heap.size();
        int iterator = 0;
        long formerKey = 0;
        Double formerRate = 0.0;
        for (Map.Entry<Long, RateList.TaxRate> entry : heap){
            iterator++;
            this.money += (long) ((1 + formerRate) * (entry.getKey() - formerKey));

            if (iterator == size) {
                this.money += (long) ((1 + entry.getValue().taxRate) * (amount - entry.getKey()));
            } else  {
                formerKey = entry.getKey();
                formerRate = entry.getValue().taxRate;
            }

        }
        if (taxType == RateList.TaxRate.TaxType.TRAFFIC_TAX) {
            RateList.PlayerTrafficData playerTrafficData = EconomyMod.rateList.getOrCreate(player);
            playerTrafficData.addMoney(-amount);
        }
        return this.money;
    }

    public String getMoneyFormatted() {
        return MoneyUtil.format(this.getMoney());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getMoney() {
        return money;
    }

    public void setMoney(long money) {
        this.money = money;
    }
}

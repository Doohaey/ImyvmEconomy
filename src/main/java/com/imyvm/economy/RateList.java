package com.imyvm.economy;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.*;
import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RateList {
    public static class TaxRate {
        public TaxRate(Double taxRate, TaxType taxType) {
            this.taxRate = taxRate;
            this.taxType = taxType;
        }

        public enum TaxType{
            STOCK_TAX,
            TRAFFIC_TAX
        }
        Double taxRate;
        TaxType taxType;

        public Double getTaxRate() {
            return taxRate;
        }

        public void setTaxRate(Double taxRate) {
            this.taxRate = taxRate;
        }

        public TaxType getTaxType() {
            return taxType;
        }

        public void setTaxType(TaxType taxType) {
            this.taxType = taxType;
        }
    }
    public static class PlayerTrafficData extends PlayerData{
        public String name;
        public long turnoverCount;
        public PlayerTrafficData(String name) {
            super(name);
            this.turnoverCount = 0;
        }
    }
    public final String RATE_RECORDER = "tax_rate_list.db";
    Map<Long, TaxRate> taxRateList;

    Map<UUID, PlayerTrafficData> trafficDataList;
    public PlayerTrafficData getOrCreate(ServerPlayerEntity player) {
        return trafficDataList.computeIfAbsent(player.getUuid(),(u) -> new PlayerTrafficData(player.getEntityName()));
    }

    public Double getTaxRate(long cashCount, TaxRate.TaxType taxType){
        Double formerValue = 0.0;
        int size = taxRateList.size();
        if (size == 0) return formerValue;

        PriorityQueue<Map.Entry<Long, TaxRate>> heap = new PriorityQueue<>(Comparator.comparing(Map.Entry::getKey));
        for (Map.Entry<Long, TaxRate> entry : taxRateList.entrySet()) {
            if (entry.getValue().taxType == taxType) {
                heap.add(entry);
            }
        }

        int index = 1;
        for (Map.Entry<Long, TaxRate> entry : heap) {
            if (entry.getKey() != Long.MIN_VALUE) {
                if (index == size || (cashCount < entry.getKey())) return formerValue;
                else formerValue = entry.getValue().getTaxRate();
            }
            index++;
        }
        return formerValue;
    }

    public Map<Long, TaxRate> getTaxRateList(){
        return this.taxRateList;
    }

    public boolean setNewRate(long segmentation, double taxRate, TaxRate.TaxType taxType) {
        for (Map.Entry<Long, TaxRate> entry : taxRateList.entrySet()) {
            if (entry.getKey() == segmentation) {
                return false;
            }
        }
        taxRateList.put(segmentation, new TaxRate(taxRate,taxType));
        return true;
    }

    public boolean deleteExistingRate(long segmentation, TaxRate.TaxType taxType) {
        for (Map.Entry<Long, TaxRate> entry : EconomyMod.rateList.getTaxRateList().entrySet()){
            if (segmentation == entry.getKey() && taxType == entry.getValue().getTaxType()) {
                EconomyMod.rateList.getTaxRateList().entrySet().remove(entry);
                return true;
            }
        }

        return false;
    }

    public void load() throws IOException {
        File file = FabricLoader.getInstance().getGameDir().resolve("world").resolve(RATE_RECORDER).toFile();

        if (!file.exists()) {
            taxRateList = new ConcurrentHashMap<>();
            return;
        }

        try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file))) {
            int size = dataInputStream.readInt();
            taxRateList = new ConcurrentHashMap<>(size);
            for (int i = 0; i < size; i++) {
                Long segmentation = dataInputStream.readLong();
                TaxRate taxRate = new TaxRate(dataInputStream.readDouble(), TaxRate.TaxType.valueOf(dataInputStream.readUTF()));
                taxRateList.put(segmentation, taxRate);
            }
        }
    }

    public void save() throws IOException {
        File file = FabricLoader.getInstance().getGameDir().resolve("world").resolve(RATE_RECORDER).toFile();

        try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(file))) {
            dataOutputStream.writeInt(taxRateList.size());
            for (Map.Entry<Long,TaxRate> entry : taxRateList.entrySet()) {
                dataOutputStream.writeLong(entry.getKey());
                dataOutputStream.writeDouble(entry.getValue().taxRate);
                dataOutputStream.writeUTF(entry.getValue().taxType.toString());
            }
        }
    }


}

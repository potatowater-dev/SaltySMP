package com.saltysmp.saltshop.model;

import org.bukkit.entity.Villager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ShopDefinition {

    private final String id;
    private String displayName;
    private Villager.Profession profession;
    private Villager.Type villagerType;
    private int villagerLevel;
    private final List<ShopTrade> trades = new ArrayList<>();

    public ShopDefinition(String id) {
        this.id = id.toLowerCase();
        this.displayName = "<light_purple>" + id;
        this.profession = Villager.Profession.NONE;
        this.villagerType = Villager.Type.PLAINS;
        this.villagerLevel = 5;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Villager.Profession getProfession() {
        return profession;
    }

    public void setProfession(Villager.Profession profession) {
        this.profession = profession == null ? Villager.Profession.NONE : profession;
    }

    public Villager.Type getVillagerType() {
        return villagerType;
    }

    public void setVillagerType(Villager.Type villagerType) {
        this.villagerType = villagerType == null ? Villager.Type.PLAINS : villagerType;
    }

    public int getVillagerLevel() {
        return villagerLevel;
    }

    public void setVillagerLevel(int villagerLevel) {
        this.villagerLevel = Math.max(1, Math.min(5, villagerLevel));
    }

    public List<ShopTrade> getTrades() {
        return Collections.unmodifiableList(trades);
    }

    public void setTrades(List<ShopTrade> newTrades) {
        trades.clear();
        if (newTrades != null) {
            for (ShopTrade trade : newTrades) {
                trades.add(trade.copy());
            }
        }
    }

    public void addTrade(ShopTrade trade) {
        trades.add(trade.copy());
    }

    public void removeTrade(int index) {
        if (index >= 0 && index < trades.size()) {
            trades.remove(index);
        }
    }

    public ShopTrade getTrade(int index) {
        if (index < 0 || index >= trades.size()) {
            return null;
        }
        return trades.get(index).copy();
    }

    public void setTrade(int index, ShopTrade trade) {
        if (index >= 0 && index < trades.size() && trade != null) {
            trades.set(index, trade.copy());
        }
    }
}

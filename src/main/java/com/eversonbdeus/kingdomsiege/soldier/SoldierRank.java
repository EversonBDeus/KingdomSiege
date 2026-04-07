package com.eversonbdeus.kingdomsiege.soldier;

import com.mojang.serialization.Codec;

import java.util.Locale;

/**
 * Rank militar do soldado.
 *
 * Representa a carreira principal da unidade, ganho por experiência militar
 * acumulada ao longo das batalhas. Responde à pergunta: "qual a patente desse soldado?".
 *
 * Separado de {@link SoldierTitleId} (identidade lendária) e de conquistas
 * (histórico rastreável). Cada camada cumpre um papel diferente.
 *
 * Progressão linear por XP:
 *   RECRUIT  →  SOLDIER  →  VETERAN  →  SERGEANT  →  CHAMPION
 *
 * Limiares de XP:
 *   RECRUIT   = 0
 *   SOLDIER   = 50
 *   VETERAN   = 150
 *   SERGEANT  = 350
 *   CHAMPION  = 700
 *
 * Referência: docs 07, 13, 18 do projeto Kingdom Siege.
 */
public enum SoldierRank {

    RECRUIT("recruit", 0),
    SOLDIER("soldier", 50),
    VETERAN("veteran", 150),
    SERGEANT("sergeant", 350),
    CHAMPION("champion", 700);

    public static final Codec<SoldierRank> CODEC =
            Codec.STRING.xmap(SoldierRank::fromName, SoldierRank::getSerializedName);

    private final String serializedName;

    /**
     * XP militar mínimo para atingir este rank.
     * O rank RECRUIT exige 0, portanto é o estado inicial de toda unidade.
     */
    private final int xpRequired;

    SoldierRank(String serializedName, int xpRequired) {
        this.serializedName = serializedName;
        this.xpRequired = xpRequired;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public int getXpRequired() {
        return xpRequired;
    }

    public String getTranslationKey() {
        return "soldier_rank." + serializedName;
    }

    /**
     * Retorna o próximo rank na progressão, ou {@code this} se já for o máximo.
     */
    public SoldierRank next() {
        SoldierRank[] values = values();
        int nextOrdinal = this.ordinal() + 1;
        return nextOrdinal < values.length ? values[nextOrdinal] : this;
    }

    /**
     * Retorna true se este é o rank máximo da progressão.
     */
    public boolean isMaxRank() {
        return this == CHAMPION;
    }

    /**
     * Determina qual rank corresponde a um total de XP acumulado.
     * Retorna o rank mais alto que o soldado já desbloqueou.
     */
    public static SoldierRank fromXp(int totalXp) {
        SoldierRank result = RECRUIT;
        for (SoldierRank rank : values()) {
            if (totalXp >= rank.xpRequired) {
                result = rank;
            }
        }
        return result;
    }

    public static SoldierRank fromName(String name) {
        if (name == null || name.isBlank()) {
            return RECRUIT;
        }

        String normalized = name.toLowerCase(Locale.ROOT);

        for (SoldierRank value : values()) {
            if (value.serializedName.equals(normalized)) {
                return value;
            }
        }

        return RECRUIT;
    }
}
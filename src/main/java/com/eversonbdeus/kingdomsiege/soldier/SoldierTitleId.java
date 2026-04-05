package com.eversonbdeus.kingdomsiege.soldier;

import com.mojang.serialization.Codec;

import java.util.Locale;

/**
 * Título conquistável do soldado.
 *
 * Representa a identidade lendária ativa da unidade. Responde à pergunta:
 * "pelo que esse soldado ficou conhecido?".
 *
 * Separado de {@link SoldierRank} (progressão de carreira), emblemas (marcas de
 * feito específico) e conquistas (histórico rastreável). O título é a camada
 * de reputação e narrativa da unidade.
 *
 * Cada soldado pode acumular vários títulos ao longo da vida, mas equipa apenas
 * um ativo por vez (armazenado em {@link SoldierIdentityData}).
 *
 * Raridades por cor (usadas na UI):
 *   COMMON  = verde    — feitos acessíveis na campanha inicial
 *   SKILLED = azul     — feitos de especialização
 *   HEROIC  = roxo     — feitos raros e memoráveis
 *   LEGEND  = ouro     — prestígio militar alto
 *   MYTH    = vermelho — feitos extremos, raridade máxima
 *
 * Referência: docs 07 e 18 do projeto Kingdom Siege.
 */
public enum SoldierTitleId {

    // ─── Sem título ───────────────────────────────────────────────────────────
    NONE("none", Rarity.NONE),

    // ─── Honra Militar — Verde (common) ──────────────────────────────────────

    /** Participou da primeira defesa bem-sucedida do castelo. */
    WALL_RECRUIT("wall_recruit", Rarity.COMMON),

    /** Eliminou 25 hostis dentro do raio do castelo. */
    GATE_GUARD("gate_guard", Rarity.COMMON),

    /** Permanecer vivo e vinculado ao mesmo dono por tempo relevante. */
    LOYAL("loyal", Rarity.COMMON),

    /** Sobreviveu a uma luta com pouca vida. */
    HARD_TO_FALL("hard_to_fall", Rarity.COMMON),

    // ─── Honra Militar — Azul (skilled) ──────────────────────────────────────

    /** Eliminou 100 hostis em modo GUARD. */
    FRONTIER_SENTINEL("frontier_sentinel", Rarity.SKILLED),

    /** Protegeu o dono muitas vezes. */
    RIGHT_ARM("right_arm", Rarity.SKILLED),

    /** Eliminou 100 mortos-vivos. */
    UNDEAD_PURIFIER("undead_purifier", Rarity.SKILLED),

    /** Venceu combate ficando com 1 coração ou menos. */
    ONE_HEART("one_heart", Rarity.SKILLED),

    // ─── Honra Militar — Roxo (heroic) ───────────────────────────────────────

    /** Derrotou 10 inimigos fortes durante ataque ao castelo. */
    SIEGE_BREAKER("siege_breaker", Rarity.HEROIC),

    /** Sobreviveu a 5 batalhas grandes defendendo o mesmo posto. */
    LIVING_WALL("living_wall", Rarity.HEROIC),

    /** Eliminou 100 endermen. */
    ENDSLAYER("endslayer", Rarity.HEROIC),

    /** Acumulou muitas kills na água sem morrer. */
    THE_UNSINKABLE("the_unsinkable", Rarity.HEROIC),

    // ─── Lenda — Ouro (legend) ────────────────────────────────────────────────

    /** Acumulou muitas defesas bem-sucedidas do castelo como soldado sênior. */
    GARRISON_LORD("garrison_lord", Rarity.LEGEND),

    /** Obteve kill final após sofrer dano quase letal. */
    DEATHS_LAUGHTER("deaths_laughter", Rarity.LEGEND),

    // ─── Mítico — Vermelho (myth) ─────────────────────────────────────────────

    /** Sobreviveu como último soldado ativo durante uma defesa crítica e ainda venceu. */
    LAST_OF_THE_WALL("last_of_the_wall", Rarity.MYTH);

    // ─────────────────────────────────────────────────────────────────────────

    public static final Codec<SoldierTitleId> CODEC =
            Codec.STRING.xmap(SoldierTitleId::fromName, SoldierTitleId::getSerializedName);

    private final String serializedName;
    private final Rarity rarity;

    SoldierTitleId(String serializedName, Rarity rarity) {
        this.serializedName = serializedName;
        this.rarity = rarity;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public Rarity getRarity() {
        return rarity;
    }

    /**
     * Chave de tradução do título para exibição na UI.
     * Exemplo: "soldier_title.siege_breaker"
     */
    public String getTranslationKey() {
        return "soldier_title." + serializedName;
    }

    /**
     * Retorna true se este título representa ausência de título ativo.
     */
    public boolean isNone() {
        return this == NONE;
    }

    public static SoldierTitleId fromName(String name) {
        if (name == null || name.isBlank()) {
            return NONE;
        }

        String normalized = name.toLowerCase(Locale.ROOT);

        for (SoldierTitleId value : values()) {
            if (value.serializedName.equals(normalized)) {
                return value;
            }
        }

        return NONE;
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Raridade do título. Usada pela UI para colorir o texto e
     * dar feedback visual sobre o prestígio do feito.
     */
    public enum Rarity {
        NONE,     // cinza — sem título
        COMMON,   // verde — campanha inicial
        SKILLED,  // azul  — especialização
        HEROIC,   // roxo  — feito raro
        LEGEND,   // ouro  — alto prestígio
        MYTH      // vermelho — raridade máxima
    }
}

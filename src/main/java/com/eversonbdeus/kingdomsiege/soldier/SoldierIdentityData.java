package com.eversonbdeus.kingdomsiege.soldier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * Dados de identidade persistida do soldado.
 *
 * Representa quem o soldado se tornou ao longo da vida: nome, patente, título ativo,
 * experiência militar acumulada e primeiros registros de histórico de batalha.
 *
 * Separado do {@link SoldierBlueprintData} (o DNA do craft) porque a identidade
 * muda com o tempo, enquanto o blueprint é definido na criação e não muda.
 *
 * Campos:
 *   customName    — nome dado pelo dono. Vazio = usar nome padrão da entidade.
 *   rank          — patente militar atual, avança por XP acumulado.
 *   activeTitle   — título lendário ativo. Apenas um por vez.
 *   militaryXp    — XP militar total, determina o rank via {@link SoldierRank#fromXp}.
 *   killCount     — [FASE 6] total de inimigos abatidos ao longo da vida.
 *   battlesCount  — [FASE 6] número de combates sobrevividos (incrementado ao receber dano em combate).
 *
 * Referência: docs 13, 17, 18 do projeto Kingdom Siege.
 */
public record SoldierIdentityData(
        Optional<String> customName,
        SoldierRank rank,
        SoldierTitleId activeTitle,
        int militaryXp,
        int killCount,
        int battlesCount
) {

    public static final Codec<SoldierIdentityData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.optionalFieldOf("custom_name")
                            .forGetter(SoldierIdentityData::customName),
                    SoldierRank.CODEC.optionalFieldOf("rank", SoldierRank.RECRUIT)
                            .forGetter(SoldierIdentityData::rank),
                    SoldierTitleId.CODEC.optionalFieldOf("active_title", SoldierTitleId.NONE)
                            .forGetter(SoldierIdentityData::activeTitle),
                    Codec.INT.optionalFieldOf("military_xp", 0)
                            .forGetter(SoldierIdentityData::militaryXp),
                    Codec.INT.optionalFieldOf("kill_count", 0)
                            .forGetter(SoldierIdentityData::killCount),
                    Codec.INT.optionalFieldOf("battles_count", 0)
                            .forGetter(SoldierIdentityData::battlesCount)
            ).apply(instance, SoldierIdentityData::new)
    );

    /**
     * Compact constructor com sanitização básica dos campos.
     */
    public SoldierIdentityData {
        customName = customName != null
                ? customName.filter(name -> !name.isBlank()).map(String::strip)
                : Optional.empty();
        rank = rank != null ? rank : SoldierRank.RECRUIT;
        activeTitle = activeTitle != null ? activeTitle : SoldierTitleId.NONE;
        militaryXp = Math.max(0, militaryXp);
        killCount = Math.max(0, killCount);
        battlesCount = Math.max(0, battlesCount);

        // Garante consistência: o rank armazenado nunca fica abaixo do que o XP já desbloqueou.
        SoldierRank rankFromXp = SoldierRank.fromXp(militaryXp);
        if (rankFromXp.ordinal() > rank.ordinal()) {
            rank = rankFromXp;
        }
    }

    // ─── Factories ───────────────────────────────────────────────────────────

    /**
     * Estado inicial padrão para toda nova unidade ao spawnar.
     * Sem nome customizado, rank RECRUIT, sem título, 0 XP, 0 kills, 0 batalhas.
     */
    public static SoldierIdentityData freshRecruit() {
        return new SoldierIdentityData(
                Optional.empty(),
                SoldierRank.RECRUIT,
                SoldierTitleId.NONE,
                0,
                0,
                0
        );
    }

    /**
     * Alias mantido por compatibilidade com chamadas já existentes no código.
     */
    public static SoldierIdentityData defaultRecruit() {
        return freshRecruit();
    }

    // ─── Mutações imutáveis (retornam nova instância) ─────────────────────────

    /**
     * Retorna nova identidade com o nome customizado definido.
     * Passar {@code null} ou string vazia equivale a remover o nome customizado.
     */
    public SoldierIdentityData withCustomName(String name) {
        Optional<String> resolved = (name != null && !name.isBlank())
                ? Optional.of(name.strip())
                : Optional.empty();
        return new SoldierIdentityData(resolved, rank, activeTitle, militaryXp, killCount, battlesCount);
    }

    /**
     * Retorna nova identidade com o título ativo trocado.
     */
    public SoldierIdentityData withActiveTitle(SoldierTitleId title) {
        return new SoldierIdentityData(
                customName, rank, title != null ? title : SoldierTitleId.NONE,
                militaryXp, killCount, battlesCount
        );
    }

    /**
     * Retorna nova identidade após ganhar {@code xpGain} pontos de XP militar.
     * O rank é recalculado automaticamente se o XP atingir o próximo threshold.
     */
    public SoldierIdentityData earnXp(int xpGain) {
        if (xpGain <= 0) {
            return this;
        }
        int newXp = militaryXp + xpGain;
        SoldierRank newRank = SoldierRank.fromXp(newXp);
        return new SoldierIdentityData(customName, newRank, activeTitle, newXp, killCount, battlesCount);
    }

    /**
     * [FASE 6] Retorna nova identidade com o contador de kills incrementado em 1.
     */
    public SoldierIdentityData withKill() {
        return new SoldierIdentityData(customName, rank, activeTitle, militaryXp, killCount + 1, battlesCount);
    }

    /**
     * [FASE 6] Retorna nova identidade com o contador de batalhas incrementado em 1.
     * Uma batalha é registrada na primeira vez que o soldado recebe dano em um combate.
     */
    public SoldierIdentityData withBattle() {
        return new SoldierIdentityData(customName, rank, activeTitle, militaryXp, killCount, battlesCount + 1);
    }

    // ─── Helpers de leitura ───────────────────────────────────────────────────

    /**
     * Retorna o nome customizado, ou uma string vazia se não houver.
     */
    public String getCustomNameOrEmpty() {
        return customName.orElse("");
    }

    /** Retorna true se o soldado tem um nome customizado definido pelo dono. */
    public boolean hasCustomName() {
        return customName.isPresent();
    }

    /** Retorna true se o soldado tem um título ativo (diferente de NONE). */
    public boolean hasActiveTitle() {
        return !activeTitle.isNone();
    }

    /**
     * Retorna true se o soldado já pode ser promovido ao próximo rank.
     * Falso se já estiver no rank máximo.
     */
    public boolean canPromote() {
        return !rank.isMaxRank() && militaryXp >= rank.next().getXpRequired();
    }
}
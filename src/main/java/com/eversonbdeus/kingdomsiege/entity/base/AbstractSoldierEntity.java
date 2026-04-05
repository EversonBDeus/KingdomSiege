package com.eversonbdeus.kingdomsiege.entity.base;

import com.eversonbdeus.kingdomsiege.soldier.SoldierBlueprintData;
import com.eversonbdeus.kingdomsiege.soldier.SoldierMode;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

/**
 * Classe base de toda unidade militar do Kingdom Siege.
 *
 * Centraliza os dados e comportamentos comuns:
 * — ownerUuid, SoldierBlueprintData, SoldierMode, homePos, guardRadius
 *
 * API de serialização do MC 26.1:
 * — ValueOutput (net.minecraft.world.level.storage) para escrita
 * — ValueInput (net.minecraft.world.level.storage) para leitura
 * — .store() / .storeNullable() + Codec para cada campo
 * — UUIDUtil.STRING_CODEC para UUID (putUUID/getUUID removidos no 26.1)
 *
 * CastleSoldierEntity pode estender esta classe quando o ChatGPT
 * fechar o trabalho na inteligência da entidade.
 */
public abstract class AbstractSoldierEntity extends PathfinderMob {

    private UUID ownerUuid;
    private SoldierBlueprintData blueprint;
    private SoldierMode commandMode = SoldierMode.GUARD;
    private BlockPos homePos;
    private int guardRadius = 16;

    // ─── Construtor ──────────────────────────────────────────────────────────

    protected AbstractSoldierEntity(EntityType<? extends AbstractSoldierEntity> entityType, Level level) {
        super(entityType, level);
    }

    // ─── Blueprint ───────────────────────────────────────────────────────────

    public SoldierBlueprintData getBlueprint() {
        return blueprint;
    }

    public void setBlueprint(SoldierBlueprintData blueprint) {
        this.blueprint = blueprint;
        if (blueprint != null) {
            applyBlueprintStats(blueprint);
        }
    }

    /**
     * Aplica vida, armadura, toughness e knockback resistance do blueprint.
     * Subclasses podem sobrescrever para adicionar efeitos por classe.
     */
    protected void applyBlueprintStats(SoldierBlueprintData blueprint) {
        if (blueprint == null) return;

        var healthAttr = this.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            double newMax = Math.max(1.0D, healthAttr.getBaseValue() + blueprint.getBonusHealth());
            healthAttr.setBaseValue(newMax);
            if (this.getHealth() > (float) newMax) {
                this.setHealth((float) newMax);
            }
        }

        var armorAttr = this.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.setBaseValue(blueprint.getArmorBonus());
        }

        var toughnessAttr = this.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (toughnessAttr != null) {
            toughnessAttr.setBaseValue(blueprint.getToughnessBonus());
        }

        var kbResistAttr = this.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kbResistAttr != null && blueprint.getKnockbackResistanceBonus() > 0.0D) {
            kbResistAttr.setBaseValue(blueprint.getKnockbackResistanceBonus());
        }
    }

    // ─── Inicialização via item de spawn ──────────────────────────────────────

    /**
     * Inicializa a unidade completa após spawn via SoldierSpawnEggItem.
     */
    public void initializeFromBlueprint(SoldierBlueprintData blueprint, UUID ownerUuid, BlockPos spawnPos) {
        this.setOwnerUuid(ownerUuid);
        this.setHomePos(spawnPos);
        this.setCommandMode(SoldierMode.GUARD);
        this.setGuardRadius(16);
        this.setBlueprint(blueprint);
    }

    // ─── Dono ────────────────────────────────────────────────────────────────

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public boolean isOwnedBy(Player player) {
        return ownerUuid != null && ownerUuid.equals(player.getUUID());
    }

    // ─── Modo de comando ─────────────────────────────────────────────────────

    public SoldierMode getCommandMode() {
        return commandMode;
    }

    public void setCommandMode(SoldierMode mode) {
        this.commandMode = (mode != null) ? mode : SoldierMode.GUARD;
    }

    public void cycleCommandMode() {
        setCommandMode(commandMode.next());
    }

    public boolean isInGuardMode()  { return commandMode == SoldierMode.GUARD; }
    public boolean isInFollowMode() { return commandMode == SoldierMode.FOLLOW; }

    // ─── Posto de guarda ─────────────────────────────────────────────────────

    public BlockPos getHomePos() {
        return homePos;
    }

    public void setHomePos(BlockPos pos) {
        this.homePos = pos != null ? pos.immutable() : null;
    }

    public int getGuardRadius() {
        return guardRadius;
    }

    public void setGuardRadius(int radius) {
        this.guardRadius = Math.max(1, radius);
    }

    public boolean isOutsideGuardRadius() {
        if (homePos == null) return false;
        double dx = homePos.getX() + 0.5D - this.getX();
        double dy = homePos.getY() + 0.5D - this.getY();
        double dz = homePos.getZ() + 0.5D - this.getZ();
        return (dx * dx + dy * dy + dz * dz) > (double) (guardRadius * guardRadius);
    }

    // ─── Persistência — MC 26.1 ValueOutput / ValueInput ─────────────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput valueOutput) {
        super.addAdditionalSaveData(valueOutput);

        valueOutput.store(
                "SoldierBlueprint",
                SoldierBlueprintData.CODEC,
                blueprint != null ? blueprint : SoldierBlueprintData.defaultRecruit()
        );
        valueOutput.store("CommandMode", SoldierMode.CODEC, commandMode);
        valueOutput.storeNullable("OwnerUuid", UUIDUtil.STRING_CODEC, ownerUuid);
        valueOutput.store("GuardRadius", Codec.INT, guardRadius);
        valueOutput.storeNullable("HomePosX", Codec.INT, homePos != null ? homePos.getX() : null);
        valueOutput.storeNullable("HomePosY", Codec.INT, homePos != null ? homePos.getY() : null);
        valueOutput.storeNullable("HomePosZ", Codec.INT, homePos != null ? homePos.getZ() : null);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {
        super.readAdditionalSaveData(valueInput);

        setBlueprint(
                valueInput.read("SoldierBlueprint", SoldierBlueprintData.CODEC)
                        .orElse(SoldierBlueprintData.defaultRecruit())
        );
        setCommandMode(
                valueInput.read("CommandMode", SoldierMode.CODEC)
                        .orElse(SoldierMode.GUARD)
        );
        setOwnerUuid(
                valueInput.read("OwnerUuid", UUIDUtil.STRING_CODEC)
                        .orElse(null)
        );
        guardRadius = Math.max(1, valueInput.read("GuardRadius", Codec.INT).orElse(16));

        Integer x = valueInput.read("HomePosX", Codec.INT).orElse(null);
        Integer y = valueInput.read("HomePosY", Codec.INT).orElse(null);
        Integer z = valueInput.read("HomePosZ", Codec.INT).orElse(null);
        if (x != null && y != null && z != null) {
            setHomePos(new BlockPos(x, y, z));
        }
    }
}
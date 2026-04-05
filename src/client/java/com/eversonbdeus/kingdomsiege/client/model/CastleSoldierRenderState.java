package com.eversonbdeus.kingdomsiege.client.model;

import com.eversonbdeus.kingdomsiege.soldier.SoldierClass;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class CastleSoldierRenderState extends LivingEntityRenderState {

    // Classe do soldado copiada da entidade durante extractRenderState.
    // Usada pelo renderer para escolher a textura correta por classe.
    // Valor padrão: SWORDSMAN (fallback seguro se o blueprint não estiver carregado).
    public SoldierClass soldierClass = SoldierClass.SWORDSMAN;
}
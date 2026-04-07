package com.eversonbdeus.kingdomsiege.client.model;

import com.eversonbdeus.kingdomsiege.soldier.SoldierClass;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

public class CastleSoldierRenderState extends HumanoidRenderState {

    // Classe copiada da entidade durante extractRenderState.
    // Usada para escolher textura e pose principal de arma.
    public SoldierClass soldierClass = SoldierClass.SWORDSMAN;
}
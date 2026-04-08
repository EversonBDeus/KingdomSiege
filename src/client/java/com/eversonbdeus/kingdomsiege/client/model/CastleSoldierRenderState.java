package com.eversonbdeus.kingdomsiege.client.model;

import com.eversonbdeus.kingdomsiege.soldier.SoldierClass;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

public class CastleSoldierRenderState extends HumanoidRenderState {

    // Classe copiada da entidade durante extractRenderState.
    public SoldierClass soldierClass = SoldierClass.SWORDSMAN;

    // Arqueiro em postura ampla de combate:
    // já detectou alvo / ameaça e deve levantar os braços.
    public boolean archerCombatReadyActive = false;

    // Arqueiro em draw real do arco.
    public boolean archerBowPoseActive = false;
}
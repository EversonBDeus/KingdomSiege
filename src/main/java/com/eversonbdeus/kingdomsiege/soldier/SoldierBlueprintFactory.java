package com.eversonbdeus.kingdomsiege.soldier;

import net.minecraft.world.item.ItemStack;

/**
 * Factory utilitária para criação de {@link SoldierBlueprintData}.
 *
 * Centraliza a lógica de construção de blueprints a partir dos itens do craft,
 * tirando essa responsabilidade das classes de receita ({@code ArcherSoldierRecipe},
 * {@code SwordsmanSoldierRecipe}) e dos métodos estáticos do próprio record.
 *
 * Vantagens:
 * — receitas ficam responsáveis apenas por validar os itens do craft
 * — a lógica de "como traduzir itens em blueprint" fica em um único lugar
 * — facilita adicionar suporte a novas classes ou novos campos do blueprint
 *   sem tocar em cada receita individualmente
 *
 * Uso esperado pelas receitas:
 * <pre>
 *   SoldierBlueprintData blueprint = SoldierBlueprintFactory.createSwordsmanBlueprint(swordStack, chestplateStack);
 *   return ModItems.createConfiguredSoldierEgg(blueprint);
 * </pre>
 *
 * Referência: doc 12 do projeto Kingdom Siege (Fase 1, item 5.1.5).
 */
public final class SoldierBlueprintFactory {

    private SoldierBlueprintFactory() {
    }

    // ─── Criação a partir do craft ────────────────────────────────────────────

    /**
     * Cria um blueprint de espadachim a partir dos itens do craft.
     *
     * A espada define:
     *   — classe (SWORDSMAN)
     *   — weapon class (SWORD)
     *   — dano base (derivado do tipo de espada)
     *   — encantamentos herdados da espada (Sharpness, Fire Aspect, Knockback)
     *
     * O peitoral define:
     *   — tier de armadura (LEATHER → NETHERITE)
     *   — vida bônus e armadura interna
     *   — encantamentos herdados do peitoral (Protection, Thorns, etc.)
     *
     * @param swordStack      espada usada no craft (não pode ser null nem empty)
     * @param chestplateStack peitoral usado no craft (não pode ser null nem empty)
     * @return blueprint completo pronto para ser gravado no item de spawn
     */
    public static SoldierBlueprintData createSwordsmanBlueprint(ItemStack swordStack, ItemStack chestplateStack) {
        return SoldierBlueprintData.swordsmanFromCraft(swordStack, chestplateStack);
    }

    /**
     * Cria um blueprint de arqueiro a partir dos itens do craft.
     *
     * O arco define:
     *   — classe (ARCHER)
     *   — weapon class (BOW)
     *   — encantamentos herdados do arco (Power, Punch, Flame)
     *
     * O peitoral define:
     *   — tier de armadura (LEATHER → NETHERITE)
     *   — vida bônus e armadura interna
     *   — encantamentos herdados do peitoral
     *
     * @param bowStack        arco usado no craft (não pode ser null nem empty)
     * @param chestplateStack peitoral usado no craft (não pode ser null nem empty)
     * @return blueprint completo pronto para ser gravado no item de spawn
     */
    public static SoldierBlueprintData createArcherBlueprint(ItemStack bowStack, ItemStack chestplateStack) {
        return SoldierBlueprintData.archerFromCraft(bowStack, chestplateStack);
    }

    // ─── Criação por conveniência (sem craft real) ────────────────────────────

    /**
     * Cria um blueprint padrão para uma classe com o tier de armadura mínimo.
     * Usado por helpers de conveniência em {@code ModItems} e em testes.
     *
     * @param soldierClass classe desejada
     * @return blueprint simples com ArmorTier.LEATHER e sem encantamentos
     */
    public static SoldierBlueprintData createDefault(SoldierClass soldierClass) {
        return SoldierBlueprintData.of(soldierClass, ArmorTier.LEATHER);
    }

    /**
     * Cria um blueprint padrão de espadachim de couro.
     * Atalho para uso em testes e helpers de criatividade.
     */
    public static SoldierBlueprintData createDefaultSwordsman() {
        return SoldierBlueprintData.of(SoldierClass.SWORDSMAN, ArmorTier.LEATHER);
    }

    /**
     * Cria um blueprint padrão de arqueiro de couro.
     * Atalho para uso em testes e helpers de criatividade.
     */
    public static SoldierBlueprintData createDefaultArcher() {
        return SoldierBlueprintData.of(SoldierClass.ARCHER, ArmorTier.LEATHER);
    }
}

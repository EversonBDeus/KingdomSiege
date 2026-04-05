package com.eversonbdeus.kingdomsiege.recipe;

import com.eversonbdeus.kingdomsiege.registry.ModItems;
import com.eversonbdeus.kingdomsiege.registry.ModRecipes;
import com.eversonbdeus.kingdomsiege.soldier.ArmorTier;
import com.eversonbdeus.kingdomsiege.soldier.SoldierBlueprintData;
import com.eversonbdeus.kingdomsiege.soldier.SoldierBlueprintFactory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Receita custom para craftar o soldado Espadachim.
 *
 * Layout da grade 3x3:
 * <pre>
 *   [ ]  [ ]  [SWORD]
 *   [ ]  [CHEST]  [ ]
 *   [CORE]  [ ]  [CATALYST?]
 * </pre>
 *
 * — (2,0) = espada qualquer material (obrigatório)
 * — (1,1) = peitoral qualquer nível (obrigatório, deve estar novo)
 * — (0,2) = soldier core (obrigatório)
 * — (2,2) = catalisador (OPCIONAL — slot vazio = CatalystType.NONE)
 * — demais slots devem estar vazios
 *
 * Alterações desta versão:
 *
 * [Etapa 8] O slot (2,2) não precisa mais estar vazio.
 * O item presente ali é identificado via {@code CatalystType.fromItem()}
 * e incluído no blueprint da unidade.
 *
 * [Etapa 9] A restrição de material combinado (EquipmentPattern) foi REMOVIDA.
 * O jogador pode usar espada de diamante com peitoral de ferro sem problemas.
 * A espada define a classe e o dano base; o peitoral define apenas a defesa interna.
 * Não há razão técnica ou de balanceamento no MVP para exigir o mesmo material.
 *
 * Espadas aceitas: madeira, pedra, ferro, ouro, diamante, netherite.
 * Peitorais aceitos: couro, corrente, ferro, ouro, diamante, netherite.
 */
public class SwordsmanSoldierRecipe extends CustomRecipe {

	public SwordsmanSoldierRecipe() {
		super();
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return getValidatedInput(input) != null;
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		ValidatedInput validatedInput = getValidatedInput(input);

		if (validatedInput == null) {
			return ItemStack.EMPTY;
		}

		// Etapa 8: passa o catalystStack para a factory, que resolve o CatalystType.
		SoldierBlueprintData blueprint = SoldierBlueprintFactory.createSwordsmanBlueprint(
				validatedInput.swordStack(),
				validatedInput.chestplateStack(),
				validatedInput.catalystStack()
		);

		return ModItems.createConfiguredSoldierEgg(blueprint);
	}

	@Override
	public RecipeSerializer<SwordsmanSoldierRecipe> getSerializer() {
		return ModRecipes.SWORDSMAN_SOLDIER;
	}

	private ValidatedInput getValidatedInput(CraftingInput input) {
		if (input == null || input.width() != 3 || input.height() != 3) {
			return null;
		}

		// ── Leitura dos slots funcionais ─────────────────────────────────────
		ItemStack swordStack       = input.getItem(2, 0); // top-right
		ItemStack chestplateStack  = input.getItem(1, 1); // center
		ItemStack soldierCoreStack = input.getItem(0, 2); // bottom-left
		ItemStack catalystStack    = input.getItem(2, 2); // bottom-right (NOVO)

		// ── Slots que devem estar vazios ──────────────────────────────────────
		// (2,2) removido desta lista — agora é o slot de catalisador (opcional).
		if (!isEmpty(input.getItem(0, 0))
				|| !isEmpty(input.getItem(1, 0))
				|| !isEmpty(input.getItem(0, 1))
				|| !isEmpty(input.getItem(2, 1))
				|| !isEmpty(input.getItem(1, 2))) {
			return null;
		}

		// ── Validações dos ingredientes obrigatórios ──────────────────────────
		if (!soldierCoreStack.is(ModItems.SOLDIER_CORE)) {
			return null;
		}

		// Etapa 9: aceitar qualquer espada válida, sem restrição de material combinado.
		if (!isSupportedSword(swordStack)) {
			return null;
		}

		if (!isSupportedChestplate(chestplateStack)) {
			return null;
		}

		// O peitoral deve estar novo.
		// Como ele define a defesa interna da unidade, não aceitamos armadura usada.
		if (isUsedChestplate(chestplateStack)) {
			return null;
		}

		// catalystStack pode ser vazio — CatalystType.fromItem() retorna NONE neste caso.
		return new ValidatedInput(swordStack, chestplateStack, catalystStack);
	}

	/**
	 * Aceita qualquer espada vanilla como ingrediente da receita.
	 *
	 * [Etapa 9] Regra expandida: o material da espada não precisa mais coincidir
	 * com o material do peitoral. Qualquer espada válida determina o dano base
	 * da unidade, independente do tier defensivo escolhido.
	 */
	private static boolean isSupportedSword(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}

		return stack.is(Items.WOODEN_SWORD)
				|| stack.is(Items.STONE_SWORD)
				|| stack.is(Items.IRON_SWORD)
				|| stack.is(Items.GOLDEN_SWORD)
				|| stack.is(Items.DIAMOND_SWORD)
				|| stack.is(Items.NETHERITE_SWORD);
	}

	/**
	 * Aceita qualquer peitoral vanilla como ingrediente da receita.
	 * O tier é lido via {@link ArmorTier#fromChestplate(ItemStack)} e define
	 * a defesa interna da unidade, independente do material da espada.
	 */
	private static boolean isSupportedChestplate(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}

		return switch (ArmorTier.fromChestplate(stack)) {
			case LEATHER, CHAIN, IRON, GOLD, DIAMOND, NETHERITE -> true;
		};
	}

	private static boolean isUsedChestplate(ItemStack stack) {
		return stack != null
				&& !stack.isEmpty()
				&& stack.isDamageableItem()
				&& stack.getDamageValue() > 0;
	}

	private static boolean isEmpty(ItemStack stack) {
		return stack == null || stack.isEmpty();
	}

	/**
	 * Ingredientes validados extraídos da grade.
	 *
	 * Etapa 8: adicionado catalystStack (pode ser EMPTY = catalisador ausente).
	 * Etapa 9: removido o campo EquipmentPattern — não é mais usado.
	 */
	private record ValidatedInput(
			ItemStack swordStack,
			ItemStack chestplateStack,
			ItemStack catalystStack
	) {
	}
}

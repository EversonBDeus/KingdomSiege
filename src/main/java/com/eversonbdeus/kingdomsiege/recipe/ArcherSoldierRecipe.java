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
 * Receita custom para craftar o soldado Arqueiro.
 *
 * Layout da grade 3x3:
 * <pre>
 *   [ ]  [ ]  [BOW]
 *   [ ]  [CHEST]  [ ]
 *   [CORE]  [ ]  [CATALYST?]
 * </pre>
 *
 * — (2,0) = arco (obrigatório)
 * — (1,1) = peitoral qualquer nível (obrigatório, deve estar novo)
 * — (0,2) = soldier core (obrigatório)
 * — (2,2) = catalisador (OPCIONAL — slot vazio = CatalystType.NONE)
 * — demais slots devem estar vazios
 *
 * Alteração da Etapa 8: o slot (2,2) não precisa mais estar vazio.
 * O item presente ali é identificado via {@code CatalystType.fromItem()}
 * e incluído no blueprint da unidade.
 */
public class ArcherSoldierRecipe extends CustomRecipe {

	public ArcherSoldierRecipe() {
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
		SoldierBlueprintData blueprint = SoldierBlueprintFactory.createArcherBlueprint(
				validatedInput.bowStack(),
				validatedInput.chestplateStack(),
				validatedInput.catalystStack()
		);

		return ModItems.createConfiguredSoldierEgg(blueprint);
	}

	@Override
	public RecipeSerializer<ArcherSoldierRecipe> getSerializer() {
		return ModRecipes.ARCHER_SOLDIER;
	}

	private ValidatedInput getValidatedInput(CraftingInput input) {
		if (input == null || input.width() != 3 || input.height() != 3) {
			return null;
		}

		// ── Leitura dos slots funcionais ─────────────────────────────────────
		ItemStack bowStack         = input.getItem(2, 0); // top-right
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

		if (!bowStack.is(Items.BOW)) {
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
		return new ValidatedInput(bowStack, chestplateStack, catalystStack);
	}

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
	 */
	private record ValidatedInput(
			ItemStack bowStack,
			ItemStack chestplateStack,
			ItemStack catalystStack
	) {
	}
}

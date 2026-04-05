package com.eversonbdeus.kingdomsiege.soldier;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BannerPattern;

import java.util.Locale;

/**
 * Tipo de catalisador usado no craft do soldado.
 *
 * Cada valor representa um item específico colocado no slot de catalisador
 * da grade de craft. NONE é o valor padrão quando o slot está vazio.
 *
 * Adicionado na Etapa 8: método {@link #fromItem(ItemStack)} para identificar
 * o catalisador a partir do ItemStack presente na grade.
 */
public enum CatalystType {
	NONE("none"),
	SHIELD("shield"),
	BANNER("banner"),
	GOLDEN_APPLE("golden_apple"),
	EMERALD("emerald"),
	REDSTONE("redstone"),
	QUARTZ("quartz"),
	OBSIDIAN("obsidian"),
	SADDLE("saddle"),
	BOOK("book");

	public static final Codec<CatalystType> CODEC = Codec.STRING.xmap(
			CatalystType::fromName,
			CatalystType::getSerializedName
	);

	private final String serializedName;

	CatalystType(String serializedName) {
		this.serializedName = serializedName;
	}

	public String getSerializedName() {
		return serializedName;
	}

	public String getTranslationKey() {
		return "catalyst_type." + serializedName;
	}

	public static CatalystType fromName(String name) {
		if (name == null || name.isBlank()) {
			return NONE;
		}

		String normalized = name.toLowerCase(Locale.ROOT);

		for (CatalystType value : values()) {
			if (value.serializedName.equals(normalized)) {
				return value;
			}
		}

		return NONE;
	}

	// ─── ETAPA 8 — identificação a partir do ItemStack ───────────────────────

	/**
	 * Identifica o CatalystType a partir do ItemStack presente no slot de
	 * catalisador da grade de craft.
	 *
	 * Retorna {@link #NONE} se o slot estiver vazio ou o item não for
	 * reconhecido como catalisador válido.
	 *
	 * Usado por {@code SoldierBlueprintFactory} nas receitas custom.
	 *
	 * @param stack ItemStack lido do slot de catalisador (pode ser EMPTY)
	 * @return o CatalystType correspondente, ou NONE
	 */
	public static CatalystType fromItem(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return NONE;
		}

		if (stack.is(Items.SHIELD)) {
			return SHIELD;
		}

		// Banners: qualquer cor de banner-item de mão
		if (stack.is(Items.WHITE_BANNER)
				|| stack.is(Items.ORANGE_BANNER)
				|| stack.is(Items.MAGENTA_BANNER)
				|| stack.is(Items.LIGHT_BLUE_BANNER)
				|| stack.is(Items.YELLOW_BANNER)
				|| stack.is(Items.LIME_BANNER)
				|| stack.is(Items.PINK_BANNER)
				|| stack.is(Items.GRAY_BANNER)
				|| stack.is(Items.LIGHT_GRAY_BANNER)
				|| stack.is(Items.CYAN_BANNER)
				|| stack.is(Items.PURPLE_BANNER)
				|| stack.is(Items.BLUE_BANNER)
				|| stack.is(Items.BROWN_BANNER)
				|| stack.is(Items.GREEN_BANNER)
				|| stack.is(Items.RED_BANNER)
				|| stack.is(Items.BLACK_BANNER)) {
			return BANNER;
		}

		if (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
			return GOLDEN_APPLE;
		}

		if (stack.is(Items.EMERALD)) {
			return EMERALD;
		}

		if (stack.is(Items.REDSTONE)) {
			return REDSTONE;
		}

		// Quartz: bloco ou item do quartz do nether
		if (stack.is(Items.QUARTZ)) {
			return QUARTZ;
		}

		if (stack.is(Items.OBSIDIAN) || stack.is(Items.CRYING_OBSIDIAN)) {
			return OBSIDIAN;
		}

		if (stack.is(Items.SADDLE)) {
			return SADDLE;
		}

		if (stack.is(Items.BOOK)
				|| stack.is(Items.WRITABLE_BOOK)
				|| stack.is(Items.WRITTEN_BOOK)
				|| stack.is(Items.ENCHANTED_BOOK)) {
			return BOOK;
		}

		return NONE;
	}
}

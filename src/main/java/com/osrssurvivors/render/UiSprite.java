package com.osrssurvivors.render;

/** Interface artwork the renderer can use when an {@link GameAssets} implementation provides it. */
public enum UiSprite
{
	HITSPLAT_DAMAGE,
	HEALTH_FRONT,
	HEALTH_BACK,
	ORB_FRAME,
	ORB_EMPTY,
	ORB_HP_FILL,
	ORB_HP_ICON,
	SPELL_PROJECTILE,
	XP_SKILL_ICON,
	SPECIAL_HITSPLAT,
	EQUIPMENT_SLOT,
	BORDER_TOP_LEFT,
	BORDER_TOP_RIGHT,
	BORDER_BOTTOM_LEFT,
	BORDER_BOTTOM_RIGHT,
	BUTTON_TOP_LEFT,
	BUTTON_TOP_RIGHT,
	BUTTON_BOTTOM_LEFT,
	BUTTON_BOTTOM_RIGHT,
	BUTTON_LEFT,
	BUTTON_TOP,
	BUTTON_RIGHT,
	BUTTON_BOTTOM,
	/** Items and spells used for effects and pickups. */
	CASKET,
	CANNON,
	CANNONBALL,
	CHINCHOMPA,
	ICE_SPELL,
	ENEMY_SPELL,
	/** The game's side panel (resizable classic): tab stones, pillars and stone backing, inventory tab selected. */
	SIDE_PANEL,
	/** The same panel collapsed: only its two rows of tab stones, none selected. */
	SIDE_TABS
}

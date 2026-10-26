package com.osrssurvivors.render;

import com.osrssurvivors.game.BossDrop;
import com.osrssurvivors.game.EnemyKind;
import com.osrssurvivors.game.Gear;
import com.osrssurvivors.game.Location;
import com.osrssurvivors.game.Offer;
import com.osrssurvivors.game.Perk;
import com.osrssurvivors.game.Pet;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

/**
 * Optional artwork for the renderer. Every method may return null (or nothing), in which case
 * the renderer falls back to plain shapes and default fonts; {@link #NONE} provides nothing at
 * all.
 */
public interface GameAssets
{
	GameAssets NONE = new GameAssets()
	{
	};

	/** Called once before each frame is drawn, e.g. to reset a per-frame work budget. */
	default void beginFrame()
	{
	}

	/**
	 * @param facing direction of travel in radians (0 = east, PI/2 = south)
	 * @param animTime seconds, for picking the animation frame
	 * @param pixelsPerUnit screen pixels per world unit
	 */
	default ActorSprite enemy(EnemyKind kind, double facing, boolean moving, double animTime, double pixelsPerUnit)
	{
		return null;
	}

	/** A monster, or its bigger champion version. */
	default ActorSprite enemy(EnemyKind kind, boolean champion, double facing, boolean moving, double animTime, double pixelsPerUnit)
	{
		return enemy(kind, facing, moving, animTime, pixelsPerUnit);
	}

	/** Picture for a location on the location menu. */
	default BufferedImage locationIcon(Location location)
	{
		return null;
	}

	/** Picture for a permanent upgrade in the shop. */
	default BufferedImage perkIcon(Perk perk)
	{
		return null;
	}

	/** A pet following the player. */
	default BufferedImage petIcon(Pet pet)
	{
		return null;
	}

	/** A symbol on the boss drop's reels. */
	default BufferedImage prizeIcon(BossDrop.Prize prize)
	{
		return null;
	}

	default ActorSprite player(PlayerPose pose, double pixelsPerUnit)
	{
		return null;
	}

	default BufferedImage sprite(UiSprite sprite)
	{
		return null;
	}

	default BufferedImage offerIcon(Offer offer)
	{
		return null;
	}

	default BufferedImage gearIcon(Gear gear)
	{
		return null;
	}

	/** Image for a spell flying from a magic weapon. */
	default BufferedImage projectile(Gear weapon)
	{
		return null;
	}

	/** Pickup image for an XP orb worth {@code xpValue}. */
	default BufferedImage gem(int xpValue)
	{
		return null;
	}

	/** The ground of a real area, or null for the plain field. */
	default Terrain terrain()
	{
		return null;
	}

	/** Scenery whose position lies in the given world rectangle (with sprites ready to draw). */
	default List<Prop> props(double minX, double minY, double maxX, double maxY, double pixelsPerUnit)
	{
		return Collections.emptyList();
	}

	default Font font(TextStyle style)
	{
		return null;
	}

	/** True when {@link #font} returns bitmap-style fonts that should be drawn without antialiasing. */
	default boolean pixelFonts()
	{
		return false;
	}
}

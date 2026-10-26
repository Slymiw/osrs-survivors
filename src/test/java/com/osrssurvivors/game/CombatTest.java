package com.osrssurvivors.game;

import static com.osrssurvivors.game.TestSessions.EPS;
import static com.osrssurvivors.game.TestSessions.quiet;
import static com.osrssurvivors.game.TestSessions.run;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.Test;

public class CombatTest
{
	private final Player player = new Player(500, 500);
	private final Weapon weapon = new Weapon();
	private final List<Strike> strikes = new ArrayList<>();
	private final List<Effects.Hitsplat> hitsplats = new ArrayList<>();

	private static Enemy enemy(double x, double y)
	{
		return new Enemy(x, y, 100, 0, 0, 1);
	}

	/** Attacks without random rolls, so every hit is the max hit. */
	private List<Enemy> attack(double dt, Enemy... enemies)
	{
		return weapon.update(dt, player, Arrays.asList(enemies), strikes, hitsplats, new java.util.ArrayList<>(), null);
	}

	@Test
	public void startsWithFists()
	{
		Build build = new Build();
		weapon.configure(build);
		assertNull(weapon.getGear());
		assertEquals(Build.FIST_DAMAGE, weapon.getMaxHit(), EPS);
		assertEquals(Build.FIST_RANGE, weapon.getRange(), EPS);
	}

	@Test
	public void punchesNearestEnemyInReach()
	{
		Enemy near = enemy(500, 530);
		Enemy far = enemy(535, 500);
		Enemy outOfReach = enemy(500 + Build.FIST_RANGE + 1, 500);
		List<Enemy> hit = attack(GameSession.STEP, far, outOfReach, near);

		assertEquals(1, hit.size());
		assertSame(near, hit.get(0));
		assertEquals(100 - Build.FIST_DAMAGE, near.health, EPS);
		assertEquals(100, far.health, EPS);
		assertEquals(100, outOfReach.health, EPS);
		assertEquals(1, strikes.size());
		assertNull("fists", strikes.get(0).weapon);
		assertEquals(1, hitsplats.size());
	}

	@Test
	public void respectsCooldown()
	{
		Enemy e = enemy(520, 500);
		assertEquals(1, attack(GameSession.STEP, e).size());
		assertTrue(attack(Build.FIST_COOLDOWN / 2, e).isEmpty());
		assertEquals(1, attack(Build.FIST_COOLDOWN / 2, e).size());
		assertEquals(100 - 2 * Build.FIST_DAMAGE, e.health, EPS);
	}

	@Test
	public void holdsFireUntilTargetInRange()
	{
		Enemy e = enemy(1000, 500);
		assertTrue(attack(5, e).isEmpty());
		e.x = 520;
		// ready immediately once something is in range
		assertEquals(1, attack(GameSession.STEP, e).size());
	}

	@Test
	public void weaponsChangeTheAttack()
	{
		Build build = new Build();
		build.equip(Gear.MAGIC_SHORTBOW);
		weapon.configure(build);
		Enemy e = enemy(500 + 200, 500);
		assertEquals("a bow reaches far", 1, attack(GameSession.STEP, e).size());
		assertEquals(Gear.MAGIC_SHORTBOW.getDamage(), 100 - e.health, EPS);
		assertSame(Gear.MAGIC_SHORTBOW, strikes.get(0).weapon);
	}

	@Test
	public void multiTargetHitsSeveralNearest()
	{
		Build build = new Build();
		build.equip(Gear.SCYTHE_OF_VITUR);
		weapon.configure(build);
		Enemy[] enemies = new Enemy[5];
		for (int i = 0; i < 5; i++)
		{
			enemies[i] = enemy(500 + (i + 1) * 10, 500);
		}
		List<Enemy> primaries = attack(GameSession.STEP, enemies);
		// the three nearest are the targets; the swings catch the two behind them as well
		assertEquals(Arrays.asList(enemies[0], enemies[1], enemies[2]), primaries);
		for (Enemy e : enemies)
		{
			assertTrue(e.health < 100);
		}
	}

	@Test
	public void hitsRollBetweenSixtyAndHundredPercent()
	{
		Random random = new Random(4);
		double min = Double.MAX_VALUE;
		double max = 0;
		for (int i = 0; i < 200; i++)
		{
			Enemy e = enemy(510, 500);
			weapon.update(Build.FIST_COOLDOWN, player, Arrays.asList(e), strikes, hitsplats, new java.util.ArrayList<>(), random);
			double dealt = 100 - e.health;
			min = Math.min(min, dealt);
			max = Math.max(max, dealt);
		}
		assertTrue(min >= Build.FIST_DAMAGE * Weapon.MIN_ROLL - EPS);
		assertTrue(max <= Build.FIST_DAMAGE + EPS);
		assertTrue("rolls vary", max - min > 1);
	}

	@Test
	public void specialAttackHitsDoubleOnItsTimer()
	{
		Build build = new Build();
		build.addRank(Passive.SPECIAL_ATTACK);
		weapon.configure(build);
		Enemy e = enemy(510, 500);
		attack(GameSession.STEP, e);
		assertEquals("first hit is a normal one", 100 - Build.FIST_DAMAGE, e.health, EPS);
		attack(build.specialInterval(), e);
		assertEquals("special doubles", 100 - 3 * Build.FIST_DAMAGE, e.health, EPS);
		assertTrue(hitsplats.get(hitsplats.size() - 1).isSpecial());
	}

	@Test
	public void killsDropXpAndCount()
	{
		GameSession s = quiet();
		Player p = s.getPlayer();
		Enemy e = s.spawnEnemyAt(p.x + 30, p.y, 1);
		s.advance(GameSession.STEP);

		assertTrue(e.isDead());
		assertTrue(s.getEnemies().isEmpty());
		assertEquals(1, s.getKills());
		assertEquals(1, s.getOrbs().size());
		// dropped where it died, then pulled one step towards the player by the magnet
		assertEquals(e.x, s.getOrbs().get(0).getX(), XpOrb.PULL_SPEED * GameSession.STEP + EPS);
	}

	@Test
	public void attackingFacesTheTarget()
	{
		GameSession s = quiet();
		Player p = s.getPlayer();
		s.spawnEnemyAt(p.x - 30, p.y, 1e9);
		s.advance(GameSession.STEP);
		assertEquals(0, p.getAttackAge(), EPS);
		assertEquals(Math.PI, Math.abs(p.getFacing()), EPS);
	}

	@Test
	public void contactDamagesPlayerOverTime()
	{
		GameSession s = quiet();
		Player p = s.getPlayer();
		Enemy e = s.spawnEnemyAt(p.x + Enemy.RADIUS + Player.RADIUS, p.y, 1e9);
		run(s, 1.0);
		// natural regeneration claws a little back; bare fists fight as melee, which guards a little
		double reduction = s.getBuild().damageReduction();
		assertEquals(Build.MELEE_GUARD, reduction, EPS);
		assertEquals(Player.BASE_MAX_HEALTH - e.contactDps * (1 - reduction), p.health, Player.BASE_REGEN + EPS);
	}

	@Test
	public void armourReducesDamage()
	{
		GameSession s = quiet();
		s.grant(Offer.gear(Gear.RUNE_PLATEBODY));
		s.grant(Offer.gear(Gear.RUNE_PLATELEGS));
		Player p = s.getPlayer();
		Enemy e = s.spawnEnemyAt(p.x + Enemy.RADIUS + Player.RADIUS, p.y, 1e9);
		run(s, 1.0);
		double reduction = s.getBuild().damageReduction();
		assertTrue(reduction > 0.2);
		assertEquals(Player.BASE_MAX_HEALTH - e.contactDps * (1 - reduction), p.health, Player.BASE_REGEN + EPS);
	}

	@Test
	public void takenDamageShowsAsPlayerHitsplats()
	{
		GameSession s = quiet();
		Player p = s.getPlayer();
		s.spawnEnemyAt(p.x + Enemy.RADIUS + Player.RADIUS, p.y, 1e9);
		run(s, 2.0);
		boolean playerSplat = false;
		for (Effects.Hitsplat h : s.getHitsplats())
		{
			playerSplat |= h.getTarget() == null && h.getDamage() > 0;
		}
		assertTrue(playerSplat);
	}

	@Test
	public void vengeanceHurtsEnemiesThatTouchYou()
	{
		GameSession s = quiet();
		s.grant(Offer.passive(Passive.VENGEANCE, 1));
		Player p = s.getPlayer();
		// out of punching reach is impossible while touching, so give it plenty of health
		Enemy e = s.spawnEnemyAt(p.x + Enemy.RADIUS + Player.RADIUS, p.y, 1e9);
		double before = e.health;
		run(s, 1.0);
		double punches = before - e.health;
		assertTrue("vengeance adds damage beyond punches", punches > 15);
	}

	@Test
	public void redemptionBuysTime()
	{
		double without = survivalTime(false);
		double with = survivalTime(true);
		assertTrue("redemption heals once, so death comes later: " + without + " vs " + with, with > without + 0.5);
	}

	/** Seconds until a swarmed player dies. */
	private static double survivalTime(boolean redemption)
	{
		GameSession s = quiet();
		if (redemption)
		{
			s.grant(Offer.passive(Passive.REDEMPTION, 1));
		}
		Player p = s.getPlayer();
		for (int i = 0; i < 4; i++)
		{
			double angle = i * Math.PI / 2;
			s.spawnEnemyAt(p.x + Math.cos(angle) * 30, p.y + Math.sin(angle) * 30, 1e9);
		}
		run(s, 30);
		assertEquals(Phase.GAME_OVER, s.getPhase());
		return s.getElapsed();
	}

	@Test
	public void noDamageWithoutContact()
	{
		GameSession s = quiet();
		Player p = s.getPlayer();
		s.spawnEnemyAt(p.x + 1000, p.y, 1e9);
		run(s, 1.0);
		assertEquals(Player.BASE_MAX_HEALTH, p.health, 0);
		assertFalse(p.isDead());
	}
}

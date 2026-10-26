package com.osrssurvivors.game;

/**
 * Monster varieties. Stats scale the time-based base values from {@link Spawner}; which kinds
 * appear, and when, depends on the {@link Location}. Some keep their distance and cast spells or
 * fire arrows instead of only walking into the player.
 */
public enum EnemyKind
{
	CHICKEN("Chicken", 0.4, 1.3, 7, 3, 1),
	RAT("Giant rat", 0.6, 1.25, 8, 4, 1),
	GOBLIN("Goblin", 0.8, 1.1, 9, 5, 1),
	MAN("Man", 1.0, 1.0, 10, 5, 1),
	MONKEY("Monkey", 0.5, 1.5, 7, 4, 1),
	CRAWLING_HAND("Crawling hand", 0.8, 1.2, 8, 6, 1),
	COW("Cow", 2.0, 0.7, 14, 6, 2),
	GIANT_SPIDER("Giant spider", 0.7, 1.4, 9, 6, 1),
	ZOMBIE("Zombie", 1.5, 0.8, 10, 8, 2),
	SKELETON("Skeleton", 1.2, 1.1, 10, 10, 3),
	GUARD("Guard", 1.6, 1.0, 10, 10, 3),
	DWARF("Dwarf", 1.4, 0.9, 9, 10, 3),
	BARBARIAN("Barbarian", 1.8, 1.05, 11, 12, 3),
	SCORPION("Scorpion", 1.3, 1.15, 11, 10, 3),
	BANDIT("Bandit", 1.8, 1.1, 10, 13, 4),
	WOLF("Wolf", 1.4, 1.5, 11, 12, 3),
	GHOUL("Ghoul", 2.2, 0.95, 11, 14, 4),
	JUNGLE_SPIDER("Jungle spider", 1.5, 1.4, 10, 14, 4),
	REVENANT_IMP("Revenant imp", 1.5, 1.45, 8, 10, 4),
	DARK_WIZARD("Dark wizard", 1.0, 0.95, 10, 4, 3, Attack.MAGIC, 5, 190, 4.0),
	CHAOS_DRUID("Chaos druid", 1.2, 1.0, 10, 5, 3, Attack.MAGIC, 5, 180, 4.2),
	SKELETON_MAGE("Skeleton mage", 1.3, 1.0, 10, 5, 3, Attack.MAGIC, 6, 190, 4.0),
	ARCHER("Archer", 1.3, 1.05, 10, 5, 3, Attack.ARROW, 5, 230, 3.5),
	WHITE_KNIGHT("White knight", 2.4, 0.95, 11, 14, 4),
	BLACK_KNIGHT("Black knight", 2.4, 1.0, 11, 15, 4),
	ICE_WARRIOR("Ice warrior", 2.6, 0.95, 11, 15, 5),
	KALPHITE_WORKER("Kalphite worker", 2.0, 1.05, 13, 12, 4),
	BANSHEE("Banshee", 2.2, 1.1, 12, 14, 5),
	CROCODILE("Crocodile", 3.0, 1.0, 15, 16, 6),
	HILL_GIANT("Hill giant", 4.0, 0.75, 17, 18, 7),
	MOSS_GIANT("Moss giant", 5.0, 0.75, 18, 20, 8),
	ICE_GIANT("Ice giant", 5.5, 0.75, 18, 21, 9),
	JOGRE("Jogre", 5.0, 0.8, 17, 20, 8),
	WEREWOLF("Werewolf", 4.0, 1.25, 12, 22, 9),
	FERAL_VAMPYRE("Feral vampyre", 4.5, 1.2, 11, 22, 10),
	KALPHITE_SOLDIER("Kalphite soldier", 6.5, 0.9, 20, 24, 12),
	FIRE_GIANT("Fire giant", 7.0, 0.8, 19, 26, 13),
	BLOODVELD("Bloodveld", 6.0, 1.0, 15, 26, 13),
	LESSER_DEMON("Lesser demon", 6.0, 0.85, 15, 22, 10),
	GREATER_DEMON("Greater demon", 8.0, 0.9, 16, 25, 15),
	REVENANT_KNIGHT("Revenant knight", 9.0, 1.0, 12, 28, 18, Attack.MAGIC, 11, 200, 3.6),
	ABYSSAL_DEMON("Abyssal demon", 9.0, 1.15, 13, 30, 20),
	GREEN_DRAGON("Green dragon", 12.0, 0.85, 20, 30, 25),
	BLACK_DEMON("Black demon", 13.0, 0.9, 17, 32, 24),
	CHAOS_ELEMENTAL("Chaos Elemental", 20.0, 0.8, 22, 40, 40, Attack.MAGIC, 20, 220, 2.8),

	// bosses: champions only, in rising order of fame. Their toughness comes from the strongest
	// monster around when they arrive; missile damage is relative to that monster's.
	GIANT_MOLE("Giant Mole", 18),
	SARACHNIS("Sarachnis", 17),
	DAGANNOTH_REX("Dagannoth Rex", 18),
	SCORPIA("Scorpia", 17),
	KALPHITE_QUEEN("Kalphite Queen", 19),
	KING_BLACK_DRAGON("King Black Dragon", 20, Attack.MAGIC, 1.0, 210, 3.2),
	GENERAL_GRAARDOR("General Graardor", 19),
	COMMANDER_ZILYANA("Commander Zilyana", 16),
	KRIL_TSUTSAROTH("K'ril Tsutsaroth", 19),
	KREEARRA("Kree'arra", 19, Attack.ARROW, 0.9, 240, 2.8),
	CERBERUS("Cerberus", 20),
	// Chambers of Xeric
	ICE_DEMON("Ice demon", 18, Attack.MAGIC, 1.0, 220, 3.0),
	MUTTADILE("Muttadile", 21),
	VANGUARD("Vanguard", 19),
	VESPULA("Vespula", 21, Attack.ARROW, 1.0, 230, 2.8),
	TEKTON("Tekton", 21),
	VASA_NISTIRIO("Vasa Nistirio", 22, Attack.MAGIC, 1.1, 230, 3.0),
	ZULRAH("Zulrah", 19, Attack.MAGIC, 1.0, 230, 2.6),
	VORKATH("Vorkath", 21, Attack.MAGIC, 1.1, 230, 3.0),
	TZTOK_JAD("TzTok-Jad", 21, Attack.MAGIC, 1.3, 240, 4.0),
	CORPOREAL_BEAST("Corporeal Beast", 21),
	ALCHEMICAL_HYDRA("Alchemical Hydra", 20, Attack.MAGIC, 1.0, 220, 2.8),
	// Tombs of Amascut
	BA_BA("Ba-Ba", 21),
	AKKHA("Akkha", 19, Attack.MAGIC, 1.0, 220, 2.8),
	KEPHRI("Kephri", 21, Attack.MAGIC, 1.0, 230, 2.8),
	TUMEKENS_WARDEN("Tumeken's Warden", 22, Attack.MAGIC, 1.2, 240, 2.6),
	THE_NIGHTMARE("The Nightmare", 21),
	PHANTOM_MUSPAH("Phantom Muspah", 19, Attack.ARROW, 1.0, 240, 2.6),
	DUKE_SUCELLUS("Duke Sucellus", 21),
	VARDORVIS("Vardorvis", 18),
	THE_LEVIATHAN("The Leviathan", 21, Attack.MAGIC, 1.1, 230, 2.6),
	THE_WHISPERER("The Whisperer", 19, Attack.MAGIC, 1.1, 220, 2.6),
	ARAXXOR("Araxxor", 21),
	NEX("Nex", 18, Attack.MAGIC, 1.2, 220, 2.4),
	VERZIK_VITUR("Verzik Vitur", 20, Attack.MAGIC, 1.2, 230, 2.4),
	YAMA("Yama", 21, Attack.MAGIC, 1.3, 230, 2.4),
	// the Fractured Archive
	DAGONHAI_ELDER("Dagon'hai Elder", 17, Attack.MAGIC, 1.2, 230, 2.4),
	THE_SMORKS("The Smorks", 20),
	BORK("Bork", 22),
	DREADNOTH("Dreadnoth the Jellied", 22),
	XARTH("Xarth", 21, Attack.ARROW, 1.2, 240, 2.4),
	MOIA("Moia", 21, Attack.MAGIC, 1.3, 230, 2.2),
	EZEKIEL("Ezekiel", 21),
	ARCHIVE_GUARDIAN("Archive Guardian", 22),
	OCELLUS("Ocellus", 21, Attack.MAGIC, 1.4, 240, 2.2);

	/** How a ranged monster attacks. */
	public enum Attack
	{
		NONE, MAGIC, ARROW
	}

	final String title;
	final double healthScale;
	final double speedScale;
	final double radius;
	final double contactDps;
	final int xp;
	final Attack attack;
	/** Damage per missile, before the time-based scaling. */
	final double attackDamage;
	final double attackRange;
	final double attackCooldown;
	/** A boss, which only ever comes as a champion. */
	final boolean boss;

	EnemyKind(String title, double healthScale, double speedScale, double radius, double contactDps, int xp)
	{
		this(title, healthScale, speedScale, radius, contactDps, xp, Attack.NONE, 0, 0, 0);
	}

	/** A boss that fights in melee. */
	EnemyKind(String title, double radius)
	{
		this(title, radius, Attack.NONE, 0, 0, 0);
	}

	/** A boss; {@code attackDamage} is relative to the monster its strength comes from. */
	EnemyKind(String title, double radius, Attack attack, double attackDamage, double attackRange, double attackCooldown)
	{
		this(title, 1, 1, radius, 0, 0, attack, attackDamage, attackRange, attackCooldown, true);
	}

	EnemyKind(String title, double healthScale, double speedScale, double radius, double contactDps, int xp, Attack attack,
		double attackDamage, double attackRange, double attackCooldown)
	{
		this(title, healthScale, speedScale, radius, contactDps, xp, attack, attackDamage, attackRange, attackCooldown, false);
	}

	EnemyKind(String title, double healthScale, double speedScale, double radius, double contactDps, int xp, Attack attack,
		double attackDamage, double attackRange, double attackCooldown, boolean boss)
	{
		this.title = title;
		this.healthScale = healthScale;
		this.speedScale = speedScale;
		this.radius = radius;
		this.contactDps = contactDps;
		this.xp = xp;
		this.attack = attack;
		this.attackDamage = attackDamage;
		this.attackRange = attackRange;
		this.attackCooldown = attackCooldown;
		this.boss = boss;
	}

	public boolean isBoss()
	{
		return boss;
	}

	public String getTitle()
	{
		return title;
	}

	public double getRadius()
	{
		return radius;
	}

	public Attack getAttack()
	{
		return attack;
	}

	public boolean isRanged()
	{
		return attack != Attack.NONE;
	}
}

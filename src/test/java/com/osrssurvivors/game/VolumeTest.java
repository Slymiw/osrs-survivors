package com.osrssurvivors.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class VolumeTest
{
	@Test
	public void volumesStartAtTheirDefaultsAndStayInRange()
	{
		Options o = new Options();
		for (Volume v : Volume.values())
		{
			assertEquals(v.getInitial(), o.getVolume(v));
		}
		o.setVolume(Volume.EFFECTS, 99);
		assertEquals(Volume.MAX, o.getVolume(Volume.EFFECTS));
		o.setVolume(Volume.EFFECTS, -3);
		assertEquals(0, o.getVolume(Volume.EFFECTS));
	}

	@Test
	public void loudnessFollowsACurve()
	{
		Options o = new Options();
		o.setVolume(Volume.MASTER, 0);
		assertEquals(0, o.gain(Volume.MASTER), 0);
		o.setVolume(Volume.MASTER, Volume.MAX);
		assertEquals(1, o.gain(Volume.MASTER), 1e-9);
		o.setVolume(Volume.MASTER, Volume.MAX / 2);
		assertEquals("half way is a quarter as loud", 0.25, o.gain(Volume.MASTER), 1e-9);
	}

	@Test
	public void muteSilencesWithoutForgettingTheVolume()
	{
		Options o = new Options();
		o.setMuted(true);
		assertEquals(0, o.gain(Volume.MASTER), 0);
		assertEquals(Volume.MASTER.getInitial(), o.getVolume(Volume.MASTER));
		assertTrue("the other buses keep their levels", o.gain(Volume.EFFECTS) > 0);
		o.setVolume(Volume.MASTER, 5);
		assertFalse("turning the master up unmutes", o.isMuted());
	}
}

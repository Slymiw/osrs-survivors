package com.osrssurvivors.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ModelRasterizerTest
{
	private static final int RED = 0xffff0000;
	private static final int BLUE = 0xff0000ff;

	/** Two upright triangles: one on the model's front (south, -z), one behind it to the north. */
	private static MeshSnapshot frontAndBack(int frontColour, int backColour)
	{
		float[] x = {-10, 10, 0, -10, 10, 0};
		float[] y = {0, 0, -40, 0, 0, -40};
		float[] z = {-20, -20, -20, 20, 20, 20};
		return new MeshSnapshot(x, y, z, new int[]{0, 3}, new int[]{1, 4}, new int[]{2, 5}, new int[]{frontColour, backColour});
	}

	@Test
	public void rendersWithFeetAtTheAnchor()
	{
		// a triangle standing on the origin, pointing up (negative y is up)
		MeshSnapshot mesh = new MeshSnapshot(new float[]{-10, 10, 0}, new float[]{0, 0, -40}, new float[]{0, 0, 0},
			new int[]{0}, new int[]{1}, new int[]{2}, new int[]{RED});
		ActorSprite s = ModelRasterizer.render(mesh, 0, 0, 1);
		assertNotNull(s);
		assertEquals(RED, s.image.getRGB(s.anchorX, s.anchorY - 5));
		assertTrue("extends upwards from the feet", s.anchorY >= 39);
		assertTrue("nothing far below the feet", s.image.getHeight() - s.anchorY <= 3);
	}

	@Test
	public void nearerFacesAreDrawnOnTop()
	{
		// unrotated, the camera is to the south, so the front (red) triangle hides the back one
		ActorSprite s = ModelRasterizer.render(frontAndBack(RED, BLUE), 0, 0, 1);
		assertEquals(RED, s.image.getRGB(s.anchorX, s.anchorY - 10));

		// facing north, the blue triangle is now the nearer one
		ActorSprite turned = ModelRasterizer.render(frontAndBack(RED, BLUE), ModelRasterizer.yawFor(-Math.PI / 2), 0, 1);
		assertEquals(BLUE, turned.image.getRGB(turned.anchorX, turned.anchorY - 10));
	}

	@Test
	public void yawTurnsTheFrontTowardsTheFacing()
	{
		assertEquals("facing south needs no turn", 0, ModelRasterizer.yawFor(Math.PI / 2), 1e-9);
		// the model's front point (0, 0, -1) lands on the screen side it should face
		double east = ModelRasterizer.yawFor(0);
		assertEquals(1, -Math.sin(east), 1e-9);
		double west = ModelRasterizer.yawFor(Math.PI);
		assertEquals(-1, -Math.sin(west), 1e-9);
	}

	@Test
	public void skipsHiddenFaces()
	{
		assertNull(ModelRasterizer.render(frontAndBack(0, 0), 0, 0, 1));
		ActorSprite s = ModelRasterizer.render(frontAndBack(0, BLUE), 0, 0, 1);
		assertEquals(BLUE, s.image.getRGB(s.anchorX, s.anchorY - 10));
	}

	@Test
	public void scaleAndPitchChangeTheSize()
	{
		ActorSprite small = ModelRasterizer.render(frontAndBack(RED, BLUE), 0, 0, 1);
		ActorSprite big = ModelRasterizer.render(frontAndBack(RED, BLUE), 0, 0, 2);
		assertTrue(big.image.getWidth() > small.image.getWidth());
		ActorSprite tilted = ModelRasterizer.render(frontAndBack(RED, BLUE), 0, Math.toRadians(35), 1);
		assertTrue("looking down shows depth", tilted.image.getHeight() > small.image.getHeight());
	}

	@Test
	public void paletteMapsLuminanceAndHue()
	{
		int[] palette = JagexPalette.build(0.7);
		assertEquals(65536, palette.length);
		assertTrue((palette[0] & 0xff) < 10);
		assertTrue((palette[127] & 0xff) > 240);
		// hue 0, high saturation, mid luminance: red dominates
		int red = palette[7 << 7 | 64];
		assertTrue((red >> 16 & 0xff) > (red >> 8 & 0xff) + 100);
	}
}

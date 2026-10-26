package com.osrssurvivors.web;

/**
 * An animation made of classic frames: each frame moves labelled groups of vertices with the
 * transforms of its framemap. Frames last a number of 20 ms game cycles.
 */
final class Sequence
{
	/** Game cycles are 20 ms. */
	private static final double SECONDS_PER_CYCLE = 0.02;

	/** Which transform each slot does and to which vertex labels. */
	static final class Framemap
	{
		final int[] types;
		final int[][] labels;

		Framemap(int[] types, int[][] labels)
		{
			this.types = types;
			this.labels = labels;
		}
	}

	static final class Frame
	{
		final Framemap framemap;
		/** Framemap slot per transform, and its arguments. */
		final int[] slots;
		final int[] dx;
		final int[] dy;
		final int[] dz;

		Frame(Framemap framemap, int[] slots, int[] dx, int[] dy, int[] dz)
		{
			this.framemap = framemap;
			this.slots = slots;
			this.dx = dx;
			this.dy = dy;
			this.dz = dz;
		}
	}

	final int id;
	final Frame[] frames;
	final int[] lengths;
	private final int totalCycles;

	Sequence(int id, Frame[] frames, int[] lengths)
	{
		this.id = id;
		this.frames = frames;
		this.lengths = lengths;
		int total = 0;
		for (int l : lengths)
		{
			total += Math.max(1, l);
		}
		this.totalCycles = Math.max(1, total);
	}

	/** Frame to show {@code seconds} into the looping animation. */
	int loopFrame(double seconds)
	{
		return frameAt(seconds / SECONDS_PER_CYCLE, true);
	}

	/** Frame at a fraction (0..1) of the animation played once. */
	int progressFrame(double progress)
	{
		return frameAt(Math.max(0, Math.min(0.999, progress)) * totalCycles, false);
	}

	private int frameAt(double cycles, boolean loop)
	{
		int cycle = (int) cycles;
		cycle = loop ? cycle % totalCycles : Math.min(cycle, totalCycles - 1);
		for (int i = 0; i < lengths.length; i++)
		{
			cycle -= Math.max(1, lengths[i]);
			if (cycle < 0)
			{
				return i;
			}
		}
		return 0;
	}
}

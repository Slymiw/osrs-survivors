package com.osrssurvivors.audio;

/**
 * An OSRS sound effect: the client's own synthesiser format (cache index 4), turned into samples
 * the same way the game client does. A sound is up to ten instruments; each one has pitch and
 * volume envelopes, optional vibrato, tremolo and gating, up to ten oscillators (square, sine,
 * saw or noise), an echo and a pole-zero filter. The client mixes at 22050 Hz into 8-bit samples;
 * this keeps the same maths but returns the mix as floats in -1..1 (16-bit precision), clipped
 * where the client would clip.
 *
 * <p>Plain Java with no library calls beyond {@link Math}, so it runs in the browser (TeaVM) and
 * in tests alike; the noise table uses its own copy of {@code java.util.Random}'s generator so the
 * noise is the client's in both.
 */
public final class SynthSound
{
	public static final int SAMPLE_RATE = 22050;
	private static final int INSTRUMENTS = 10;
	private static final int OSCILLATORS = 10;

	private static final int[] NOISE = new int[32768];
	private static final int[] SINE = new int[32768];

	static
	{
		// java.util.Random(0): the client fills its noise table from it
		long seed = (0L ^ 0x5DEECE66DL) & ((1L << 48) - 1);
		for (int i = 0; i < NOISE.length; i++)
		{
			seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
			int next = (int) (seed >>> 16);
			NOISE[i] = (next & 2) - 1;
		}
		for (int i = 0; i < SINE.length; i++)
		{
			SINE[i] = (int) (Math.sin(i / 5215.1903) * 16384.0);
		}
	}

	private final Instrument[] instruments = new Instrument[INSTRUMENTS];
	private int loopStart;
	private int loopEnd;

	private SynthSound()
	{
	}

	/** Reads a sound effect from its cache file. Throws IllegalArgumentException if it's malformed. */
	public static SynthSound decode(byte[] data)
	{
		Reader in = new Reader(data);
		SynthSound s = new SynthSound();
		try
		{
			for (int i = 0; i < INSTRUMENTS; i++)
			{
				if (in.peek() != 0)
				{
					s.instruments[i] = Instrument.decode(in);
				}
				else
				{
					in.pos++;
				}
			}
			s.loopStart = in.u16();
			s.loopEnd = in.u16();
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			throw new IllegalArgumentException("sound effect data ends early");
		}
		return s;
	}

	/** Length in milliseconds: the end of the last instrument. */
	public int getDurationMs()
	{
		int ms = 0;
		for (Instrument i : instruments)
		{
			if (i != null)
			{
				ms = Math.max(ms, i.duration + i.offset);
			}
		}
		return ms;
	}

	/** The mixed sound at {@link #SAMPLE_RATE}, samples in -1..1. Empty for a silent sound. */
	public float[] mix()
	{
		int length = getDurationMs() * SAMPLE_RATE / 1000;
		int[] sum = new int[length];
		for (Instrument ins : instruments)
		{
			if (ins == null)
			{
				continue;
			}
			int steps = ins.duration * SAMPLE_RATE / 1000;
			int start = ins.offset * SAMPLE_RATE / 1000;
			int[] samples = ins.synthesize(steps, ins.duration);
			for (int i = 0; i < steps && start + i < length; i++)
			{
				int v = sum[start + i] + samples[i];
				// the client clips its 8-bit mix; this is the same limit at 16 bits
				sum[start + i] = Math.max(-32768, Math.min(32767, v));
			}
		}
		float[] out = new float[length];
		for (int i = 0; i < length; i++)
		{
			out[i] = sum[i] / 32768f;
		}
		return out;
	}

	/** Below this a sample counts as silence, for {@link #trimLead}. */
	static final float SILENCE = 0.004f;
	/** Kept before the first sound, so its attack isn't cut. */
	static final int LEAD_KEEP = SAMPLE_RATE / 200;

	/**
	 * The samples without their leading silence. The game's attack sounds start up to 0.7 s in, to
	 * meet the impact frame of the client's attack animation; here hits show at once, so the
	 * sound must too.
	 */
	public static float[] trimLead(float[] samples)
	{
		int first = 0;
		while (first < samples.length && Math.abs(samples[first]) < SILENCE)
		{
			first++;
		}
		first = Math.max(0, first - LEAD_KEEP);
		if (first == 0 || first >= samples.length)
		{
			return samples;
		}
		float[] out = new float[samples.length - first];
		System.arraycopy(samples, first, out, 0, out.length);
		return out;
	}

	/** Root mean square of the samples: how loud they are overall. */
	public static double rms(float[] samples)
	{
		double sum = 0;
		for (float f : samples)
		{
			sum += f * f;
		}
		return samples.length == 0 ? 0 : Math.sqrt(sum / samples.length);
	}

	/** Big-endian reads over the cache file, with the client's "smart" numbers. */
	private static final class Reader
	{
		final byte[] data;
		int pos;

		Reader(byte[] data)
		{
			this.data = data;
		}

		int peek()
		{
			return data[pos] & 0xff;
		}

		int u8()
		{
			return data[pos++] & 0xff;
		}

		int u16()
		{
			return u8() << 8 | u8();
		}

		int s32()
		{
			return u8() << 24 | u8() << 16 | u8() << 8 | u8();
		}

		/** 0..32767 in one or two bytes. */
		int uSmart()
		{
			return peek() < 128 ? u8() : u16() - 32768;
		}

		/** -64..63 in one byte, else -16384..16383 in two. */
		int sSmart()
		{
			return peek() < 128 ? u8() - 64 : u16() - 49152;
		}
	}

	/** A piecewise-linear curve over an instrument's length, with a waveform for modulators. */
	private static final class Envelope
	{
		int form;
		int start;
		int end;
		int segments = 2;
		int[] durations = {0, 65535};
		int[] peaks = {0, 65535};

		int threshold;
		int position;
		int step;
		int amplitude;
		int ticks;

		static Envelope decode(Reader in)
		{
			Envelope e = new Envelope();
			e.form = in.u8();
			e.start = in.s32();
			e.end = in.s32();
			e.decodeShape(in);
			return e;
		}

		void decodeShape(Reader in)
		{
			segments = in.u8();
			durations = new int[segments];
			peaks = new int[segments];
			for (int i = 0; i < segments; i++)
			{
				durations[i] = in.u16();
				peaks[i] = in.u16();
			}
		}

		void reset()
		{
			threshold = 0;
			position = 0;
			step = 0;
			amplitude = 0;
			ticks = 0;
		}

		int step(int period)
		{
			if (ticks >= threshold)
			{
				amplitude = peaks[position++] << 15;
				if (position >= segments)
				{
					position = segments - 1;
				}
				threshold = (int) (durations[position] / 65536.0 * period);
				if (threshold > ticks)
				{
					step = ((peaks[position] << 15) - amplitude) / (threshold - ticks);
				}
			}
			amplitude += step;
			ticks++;
			return amplitude - step >> 15;
		}
	}

	/** The client's pole-zero filter, its poles and zeros moving between two settings. */
	private static final class Filter
	{
		final int[] pairs = new int[2];
		final int[][][] frequencies = new int[2][2][4];
		final int[][][] magnitudes = new int[2][2][4];
		final int[] unity = new int[2];

		// worked out by compute(): the coefficients for the current position, as 16.16 numbers
		final float[][] coefficientsF = new float[2][8];
		final int[][] coefficients = new int[2][8];
		float forwardGainF;
		int forwardGain;

		void decode(Reader in, Envelope envelope)
		{
			int count = in.u8();
			pairs[0] = count >> 4;
			pairs[1] = count & 15;
			if (count == 0)
			{
				unity[0] = unity[1] = 0;
				return;
			}
			unity[0] = in.u16();
			unity[1] = in.u16();
			int migration = in.u8();
			for (int dir = 0; dir < 2; dir++)
			{
				for (int term = 0; term < pairs[dir]; term++)
				{
					frequencies[dir][0][term] = in.u16();
					magnitudes[dir][0][term] = in.u16();
				}
			}
			for (int dir = 0; dir < 2; dir++)
			{
				for (int term = 0; term < pairs[dir]; term++)
				{
					if ((migration & 1 << dir * 4 << term) != 0)
					{
						frequencies[dir][1][term] = in.u16();
						magnitudes[dir][1][term] = in.u16();
					}
					else
					{
						frequencies[dir][1][term] = frequencies[dir][0][term];
						magnitudes[dir][1][term] = magnitudes[dir][0][term];
					}
				}
			}
			if (migration != 0 || unity[1] != unity[0])
			{
				envelope.decodeShape(in);
			}
		}

		private float magnitude(int dir, int k, float f)
		{
			float alpha = magnitudes[dir][0][k] + f * (magnitudes[dir][1][k] - magnitudes[dir][0][k]);
			alpha *= 0.0015258789f;
			return 1.0f - (float) Math.pow(10.0, -alpha / 20.0f);
		}

		private static float normalise(float f)
		{
			float hz = 32.703197f * (float) Math.pow(2.0, f);
			return hz * 3.1415927f / 11025.0f;
		}

		private float phase(int dir, int k, float f)
		{
			float p = frequencies[dir][0][k] + f * (frequencies[dir][1][k] - frequencies[dir][0][k]);
			p *= 1.2207031e-4f;
			return normalise(p);
		}

		/** Works out one side's coefficients at position {@code f} (0..1); returns how many there are. */
		int compute(int dir, float f)
		{
			if (dir == 0)
			{
				float gain = unity[0] + (unity[1] - unity[0]) * f;
				gain *= 0.0030517578f;
				forwardGainF = (float) Math.pow(0.1, gain / 20.0f);
				forwardGain = (int) (forwardGainF * 65536.0f);
			}
			if (pairs[dir] == 0)
			{
				return 0;
			}
			float[] c = coefficientsF[dir];
			float m = magnitude(dir, 0, f);
			c[0] = -2.0f * m * (float) Math.cos(phase(dir, 0, f));
			c[1] = m * m;
			for (int k = 1; k < pairs[dir]; k++)
			{
				float mk = magnitude(dir, k, f);
				float a = -2.0f * mk * (float) Math.cos(phase(dir, k, f));
				float b = mk * mk;
				c[k * 2 + 1] = c[k * 2 - 1] * b;
				c[k * 2] = c[k * 2 - 1] * a + c[k * 2 - 2] * b;
				for (int j = k * 2 - 1; j >= 2; j--)
				{
					c[j] += c[j - 1] * a + c[j - 2] * b;
				}
				c[1] += c[0] * a + b;
				c[0] += a;
			}
			if (dir == 0)
			{
				for (int i = 0; i < pairs[0] * 2; i++)
				{
					c[i] *= forwardGainF;
				}
			}
			for (int i = 0; i < pairs[dir] * 2; i++)
			{
				coefficients[dir][i] = (int) (c[i] * 65536.0f);
			}
			return pairs[dir] * 2;
		}
	}

	/** One voice of a sound: oscillators shaped by envelopes, then echoed and filtered. */
	private static final class Instrument
	{
		Envelope pitch;
		Envelope volume;
		Envelope pitchModifier;
		Envelope pitchModifierAmplitude;
		Envelope volumeModifier;
		Envelope volumeModifierAmplitude;
		Envelope release;
		Envelope attack;
		final int[] oscillatorVolume = new int[OSCILLATORS];
		final int[] oscillatorPitch = new int[OSCILLATORS];
		final int[] oscillatorDelay = new int[OSCILLATORS];
		int delayTime;
		int delayDecay = 100;
		Filter filter;
		Envelope filterEnvelope;
		/** Milliseconds. */
		int duration = 500;
		int offset;

		static Instrument decode(Reader in)
		{
			Instrument ins = new Instrument();
			ins.pitch = Envelope.decode(in);
			ins.volume = Envelope.decode(in);
			if (in.peek() != 0)
			{
				ins.pitchModifier = Envelope.decode(in);
				ins.pitchModifierAmplitude = Envelope.decode(in);
			}
			else
			{
				in.pos++;
			}
			if (in.peek() != 0)
			{
				ins.volumeModifier = Envelope.decode(in);
				ins.volumeModifierAmplitude = Envelope.decode(in);
			}
			else
			{
				in.pos++;
			}
			if (in.peek() != 0)
			{
				ins.release = Envelope.decode(in);
				ins.attack = Envelope.decode(in);
			}
			else
			{
				in.pos++;
			}
			for (int i = 0; i < OSCILLATORS; i++)
			{
				int v = in.uSmart();
				if (v == 0)
				{
					break;
				}
				ins.oscillatorVolume[i] = v;
				ins.oscillatorPitch[i] = in.sSmart();
				ins.oscillatorDelay[i] = in.uSmart();
			}
			ins.delayTime = in.uSmart();
			ins.delayDecay = in.uSmart();
			ins.duration = in.u16();
			ins.offset = in.u16();
			ins.filter = new Filter();
			ins.filterEnvelope = new Envelope();
			ins.filter.decode(in, ins.filterEnvelope);
			return ins;
		}

		private static int wave(int amplitude, int phase, int form)
		{
			switch (form)
			{
				case 1:
					return (phase & 0x7fff) < 16384 ? amplitude : -amplitude;
				case 2:
					return SINE[phase & 0x7fff] * amplitude >> 14;
				case 3:
					return ((phase & 0x7fff) * amplitude >> 14) - amplitude;
				case 4:
					return NOISE[phase / 2607 & 0x7fff] * amplitude;
				default:
					return 0;
			}
		}

		int[] synthesize(int steps, int durationMs)
		{
			int[] out = new int[Math.max(0, steps)];
			if (durationMs < 10 || steps <= 0)
			{
				return out;
			}
			double perMs = steps / (double) durationMs;
			pitch.reset();
			volume.reset();
			int pitchModStep = 0;
			int pitchModBase = 0;
			int pitchModPhase = 0;
			if (pitchModifier != null)
			{
				pitchModifier.reset();
				pitchModifierAmplitude.reset();
				pitchModStep = (int) ((pitchModifier.end - pitchModifier.start) * 32.768 / perMs);
				pitchModBase = (int) (pitchModifier.start * 32.768 / perMs);
			}
			int volumeModStep = 0;
			int volumeModBase = 0;
			int volumeModPhase = 0;
			if (volumeModifier != null)
			{
				volumeModifier.reset();
				volumeModifierAmplitude.reset();
				volumeModStep = (int) ((volumeModifier.end - volumeModifier.start) * 32.768 / perMs);
				volumeModBase = (int) (volumeModifier.start * 32.768 / perMs);
			}
			int[] phases = new int[OSCILLATORS];
			int[] delays = new int[OSCILLATORS];
			int[] volumeSteps = new int[OSCILLATORS];
			int[] pitchSteps = new int[OSCILLATORS];
			int[] pitchBases = new int[OSCILLATORS];
			for (int j = 0; j < OSCILLATORS; j++)
			{
				if (oscillatorVolume[j] != 0)
				{
					delays[j] = (int) (oscillatorDelay[j] * perMs);
					volumeSteps[j] = (oscillatorVolume[j] << 14) / 100;
					pitchSteps[j] = (int) ((pitch.end - pitch.start) * 32.768 * Math.pow(1.0057929410678534, oscillatorPitch[j]) / perMs);
					pitchBases[j] = (int) (pitch.start * 32.768 / perMs);
				}
			}
			for (int k = 0; k < steps; k++)
			{
				int pitchChange = pitch.step(steps);
				int volumeChange = volume.step(steps);
				if (pitchModifier != null)
				{
					int mod = pitchModifier.step(steps);
					int modAmp = pitchModifierAmplitude.step(steps);
					pitchChange += wave(modAmp, pitchModPhase, pitchModifier.form) >> 1;
					pitchModPhase += (mod * pitchModStep >> 16) + pitchModBase;
				}
				if (volumeModifier != null)
				{
					int mod = volumeModifier.step(steps);
					int modAmp = volumeModifierAmplitude.step(steps);
					volumeChange = volumeChange * ((wave(modAmp, volumeModPhase, volumeModifier.form) >> 1) + 32768) >> 15;
					volumeModPhase += (mod * volumeModStep >> 16) + volumeModBase;
				}
				for (int j = 0; j < OSCILLATORS; j++)
				{
					if (oscillatorVolume[j] != 0)
					{
						int at = k + delays[j];
						if (at < steps)
						{
							out[at] += wave(volumeChange * volumeSteps[j] >> 15, phases[j], pitch.form);
							phases[j] += (pitchChange * pitchSteps[j] >> 16) + pitchBases[j];
						}
					}
				}
			}
			if (release != null)
			{
				release.reset();
				attack.reset();
				int counter = 0;
				boolean muted = true;
				for (int k = 0; k < steps; k++)
				{
					int on = release.step(steps);
					int off = attack.step(steps);
					int threshold = muted
						? release.start + ((release.end - release.start) * on >> 8)
						: release.start + ((release.end - release.start) * off >> 8);
					counter += 256;
					if (counter >= threshold)
					{
						counter = 0;
						muted = !muted;
					}
					if (muted)
					{
						out[k] = 0;
					}
				}
			}
			if (delayTime > 0 && delayDecay > 0)
			{
				int delay = (int) (delayTime * perMs);
				for (int k = delay; k < steps; k++)
				{
					out[k] += out[k - delay] * delayDecay / 100;
				}
			}
			if (filter.pairs[0] > 0 || filter.pairs[1] > 0)
			{
				applyFilter(out, steps);
			}
			for (int k = 0; k < steps; k++)
			{
				out[k] = Math.max(-32768, Math.min(32767, out[k]));
			}
			return out;
		}

		/** {@code (int) ((long) a * b >> 16)}, in doubles (exact here), which run faster than longs in the browser. */
		private static int mul16(int a, int b)
		{
			return (int) Math.floor(a * (double) b / 65536.0);
		}

		private void applyFilter(int[] out, int steps)
		{
			filterEnvelope.reset();
			int t = filterEnvelope.step(steps + 1);
			int m = filter.compute(0, t / 65536.0f);
			int n = filter.compute(1, t / 65536.0f);
			if (steps < m + n)
			{
				return;
			}
			int[][] c = filter.coefficients;
			int k = 0;
			int limit = Math.min(n, steps - m);
			for (; k < limit; k++)
			{
				int y = mul16(out[k + m], filter.forwardGain);
				for (int i = 0; i < m; i++)
				{
					y += mul16(out[k + m - 1 - i], c[0][i]);
				}
				for (int i = 0; i < k; i++)
				{
					y -= mul16(out[k - 1 - i], c[1][i]);
				}
				out[k] = y;
				t = filterEnvelope.step(steps + 1);
			}
			int block = 128;
			limit = block;
			while (true)
			{
				if (limit > steps - m)
				{
					limit = steps - m;
				}
				for (; k < limit; k++)
				{
					int y = mul16(out[k + m], filter.forwardGain);
					for (int i = 0; i < m; i++)
					{
						y += mul16(out[k + m - 1 - i], c[0][i]);
					}
					for (int i = 0; i < n; i++)
					{
						y -= mul16(out[k - 1 - i], c[1][i]);
					}
					out[k] = y;
					t = filterEnvelope.step(steps + 1);
				}
				if (k >= steps - m)
				{
					break;
				}
				m = filter.compute(0, t / 65536.0f);
				n = filter.compute(1, t / 65536.0f);
				limit += block;
			}
			for (; k < steps; k++)
			{
				int y = 0;
				for (int i = k + m - steps; i < m; i++)
				{
					y += mul16(out[k + m - 1 - i], c[0][i]);
				}
				for (int i = 0; i < n; i++)
				{
					y -= mul16(out[k - 1 - i], c[1][i]);
				}
				out[k] = y;
				filterEnvelope.step(steps + 1);
			}
		}
	}
}

package com.osrssurvivors.web;

import com.osrssurvivors.audio.SoundBank;
import com.osrssurvivors.audio.SoundCues;
import com.osrssurvivors.audio.SoundRequest;
import com.osrssurvivors.game.Cue;
import com.osrssurvivors.game.Options;
import com.osrssurvivors.game.Player;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.teavm.jso.typedarrays.Int8Array;

/**
 * The game's sound in the page: the exported table (assets/sounds.js) played through the page's
 * audio engine. Takes the run's cues each frame and the menus' button presses.
 */
final class SoundSystem
{
	private final SoundBank bank;
	private final SoundCues cues = new SoundCues(new Random());

	private SoundSystem(SoundBank bank)
	{
		this.bank = bank;
	}

	/** The sounds, or a silent stand-in when assets/sounds.js is missing or unreadable. */
	static SoundSystem load()
	{
		Map<String, Integer> table = new HashMap<>();
		String json = WebAudio.table();
		if (json != null)
		{
			try
			{
				for (Map.Entry<String, Object> e : Json.object(Json.parse(json)).entrySet())
				{
					table.put(e.getKey(), ((Number) e.getValue()).intValue());
				}
			}
			catch (RuntimeException e)
			{
				table.clear();
			}
		}
		return new SoundSystem(new SoundBank(table, new Device()));
	}

	/** Plays what happened this frame, and gets a few more sounds ready. */
	void frame(List<Cue> happened, Player player, Options options, double nowSeconds)
	{
		bank.setVolumes(options);
		for (Cue c : happened)
		{
			for (SoundRequest r : cues.forCue(c, nowSeconds, player.getX(), player.getY()))
			{
				bank.play(r);
			}
		}
		bank.warm();
	}

	/** A menu sound, such as a click. */
	void play(SoundRequest r)
	{
		bank.play(r);
	}

	private static final class Device implements SoundBank.Device
	{
		@Override
		public boolean ready()
		{
			return WebAudio.ready();
		}

		@Override
		public double now()
		{
			return WebAudio.now();
		}

		@Override
		public byte[] raw(int id)
		{
			Int8Array raw = WebAudio.raw(id);
			return raw == null ? null : Browser.bytes(raw);
		}

		@Override
		public void add(int id, float[] samples, int rate)
		{
			WebAudio.add(id, samples, rate);
		}

		@Override
		public int play(int id, int bus, double gain, double rate, double pan, double delay)
		{
			return WebAudio.play(id, bus, gain, rate, pan, delay);
		}

		@Override
		public void stop(int voice)
		{
			WebAudio.stop(voice);
		}

		@Override
		public void volumes(double master, double effects, double ui)
		{
			WebAudio.volumes(master, effects, ui);
		}
	}
}

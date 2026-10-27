package com.osrssurvivors.web;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSByRef;
import org.teavm.jso.typedarrays.Int8Array;

/** The page's sound engine (window.OSRS.audio in index.html): buffers, buses and voices. */
final class WebAudio
{
	private WebAudio()
	{
	}

	/** The exported sound table (key to sound id) as JSON, or null without assets/sounds.js. */
	@JSBody(script = "var s = window.OSRS_SOUNDS; return s && s.table ? JSON.stringify(s.table) : null;")
	static native String table();

	/** Whether sound can play: the player has pressed something and the page is showing. */
	@JSBody(script = "return window.OSRS.audio.ready();")
	static native boolean ready();

	/** The sound clock, in seconds. */
	@JSBody(script = "return window.OSRS.audio.now();")
	static native double now();

	@JSBody(params = "id", script = "return window.OSRS.audio.raw(id);")
	static native Int8Array raw(int id);

	@JSBody(params = "id", script = "return window.OSRS.audio.has(id);")
	static native boolean has(int id);

	@JSBody(params = {"id", "samples", "rate"}, script = "window.OSRS.audio.add(id, samples, rate);")
	static native void add(int id, @JSByRef float[] samples, int rate);

	@JSBody(params = {"master", "effects", "ui"}, script = "window.OSRS.audio.volumes(master, effects, ui);")
	static native void volumes(double master, double effects, double ui);

	@JSBody(params = {"id", "bus", "gain", "rate", "pan", "delay"},
		script = "return window.OSRS.audio.play(id, bus, gain, rate, pan, delay);")
	static native int play(int id, int bus, double gain, double rate, double pan, double delay);

	@JSBody(params = "voice", script = "window.OSRS.audio.stop(voice);")
	static native void stop(int voice);
}

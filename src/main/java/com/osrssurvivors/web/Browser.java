package com.osrssurvivors.web;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.dom.html.HTMLImageElement;
import org.teavm.jso.typedarrays.Int8Array;

/**
 * The page's side of things (index.html): the exported artwork it has decoded, loading a
 * location's script, local storage and the clock.
 */
final class Browser
{
	private Browser()
	{
	}

	@JSFunctor
	interface LocationLoaded extends JSObject
	{
		/** Either the ground picture and the location's data, or an error message. */
		void loaded(HTMLImageElement ground, Int8Array data, String error);
	}

	@JSBody(script = "return window.OSRS.manifest;")
	static native String manifest();

	@JSBody(script = "return window.OSRS.models;")
	static native Int8Array models();

	@JSBody(params = "name", script = "return window.OSRS.images[name] || null;")
	static native HTMLImageElement image(String name);

	@JSBody(params = {"name", "callback"}, script = "window.OSRS.loadLocation(name, callback);")
	static native void loadLocation(String name, LocationLoaded callback);

	@JSBody(params = "key", script = "try { return window.localStorage.getItem(key); } catch (e) { return null; }")
	static native String getItem(String key);

	@JSBody(params = {"key", "value"}, script = "try { window.localStorage.setItem(key, value); } catch (e) {}")
	static native void setItem(String key, String value);

	@JSBody(params = "key", script = "try { window.localStorage.removeItem(key); } catch (e) {}")
	static native void removeItem(String key);

	/** Milliseconds, with sub-millisecond precision. */
	@JSBody(script = "return performance.now();")
	static native double now();

	@JSBody(params = "name", script = "return new URLSearchParams(window.location.search).has(name);")
	static native boolean hasParam(String name);

	@JSBody(params = "text", script = "window.OSRS.status(text);")
	static native void status(String text);

	static byte[] bytes(Int8Array array)
	{
		int n = array.getLength();
		byte[] out = new byte[n];
		for (int i = 0; i < n; i++)
		{
			out[i] = array.get(i);
		}
		return out;
	}
}

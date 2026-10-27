package com.osrssurvivors.web;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.dom.html.HTMLImageElement;
import org.teavm.jso.typedarrays.Int8Array;

/**
 * The page's side of things (index.html): the exported artwork it has decoded, loading a
 * location's script, local storage, the clock, and the clipboard and dialogs for save codes.
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

	/** Copies text to the clipboard; false when the browser won't allow it. */
	@JSBody(params = "text", script = "try { var t = document.createElement('textarea'); t.value = text;"
		+ " t.setAttribute('readonly', ''); t.style.position = 'fixed'; t.style.opacity = '0'; document.body.appendChild(t);"
		+ " t.select(); var ok = document.execCommand('copy'); document.body.removeChild(t);"
		+ " if (!ok && navigator.clipboard) { navigator.clipboard.writeText(text); } return ok; } catch (e) { return false; }")
	static native boolean copyText(String text);

	/** Shows text selected in a box, ready to copy. */
	@JSBody(params = {"message", "text"}, script = "window.prompt(message, text);")
	static native void showText(String message, String text);

	/** Asks the player to type or paste something; null when they cancel. */
	@JSBody(params = "message", script = "var r = window.prompt(message, ''); return r === null ? null : r;")
	static native String ask(String message);

	@JSBody(params = "message", script = "return window.confirm(message);")
	static native boolean confirm(String message);

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

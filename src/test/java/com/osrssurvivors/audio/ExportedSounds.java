package com.osrssurvivors.audio;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/** The committed sound export (src/main/webapp/assets/sounds.js), read for tests. */
public final class ExportedSounds
{
	public final Map<String, Integer> table;
	public final Map<String, String> names;
	private final Map<String, String> data;

	private static ExportedSounds loaded;

	private ExportedSounds(Map<String, Integer> table, Map<String, String> names, Map<String, String> data)
	{
		this.table = table;
		this.names = names;
		this.data = data;
	}

	public static synchronized ExportedSounds load()
	{
		if (loaded == null)
		{
			try
			{
				Gson gson = new Gson();
				Map<String, Integer> table = new HashMap<>();
				Map<String, String> names = new HashMap<>();
				Map<String, String> data = new HashMap<>();
				for (String line : Files.readAllLines(Paths.get("src/main/webapp/assets/sounds.js"), StandardCharsets.UTF_8))
				{
					String json = line.replaceAll(",$", "");
					if (line.startsWith("table: "))
					{
						table = gson.fromJson(json.substring(7), new TypeToken<Map<String, Integer>>() { }.getType());
					}
					else if (line.startsWith("names: "))
					{
						names = gson.fromJson(json.substring(7), new TypeToken<Map<String, String>>() { }.getType());
					}
					else if (line.startsWith("data: "))
					{
						data = gson.fromJson(json.substring(6), new TypeToken<Map<String, String>>() { }.getType());
					}
				}
				loaded = new ExportedSounds(table, names, data);
			}
			catch (IOException e)
			{
				throw new UncheckedIOException(e);
			}
		}
		return loaded;
	}

	/** A sound's cache file, or null. */
	public byte[] raw(int id)
	{
		String text = data.get(String.valueOf(id));
		return text == null ? null : Base64.getDecoder().decode(text);
	}

	public Iterable<String> ids()
	{
		return data.keySet();
	}
}

package com.osrssurvivors.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small JSON reader and writer for the manifest and the saves (Gson needs reflection, which
 * TeaVM doesn't offer). Objects become {@code Map<String, Object>}, arrays {@code List<Object>},
 * numbers {@code Double}, plus String, Boolean and null.
 */
final class Json
{
	private final String s;
	private int i;

	private Json(String s)
	{
		this.s = s;
	}

	static Object parse(String text)
	{
		Json p = new Json(text);
		p.space();
		Object v = p.value();
		p.space();
		if (p.i != p.s.length())
		{
			throw new IllegalArgumentException("Trailing characters at " + p.i);
		}
		return v;
	}

	@SuppressWarnings("unchecked")
	static Map<String, Object> object(Object o)
	{
		return o instanceof Map ? (Map<String, Object>) o : null;
	}

	@SuppressWarnings("unchecked")
	static List<Object> array(Object o)
	{
		return o instanceof List ? (List<Object>) o : null;
	}

	static int integer(Object o, int otherwise)
	{
		return o instanceof Number ? ((Number) o).intValue() : otherwise;
	}

	static double number(Object o, double otherwise)
	{
		return o instanceof Number ? ((Number) o).doubleValue() : otherwise;
	}

	static String string(Object o)
	{
		return o instanceof String ? (String) o : null;
	}

	private void space()
	{
		while (i < s.length() && Character.isWhitespace(s.charAt(i)))
		{
			i++;
		}
	}

	private char peek()
	{
		if (i >= s.length())
		{
			throw new IllegalArgumentException("Unexpected end");
		}
		return s.charAt(i);
	}

	private void expect(char c)
	{
		if (peek() != c)
		{
			throw new IllegalArgumentException("Expected " + c + " at " + i);
		}
		i++;
	}

	private Object value()
	{
		char c = peek();
		switch (c)
		{
			case '{':
				return parseObject();
			case '[':
				return parseArray();
			case '"':
				return parseString();
			case 't':
				literal("true");
				return Boolean.TRUE;
			case 'f':
				literal("false");
				return Boolean.FALSE;
			case 'n':
				literal("null");
				return null;
			default:
				return parseNumber();
		}
	}

	private void literal(String word)
	{
		if (!s.startsWith(word, i))
		{
			throw new IllegalArgumentException("Expected " + word + " at " + i);
		}
		i += word.length();
	}

	private Map<String, Object> parseObject()
	{
		expect('{');
		Map<String, Object> map = new LinkedHashMap<>();
		space();
		if (peek() == '}')
		{
			i++;
			return map;
		}
		while (true)
		{
			space();
			String key = parseString();
			space();
			expect(':');
			space();
			map.put(key, value());
			space();
			if (peek() == ',')
			{
				i++;
				continue;
			}
			expect('}');
			return map;
		}
	}

	private List<Object> parseArray()
	{
		expect('[');
		List<Object> list = new ArrayList<>();
		space();
		if (peek() == ']')
		{
			i++;
			return list;
		}
		while (true)
		{
			space();
			list.add(value());
			space();
			if (peek() == ',')
			{
				i++;
				continue;
			}
			expect(']');
			return list;
		}
	}

	private String parseString()
	{
		expect('"');
		StringBuilder b = new StringBuilder();
		while (true)
		{
			char c = peek();
			i++;
			if (c == '"')
			{
				return b.toString();
			}
			if (c != '\\')
			{
				b.append(c);
				continue;
			}
			char e = peek();
			i++;
			switch (e)
			{
				case 'n':
					b.append('\n');
					break;
				case 't':
					b.append('\t');
					break;
				case 'r':
					b.append('\r');
					break;
				case 'b':
					b.append('\b');
					break;
				case 'f':
					b.append('\f');
					break;
				case 'u':
					b.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
					i += 4;
					break;
				default:
					b.append(e);
					break;
			}
		}
	}

	private Double parseNumber()
	{
		int start = i;
		while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0)
		{
			i++;
		}
		if (start == i)
		{
			throw new IllegalArgumentException("Unexpected character at " + i);
		}
		return Double.parseDouble(s.substring(start, i));
	}

	/** Writes maps, lists, strings, numbers, booleans and null. */
	static String write(Object value)
	{
		StringBuilder b = new StringBuilder();
		write(b, value);
		return b.toString();
	}

	private static void write(StringBuilder b, Object v)
	{
		if (v == null)
		{
			b.append("null");
		}
		else if (v instanceof String)
		{
			quote(b, (String) v);
		}
		else if (v instanceof Double || v instanceof Float)
		{
			double d = ((Number) v).doubleValue();
			if (Double.isNaN(d) || Double.isInfinite(d))
			{
				b.append("null");
			}
			else if (d == Math.rint(d) && Math.abs(d) < 1e15)
			{
				b.append((long) d);
			}
			else
			{
				b.append(d);
			}
		}
		else if (v instanceof Number || v instanceof Boolean)
		{
			b.append(v);
		}
		else if (v instanceof Map)
		{
			b.append('{');
			boolean first = true;
			for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet())
			{
				if (!first)
				{
					b.append(',');
				}
				first = false;
				quote(b, String.valueOf(e.getKey()));
				b.append(':');
				write(b, e.getValue());
			}
			b.append('}');
		}
		else if (v instanceof Iterable)
		{
			b.append('[');
			boolean first = true;
			for (Object o : (Iterable<?>) v)
			{
				if (!first)
				{
					b.append(',');
				}
				first = false;
				write(b, o);
			}
			b.append(']');
		}
		else
		{
			quote(b, v.toString());
		}
	}

	private static void quote(StringBuilder b, String s)
	{
		b.append('"');
		for (int k = 0; k < s.length(); k++)
		{
			char c = s.charAt(k);
			switch (c)
			{
				case '"':
					b.append("\\\"");
					break;
				case '\\':
					b.append("\\\\");
					break;
				case '\n':
					b.append("\\n");
					break;
				case '\r':
					b.append("\\r");
					break;
				case '\t':
					b.append("\\t");
					break;
				default:
					if (c < 0x20)
					{
						String hex = Integer.toHexString(c);
						b.append("\\u");
						for (int p = hex.length(); p < 4; p++)
						{
							b.append('0');
						}
						b.append(hex);
					}
					else
					{
						b.append(c);
					}
					break;
			}
		}
		b.append('"');
	}
}

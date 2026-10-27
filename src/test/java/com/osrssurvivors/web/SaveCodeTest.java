package com.osrssurvivors.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.junit.Test;

public class SaveCodeTest
{
	private static final String PROFILE = "{\"coins\":1234,\"perks\":{\"HASTE\":3,\"MIGHT\":2},\"pets\":[\"TZREK_JAD\"]}";
	private static final String TIMES = "{\"LUMBRIDGE\":612.5,\"VARROCK\":301}";
	private static final String RUN = "{\"version\":3,\"location\":\"LUMBRIDGE\",\"level\":12,\"bag\":[\"GEAR:RUNE_SCIMITAR\",\"\"]}";

	@Test
	public void roundTripsEveryRecord()
	{
		String code = SaveCode.encode(new SaveCode.Contents(PROFILE, TIMES, RUN));
		assertTrue(code.startsWith(SaveCode.PREFIX));
		SaveCode.Contents back = SaveCode.decode(code);
		assertEquals(Json.write(Json.parse(PROFILE)), back.profile);
		assertEquals(Json.write(Json.parse(TIMES)), back.bestTimes);
		assertEquals(Json.write(Json.parse(RUN)), back.run);
		assertEquals("1234 coins, 5 upgrade ranks, 1 pet, best times in 2 places and a saved run (level 12)", back.describe());
	}

	@Test
	public void missingRecordsStayMissing()
	{
		SaveCode.Contents back = SaveCode.decode(SaveCode.encode(new SaveCode.Contents(PROFILE, null, "not json")));
		assertNull(back.bestTimes);
		assertNull(back.run);
		assertEquals("1234 coins, 5 upgrade ranks, 1 pet, best times in 0 places", back.describe());
	}

	@Test
	public void pastedLineBreaksAndSpacesAreIgnored()
	{
		String code = SaveCode.encode(new SaveCode.Contents(PROFILE, TIMES, RUN));
		String messy = "  " + code.substring(0, 20) + "\n" + code.substring(20, 41) + " \r\n" + code.substring(41) + "\n";
		assertEquals(Json.write(Json.parse(PROFILE)), SaveCode.decode(messy).profile);
	}

	@Test
	public void damagedCodesAreRefused()
	{
		String code = SaveCode.encode(new SaveCode.Contents(PROFILE, TIMES, RUN));
		refused("");
		refused("hello");
		refused(code.substring(0, code.length() - 12) + code.substring(code.length() - 9));
		char c = code.charAt(30);
		refused(code.substring(0, 30) + (c == 'A' ? 'B' : 'A') + code.substring(31));
		String body = SaveCode.base64("{\"v\":2}".getBytes(StandardCharsets.UTF_8));
		refused(SaveCode.PREFIX + body + "-" + SaveCode.checksum(body));
	}

	@Test
	public void base64RoundTripsEveryLength()
	{
		Random random = new Random(7);
		for (int n = 0; n < 40; n++)
		{
			byte[] data = new byte[n];
			random.nextBytes(data);
			assertEquals(java.util.Arrays.toString(data), java.util.Arrays.toString(SaveCode.unbase64(SaveCode.base64(data))));
		}
	}

	private static void refused(String code)
	{
		try
		{
			SaveCode.decode(code);
			fail("accepted " + code);
		}
		catch (IllegalArgumentException e)
		{
			assertTrue(e.getMessage().length() > 0);
		}
	}
}

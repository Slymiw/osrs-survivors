package com.osrssurvivors.map;

/**
 * Where map data comes from: the game client's cache at runtime, or files in tests. Any method
 * may return null when the data isn't available.
 */
public interface MapSource
{
	/** The decoded map square with this region id, or null if there is none. */
	MapSquare square(int region);

	ObjectDefinition object(int id);

	/** Underlay definition by id (the map stores id + 1). */
	FloorDefinition underlay(int id);

	/** Overlay definition by id (the map stores id + 1). */
	FloorDefinition overlay(int id);

	/** Average RGB of a ground texture, or -1 if unknown. */
	int textureColour(int texture);
}

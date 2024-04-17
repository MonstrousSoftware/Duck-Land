package com.monstrous.impostors;

public class Settings {
    public static int LOD_LEVELS = 3;

    public static int lodLevel = 3;

    public static boolean multiple = true;


    public static boolean decalsDebug = false;       // highlight decals with random background colour


    public static float lod1Distance = 50f;
    public static float lod2Distance = 2*lod1Distance;
    public static float impostorDistance = 2*lod2Distance;

    public static boolean cascadedShadows = false;

    // Terrain
    static public float     terrainChunkSize = 4096;
    static public int       terrainChunkCacheSize = 100;
    static public boolean   debugTerrainChunkAllocation = false;
}

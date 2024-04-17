package com.monstrous.impostors;

public class Settings {


    // Level of Detail
    public static int LOD_LEVELS = 3;           // must match the nr of glb files provided
    public static int lodLevel = 3;

    public static float lod1Distance = 80f;
    public static float lod2Distance = 2*lod1Distance;
    public static float impostorDistance = 2*lod2Distance;

    public static boolean decalsDebug = false;       // highlight decals with random background colour


    // Lighting
    public static float ambientLightLevel = 0.3f;
    public static boolean cascadedShadows = false;

    // Terrain
    static public float     terrainChunkSize = 4096;
    static public int       terrainChunkCacheSize = 100;
    static public boolean   debugTerrainChunkAllocation = false;
}

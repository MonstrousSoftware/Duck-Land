package com.monstrous.impostors;

public class Settings {


    // Level of Detail
    public static int       LOD_LEVELS = 3;           // must match the nr of glb files provided (e.g. <name>-lod0.glb)
    public static int       lodLevel = 3;

    public static boolean   dynamicLODAdjustment = false;
    public static float     lod1Distance = 64f; //91f; //80f;
    public static float     lod2Distance = 2*lod1Distance;
    public static float     impostorDistance = 2*lod2Distance;

    public static boolean   decalsDebug = false;       // highlight decals with random background colour


    // Lighting
    public static float     ambientLightLevel = 0.3f;
    public static boolean   cascadedShadows = false;

    // Terrain
    static public float     terrainChunkSize = 2048;
    static public int       terrainChunkCacheSize = 100;
    static public boolean   debugTerrainChunkAllocation = false;


    static public boolean   debugSceneryChunkAllocation = false;
    static public int       sceneryChunkCacheSize = 20000;

    static public boolean   skipChecksWhenCameraStill = true;       // don't recalculate when camera doesn't move, set to false when tuning for performance


    static public float     cameraFOV = 90f;

    static public boolean   invertLook = false;

    static public boolean   singleInstance = false;
}

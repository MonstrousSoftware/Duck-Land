package com.monstrous.impostors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;

import static com.badlogic.gdx.Application.ApplicationType.Desktop;

public class Settings {


    // Level of Detail
    public static int       LOD_LEVELS = 3;           // must match the nr of glb files provided (e.g. <name>-lod0.glb)
    public static int       lodLevel = 3;

    public static boolean   dynamicLODAdjustment = false;
    private static float    lod1Distance = 60f;
    public static float[]   lodDistances = { lod1Distance, 2f*lod1Distance, 4f*lod1Distance };  // distance for LOD1, LOD2, Impostors

    public static boolean   loadAtlasFromFile = (Gdx.app.getType() != Desktop);     // only on desktop can we generate atlas on the fly
    public static boolean   decalsDebug = false;       // highlight decals with random background colour


    // Lighting
    public static float     ambientLightLevel = 0.3f;
    public static boolean   cascadedShadows = (Gdx.app.getType() == Desktop);       // breaks teaVM

    // Terrain
    static public float     terrainChunkSize = 2048;        // terrain size in world units
    static public int       terrainChunkCacheSize = 100;
    static public boolean   debugTerrainChunkAllocation = false;


    static public boolean   debugSceneryChunkAllocation = false;
    static public int       sceneryChunkCacheSize = 20000;
    static public float     scenerySeparationDistance = 25f;

    static public boolean   skipChecksWhenCameraStill = true;       // don't recalculate when camera doesn't move, set to false when tuning for performance


    static public float     cameraFOV = 70f;
    static public float     cameraFar = 8000f;


    static public float     fogNear = 100f;
    static public float     fogFar = 8000f;
    static public float fogExponent = 1.0f;
    static public Color     fogColor = Color.SKY;
    static public boolean   showFogSettings = false;

    static public boolean   invertLook = false;

    static public boolean   singleInstance = false;
}

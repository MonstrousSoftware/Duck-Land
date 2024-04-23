# Duck Land
23/04/2024

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/tommyettinger/gdx-liftoff).

![screenshot](https://github.com/MonstrousSoftware/Duck-Land/assets/49096535/928ce604-b8cb-4ca6-8402-e4d5f1b700f0)

Demo of rendering many items at reasonable frame rate.
REQUIRES OPENGL 4.3.

Uses multiple levels of details for the model (need to be supplied as glb files). 
Uses impostors (billboards) at long distance (these are generated on the fly).
Uses OpenGL instancing for LOD models and for the impostors.


If this demo runs too slow on your computer, it will automatically adapt the quality settings (LOD distances) to try and achieve at least 60 fps.
This make take a few seconds to adjust.  If this is not sufficient, please increase scenerySeparationDistance in Settings.java.

Based on a demo by Erkka from Enormous Elk shared in the LibGDX Discord server.


Movement Controls:
- use mouse to move camera
- WASD to move
- Q/E to move up and down
- SPACE for turbo boost

Demo Options:
- TAB to cycle through LOD0, LOD1, LOD2, Impostors and all
- T to show terrain chunks overlay
- P to toggle scenery chunks overlay (colour indicated LOD level)
- Z, X to change LOD distances
- M to toggle single instance view (use TAB to view the different LOD models)




Notes:
- work in progress, code needs cleaning up.
 
- decals get clipped when leaning forward.  This is because the clipping rect is calculated from low elevation. At high elevation view the front spills over
 below the clipped region.  If we add a safety margin, the decals appear to be floating in the air when upright.

- Known issue: when viewing from above the impostors flatten out sideways leaving a hole in the middle.

- For teavm (web) variant, it is necessary to use gltf-separate format instead of glb. (Although groundPlane.glb loads okay, perhaps because it is not textured).


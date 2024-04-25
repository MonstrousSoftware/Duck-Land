# Duck Land
23/04/2024

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/tommyettinger/gdx-liftoff).

![screenshot](https://github.com/MonstrousSoftware/Duck-Land/assets/49096535/0c6ce918-a332-473c-b4cf-a95b23acc235)

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

- (gdx-teavm version not working yet as the impostors don't appear.)


Modeling caveats
----------------
All models and LOD versions can be stored in the same gltf file.  The object names in Blender are used as node names to distinguish them.
For models that have different levels of detail the naming convention in Blender is that they are called name.LOD0, name.LOD1 and name.LOD2 (substitute the model name for "name").

The LOD models don't have to be positioned at the same spot in Blender. But you need to use Apply Transforms with each LOD model located at the origin.  Once you have done that
you can move them to a different location. The location in Blender will be ignored on import, each object will be repositioned to where your performed Apply Transforms.

Beware that a model with different materials will be split into different mesh parts and different meshes.  Only the first mesh will be instanced.
If you use a shared palette and just select different colours (like in Imphenzia's low poly modeling tutorials) it will work fine.
If you are applying one texture for the bark of a tree and another texture for the leaves, it won't work.  The stem will be instanced, but not the leaves.





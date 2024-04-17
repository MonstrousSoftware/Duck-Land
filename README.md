# Impostors

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/tommyettinger/gdx-liftoff).

Demo of rendering many items at reasonable frame rate.
Uses multiple levels of details for the model (need to be supplied as glb files).
Uses impostors (billboards) at long distance (these are generated on the fly).
Uses OpenGL instancing for LOD models and for the impostors.






Notes:
- decals get clipped when leaning forward.  This is because the clipping rect is calculated from low elevation. At high elevation view the front spills over
 below the clipped region.  If we add a safety margin, the decals appear to be floating in the air when upright.

- sometimes model disappear on camera movement

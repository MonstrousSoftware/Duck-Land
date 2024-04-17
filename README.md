# Impostors aka Duck Desert
17/04/2024

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/tommyettinger/gdx-liftoff).

Demo of rendering many items at reasonable frame rate.

Uses multiple levels of details for the model (need to be supplied as glb files). 
Uses impostors (billboards) at long distance (these are generated on the fly).
Uses OpenGL instancing for LOD models and for the impostors.


If this demo runs too slow on your computer, it will automatically adapt the quality settings (LOD distances) to try and achieve at least 60 fps.
This make take a few seconds to adjust.

Based on a demo by Erkka from Enormous Elk shared in the LibGDX Discord server.


Controls:
- use mouse to move camera, scroll wheel to zoom in/out
- TAB to cycle through LOD0, LOD1, LOD2, Impostors and all
- T to show terrain chunks
- Z, X to change LOD distances






Notes:
- work in progress, code needs cleaning up.
- 
- decals get clipped when leaning forward.  This is because the clipping rect is calculated from low elevation. At high elevation view the front spills over
 below the clipped region.  If we add a safety margin, the decals appear to be floating in the air when upright.

- The decals appear a bit darker than the real models.  
- Known issue: when viewing from above the impostors flatten out sideways leaving a hole in the middle.

To do/ideas:
- There is not frustum cilling.  Frustum culling would boost the performance even with instanced objects. However, tests so far has the clipping a bit inaccurate sometimes leaving gaps left and right.

- The instances could be generated in chunks for an infinite amount (like the terrain chunks)
- LOD level could be determined per chunk for faster allocation
- Could use an indirect buffer to index the instance transforms, to exchange less data per frame (1 integer instead of 16 floats).  Because the locations are static, they are just allocated
to different LOD models over time.

- we could perhaps avoid the copying of the FloatBuffer performed by setInstanceData() if we get hold of the buffer via InstanceData.getBuffer() (but Mesh.instances is package private).




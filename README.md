# Impostors

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/tommyettinger/gdx-liftoff).

This project was generated with a template including simple application launchers and a main class extending `Game` that sets the first screen.

Notes:
- decals get clipped when leaning forward.  This is because the clipping rect is calculated from low elevation. At high elevation view the front spills over
- below the clipped region.  If we add a safety margin, the decals appear to be floating in the air when upright.

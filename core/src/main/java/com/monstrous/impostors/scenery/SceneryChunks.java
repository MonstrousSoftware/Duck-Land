package com.monstrous.impostors.scenery;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.monstrous.impostors.Impostor;
import com.monstrous.impostors.ImpostorBuilder;
import com.monstrous.impostors.Settings;
import com.monstrous.impostors.Statistic;
import com.monstrous.impostors.shaders.InstancedDecalShaderProvider;
import com.monstrous.impostors.terrain.Terrain;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

import java.nio.FloatBuffer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;


// Class to manage scenery chunks for a finite or infinite area.
// Creates chunks as needed depending on the camera position and direction.
// Returns list of visible chunks sorted by distance.



public class SceneryChunks implements Disposable {
    public static final int RANGE = 35;               // viewing range in chunks

    private final Terrain terrain;
    private final float separationDistance;
    final Map<Integer, SceneryChunk> chunks;        // map of scenery chunk per grid point
    private final Array<SceneryChunk> chunksInRange;
    private final Array<SceneryChunk> visibleChunks;
    private int timeCounter;                                // used as timestamp for chunk creation time
    int lastCameraChange;                                   // time stamp of last camera change
    private final ChunkComparator comparator;
    private GridPoint2 min, max;
    private GridPoint2 centre = new GridPoint2();
    private GridPoint2 gp = new GridPoint2();
    private GridPoint2 prevCentre = new GridPoint2(Integer.MAX_VALUE,Integer.MAX_VALUE);
    private PerspectiveCamera prevCam = new PerspectiveCamera();

    private class ChunkComparator implements Comparator<SceneryChunk> {

        @Override
        public int compare(SceneryChunk o1, SceneryChunk o2) {

            // sort in ascending order of distance
            return (int)(o1.distance - o2.distance);
        }
    }

    // if worldSize <= 0 it means infinite terrain.

    public SceneryChunks(float worldSize, Terrain terrain, float separationDistance ) {
        this.terrain = terrain;
        this.separationDistance = separationDistance;
        comparator = new ChunkComparator();

        chunks = new HashMap<>();
        chunksInRange = new Array<>();
        visibleChunks = new Array<>();
        timeCounter = 0;

        if(worldSize > 0){
            // limit chunk creation to fixed square area
            int halfRange = MathUtils.ceil(0.5f * worldSize / SceneryChunk.CHUNK_SIZE);
            min = new GridPoint2(-halfRange, -halfRange);
            max = new GridPoint2(halfRange, halfRange);
        }

    }


    public Array<SceneryChunk> getVisibleChunks(){
        return visibleChunks;
    }

    public void update(PerspectiveCamera cam, boolean forceUpdate){
        timeCounter++;


        // quick exit if camera has not changed in position, direction or other parameters, because the instance data is then still valid
        if(!Settings.singleInstance &&
            !forceUpdate &&
            cam.position.equals(prevCam.position) && cam.direction.equals(prevCam.direction)
            && cam.up.equals(prevCam.up) && cam.near == prevCam.near && cam.far == prevCam.far && cam.fieldOfView == prevCam.fieldOfView)
            return;

        lastCameraChange = timeCounter;

        // remember current camera settings for next call
        prevCam.position.set(cam.position);
        prevCam.direction.set(cam.direction);
        prevCam.up.set(cam.up);
        prevCam.near = cam.near;
        prevCam.far = cam.far;
        prevCam.fieldOfView = cam.fieldOfView;

        int px = (int)Math.floor(cam.position.x/SceneryChunk.CHUNK_SIZE);
        int pz = (int)Math.floor(cam.position.z/SceneryChunk.CHUNK_SIZE);
        centre.set(px,pz);


        // Create a list of chunks within visual range of the camera.
        // Chunks are created if necessary.
        //
        if( !centre.equals(prevCentre) ) {  // if camera moved to new square
            chunksInRange.clear();
            for (int cx = px - RANGE; cx <= px + RANGE; cx++) {
                for (int cz = pz - RANGE; cz <= pz + RANGE; cz++) {

                    // cap to the world size if defined
                    if(min != null){
                        if(cx < min.x || cz < min.y || cx > max.x || cz > max.y)
                            continue;
                    }

                    // quickly discard chunks outside a circular range
                    gp.set(cx, cz);
                    if (gp.dst2(centre) >= RANGE * RANGE)
                        continue;

                    Integer key = makeKey(cx, cz);

                    SceneryChunk chunk = chunks.get(key);
                    if (chunk == null) {
                        chunk = new SceneryChunk(cx, cz, timeCounter, key, terrain, separationDistance);
                        chunks.put(key, chunk);
                        //Gdx.app.log("creating scenery chunk", "num chunks "+chunks.size());
                    }
                    chunksInRange.add(chunk);
                }
            }
            prevCentre.set(centre);
        }

        // Select chunks that are in camera frustum
        //
        visibleChunks.clear();
        for(SceneryChunk chunk : chunksInRange ) {
            if (cam.frustum.boundsInFrustum(chunk.bbox)) {  // frustum culling
                visibleChunks.add(chunk);
                chunk.lastSeen = timeCounter;
                chunk.distance = cam.position.dst(chunk.getWorldPosition());    // note: distance to chunk centre
            }
        }
        visibleChunks.sort( comparator );   // sort closest chunk first

//        float estimatedChunksInRange = MathUtils.ceil(MathUtils.PI * (float)Math.pow(RANGE, 2.0));
//        float estimatedChunksInView = 1.7f * estimatedChunksInRange * cam.fieldOfView / 360f;
//
//                Gdx.app.log("chunks in range", ""+chunksInRange.size+" estimated: "+ estimatedChunksInRange);
//                Gdx.app.log("chunks visible", ""+visibleChunks.size+" estimated: "+ estimatedChunksInView );
        reaper();


    }


    private void reaper(){
        if( chunks.size() > Settings.sceneryChunkCacheSize) {
            // find the last seen chunk
            SceneryChunk oldest = null;
            for (SceneryChunk chunk : chunks.values()) {
                if (oldest == null || chunk.lastSeen < oldest.lastSeen)
                    oldest = chunk;
            }
            // now remove this chunk
            if (oldest != null) {
                int before = chunks.size();
                chunks.remove(oldest.key);
                oldest.dispose();
                oldest = null;
                Gdx.app.log("deleting scenery chunk", "num chunks " + chunks.size() + " before: " + before);
            }
        }
    }

    // convert chunk (X,Y) to a single integer for easy use as a key in the hash map
    private int makeKey(int cx, int cz) {
        return cx + 1000 * cz;
    }


    @Override
    public void dispose() {
        for(SceneryChunk chunk : chunks.values())
            chunk.dispose();
        chunks.clear();
        chunksInRange.clear();
        visibleChunks.clear();
    }
}

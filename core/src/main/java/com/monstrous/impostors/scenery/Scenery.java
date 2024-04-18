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
import java.util.HashMap;
import java.util.Map;


// Class to manage many scenery items for an infinite area, placed at terrain height.
// Rendered at different LOD levels or as impostors
// (currently only supports one type of item)


public class Scenery implements Disposable {
    private static final int RANGE = 32;               // viewing range in chunks

    private static final int MAX_INSTANCES = 500000;


    private final Terrain terrain;
    private final float separationDistance;
    public Statistic[] statistics;
    public int instanceCount = 1;
    final Map<Integer, SceneryChunk> chunks;        // map of terrain chunk per grid point
    int timeCounter;                                // used as timestamp for chunk creation time
    int lastCameraChange;                           // time stamp of last camera change
    int lastCameraMove;                             // time stamp of last camera move (to new chunk)
    private final Array<Scene> scenes;
    private final Scene[] lodScenes;                // array of Scenes at different level of detail
    private final float radius;
    private Array<Vector4>[] positions;
    private final ImpostorBuilder builder;
    private final Texture impostorTexture;
    private final Vector2 regionSize;
    private final Array<ModelInstance> decalInstances;
    private final ModelInstance decalInstance;
    private final TextureRegion atlasRegion;
    private final TextureRegion textureRegion0;
    private float elevationStep;
    private int elevations;
    private final Array<SceneryChunk> chunksInRange;
    private final Array<SceneryChunk> visibleChunks;
    private FloatBuffer instanceData;   // temp buffer to transfer instance data

    public Scenery( Terrain terrain, float separationDistance ) {
        this.terrain = terrain;
        this.separationDistance = separationDistance;

        chunks = new HashMap<>();
        chunksInRange = new Array<>();
        visibleChunks = new Array<>();
        lodScenes = new Scene[Settings.LOD_LEVELS];
        scenes = new Array<>();
        decalInstances = new Array<>();
        timeCounter = 0;
        lastCameraMove = 0;

        for(int lod = 0; lod < Settings.LOD_LEVELS;lod++) {
            //SceneAsset sceneAsset = new GLBLoader().load(Gdx.files.internal("models/birch-lod" + lod + ".glb"));
            SceneAsset sceneAsset = new GLBLoader().load(Gdx.files.internal("models/ducky-lod" + lod + ".glb"));
            lodScenes[lod] = new Scene(sceneAsset.scene);
        }

        BoundingBox modelBoundingBox = new BoundingBox();
        lodScenes[0].modelInstance.calculateBoundingBox(modelBoundingBox);          // get dimensions of model
        Vector3 dimensions = new Vector3();
        modelBoundingBox.getDimensions(dimensions);
        radius = dimensions.len() / 2f;     // determine model radius for frustum clipping


        positions = new Array[Settings.LOD_LEVELS+1];
        for(int lod = 0; lod < Settings.LOD_LEVELS+1; lod++)
            positions[lod] = new Array<>();

        // Create offset FloatBuffer that will contain instance data to pass to shader
        // we are dimensioning it for a worst case, which means we probably waste a lot of memory here (e.g. 32Mb)
        instanceData = BufferUtils.newFloatBuffer(MAX_INSTANCES * 16);   // 16 floats for the matrix

        // Create impostors
        //
        builder = new ImpostorBuilder();
        int textureSize = 2048;
        regionSize = new Vector2();
        // make a texture of the model from different angles
        impostorTexture = builder.createImpostor(lodScenes[0], textureSize, regionSize);
        Gdx.app.log("region size", ""+regionSize.x+" , "+regionSize.y);


        int numAngles =  ImpostorBuilder.NUM_ANGLES;
        int width = textureSize / numAngles;
        int height = (int)regionSize.y;
        elevations = textureSize / height;
        elevationStep = 60f/elevations;   // degrees per elevation step

        atlasRegion = new TextureRegion(impostorTexture, 0f, 0f, 1.0f, 1.0f);
        textureRegion0 = new TextureRegion(impostorTexture, 0, 0, width, height);
        textureRegion0.flip(false, true);

        int y = 0;



        // create decal instance
        Model model = Impostor.createImposterModel(textureRegion0, lodScenes[0].modelInstance);
        decalInstance = new ModelInstance(model, 0, 0, 0);

        // use user data to pass info  on the texture atlas to the shader
        decalInstance.userData = new InstancedDecalShaderProvider.UVSize(regionSize.x/textureSize, regionSize.y/textureSize);


        statistics = new Statistic[Settings.LOD_LEVELS+1];
        for(int lod = 0; lod < Settings.LOD_LEVELS+1; lod++) {
            statistics[lod] = new Statistic();
            if(lod < Settings.LOD_LEVELS)
                statistics[lod].vertexCount = lodScenes[lod].modelInstance.model.meshes.first().getNumVertices();
            else
                statistics[lod].vertexCount = decalInstance.model.meshes.first().getNumVertices();
        }

        for(int lod = 0; lod < Settings.LOD_LEVELS; lod++) {
            makeInstanced(lodScenes[lod].modelInstance);
            scenes.add(lodScenes[lod]);
        }

        // enable instancing for impostors
        makeInstancedDecals(decalInstance);
        decalInstances.add(decalInstance);

        Settings.lodLevel = -1;     // show all LOD levels and impostors

    }

    public Array<Scene> getScenes(){
        // for the sake of debug options we rebuild this as needed
        scenes.clear();
        if(Settings.lodLevel == -1) {
            for (int lod = 0; lod < Settings.LOD_LEVELS; lod++) {
                scenes.add(lodScenes[lod]);
            }
        }
        else if (Settings.lodLevel < Settings.LOD_LEVELS)
            scenes.add(lodScenes[Settings.lodLevel]);
        return scenes;
    }

    // need to be rendered with the instanced decal shader
    public Array<ModelInstance> getDecalInstances(){
        return decalInstances;
    }


    private GridPoint2 centre = new GridPoint2();
    private GridPoint2 v = new GridPoint2();
    private GridPoint2 prevCentre = new GridPoint2(Integer.MAX_VALUE,Integer.MAX_VALUE);
    private PerspectiveCamera prevCam = new PerspectiveCamera();


    public void update(PerspectiveCamera cam, boolean forceUpdate){
        timeCounter++;

        // quick exit if camera has not changed in position, direction or other parameters, because the instance data is then still valid
        if(!forceUpdate && cam.position.equals(prevCam.position) && cam.direction.equals(prevCam.direction)
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
            lastCameraMove = timeCounter;
            chunksInRange.clear();
            for (int cx = px - RANGE; cx <= px + RANGE; cx++) {
                for (int cz = pz - RANGE; cz <= pz + RANGE; cz++) {

                    // quickly discard chunks outside a circular range
                    v.set(cx, cz);
                    if (v.dst2(centre) > RANGE * RANGE)
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
            }
        }

        // Now get the instance data from all visible chunks
        //
        for(int lod = 0; lod < Settings.LOD_LEVELS+1; lod++)        // clear buffers per LOD level and for Impostors
            positions[lod].clear();

        Integer centreKey = makeKey(centre.x, centre.y);
        for(SceneryChunk chunk : visibleChunks ){

            int level;

            if(chunk.key == centreKey)      // make sure the chunk we're inside is at level 0, even if we are far from its centre
                level = 0;
            else {
                float distance2 = cam.position.dst2(chunk.getWorldPosition());
                level = determineLODlevel(distance2);
                chunk.setLodLevel(level);
            }

            if(level == 0)      // for chunks at LOD0 level, go to individual instance level
                allocateInstances(cam, chunk.getPositions() );
            else
                positions[level].addAll( chunk.getPositions() );
        }


        // Update instance data for every LOD model and the impostor model
        //
        for(int lod = 0; lod < Settings.LOD_LEVELS; lod++)
            updateInstanced(lodScenes[lod].modelInstance, positions[lod]);
        updateInstancedDecals(decalInstance, positions[Settings.LOD_LEVELS]);    // instances for decal

        // Update the stats for the GUI
        //
        instanceCount = 0;
        for (int lod = 0; lod < Settings.LOD_LEVELS + 1; lod++) {
            statistics[lod].instanceCount = positions[lod].size;
            instanceCount += positions[lod].size;
        }
        if (instanceCount > MAX_INSTANCES) throw new GdxRuntimeException("Too many instances! > " + MAX_INSTANCES);


        if( chunks.size() > Settings.sceneryChunkCacheSize){
            // find the last seen chunk
            SceneryChunk oldest = null;
            for(SceneryChunk chunk : chunks.values()){
                if(oldest == null || chunk.lastSeen < oldest.lastSeen)
                    oldest = chunk;
            }
            // now remove this chunk
            if(oldest != null) {
                int before = chunks.size();
                chunks.remove(oldest.key);
                oldest.dispose();
                oldest = null;
                Gdx.app.log("deleting scenery chunk", "num chunks "+chunks.size() + " before: "+before);
            }
        }
    }

    // convert chunk (X,Y) to a single integer for easy use as a key in the hash map
    private int makeKey(int cx, int cz) {
        return cx + 1000 * cz;
    }

    private Vector3 instancePosition = new Vector3();

    // allocate instanced from this list on individual basis to LOD level
    // also perform individual frustum clipping
    //
    private void allocateInstances( Camera cam, Array<Vector4> positionsFromChunk ){
        for(Vector4 position : positionsFromChunk ){

            instancePosition.set( position.x, position.y, position.z );
            if(cam.frustum.sphereInFrustum(instancePosition, radius)) {
                float distance2 = cam.position.dst2(instancePosition);
                int level = determineLODlevel(distance2);
                positions[level].add(position);
            }
        }
    }


    private int determineLODlevel( float distance2 ){
        // allocate this instance to one of the LOD levels depending on the distance

        if (distance2 >= Settings.impostorDistance*Settings.impostorDistance   )        // optimized: most common case first
            return 3;
        else if ( distance2 >= Settings.lod2Distance * Settings.lod2Distance )
            return 2;
        else if (distance2 >= Settings.lod1Distance * Settings.lod1Distance  )
            return 1;
        else
            return 0;
        // todo : this hard codes the LOD levels to 3
    }





    private void makeInstanced( ModelInstance modelInstance ) {

        Mesh mesh = modelInstance.model.meshes.first();       // assumes model is one mesh

        // add matrix per instance
        mesh.enableInstancedRendering(false, MAX_INSTANCES,      // pass maximum instance count
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 0),
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 1),
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 2),
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 3)   );
    }

    private void updateInstanced( ModelInstance modelInstance, Array<Vector4> positions ) {

        if(positions.size >= MAX_INSTANCES) throw new GdxRuntimeException("too many instances for instance buffer: "+positions.size);

        Mesh mesh = modelInstance.model.meshes.first();       // assumes model is one mesh

        // fill instance data buffer
        instanceData.clear();
        Matrix4 instanceTransform = new Matrix4();
        for(Vector4 pos: positions) {

            instanceTransform.setToRotationRad(Vector3.Y, pos.w);
            instanceTransform.setTranslation(pos.x, pos.y, pos.z);
            // transpose matrix for GLSL
            instanceData.put(instanceTransform.tra().getValues());                // transpose matrix for GLSL
        }
        instanceData.limit( positions.size * 16 );  // amount of data in buffer
        instanceData.position(0);      // rewind float buffer to start
        mesh.setInstanceData(instanceData);
    }


    private void makeInstancedDecals( ModelInstance modelInstance ) {
        Mesh mesh = modelInstance.model.meshes.first();       // assumes model is one mesh

        // add vector4 per instance containing position and Y rotation
        mesh.enableInstancedRendering(false, MAX_INSTANCES,
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_offset", 0));
    }

    private float[] tmpFloat4 = { 1, 2, 3, 4 };

    private void updateInstancedDecals( ModelInstance modelInstance, Array<Vector4> positions ) {

        Mesh mesh = modelInstance.model.meshes.first();       // assumes model is one mesh

        instanceData.clear();
        for(Vector4 pos: positions) {
            tmpFloat4[0] = pos.x;
            tmpFloat4[1] = pos.y;
            tmpFloat4[2] = pos.z;
            tmpFloat4[3] = pos.w;
            instanceData.put( tmpFloat4 );
        }
        instanceData.limit( positions.size * 4 );
        instanceData.position(0);      // rewind float buffer to start
        mesh.setInstanceData(instanceData);
    }



    @Override
    public void dispose() {
        scenes.clear();
        for(SceneryChunk chunk : chunks.values())
            chunk.dispose();
        chunks.clear();
    }
}

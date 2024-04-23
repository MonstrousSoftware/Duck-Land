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


// Class to manage many scenery items for an infinite area, placed at terrain height.
// Rendered at different LOD levels or as impostors
// (currently only supports one type of item)


public class Scenery implements Disposable {

    private static final int MAX_MODEL_INSTANCES =  500;
    private static final int MAX_DECAL_INSTANCES = 25000;

    SceneryChunks sceneryChunks;
    public Statistic[] statistics;
    public int instanceCount = 1;
    private final Array<Scene> scenes;
    private final Scene[] lodScenes;                // array of Scenes at different level of detail
    private final Vector3 modelCentre;
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
    private FloatBuffer instanceData;   // temp buffer to transfer instance data

    public Scenery( Terrain terrain, float separationDistance ) {
        sceneryChunks = new SceneryChunks(0, terrain, separationDistance );

        lodScenes = new Scene[Settings.LOD_LEVELS];
        scenes = new Array<>();
        decalInstances = new Array<>();

        for(int lod = 0; lod < Settings.LOD_LEVELS;lod++) {
            SceneAsset sceneAsset = new GLBLoader().load(Gdx.files.internal("models/ducky-lod" + lod + ".glb"));
            lodScenes[lod] = new Scene(sceneAsset.scene);
        }

        BoundingBox modelBoundingBox = new BoundingBox();
        lodScenes[0].modelInstance.calculateBoundingBox(modelBoundingBox);          // get dimensions of model
        Vector3 dimensions = new Vector3();
        modelBoundingBox.getDimensions(dimensions);
        radius =  dimensions.len() / 2f;     // determine model radius for frustum clipping
        modelCentre = modelBoundingBox.getCenter(new Vector3());        // offset from model origin to model centre (origin should be at floor level)


        positions = new Array[Settings.LOD_LEVELS+1];
        for(int lod = 0; lod < Settings.LOD_LEVELS+1; lod++)
            positions[lod] = new Array<>();

        // Create offset FloatBuffer that will contain instance data to pass to shader
        // we are dimensioning it for the worst case, which means we probably waste a lot of memory here
        int bufferSize = Math.max(MAX_MODEL_INSTANCES * 16, MAX_DECAL_INSTANCES * 4);
        instanceData = BufferUtils.newFloatBuffer(bufferSize);   // 16 floats for the matrix, 4 floats per impostor

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
            float estimate = (float)Math.PI*(float)Math.pow((SceneryChunks.RANGE),2f);// * (Settings.cameraFOV / 360f);
            Gdx.app.log("level "+lod, "chunks: "+estimate);
            makeInstanced(lodScenes[lod].modelInstance, MAX_MODEL_INSTANCES);       // could make an estimate per LOD level here
            scenes.add(lodScenes[lod]);
        }

        // enable instancing for impostors
        makeInstancedDecals(decalInstance, MAX_DECAL_INSTANCES);
        decalInstances.add(decalInstance);

        Settings.lodLevel = -1;     // show all LOD levels and impostors

    }

    public Array<Scene> getScenes(){
        // for the sake of debug options we rebuild this as needed
        scenes.clear();
        if(Settings.lodLevel == -1) {
            for (int lod = 0; lod < Settings.LOD_LEVELS; lod++) {
                if(positions[lod].size > 0)
                    scenes.add(lodScenes[lod]);
            }
        }
        else if (Settings.lodLevel < Settings.LOD_LEVELS) {
            if(positions[Settings.lodLevel].size > 0)
                scenes.add(lodScenes[Settings.lodLevel]);
        }
        return scenes;
    }

    // need to be rendered with the instanced decal shader
    public Array<ModelInstance> getImpostors(){
        return decalInstances;
    }

    private int timeCounter;

    public void update(PerspectiveCamera cam, boolean forceUpdate){
        timeCounter++;

        sceneryChunks.update(cam, forceUpdate);
        Array<SceneryChunk> visibleChunks = sceneryChunks.getVisibleChunks();


        // Now get the instance data from all visible chunks
        //
        for(int lod = 0; lod < Settings.LOD_LEVELS+1; lod++)        // clear buffers per LOD level and for Impostors
            positions[lod].clear();


        float diagonalDistance = 0.707f * SceneryChunk.CHUNK_SIZE;        // subtract distance from corner to centre of chunk in case the camera is in corner of chunk (0.5*sqrt(2))

        for(SceneryChunk chunk : visibleChunks ){

            int level = determineLODlevel(chunk.distance - diagonalDistance);
            chunk.setLodLevel(level);

            if(level <= 2)      // for chunks at LOD0 level (high poly count), test at individual instance level
                allocateInstances(cam, chunk.getPositions() );
            else
                positions[level].addAll( chunk.getPositions() );
        }

        if(Settings.singleInstance){
            // if we're in single instance mode, forget all of the above
            // clear all positions, except one for the current LOD level
            for(int lod = 0; lod < Settings.LOD_LEVELS+1; lod++){
                positions[lod].clear();
            }
            if(Settings.lodLevel < 0)   // don't allow multiple LOD levels
                Settings.lodLevel = 0;
            Vector4 singlePos = new Vector4(0,0,0,timeCounter * 0.0005f);       // slowly rotate
            positions[Settings.lodLevel].add(singlePos);
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
        if (instanceCount > MAX_MODEL_INSTANCES+MAX_DECAL_INSTANCES) throw new GdxRuntimeException("Too many instances! > " + MAX_MODEL_INSTANCES+MAX_DECAL_INSTANCES);
    }

    private Vector3 instancePosition = new Vector3();

    // allocate instances from this list on individual basis to LOD level
    // also perform individual frustum clipping
    //
    private void allocateInstances( Camera cam, Array<Vector4> positionsFromChunk ){
        for(Vector4 position : positionsFromChunk ){

            instancePosition.set( position.x, position.y, position.z ).add(modelCentre);
            if(cam.frustum.sphereInFrustum(instancePosition, radius)) {        // some margin to prevent popping
                // determine level of detail from distance to camera
                float distance = cam.position.dst(instancePosition);
                int level = determineLODlevel(distance);
                positions[level].add(position);
            }
        }
    }



    private int determineLODlevel( float distance ){
        // allocate this instance to one of the LOD levels depending on the distance

        for(int lod = Settings.LOD_LEVELS-1; lod >= 0; lod--) {
            if (distance >= Settings.lodDistances[lod]   )        // optimized: most common case first
                return lod+1;
        }
        return 0;       // LOD level 0, highest poly count
    }





    private void makeInstanced( ModelInstance modelInstance, int maxInstances ) {

        Mesh mesh = modelInstance.model.meshes.first();       // assumes model is one mesh

        // add matrix per instance
        mesh.enableInstancedRendering(false, maxInstances,      // pass maximum instance count
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 0),
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 1),
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 2),
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 3)   );
    }

    private void updateInstanced( ModelInstance modelInstance, Array<Vector4> positions ) {

        if(positions.size >= MAX_MODEL_INSTANCES) throw new GdxRuntimeException("too many instances for instance buffer: "+positions.size);

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


    private void makeInstancedDecals( ModelInstance modelInstance, int maxInstances ) {
        Mesh mesh = modelInstance.model.meshes.first();       // assumes model is one mesh

        // add vector4 per instance containing position and Y rotation
        mesh.enableInstancedRendering(false, maxInstances,
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_offset", 0));
    }

    private float[] tmpFloat4 = { 1, 2, 3, 4 };

    private void updateInstancedDecals( ModelInstance modelInstance, Array<Vector4> positions ) {
        if(positions.size >= MAX_DECAL_INSTANCES) throw new GdxRuntimeException("too many instances for instance buffer: "+positions.size);

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
        sceneryChunks.dispose();
    }
}

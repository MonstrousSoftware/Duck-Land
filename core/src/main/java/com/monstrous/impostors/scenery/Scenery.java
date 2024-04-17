package com.monstrous.impostors.scenery;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Vector4;
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
import com.monstrous.impostors.terrain.TerrainChunk;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

public class Scenery implements Disposable {
    private static final int RANGE = 32;               // viewing range in chunks

    private static final int MAX_INSTANCES = 500000;


    private final Terrain terrain;
    public Statistic[] statistics;
    public int instanceCount = 1;
    final Map<Integer, SceneryChunk> chunks;      // map of terrain chunk per grid point
    int timeCounter;                            // used as timestamp for chunk creation time
    private final Array<Scene> scenes;
    private final Scene[] lodScenes;          // array of Scenes at different level of detail
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
    private final Array<SceneryChunk> visibleChunks;
    private FloatBuffer instanceData;   // temp buffer to transfer instance data

    public Scenery( Terrain terrain ) {
        this.terrain = terrain;

        chunks = new HashMap<>();
        visibleChunks = new Array<>();
        lodScenes = new Scene[Settings.LOD_LEVELS];
        scenes = new Array<>();
        decalInstances = new Array<>();
        timeCounter = 0;


        for(int lod = 0; lod < Settings.LOD_LEVELS;lod++) {
            //sceneAsset = new GLBLoader().load(Gdx.files.internal("models/birch-lod" + lod + ".glb"));
            SceneAsset sceneAsset = new GLBLoader().load(Gdx.files.internal("models/ducky-lod" + lod + ".glb"));
            lodScenes[lod] = new Scene(sceneAsset.scene);
        }




        positions = new Array[Settings.LOD_LEVELS+1];
        for(int lod = 0; lod < Settings.LOD_LEVELS+1; lod++)
            positions[lod] = new Array<>();

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
        return scenes;
    }

    // need to be rendered with the instanced decal shader
    public Array<ModelInstance> getDecalInstances(){
        return decalInstances;
    }


    public void update(Camera cam){
        timeCounter++;

        int px = (int)Math.floor(cam.position.x/SceneryChunk.CHUNK_SIZE);
        int pz = (int)Math.floor(cam.position.z/SceneryChunk.CHUNK_SIZE);

        // Add a NxN square of chunks to the scenes array (is RANGE is 2, this is 5x5)
        // Create chunks as needed

        visibleChunks.clear();
        for (int cx = px-RANGE; cx <= px+RANGE; cx++) {
            for (int cz = pz-RANGE; cz <= pz+RANGE; cz++) {

                Integer key = makeKey(cx, cz);

                SceneryChunk chunk = chunks.get(key);
                if(chunk == null) {
                    chunk = new SceneryChunk(cx, cz, timeCounter, terrain);
                    chunks.put(key, chunk);
                }
                if(chunk != null && cam.frustum.boundsInFrustum(chunk.bbox)) {  // frustum culling
                    visibleChunks.add(chunk);
                    chunk.lastSeen = timeCounter;
                }
            }
        }

        // Now get the instance date from all visible chunks
        //
        for(int lod = 0; lod < Settings.LOD_LEVELS+1; lod++)
            positions[lod].clear();

        for(SceneryChunk chunk : visibleChunks ){
            float distance = cam.position.dst( chunk.getWorldPosition() );
            int level = determineLODlevel(distance);
            chunk.setLodLevel(level);

            positions[level].addAll( chunk.getPositions() );
        }

        // Update the stats for the GUI
        //
        instanceCount = 0;
        for(int lod = 0; lod < Settings.LOD_LEVELS+1; lod++){
            statistics[lod].instanceCount = positions[lod].size;
            instanceCount += positions[lod].size;
        }
        if(instanceCount > MAX_INSTANCES) throw new GdxRuntimeException("Too many instances! > "+MAX_INSTANCES);

        // Update instance data for every LOD model and the impostor model
        //
        for(int lod = 0; lod < Settings.LOD_LEVELS; lod++)
            updateInstanced(lodScenes[lod].modelInstance, positions[lod]);
        updateInstancedDecals(decalInstance, positions[Settings.LOD_LEVELS]);    // instances for decal
    }

    // convert chunk (X,Y) to a single integer for easy use as a key in the hash map
    private int makeKey(int cx, int cz) {
        return cx + 1000 * cz;
    }

    private int determineLODlevel( float distance ){
        // allocate this instance to one of the LOD levels depending on the distance

        if (distance < Settings.lod1Distance   )
            return 0;
        else if ( distance < Settings.lod2Distance  )
            return 1;
        else if (distance < Settings.impostorDistance  )
            return 2;
        else
            return 3;
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

        if(positions.size > instanceCount) throw new GdxRuntimeException("too many instances for instance buffer");

        Mesh mesh = modelInstance.model.meshes.first();       // assumes model is one mesh

        if(instanceData == null) { // on first use
            // Create offset FloatBuffer that will contain instance data to pass to shader
            instanceData = BufferUtils.newFloatBuffer(MAX_INSTANCES * 16);   // 16 floats for the matrix
        }

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

    private void updateInstancedDecals( ModelInstance modelInstance, Array<Vector4> positions ) {

        Mesh mesh = modelInstance.model.meshes.first();       // assumes model is one mesh

        instanceData.clear();
        for(Vector4 pos: positions) {
            instanceData.put(new float[] { pos.x, pos.y, pos.z, pos.w });
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
    }
}

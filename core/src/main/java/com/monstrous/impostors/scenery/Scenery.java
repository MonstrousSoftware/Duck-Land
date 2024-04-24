package com.monstrous.impostors.scenery;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
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
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

import java.nio.FloatBuffer;


// Class to manage many scenery items for an infinite area, placed at terrain height.
// Rendered at different LOD levels or as impostors
// (currently only supports one type of item)


public class Scenery implements SceneryInterface, Disposable {

    private static final int MAX_MODEL_INSTANCES =  500;
    private static final int MAX_DECAL_INSTANCES = 35000;

    SceneryChunks sceneryChunks;
    LodModel lodModel;
    public Statistic[] statistics;
    public int instanceCount = 1;
    private final SceneAsset sceneAsset;
    private final Array<Scene> scenes;
    private final Array<ModelInstance> decalInstances;


    public Scenery( Terrain terrain, float separationDistance ) {
        sceneryChunks = new SceneryChunks(0, terrain, separationDistance );

        sceneAsset = new GLTFLoader().load(Gdx.files.internal("models/duck-land.gltf"));
        lodModel = new LodModel(sceneAsset, "ducky", 3, MAX_MODEL_INSTANCES, MAX_DECAL_INSTANCES);

        scenes = new Array<>();
        decalInstances = new Array<>();


        statistics = new Statistic[Settings.LOD_LEVELS+1];
        for(int lod = 0; lod < Settings.LOD_LEVELS+1; lod++) {
            statistics[lod] = new Statistic();
            statistics[lod].vertexCount = lodModel.getVertexCount(lod);
        }

        decalInstances.add(lodModel.getImpostor());

        Settings.lodLevel = -1;     // show all LOD levels and impostors

    }

    public Array<Scene> getScenes(){
        // for the sake of debug options we rebuild this as needed
        Scene[] lodScenes = lodModel.getScenes();

        scenes.clear();
        if(Settings.lodLevel == -1) {
            for (int lod = 0; lod < Settings.LOD_LEVELS; lod++) {
               if(lodModel.getInstanceCount(lod) > 0)
                    scenes.add(lodScenes[lod]);
            }
        }
        else if (Settings.lodLevel < Settings.LOD_LEVELS) {
            if(lodModel.getInstanceCount(Settings.lodLevel) > 0)
                scenes.add(lodScenes[Settings.lodLevel]);
        }
        return scenes;
    }

    // need to be rendered with the instanced decal shader
    public Array<ModelInstance> getImpostors(){
        return decalInstances;
    }

    private float rotation;

    public void update(float deltaTime, PerspectiveCamera cam, boolean forceUpdate){
        sceneryChunks.update(cam, forceUpdate);
        Array<SceneryChunk> visibleChunks = sceneryChunks.getVisibleChunks();

        // Now get the instance data from all visible chunks
        //
        lodModel.beginInstances();

        if(Settings.singleInstance){
            if(Settings.lodLevel < 0)   // don't allow multiple LOD levels
                Settings.lodLevel = 0;
            rotation += deltaTime*0.5f;
            Vector4 singlePos = new Vector4(0,0,0,rotation);       // slowly rotate
            lodModel.addInstance(Settings.lodLevel, singlePos);

        } else {

            float diagonalDistance = 0.707f * SceneryChunk.CHUNK_SIZE;        // subtract distance from corner to centre of chunk in case the camera is in corner of chunk (0.5*sqrt(2))

            for (SceneryChunk chunk : visibleChunks) {

                int level = determineLODlevel(chunk.distance - diagonalDistance);
                chunk.setLodLevel(level);

                if (level <= 2)      // for chunks at high LOD level (high poly count), test at individual instance level
                    lodModel.addInstances(cam, chunk.getPositions());
                else
                    lodModel.addInstances(level, chunk.getPositions());
            }

        }

        lodModel.endInstances();


        // Update the stats for the GUI
        //
        instanceCount = 0;
        for (int lod = 0; lod < Settings.LOD_LEVELS + 1; lod++) {
            int num = lodModel.getInstanceCount(lod);
            statistics[lod].instanceCount = num;
            instanceCount += num;
        }
        if (instanceCount > MAX_MODEL_INSTANCES+MAX_DECAL_INSTANCES) throw new GdxRuntimeException("Too many instances! > " + MAX_MODEL_INSTANCES+MAX_DECAL_INSTANCES);
    }

    private int determineLODlevel( float distance ){
        // allocate this instance to one of the LOD levels depending on the distance

        for(int lod = Settings.LOD_LEVELS-1; lod >= 0; lod--) {
            if (distance >= Settings.lodDistances[lod]   )        // optimized: most common case first
                return lod+1;
        }
        return 0;       // LOD level 0, highest poly count
    }

    @Override
    public void dispose() {
        scenes.clear();
        sceneryChunks.dispose();
        sceneAsset.dispose();
        lodModel.dispose();
    }
}

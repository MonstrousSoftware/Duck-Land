package com.monstrous.impostors.scenery;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.monstrous.impostors.Settings;
import com.monstrous.impostors.Statistics;
import com.monstrous.impostors.terrain.Terrain;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;


// Class to manage many scenery items for an infinite area, placed at terrain height.
// Rendered at different LOD levels or as impostors
// (currently only supports one type of item)


public class Scenery implements SceneryInterface, Disposable {

    private static final int MAX_MODEL_INSTANCES =  500;
    private static final int MAX_DECAL_INSTANCES = 35000;

    SceneryChunks sceneryChunks;
    Array<LodModel> lodModels;
    public Statistics statistics;
    public int numTypes;
    public int instanceCount = 1;
    private final SceneAsset sceneAsset;
    private final Array<Scene> scenes;
    private final Array<ModelInstance> decalInstances;


    public Scenery( Terrain terrain, float separationDistance ) {
        lodModels = new Array<>();


        sceneAsset = new GLTFLoader().load(Gdx.files.internal("models/duck-land.gltf"));
        lodModels.add( new LodModel(sceneAsset, "ducky", Settings.LOD_LEVELS, MAX_MODEL_INSTANCES, MAX_DECAL_INSTANCES) );
        lodModels.add( new LodModel(sceneAsset, "simplePalm", Settings.LOD_LEVELS, MAX_MODEL_INSTANCES, MAX_DECAL_INSTANCES));

        numTypes = lodModels.size;
        statistics = new Statistics(numTypes, Settings.LOD_LEVELS);
        statistics.setName(0, "Duck");
        statistics.setName(1, "Palm Tree");

        float[]  bias = { .98f, .02f };       // relative probabilities per type, should add up to 1.0

        sceneryChunks = new SceneryChunks(0, terrain, numTypes, bias, separationDistance );


        scenes = new Array<>();
        decalInstances = new Array<>();



        for(int type = 0; type < numTypes; type++ ) {
            for (int lod = 0; lod < Settings.LOD_LEVELS + 1; lod++) {
                statistics.setVertexCount(type, lod, lodModels.get(type).getVertexCount(lod));
//                statistics[lod] = new Statistics();
//                statistics[lod].vertexCount = lodModels.first().getVertexCount(lod);
            }
        }

        for(LodModel lodModel : lodModels)
            decalInstances.add(lodModel.getImpostor());

        Settings.lodLevel = -1;     // show all LOD levels and impostors

    }

    public Array<Scene> getScenes(){
        scenes.clear();

        // for the sake of debug options we rebuild this as needed
        for(LodModel lodModel : lodModels) {
            Scene[] lodScenes = lodModel.getScenes();

            if (Settings.lodLevel == -1) {
                for (int lod = 0; lod < Settings.LOD_LEVELS; lod++) {
                    if (lodModel.getInstanceCount(lod) > 0)
                        scenes.add(lodScenes[lod]);
                }
            } else if (Settings.lodLevel < Settings.LOD_LEVELS) {
                if (lodModel.getInstanceCount(Settings.lodLevel) > 0)
                    scenes.add(lodScenes[Settings.lodLevel]);
            }
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
        for(LodModel lodModel : lodModels)
            lodModel.beginInstances();

        if(Settings.singleInstance){
            if(Settings.lodLevel < 0)   // don't allow multiple LOD levels
                Settings.lodLevel = 0;
            rotation += deltaTime*0.5f;
            Vector4 singlePos = new Vector4(0,0,0,rotation);       // slowly rotate
            lodModels.get(1).addInstance(Settings.lodLevel, singlePos);

        } else {

            float diagonalDistance = 0.707f * SceneryChunk.CHUNK_SIZE;        // subtract distance from corner to centre of chunk in case the camera is in corner of chunk (0.5*sqrt(2))

            for (SceneryChunk chunk : visibleChunks) {

                int level = determineLODlevel(chunk.distance - diagonalDistance);
                chunk.setLodLevel(level);

                int type = 0;
                for(LodModel lodModel : lodModels) {    // for each scenery type
                    if (level <= 2)      // for chunks at high LOD level (high poly count), test at individual instance level
                        lodModel.addInstances(cam, chunk.getPositions(type));
                    else
                        lodModel.addInstances(level, chunk.getPositions(type));
                    type++;
                }
            }

        }
        for(LodModel lodModel : lodModels)
            lodModel.endInstances();


        // Update the stats for the GUI
        //
        instanceCount = 0;
        for(int type = 0; type < numTypes; type++ ) {
            for (int lod = 0; lod < Settings.LOD_LEVELS + 1; lod++) {
                int num = lodModels.get(type).getInstanceCount(lod);
                statistics.setInstanceCount(type, lod, num);
                instanceCount += num;
            }
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
        for(LodModel lodModel : lodModels)
            lodModel.dispose();
    }
}

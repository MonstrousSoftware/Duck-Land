package com.monstrous.impostors.scenery;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Vector4;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.monstrous.impostors.Impostor;
import com.monstrous.impostors.ImpostorBuilder;
import com.monstrous.impostors.Settings;
import com.monstrous.impostors.shaders.InstancedDecalShaderProvider;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

import java.nio.FloatBuffer;

public class LodModel implements Disposable {

    private String nodeName;
    private int lodLevels;
    private int maxModelInstances;
    private int maxImpostorInstances;

    private final Scene[] lodScenes;                // array of Scenes at different level of detail
    private Model impostorModel;
    private ModelInstance impostorInstance;
    private Vector3 modelCentre;
    private float radius;
    private ImpostorBuilder builder;
    private Texture impostorTexture;
    private Vector2 regionSize;
    private Array<Vector4>[] positions;
    private TextureRegion atlasRegion;
    private TextureRegion textureRegion0;
    private float elevationStep;
    private int elevations;
    private FloatBuffer instanceData;   // temp buffer to transfer instance data

    // nodeNameRoot is "tree" if the LOD nodes are called "tree.LOD0", "tree.LOD1", "tree.LOD2"
    //
    public LodModel(SceneAsset sceneAsset, String nodeNameRoot, int lodLevels, int maxModelInstances, int maxImpostorInstances) {
        this.nodeName = nodeName;
        this.lodLevels = lodLevels;
        this.maxModelInstances = maxModelInstances;
        this.maxImpostorInstances = maxImpostorInstances;

        lodScenes = new Scene[lodLevels];

        for(int lod = 0; lod < lodLevels; lod++) {

            // LOD nodes need to be named as "tree.LOD0", "tree.LOD1", "tree.LOD2"
            String name = nodeNameRoot + ".LOD" + lod;
            lodScenes[lod] = new Scene(sceneAsset.scene, name);
            if(lodScenes[lod].modelInstance.nodes.size == 0) {
                Gdx.app.error("GLTF load error: node not found", name);
                Gdx.app.exit();
            }

            Node node = lodScenes[lod].modelInstance.nodes.first();

            // reset node position to origin
            node.translation.set(0, 0, 0);
            node.scale.set(1, 1, 1);
            node.rotation.idt();
            lodScenes[lod].modelInstance.calculateTransforms();
            makeInstanced(lodScenes[lod].modelInstance, maxModelInstances);       // could make an estimate per LOD level here
        }

        impostorInstance = makeImpostor();
        // enable instancing for impostors
        makeInstancedDecals(impostorInstance, maxImpostorInstances);

        positions = new Array[lodLevels+1];
        for(int lod = 0; lod < lodLevels+1; lod++)
            positions[lod] = new Array<>();

        // Create offset FloatBuffer that will contain instance data to pass to shader
        // we are dimensioning it for the worst case, which means we probably waste a lot of memory here
        int bufferSize = Math.max(maxModelInstances * 16, maxImpostorInstances * 4);
        instanceData = BufferUtils.newFloatBuffer(bufferSize);   // 16 floats for the matrix, 4 floats per impostor
    }

    // get (instanced) Scenes for the different LOD models.  If you have 3 LOD levels, this will return an array of 3 scenes.
    public Scene[] getScenes() {
        return lodScenes;
    }

    // get the (instanced) model instance to uses as impostor.
    public ModelInstance getImpostor() {
        return impostorInstance;
    }

    public int getVertexCount(int level ) {
        Node node;

        if(level == lodLevels)
            node = impostorInstance.nodes.first();
        else
            node = lodScenes[level].modelInstance.nodes.first();
        return node.parts.first().meshPart.mesh.getNumVertices();
    }



    public void beginInstances(){
        for(int lod = 0; lod < lodLevels+1; lod++)        // clear buffers per LOD level and for Impostors
            positions[lod].clear();
    }


    public void addInstances( int level, Array<Vector4> instanceData ){
        positions[level].addAll( instanceData );
    }

    public void addInstance( int level, Vector4 instanceData ){
        positions[level].add( instanceData );
    }


    private Vector3 tmpPos = new Vector3();

    // allocate instances from this list on individual basis to LOD level
    // also perform individual frustum clipping
    //
    public void addInstances(Camera cam, Array<Vector4> instanceData ){
        for(Vector4 position : instanceData ){

            tmpPos.set( position.x, position.y, position.z ).add(modelCentre);
            if(cam.frustum.sphereInFrustum(tmpPos, radius)) {        // some margin to prevent popping
                // determine level of detail from distance to camera
                float distance = cam.position.dst(tmpPos);
                int level = determineLODlevel(distance);
                positions[level].add(position);
            }
        }
    }

    private int determineLODlevel( float distance ){
        // allocate this instance to one of the LOD levels depending on the distance

        for(int lod = lodLevels-1; lod >= 0; lod--) {
            if (distance >= Settings.lodDistances[lod]   )        // optimized: most common case first
                return lod+1;
        }
        return 0;       // LOD level 0, highest poly count
    }

    public void endInstances() {
        // Update instance data for every LOD model and the impostor model
        //
        for(int lod = 0; lod < Settings.LOD_LEVELS; lod++)
            updateInstanced(lodScenes[lod].modelInstance, positions[lod]);
        updateInstancedDecals(impostorInstance, positions[Settings.LOD_LEVELS]);    // instances for decal
    }

    public int getInstanceCount(int level ) {
        return positions[level].size;
    }


    private ModelInstance makeImpostor(){
        BoundingBox modelBoundingBox = new BoundingBox();
        lodScenes[0].modelInstance.calculateBoundingBox(modelBoundingBox);          // get dimensions of model
        Vector3 dimensions = new Vector3();
        modelBoundingBox.getDimensions(dimensions);
        radius =  dimensions.len() / 2f;     // determine model radius for frustum clipping
        modelCentre = modelBoundingBox.getCenter(new Vector3());        // offset from model origin to model centre (origin should be at floor level)


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
        impostorModel = Impostor.createImposterModel(textureRegion0, lodScenes[0].modelInstance);
        ModelInstance instance = new ModelInstance(impostorModel, 0, 0, 0);

        // use user data to pass info  on the texture atlas to the shader
        instance.userData = new InstancedDecalShaderProvider.UVSize(regionSize.x/textureSize, regionSize.y/textureSize);
        return instance;
    }



    private void makeInstanced( ModelInstance modelInstance, int maxInstances ) {

        Mesh mesh = modelInstance.nodes.first().parts.first().meshPart.mesh;       // get mesh belonging to the node (assuming there is not more than one)

        // add matrix per instance
        mesh.enableInstancedRendering(false, maxInstances,      // pass maximum instance count
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 0),
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 1),
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 2),
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 3)   );
    }

    private void updateInstanced( ModelInstance modelInstance, Array<Vector4> positions ) {

        int count = positions.size;
        if(positions.size >= maxModelInstances) {
            Gdx.app.error("buffer size", "too many instances for instance buffer: " + positions.size);
            count = maxModelInstances-1;
        }

        Mesh mesh = modelInstance.nodes.first().parts.first().meshPart.mesh;       // get mesh belonging to the node (assuming there is not more than one)

        // fill instance data buffer
        instanceData.clear();
        Matrix4 instanceTransform = new Matrix4();
        for(int i = 0; i < count; i++) {
            Vector4 pos = positions.get(i);

            instanceTransform.setToRotationRad(Vector3.Y, pos.w);
            instanceTransform.setTranslation(pos.x, pos.y, pos.z);
            // transpose matrix for GLSL
            instanceData.put(instanceTransform.tra().getValues());                // transpose matrix for GLSL
        }
        instanceData.limit( count * 16 );  // amount of data in buffer
        instanceData.position(0);      // rewind float buffer to start
        mesh.setInstanceData(instanceData);
    }


    private void makeInstancedDecals( ModelInstance modelInstance, int maxInstances ) {
        Mesh mesh = modelInstance.nodes.first().parts.first().meshPart.mesh;       // get mesh belonging to the node (assuming there is not more than one)


        // add vector4 per instance containing position and Y rotation
        mesh.enableInstancedRendering(false, maxInstances,
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_offset", 0));
    }

    private float[] tmpFloat4 = { 1, 2, 3, 4 };

    private void updateInstancedDecals( ModelInstance modelInstance, Array<Vector4> positions ) {
        if(positions.size >= maxImpostorInstances) throw new GdxRuntimeException("too many instances for impostor instance buffer: "+positions.size);

        Mesh mesh = modelInstance.nodes.first().parts.first().meshPart.mesh;       // get mesh belonging to the node (assuming there is not more than one)

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
        impostorModel.dispose();
    }
}

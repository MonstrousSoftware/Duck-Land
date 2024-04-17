package com.monstrous.impostors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ScreenUtils;
import com.monstrous.impostors.gui.GUI;
import com.monstrous.impostors.shaders.InstancedDecalShaderProvider;
import com.monstrous.impostors.shaders.InstancedPBRDepthShaderProvider;
import com.monstrous.impostors.shaders.InstancedPBRShaderProvider;
import com.monstrous.impostors.terrain.Terrain;
import com.monstrous.impostors.terrain.TerrainDebug;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight;
import net.mgsx.gltf.scene3d.scene.*;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;

import java.nio.Buffer;
import java.nio.FloatBuffer;


public class GameScreen extends ScreenAdapter {

    // members displayed by gui
    public Statistic[] statistics;
    public int instanceCount = 1;

    public static final int AREA_LENGTH = 10000;
    private static final int SEPARATION_DISTANCE = 20;
    private static final int SHADOW_MAP_SIZE = 4096;

    private SceneManager sceneManager;
    private SceneAsset sceneAsset;
    private Scene groundScene;
    private PerspectiveCamera camera;
    private Cubemap diffuseCubemap;
    private Cubemap environmentCubemap;
    private Cubemap specularCubemap;
    private Texture brdfLUT;
    private SceneSkybox skybox;
    private DirectionalShadowLight light;
    private CameraInputController camController;
    private float cameraDistance;
    private Scene[] lodScenes;
    private GUI gui;
    private ImpostorBuilder builder;
    private TextureRegion[] textureRegions;
    private Texture impostorTexture;
    private ModelInstance decalInstance;
    //private ModelInstance[] treeDecalInstances;  // decals from different angles
    private TextureRegion atlasRegion;
    private SpriteBatch batch;
    private float elevationStep;
    private int elevations;
    private Array<Vector2> points;
    private Array<Vector4> allPositions;
    private Array<Vector4>[] positions;
    private ModelBatch modelBatch;
    Vector2 regionSize = new Vector2();
    private CascadeShadowMap csm;
    private Terrain terrain;
    private TerrainDebug terrainDebug;
    private Vector3 origin;


    @Override
    public void show() {

        if (Gdx.gl30 == null) {
            throw new GdxRuntimeException("GLES 3.0 profile required for this programme.");
        }

        statistics = new Statistic[Settings.LOD_LEVELS+1];
        for(int lod = 0; lod < Settings.LOD_LEVELS+1; lod++)
            statistics[lod] = new Statistic();

        gui = new GUI( this );

        lodScenes = new Scene[Settings.LOD_LEVELS];

        // create scene manager
        // but use our own shader providers for PBR shaders that support instanced meshes
        sceneManager = new SceneManager( new InstancedPBRShaderProvider(), new InstancedPBRDepthShaderProvider() );

        for(int lod = 0; lod < Settings.LOD_LEVELS;lod++) {
            //sceneAsset = new GLBLoader().load(Gdx.files.internal("models/birch-lod" + lod + ".glb"));
            sceneAsset = new GLBLoader().load(Gdx.files.internal("models/ducky-lod" + lod + ".glb"));
            lodScenes[lod] = new Scene(sceneAsset.scene);
        }






        // setup camera
        camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cameraDistance = 40f;
        camera.near = 0.01f;
        camera.far = 30000f;
        camera.position.set(0, 20, 50);
		camera.up.set(Vector3.Y);
		camera.lookAt(Vector3.Zero);
		camera.update();
        sceneManager.setCamera(camera);

        terrain = new Terrain(camera.position);
        terrainDebug = new TerrainDebug(terrain);

        camera.position.set(0, terrain.getHeight(0, 50) + 10, 50);
        origin = new Vector3(0, terrain.getHeight(0, 0), 0);

//        sceneAsset = new GLBLoader().load(Gdx.files.internal("models/groundPlane.glb"));
//        groundScene = new Scene(sceneAsset.scene);
//        if(Settings.multiple)
//            groundScene.modelInstance.transform.setToScaling(AREA_LENGTH/20, 20, AREA_LENGTH/20);
//        sceneManager.addScene(groundScene);


        // input multiplexer to input to GUI and to cam controller
        InputMultiplexer im = new InputMultiplexer();
        Gdx.input.setInputProcessor(im);
        camController = new CameraInputController(camera);
        camController.scrollFactor = -1f;   // fast zoom
        im.addProcessor(gui.stage);
        im.addProcessor(camController);

        sceneManager.environment.set(new PBRFloatAttribute(PBRFloatAttribute.ShadowBias, 0.01f));

        if(Settings.cascadedShadows) {
            csm = new CascadeShadowMap(2);
            sceneManager.setCascadeShadowMap(csm);
        }

        // setup light
        // set the light parameters so that your area of interest is in the shadow light frustum
        // but keep it reasonably tight to keep sharper shadows

        float farPlane = 300;
        float nearPlane = 0;
        float VP_SIZE = 300f;
        light = new DirectionalShadowLight(SHADOW_MAP_SIZE, SHADOW_MAP_SIZE).setViewport(VP_SIZE,VP_SIZE,nearPlane,farPlane);

        light.direction.set(1, -3, 1).nor();
        light.color.set(Color.WHITE);
        sceneManager.environment.add(light);

        // setup quick IBL (image based lighting)
        IBLBuilder iblBuilder = IBLBuilder.createOutdoor(light);
		environmentCubemap = iblBuilder.buildEnvMap(1024);
        diffuseCubemap = iblBuilder.buildIrradianceMap(256);
        specularCubemap = iblBuilder.buildRadianceMap(10);
        iblBuilder.dispose();

        // This texture is provided by the library, no need to have it in your assets.
        brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));

        sceneManager.setAmbientLight(0.3f);
        sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
        sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap));
        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap));

        // setup skybox
        skybox = new SceneSkybox(environmentCubemap);
        sceneManager.setSkyBox(skybox);

        builder = new ImpostorBuilder();
        int textureSize = 2048;

        impostorTexture = builder.createImpostor(lodScenes[0], textureSize, regionSize);
        Gdx.app.log("region size", ""+regionSize.x+" , "+regionSize.y);



        int numAngles =  ImpostorBuilder.NUM_ANGLES;
        int width = textureSize / numAngles;
        int height = (int)regionSize.y;
        elevations = textureSize / height;
        elevationStep = 60f/elevations;   // degrees per elevation step


        textureRegions = new TextureRegion[numAngles * elevations];
        //treeDecalInstances = new ModelInstance[numAngles * elevations];




        atlasRegion = new TextureRegion(impostorTexture, 0f, 0f, 1.0f, 1.0f);

        int y = 0;

        for(int elevation = 0; elevation < elevations; elevation++) {
            int x = 0;
            for (int angle = 0; angle < numAngles; angle++) {

                TextureRegion region = new TextureRegion(impostorTexture, x, y, width, height);
                region.flip(false, true);        // note the texture is upside down, so flip the texture region
                textureRegions[elevation * numAngles + angle] = region;
                x += width;
            }
            y += height;
        }

        // create decal instance
        Model model = Impostor.createImposterModel(textureRegions[0], lodScenes[0].modelInstance);
        decalInstance = new ModelInstance(model, 0, 0, 0);
        // use user data to pass info  on the texture atlas to the shader
        decalInstance.userData = new InstancedDecalShaderProvider.UVSize(regionSize.x/textureSize, regionSize.y/textureSize);


        modelBatch = new ModelBatch( new InstancedDecalShaderProvider() );
        batch = new SpriteBatch();

        // generate a random poisson distribution of instances over a rectangular area, meaning instances are never too close together
        MathUtils.random.setSeed(1234);         // fix the random distribution to always be identical
        Rectangle area = new Rectangle(1, 1, AREA_LENGTH, AREA_LENGTH);
        points = PoissonDistribution.generatePoissonDistribution(SEPARATION_DISTANCE, area);
        instanceCount = points.size;

        // convert 2d points to 3d positions

        allPositions = new Array<>();
        MathUtils.random.setSeed(1234);         // fix the random rotation to always be identical
        for(Vector2 point : points ) {
            float x = point.x - AREA_LENGTH/2f;
            float z = point.y - AREA_LENGTH/2f;
            float h = terrain.getHeight(x, z);
            float angleY = MathUtils.random(0.0f, (float)Math.PI*2.0f);      // random rotation around Y (up) axis
            Vector4 position = new Vector4( x, h, z, angleY);
            allPositions.add( position );
        }

        positions = new Array[Settings.LOD_LEVELS+1];
        for(int lod = 0; lod < Settings.LOD_LEVELS+1; lod++)
            positions[lod] = new Array<>();

        allocateLodInstances( origin );

        if(Settings.multiple) {

            // instances for every LOD model
            for(int lod = 0; lod < Settings.LOD_LEVELS; lod++)
                makeInstanced(lodScenes[lod].modelInstance, positions[lod]);

            makeInstancedDecals(decalInstance, positions[Settings.LOD_LEVELS]);    // instances for decal
            updateInstanceData();
        }
        //else
            //instanceCount = 1;

        Settings.lodLevel = -1;

    }


    private Vector3 pos = new Vector3();

    private void allocateLodInstances( Vector3 reference ) {
        float scale = AREA_LENGTH;
        for(int lod = 0; lod < Settings.LOD_LEVELS+1; lod++)
            positions[lod].clear();

        for (Vector4 position : allPositions) {
            pos.set(position.x, position.y, position.z);
//            if(!camera.frustum.sphereInFrustum(pos, 2f))
//                continue;
            float distance = pos.dst(reference);
            if (distance < Settings.lod1Distance   )
                positions[0].add(position);
            else if ( distance < Settings.lod2Distance  )
                positions[1].add(position);
            else if (distance < Settings.impostorDistance  )
                positions[2].add(position);
            else
                positions[3].add(position);
        }
        //Gdx.app.log("Distribution:", "LOD0: " + positions[0].size + " LOD1: " + positions[1].size + " LOD2: " + positions[2].size + " IMP: " + positions[3].size);

        for(int lod = 0; lod < Settings.LOD_LEVELS+1; lod++){
            statistics[lod].instanceCount = positions[lod].size;
            if(lod < Settings.LOD_LEVELS)
                statistics[lod].vertexCount = lodScenes[lod].modelInstance.model.meshes.first().getNumVertices();
            else
                statistics[lod].vertexCount = decalInstance.model.meshes.first().getNumVertices();
        }
    }

    private void updateInstanceData() {

        // instances for every LOD model
        for(int lod = 0; lod < Settings.LOD_LEVELS; lod++)
            updateInstanced(lodScenes[lod].modelInstance, positions[lod]);

        updateInstancedDecals(decalInstance, positions[Settings.LOD_LEVELS]);    // instances for decal
    }



    private Vector3 forward = new Vector3();
    private Vector3 right = new Vector3();
    private Vector3 up = new Vector3();
    private Quaternion q = new Quaternion();

    private float lodUpdate = 0.5f;

   // @Override
    public void render(float deltaTime) {

        if(Gdx.input.isKeyJustPressed(Input.Keys.TAB))
            Settings.lodLevel = (Settings.lodLevel + 1 ) % (Settings.LOD_LEVELS+1);
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_0))
            Settings.lodLevel = 0;
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_1))
            Settings.lodLevel = 1;
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_2))
            Settings.lodLevel = 2;
        if(Gdx.input.isKeyJustPressed(Input.Keys.M))
            Settings.multiple = !Settings.multiple;

        if(Gdx.input.isKeyJustPressed(Input.Keys.T))
            Settings.debugTerrainChunkAllocation = !Settings.debugTerrainChunkAllocation;

        lodUpdate -= deltaTime;     // todo or if camera moved
        if(lodUpdate < 0) {
            lodUpdate = 0.1f;
            allocateLodInstances( camera.position );
            updateInstanceData();
        }

        terrain.update( camera );


        // animate camera
//        viewAngle += deltaTime;
//        camera.position.x = cameraDistance * (float)Math.cos(viewAngle);
//        camera.position.z = cameraDistance * (float)Math.sin(viewAngle);
//        //camera.position.y = cameraDistance * (float) Math.sin((time*10f) * MathUtils.degreesToRadians);
//        camera.position.y = .3f*cameraDistance;

		camera.up.set(Vector3.Y);
		camera.lookAt(Vector3.Zero);
		camera.update();
        camController.update();

        if(Settings.cascadedShadows) {
            csm.setCascades(sceneManager.camera, light, 0, 10f);
        }
        else
            light.setCenter(camera.position); // keep shadow light on player so that we have shadows

        sceneManager.getRenderableProviders().clear();
        //sceneManager.addScene(groundScene);

        // terrain chunks are taken directly from the Terrain class, these are not game objects
        for(Scene scene : terrain.scenes)
            sceneManager.addScene(scene, false);

        if(Settings.lodLevel < 0) { // mixed levels, add all LOD instances
            for(int lod = 0; lod < Settings.LOD_LEVELS; lod++){
                sceneManager.addScene(lodScenes[lod]);
            }

        }
        else if(Settings.lodLevel < Settings.LOD_LEVELS) {      // one specific LOD level
            sceneManager.addScene(lodScenes[Settings.lodLevel]);

            // numVertices = lodScenes[Settings.lodLevel].modelInstance.model.meshes.first().getNumVertices();
        }
        // render
        ScreenUtils.clear(Color.SKY, true);
        sceneManager.update(deltaTime);
        sceneManager.render();

        int index = 0;


        if(Settings.lodLevel == Settings.LOD_LEVELS || Settings.lodLevel < 0 ) {      // impostors
//            if(Settings.multiple)
//                index = 0;          // test
//            else
//                // which decal to use? Depends on viewing angle
//                index = getViewingIndex(forward);
//
//
////            Gdx.app.log("view angle:", ""+angle+" index:"+index);
//
//            ModelInstance instance = decalInstance;
//
//            if(!Settings.multiple) {
//                // get billboard to face the camera
//                instance.transform.getTranslation(pos);         // get instance position
//                forward.set(camera.position).sub(pos).nor();        // vector towards camera
//                right.set(camera.up).crs(forward).nor();
//                up.set(forward).crs(right).nor();
//                q.setFromAxes(right.x, up.x, forward.x, right.y, up.y, forward.y, right.z, up.z, forward.z);
//                instance.transform.set(q).setTranslation(pos);
//            }

            if(positions[Settings.LOD_LEVELS].size > 0) {
                modelBatch.begin(camera);
                modelBatch.render(decalInstance);
                modelBatch.end();
            }

            //numVertices = instance.model.meshes.first().getNumVertices();
        }

        terrainDebug.debugRender(Vector3.Zero, camera.position);

        batch.begin();
        batch.draw(textureRegions[index], 0, 0, regionSize.x, regionSize.y);
        batch.end();

        gui.render(deltaTime);
    }


    // determine which decal texture region to use depending on the viewing angle
    private int getViewingIndex(Vector3 forward ){
        // which decal to use? Depends on viewing angle
        float angle = (float)Math.toDegrees(Math.atan2(forward.z, forward.x));
        angle -=90f;
        if(angle < 0)   // avoid negative angles
            angle += 360f;

        int indexHorizontal = (int)(ImpostorBuilder.NUM_ANGLES * angle / 360f);

        float h = (float)Math.sqrt(forward.x*forward.x + forward.z*forward.z);
        float elevationAngle = (float)Math.toDegrees(Math.atan2(forward.y, h));

        int indexVertical = (int)(elevationAngle / elevationStep);
        if(indexVertical >= elevations)
            indexVertical = elevations - 1;
        if(indexVertical < 0)
            indexVertical = 0;

        int index = indexVertical * ImpostorBuilder.NUM_ANGLES + indexHorizontal;
        //Gdx.app.log("index", ""+index+" horiz: "+indexHorizontal + " vert: "+indexVertical+" elev: "+elevationAngle);
        return index;
    }



    private void makeInstanced( ModelInstance modelInstance, Array<Vector4> positions ) {

        Mesh mesh = modelInstance.model.meshes.first();       // assumes model is one mesh

        // add matrix per instance
        mesh.enableInstancedRendering(true, instanceCount,
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 0),
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 1),
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 2),
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 3)   );


        // Create offset FloatBuffer that will contain instance data to pass to shader
        FloatBuffer offsets = BufferUtils.newFloatBuffer(positions.size * 16);   // 16 floats for the matrix

        // fill instance data buffer
        Matrix4 instanceTransform = new Matrix4();
        for(Vector4 pos: positions) {

            instanceTransform.setToRotationRad(Vector3.Y, pos.w);
            instanceTransform.setTranslation(pos.x, pos.y, pos.z);
                          // transpose matrix for GLSL
            offsets.put(instanceTransform.tra().getValues());                // transpose matrix for GLSL
        }

        ((Buffer)offsets).position(0);      // rewind float buffer to start
        mesh.setInstanceData(offsets);
    }

    private void updateInstanced( ModelInstance modelInstance, Array<Vector4> positions ) {

        Mesh mesh = modelInstance.model.meshes.first();       // assumes model is one mesh


        // Create offset FloatBuffer that will contain instance data to pass to shader
        FloatBuffer offsets = BufferUtils.newFloatBuffer(positions.size * 16);   // 16 floats for the matrix

        // fill instance data buffer
        Matrix4 instanceTransform = new Matrix4();
        for(Vector4 pos: positions) {

            instanceTransform.setToRotationRad(Vector3.Y, pos.w);
            instanceTransform.setTranslation(pos.x, pos.y, pos.z);
            // transpose matrix for GLSL
            offsets.put(instanceTransform.tra().getValues());                // transpose matrix for GLSL
        }

        ((Buffer)offsets).position(0);      // rewind float buffer to start
        mesh.updateInstanceData(0, offsets);
    }


    private void makeInstancedDecals( ModelInstance modelInstance, Array<Vector4> positions ) {

        Mesh mesh = modelInstance.model.meshes.first();       // assumes model is one mesh

        // add matrix per instance
        mesh.enableInstancedRendering(false, instanceCount,
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_offset", 0));

//        // Create offset FloatBuffer that will contain instance data to pass to shader
//        FloatBuffer offsets = BufferUtils.newFloatBuffer(instanceCount * 4);
//        for(Vector4 pos: positions) {
//            offsets.put(new float[] { pos.x, pos.y, pos.z, pos.w });
//        }
//
//        ((Buffer)offsets).position(0);      // rewind float buffer to start
//        mesh.setInstanceData(offsets);

        mesh.setInstanceData(new float[4*instanceCount]);
    }

    private void updateInstancedDecals( ModelInstance modelInstance, Array<Vector4> positions ) {

        Mesh mesh = modelInstance.model.meshes.first();       // assumes model is one mesh


        // Create offset FloatBuffer that will contain instance data to pass to shader
        FloatBuffer offsets = BufferUtils.newFloatBuffer(positions.size * 4);
        for(Vector4 pos: positions) {
            offsets.put(new float[] { pos.x, pos.y, pos.z, pos.w });
        }

        ((Buffer)offsets).position(0);      // rewind float buffer to start
        mesh.setInstanceData(offsets);
    }

    @Override
    public void resize(int width, int height) {
        sceneManager.updateViewport(width, height);
        gui.resize(width, height);
    }


    @Override
    public void hide () {
        dispose();
    }

    @Override
    public void dispose() {
        sceneManager.dispose();
        sceneAsset.dispose();
        environmentCubemap.dispose();
        diffuseCubemap.dispose();
        specularCubemap.dispose();
        brdfLUT.dispose();
        skybox.dispose();
        gui.dispose();
        builder.dispose();
        terrain.dispose();
        terrainDebug.dispose();
    }

}

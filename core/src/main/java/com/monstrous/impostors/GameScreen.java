package com.monstrous.impostors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ScreenUtils;
import com.monstrous.impostors.gui.GUI;
import com.monstrous.impostors.inputs.CameraController;
import com.monstrous.impostors.scenery.Scenery;
import com.monstrous.impostors.scenery.SceneryDebug;
import com.monstrous.impostors.shaders.InstancedDecalShaderProvider;
import com.monstrous.impostors.shaders.InstancedPBRDepthShaderProvider;
import com.monstrous.impostors.shaders.InstancedPBRShaderProvider;
import com.monstrous.impostors.terrain.Terrain;
import com.monstrous.impostors.terrain.TerrainDebug;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight;
import net.mgsx.gltf.scene3d.scene.*;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;


public class GameScreen extends ScreenAdapter {

    private static final int SHADOW_MAP_SIZE = 4096;

    private SceneManager sceneManager;
    private SceneAsset sceneAsset;
    private Scene groundPlane;
    private PerspectiveCamera camera;
    private Cubemap diffuseCubemap;
    private Cubemap environmentCubemap;
    private Cubemap specularCubemap;
    private Texture brdfLUT;
    private SceneSkybox skybox;
    private DirectionalShadowLight light;
    private CameraController camController;
    private float cameraDistance;
    private GUI gui;
    private ModelBatch modelBatch;
    private CascadeShadowMap csm;
    private Terrain terrain;
    private TerrainDebug terrainDebug;
    private SceneryDebug sceneryDebug;
    public Scenery scenery;


    @Override
    public void show() {

        if (Gdx.gl30 == null) {
            throw new GdxRuntimeException("GLES 3.0 profile required for this programme.");
        }

        gui = new GUI( this );

        // hide the mouse cursor and fix it to screen centre, so it doesn't go out the window canvas
        Gdx.input.setCursorCatched(true);
        Gdx.input.setCursorPosition(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);

        // create scene manager
        // but use our own shader providers for PBR shaders that support instanced meshes
        sceneManager = new SceneManager( new InstancedPBRShaderProvider(), new InstancedPBRDepthShaderProvider() );

        // setup camera
        camera = new PerspectiveCamera(Settings.cameraFOV, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cameraDistance = 40f;
        camera.near = 1f;
        camera.far = 8192f;
        camera.position.set(0, 20, 50);
		camera.up.set(Vector3.Y);
		camera.lookAt(Vector3.Zero);
		camera.update();
        sceneManager.setCamera(camera);

        terrain = new Terrain(camera.position);
        terrainDebug = new TerrainDebug(terrain);

        camera.position.set(0, terrain.getHeight(0, 50) + 10, 50);

        // input multiplexer to input to GUI and to cam controller
        InputMultiplexer im = new InputMultiplexer();
        Gdx.input.setInputProcessor(im);
        camController = new CameraController(camera, terrain);
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

        sceneManager.setAmbientLight(Settings.ambientLightLevel);
        sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
        sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap));
        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap));

        // setup skybox
        skybox = new SceneSkybox(environmentCubemap);
        sceneManager.setSkyBox(skybox);

        scenery = new Scenery(terrain, Settings.scenerySeparationDistance);
        sceneryDebug = new SceneryDebug( scenery );

        sceneAsset = new GLTFLoader().load(Gdx.files.internal("models/duck-land.gltf"));
        groundPlane = new Scene(sceneAsset.scene, "groundPlane");

        modelBatch = new ModelBatch( new InstancedDecalShaderProvider() );      // to render the impostors
    }


    @Override
    public void render(float deltaTime) {

        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)){
            Gdx.app.exit();
        }

        if(Gdx.input.isKeyJustPressed(Input.Keys.TAB)){
            if(Settings.lodLevel == Settings.LOD_LEVELS)
                Settings.lodLevel = -1; // mixed mode
            else
                Settings.lodLevel++;
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.T))
            Settings.debugTerrainChunkAllocation = !Settings.debugTerrainChunkAllocation;
        if(Gdx.input.isKeyJustPressed(Input.Keys.P))
            Settings.debugSceneryChunkAllocation = !Settings.debugSceneryChunkAllocation;
        if(Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            Settings.singleInstance = !Settings.singleInstance;
            if(Settings.singleInstance) {
                camera.position.set(0, 20, 50);
                camera.lookAt(Vector3.Zero);
            }
            else
                Settings.lodLevel = -1;
        }

        if(Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
            for(int lod = 0; lod < Settings.LOD_LEVELS; lod++)
                Settings.lodDistances[lod] = 1.1f * Settings.lodDistances[lod];
            Settings.dynamicLODAdjustment = false;
            Gdx.app.log("Update LOD1 distance to:", ""+Settings.lodDistances[0]);
            scenery.update( deltaTime, camera, true );
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.X)) {
            for(int lod = 0; lod < Settings.LOD_LEVELS; lod++)
                Settings.lodDistances[lod] = 0.9f * Settings.lodDistances[lod];
            Gdx.app.log("Update LOD1 distance to:", ""+Settings.lodDistances[0]);
            Settings.dynamicLODAdjustment = false;
            scenery.update( deltaTime, camera, true );
        }


        // animate camera
//        viewAngle += deltaTime;
//        camera.position.x = cameraDistance * (float)Math.cos(viewAngle);
//        camera.position.z = cameraDistance * (float)Math.sin(viewAngle);
//        //camera.position.y = cameraDistance * (float) Math.sin((time*10f) * MathUtils.degreesToRadians);
//        camera.position.y = .3f*cameraDistance;

        camera.up.set(Vector3.Y);
        camController.update( deltaTime );

        terrain.update( camera );
        scenery.update( deltaTime, camera, !Settings.skipChecksWhenCameraStill );

        if(Settings.cascadedShadows) {
            csm.setCascades(sceneManager.camera, light, 0, 10f);
        }
        else
            light.setCenter(camera.position); // keep shadow light on player so that we have shadows

        sceneManager.getRenderableProviders().clear();

        // terrain chunks are taken directly from the Terrain class
        if(!Settings.singleInstance) {
            for (Scene scene : terrain.getScenes())
                sceneManager.addScene(scene, false);
        } else {
            sceneManager.addScene(groundPlane, false);
        }

        // add scenery
        for(Scene scene : scenery.getScenes())
            sceneManager.addScene(scene, false);

        // render
        ScreenUtils.clear(Color.SKY, true);

        sceneManager.update(deltaTime);
        sceneManager.render();

        if(Settings.lodLevel == Settings.LOD_LEVELS || Settings.lodLevel < 0 ) {      // impostors
            modelBatch.begin(camera);
            modelBatch.render(scenery.getImpostors());
            modelBatch.end();
        }

        terrainDebug.debugRender(Vector3.Zero, camera.position);
        sceneryDebug.debugRender(Vector3.Zero, camera.position);

        gui.render(deltaTime);

        if(Settings.dynamicLODAdjustment)
            adjustDetailToFrameRate(deltaTime, 60);
    }


    private int numSamples = 0;
    private float totalTime = 0;
    private float sampleTime = 1;

    // dynamic adjustment of quality settings to achieve an acceptable minimum frame rate.
    // This checks often on start up, once the minimum frame rate is achieved it will check less often just in case the frame rate gets worse.
    // Note: in case of a very high frame rate, we don't reduce the LOD levels to make it slower/better quality.
    //
    private void adjustDetailToFrameRate(float deltaTime, float targetFrameRate ){

        totalTime += deltaTime;
        numSamples++;
        if(totalTime > sampleTime){        // every few seconds check frame rate
            float frameRate =  numSamples / totalTime;
            Gdx.app.log("fps (avg)", ""+frameRate);

            if(frameRate < targetFrameRate ){
                // to improve performance, make LOD distances smaller
                for(int lod = 0; lod < Settings.LOD_LEVELS; lod++)
                    Settings.lodDistances[lod] = 0.7f * Settings.lodDistances[lod];

                Gdx.app.log("Frame rate too low, increasing LOD1 distance to:", ""+Settings.lodDistances[0]);
            }
            else {
                if(sampleTime < 10)
                    Gdx.app.log("Target frame rate achieved", "(min "+targetFrameRate+")");
                sampleTime = 10f;   // recheck every so often, but with lower frequency
            }
            numSamples = 0;
            totalTime = 0;
        }

    }




    @Override
    public void resize(int width, int height) {
        sceneManager.updateViewport(width, height);
        gui.resize(width, height);
    }


    @Override
    public void hide () {

        Gdx.input.setCursorCatched(false);
        dispose();
    }

    @Override
    public void dispose() {
        sceneManager.dispose();
        environmentCubemap.dispose();
        diffuseCubemap.dispose();
        specularCubemap.dispose();
        brdfLUT.dispose();
        skybox.dispose();
        gui.dispose();
        terrain.dispose();
        terrainDebug.dispose();
        scenery.dispose();
        sceneAsset.dispose();
    }

}

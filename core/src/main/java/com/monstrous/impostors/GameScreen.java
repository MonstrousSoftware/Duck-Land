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
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;


public class GameScreen extends ScreenAdapter {
    private static final int SHADOW_MAP_SIZE = 4096;

    private SceneManager sceneManager;
    private SceneAsset sceneAsset;
    private Scene groundScene;
    private PerspectiveCamera camera;
    private Cubemap diffuseCubemap;
    private Cubemap environmentCubemap;
    private Cubemap specularCubemap;
    private Texture brdfLUT;
    private float time;
    private SceneSkybox skybox;
    private DirectionalLightEx light;
    private CameraInputController camController;
    private float cameraDistance;
    private Scene[] lodScenes;
    private GUI gui;
    private ImpostorBuilder builder;
    private TextureRegion[] textureRegions;
    private Texture impostorTexture;
    private Array<ModelInstance> instances;
    private ModelInstance instance;
    private SpriteBatch batch;

    private ModelBatch modelBatch;
    Vector2 regionSize = new Vector2();


    @Override
    public void show() {



        gui = new GUI();

        lodScenes = new Scene[Settings.LOD_LEVELS];

        // create scene
        sceneManager = new SceneManager();

        for(int lod = 0; lod < Settings.LOD_LEVELS;lod++) {
            sceneAsset = new GLBLoader().load(Gdx.files.internal("models/birch-lod" + lod + ".glb"));
            lodScenes[lod] = new Scene(sceneAsset.scene);
        }

        sceneAsset = new GLBLoader().load(Gdx.files.internal("models/groundPlane.glb"));
        groundScene = new Scene(sceneAsset.scene);
        sceneManager.addScene(groundScene);


        // setup camera
        camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cameraDistance = 40f;
        camera.near = 0.01f;
        camera.far = 400f;
        camera.position.setFromSpherical(MathUtils.PI/4, .4f).scl(cameraDistance);
		camera.up.set(Vector3.Y);
		camera.lookAt(Vector3.Zero);
		camera.update();
        sceneManager.setCamera(camera);

        // input multiplexer to input to GUI and to cam controller
        InputMultiplexer im = new InputMultiplexer();
        Gdx.input.setInputProcessor(im);
        camController = new CameraInputController(camera);
        im.addProcessor(camController);

        sceneManager.environment.set(new PBRFloatAttribute(PBRFloatAttribute.ShadowBias, 0.001f));

        // setup light

        light = new DirectionalShadowLight(SHADOW_MAP_SIZE, SHADOW_MAP_SIZE).setViewport(25,25,5,40);

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

        sceneManager.setAmbientLight(0.1f);
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


        textureRegions = new TextureRegion[ImpostorBuilder.NUM_ANGLES];
        int width = textureSize / ImpostorBuilder.NUM_ANGLES;
        int x = 0;
        for(int angle = 0; angle < ImpostorBuilder.NUM_ANGLES; angle++) {

            textureRegions[angle] = new TextureRegion(impostorTexture, x, 0, (int)width, (int)regionSize.y);
            textureRegions[angle].flip(false, true);        // note the texture is upside down, so flip the texture region
            x+= width;
        }

        Model model = Impostor.createImposterModel(textureRegions[0], lodScenes[0].modelInstance );

        instances = new Array<>();

        instance = new ModelInstance(model, 0, 0, 0);
        instances.add( instance );

        modelBatch = new ModelBatch();
        batch = new SpriteBatch();
    }


    private Vector3 pos = new Vector3();
    private Vector3 forward = new Vector3();
    private Vector3 right = new Vector3();
    private Vector3 up = new Vector3();
    private Quaternion q = new Quaternion();
    float viewAngle = 0;


   // @Override
    public void render(float deltaTime) {
        time += deltaTime;

        if(Gdx.input.isKeyJustPressed(Input.Keys.TAB))
            Settings.lodLevel = (Settings.lodLevel + 1 ) % (Settings.LOD_LEVELS+1);
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_0))
            Settings.lodLevel = 0;
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_1))
            Settings.lodLevel = 1;
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_2))
            Settings.lodLevel = 2;

        // animate camera
        viewAngle += deltaTime;
        camera.position.x = cameraDistance * (float)Math.cos(viewAngle);
        camera.position.z = cameraDistance * (float)Math.sin(viewAngle);
        camera.position.y = .3f*cameraDistance;
		//camera.position.setFromSpherical( MathUtils.PI/4,  time*.3f).scl(cameraDistance);
		camera.up.set(Vector3.Y);
		camera.lookAt(Vector3.Zero);
		camera.update();
//        camController.update();



        sceneManager.getRenderableProviders().clear();
        sceneManager.addScene(groundScene);
        if(Settings.lodLevel < Settings.LOD_LEVELS)
            sceneManager.addScene( lodScenes[Settings.lodLevel] );


        // render
        ScreenUtils.clear(Color.SKY, true);
        sceneManager.update(deltaTime);
        sceneManager.render();

        // get billboard to face the camera
        instance.transform.getTranslation(pos);         // get instance position
        forward.set(camera.position).sub(pos).nor();        // vector towards camera
        right.set(camera.up).crs(forward).nor();
        up.set(forward).crs(right).nor();
        q.setFromAxes(right.x, up.x, forward.x, right.y, up.y, forward.y, right.z, up.z, forward.z);
        instance.transform.set(q).setTranslation(pos);

        float angle = (float)Math.atan2(forward.z, forward.x);
        int index = (int)(angle / ((float)Math.PI/4f));

        if(Settings.lodLevel == Settings.LOD_LEVELS) {
            modelBatch.begin(camera);
            modelBatch.render(instances);
            modelBatch.end();
        }

//        decal.lookAt(camera.position, camera.up);
//        decalBatch.add(decal);
//        decalBatch.flush();

        batch.begin();
        batch.draw(textureRegions[0], 0, 0, regionSize.x/4, regionSize.y/4);//, 50, 150);
        batch.end();

        gui.render(deltaTime);
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
    }

}

package com.monstrous.impostors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
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


// to create texture images from a model for use as impostor


public class ImpostorBuilder {
    private static final int SHADOW_MAP_SIZE = 1024;
    private static final String debugFilePath = "tmp/lodtest";

    private int width, height;
    private FrameBuffer fbo;
    private PerspectiveCamera camera;
    private SceneManager sceneManager;
    private Cubemap diffuseCubemap;
    private Cubemap environmentCubemap;
    private Cubemap specularCubemap;
    private Texture brdfLUT;
    private SceneSkybox skybox;
    private DirectionalLightEx light;
    private float cameraDistance;

    public ImpostorBuilder(int width, int height) {
        this.width = width;
        this.height = height;

        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, true);

        // create scene
        sceneManager = new SceneManager();

        // setup camera
        camera = new PerspectiveCamera(60f, width, height);
        cameraDistance = 40f;
        camera.near = 0.01f;
        camera.far = 400f;
        camera.position.setFromSpherical(0, .0f).scl(cameraDistance);
        camera.up.set(Vector3.Y);
        camera.lookAt(Vector3.Zero);
        camera.update();
        sceneManager.setCamera(camera);

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

        sceneManager.setAmbientLight(1.0f);
        sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
        sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap));
        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap));

        // setup skybox
        skybox = new SceneSkybox(environmentCubemap);
        sceneManager.setSkyBox(skybox);
    }

    public void createImpostor(Scene model){
        ModelBatch modelBatch = new ModelBatch();

       // sceneManager.getRenderableProviders().clear();

        SceneAsset sceneAsset = new GLBLoader().load(Gdx.files.internal("models/groundPlane.glb"));
        Scene groundScene = new Scene(sceneAsset.scene);
        sceneManager.addScene(groundScene);

        sceneManager.addScene(model);

        sceneManager.renderShadows();

        ScreenUtils.clear(new Color(0,0,0,0), true);

        // note: using scenemanager doesn't result in a framebuffer we can capture with colours

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        modelBatch.begin(camera);
        modelBatch.render(model.modelInstance);
        modelBatch.end();

        // note we cannot write an fbo to file, we use the screen buffer for this
        Pixmap fboPixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        if (debugFilePath != null) {
            PixmapIO.writePNG(Gdx.files.external(debugFilePath).child("fbo2.png"), fboPixmap,0,true);
        }
        fboPixmap.dispose();
    }

    public void dispose() {
        sceneManager.dispose();
        environmentCubemap.dispose();
        diffuseCubemap.dispose();
        specularCubemap.dispose();
        brdfLUT.dispose();
        skybox.dispose();
        fbo.dispose();
    }
}

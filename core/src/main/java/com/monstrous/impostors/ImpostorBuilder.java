package com.monstrous.impostors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
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


// name could be improved
// to create texture images from a model for use as impostor


public class ImpostorBuilder {
    private static final int SHADOW_MAP_SIZE = 1024;
    private static final String debugFilePath = null; // = "tmp/lodtest";

    private PerspectiveCamera camera;
    private SceneManager sceneManager;
    private Cubemap diffuseCubemap;
    private Cubemap environmentCubemap;
    private Cubemap specularCubemap;
    private Texture brdfLUT;
    private DirectionalLightEx light;
    private float cameraDistance;


    public ImpostorBuilder() {

        // create scene
        sceneManager = new SceneManager();

        // setup camera
        camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
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
        // keep similar to game lighting

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
    }

    public Texture createImpostor(Scene model){

        sceneManager.getRenderableProviders().clear();
        sceneManager.addScene(model);

        sceneManager.update(0.1f);  // important for rendering

        // clear with alpha zero to give transparent background
        ScreenUtils.clear(Color.CLEAR, true);

        sceneManager.render();

        // we can't create pixmap from an fbo, only from the screen buffer
        Pixmap fboPixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        Gdx.app.log("pixmap format:", fboPixmap.getFormat().toString());

        if (debugFilePath != null) {
            PixmapIO.writePNG(Gdx.files.external(debugFilePath).child("fbo.png"), fboPixmap,0,false);
        }


        // get bounding box for model
        BoundingBox bbox = new BoundingBox();
        model.modelInstance.calculateBoundingBox(bbox);
        Vector3 v1 = new Vector3();
        v1.x = bbox.min.x;
        v1.y = bbox.min.y;
        v1.z = bbox.getCenterZ();       // assumes we are looking from front
        Vector3 v2 = new Vector3();
        v2.x = bbox.max.x;
        v2.y = bbox.max.y;
        v2.z = bbox.getCenterZ();
        Gdx.app.log("v1", v1.toString());
        Gdx.app.log("v2", v2.toString());

        // project corners to screen coordinates
        camera.project(v1);
        camera.project(v2);

        Gdx.app.log("v1", v1.toString());
        Gdx.app.log("v2", v2.toString());

        // clip the desired rectangle to a pixmap
        Pixmap clippedPixmap = Pixmap.createFromFrameBuffer((int) v1.x, (int) v1.y, (int) (1 + v2.x - v1.x), (int) (1+v2.y - v1.y));
        if (debugFilePath != null) {
            PixmapIO.writePNG(Gdx.files.external(debugFilePath).child("clipped.png"), clippedPixmap,0,true);
        }


        Texture texture = new Texture(clippedPixmap, true);


        fboPixmap.dispose();
        clippedPixmap.dispose();
        return texture;
    }

    public void dispose() {
        sceneManager.dispose();
        environmentCubemap.dispose();
        diffuseCubemap.dispose();
        specularCubemap.dispose();
        brdfLUT.dispose();
    }
}

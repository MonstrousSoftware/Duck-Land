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
import com.badlogic.gdx.math.Vector2;
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
    public static final int NUM_ANGLES = 8;
    private static final int SHADOW_MAP_SIZE = 2048;
    private static final String debugFilePath = "tmp/lodtest";

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
        cameraDistance = 100f;  // will be modified later
        camera.near = 0.01f;
        camera.far = 400f;
        camera.position.x = 0;
        camera.position.z = cameraDistance;
        camera.position.y = 0;
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

    private float setOptimalCameraDistance(Camera cam, float modelWidth, float pixelWidth){

        float centre =  Gdx.graphics.getWidth()/2;
        Vector3 yardStick = new Vector3( modelWidth,0, 0);
        Vector3 projected = new Vector3();
        cam.position.set(0, 0, cameraDistance);
        cam.near = 0f;
        cam.far = cameraDistance*10f;
        cam.up.set(Vector3.Y);
        cam.lookAt(Vector3.Zero);
        cam.update();
        projected.set(yardStick);
        cam.project(projected);

        float ratio = (projected.x - centre) / pixelWidth;
        Gdx.app.log("projected", "" + modelWidth + " to " + (projected.x -centre) + " target: " + pixelWidth + "ratio: " + ratio + " distance: " + cameraDistance);
        cameraDistance *= ratio;

        cameraDistance *= 1.1f;   // and add a bit of margin to avoid clipping off extremities
        cam.position.set(0, 0, cameraDistance);
        cam.near = 0.1f*cameraDistance;
        cam.far = cameraDistance*10f;
        cam.up.set(Vector3.Y);
        cam.lookAt(Vector3.Zero);
        cam.update();

        return cameraDistance;
    }


    public Texture createImpostor(Scene model, int textureSize, Vector2 regionSize){


        Pixmap atlasPixmap = new Pixmap(textureSize, textureSize, Pixmap.Format.RGBA8888);

        sceneManager.getRenderableProviders().clear();
        sceneManager.addScene(model);

        // get bounding box for model
        // note: we reuse the same dimensions for all angles assuming the model is somewhat cylindrical
        BoundingBox bbox = new BoundingBox();
        model.modelInstance.calculateBoundingBox(bbox);

        cameraDistance = setOptimalCameraDistance(camera, bbox.getWidth(), textureSize/NUM_ANGLES);

        Vector3 v1 = new Vector3(bbox.min.x, bbox.min.y, bbox.min.z);
        Vector3 v2 = new Vector3(bbox.max.x, bbox.max.y, bbox.max.z);
        camera.project(v1);
        camera.project(v2);

        if(v2.x < 0 || v2.y < 0) throw new RuntimeException("model goes off screen");

        int clipWidth = (int) (1 +v2.x - v1.x);
        int clipHeight = (int)(1+v2.y-v1.y);

        int texWidth = textureSize/NUM_ANGLES;
        int texHeight = texWidth * clipHeight/clipWidth;  // keep aspect ratio
        regionSize.set(texWidth, texHeight);        // export region size to caller

        for(int angle = 0; angle < NUM_ANGLES; angle++) {

            float viewAngle = (float)angle * (float)Math.PI * 2f / NUM_ANGLES;

            camera.position.x = cameraDistance * (float)Math.sin(-viewAngle);
            camera.position.z = cameraDistance * (float)Math.cos(viewAngle);
            camera.position.y = bbox.getCenterY();
            Gdx.app.log("angle", ""+angle+" "+viewAngle+" x:"+camera.position.x+" v1.x: "+v1.x+" v2.x: "+v2.x);

            //camera.position.setFromSpherical(angle * (float)Math.PI*2f/(float)NUM_ANGLES, .0f).scl(cameraDistance);
            camera.up.set(Vector3.Y);
            camera.lookAt(Vector3.Zero);
            camera.update();

            sceneManager.update(0.1f);  // important for rendering

            // clear with alpha zero to give transparent background
            ScreenUtils.clear(Color.CLEAR, true);

            sceneManager.render();

            // we can't create pixmap from an fbo, only from the screen buffer
            // or we could work with Texture instead of Pixmap (but Pixmap allows us to write export debug images to file)
            Pixmap fboPixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

            if (debugFilePath != null) {
                PixmapIO.writePNG(Gdx.files.external(debugFilePath).child("fbo.png"), fboPixmap, 0, false);
            }


            // clip the desired rectangle to a pixmap
            Pixmap clippedPixmap = Pixmap.createFromFrameBuffer((int) v1.x, (int) v1.y, clipWidth, clipHeight);
            if (debugFilePath != null) {
                PixmapIO.writePNG(Gdx.files.external(debugFilePath).child("clipped"+angle+".png"), clippedPixmap, 0, true);
            }

            // add this clipped image to the atlas which contains screenshots from different angles
            // rotation around Y is shown as 8 images left to right
            // (we spread horizontally rather than vertically because for a high model like a tree we should get better resolution per decal for the common case of a side view)
            //

            int offsetX = angle * texWidth;
            int offsetY = 0;

            Gdx.app.log("stretch", "from: "+clippedPixmap.getWidth()+" to: "+texWidth);

            // beware: we are stretching here. we should move the camera to get the desired width
            atlasPixmap.setFilter(Pixmap.Filter.BiLinear);
            atlasPixmap.drawPixmap(clippedPixmap, 0,0, clippedPixmap.getWidth(), clippedPixmap.getHeight(), offsetX, offsetY, texWidth, texHeight);

            fboPixmap.dispose();
            clippedPixmap.dispose();
        }

        if (debugFilePath != null) {
            PixmapIO.writePNG(Gdx.files.external(debugFilePath).child("atlas.png"), atlasPixmap, 0, false);
        }

        Texture texture = new Texture(atlasPixmap, true);
        atlasPixmap.dispose();
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

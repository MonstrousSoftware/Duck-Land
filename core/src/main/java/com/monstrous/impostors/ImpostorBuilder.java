package com.monstrous.impostors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.ScreenUtils;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;


// name could be improved
// to create texture images from a model for use as impostor


public class ImpostorBuilder {
    public static final int NUM_ANGLES = 16;         // should be power of two to divide texture width evenly
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

        sceneManager.setAmbientLight(0.3f);
        sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
        sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap));
        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap));
    }

    // note: we determine the ideal camera distance to fill the desired width in pixels
    // we do this for the front view only and assume other views are not wider than this!
    //
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
//        Gdx.app.log("projected", "" + modelWidth + " to " + (projected.x -centre) + " target: " + pixelWidth + "ratio: " + ratio + " distance: " + cameraDistance);
        cameraDistance *= ratio;

        //cameraDistance *= 1.1f;   // and add a bit of margin to avoid clipping off extremities
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

        int texWidth = textureSize/NUM_ANGLES;

        cameraDistance = setOptimalCameraDistance(camera, bbox.getWidth(), texWidth);

        Vector3 v1 = new Vector3(bbox.min.x, bbox.min.y, bbox.min.z);
        Vector3 v2 = new Vector3(bbox.max.x, bbox.max.y, bbox.max.z);
        camera.project(v1);
        camera.project(v2);

        if(v2.x < 0 || v2.y < 0) throw new RuntimeException("model goes off screen");

        int clipWidth =  (int)(1 + v2.x - v1.x);
        int clipHeight = (int)(1 + v2.y - v1.y);

//        v1.x -= clipWidth*0.2f;
//        v1.y -= clipHeight*0.1f;
//        clipWidth *= 1.4;
//        clipHeight *= 1.2;


        int texHeight = clipHeight; //texWidth * clipHeight/clipWidth;  // keep aspect ratio
        //int texHeight = (int)(textureSize /4f);
        regionSize.set(texWidth, texHeight);        // export region size to caller
        Gdx.app.log("decal size", "width: " +texWidth + " height: " + texHeight);

        int elevations = textureSize / texHeight;
        float elevationStep = 90f/elevations;   // degrees per elevation step

        Rectangle rect = new Rectangle();

        for(int elevation = 0; elevation < elevations; elevation++) {

            float elevationAngle =  elevation * elevationStep;

            for (int angle = 0; angle < NUM_ANGLES; angle++) {

                float viewAngle = (float) angle * (float) Math.PI * 2f / NUM_ANGLES;

                float alpha = elevationAngle * MathUtils.degreesToRadians;

                camera.position.x = cameraDistance * (float) (Math.sin(-viewAngle)*Math.cos(alpha));
                camera.position.z = cameraDistance * (float) (Math.cos(viewAngle)*Math.cos(alpha));
                camera.position.y = cameraDistance * (float) Math.sin(alpha);
                //Gdx.app.log("angle", "" + angle + " " + viewAngle + " x:" + camera.position.x + " v1.x: " + v1.x + " v2.x: " + v2.x);

                camera.up.set(Vector3.Y);
                camera.lookAt(Vector3.Zero);
                camera.update();


                //findScreenExtents(bbox, rect);


                sceneManager.update(0.1f);  // important for rendering

                // clear with alpha zero to give transparent background
                if(Settings.decalsDebug)
                    ScreenUtils.clear(new Color(MathUtils.random(0,1.f),MathUtils.random(0,1.f),MathUtils.random(0,1.f), 1.0f), true);  // debug, give background random colour to show decals in action
                else
                    ScreenUtils.clear(Color.CLEAR, true);
                sceneManager.render();

                // we can't create pixmap from an fbo, only from the screen buffer
                // or we could work with Texture instead of Pixmap (but Pixmap allows us to write export debug images to file)
//                Pixmap fboPixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

//                if (debugFilePath != null) {
//                    PixmapIO.writePNG(Gdx.files.external(debugFilePath).child("fbo.png"), fboPixmap, 0, false);
//                }


                // clip the desired rectangle to a pixmap
                Pixmap clippedPixmap = Pixmap.createFromFrameBuffer((int) v1.x, (int) v1.y, clipWidth, clipHeight);

                //Pixmap clippedPixmap = Pixmap.createFromFrameBuffer((int)rect.x, (int)rect.y, (int)rect.width, (int)rect.height);
//                if (debugFilePath != null) {
//                    PixmapIO.writePNG(Gdx.files.external(debugFilePath).child("clipped" + elevation+"x"+angle + ".png"), clippedPixmap, 0, true);
//                }

                // add this clipped image to the atlas which contains screenshots from different angles
                // rotation around Y is shown as 8 images left to right
                // (we spread horizontally rather than vertically because for a high model like a tree we should get better resolution per decal for the common case of a side view)
                //

                int offsetX = angle * texWidth;
                int offsetY = elevation * texHeight;

                //Gdx.app.log("stretch", "from: " + clippedPixmap.getWidth() + " to: " + texWidth);

                // beware: we are stretching here. we should move the camera to get the desired width
                atlasPixmap.setFilter(Pixmap.Filter.BiLinear);
                atlasPixmap.drawPixmap(clippedPixmap, 0, 0,texWidth, texHeight, offsetX, offsetY, texWidth, texHeight);

//                fboPixmap.dispose();
                clippedPixmap.dispose();
            }
        }

        if (debugFilePath != null) {
            PixmapIO.writePNG(Gdx.files.external(debugFilePath).child("atlas.png"), atlasPixmap, 0, false);
        }

        Texture texture = new Texture(atlasPixmap, true);
        atlasPixmap.dispose();
        return texture;
    }


    private void findScreenExtents(BoundingBox bbox, Rectangle rect) {

        Vector3[] corners = new Vector3[8];
        for(int i = 0; i < 8; i++)
            corners[i] = new Vector3();
        bbox.getCorner000(corners[0]);
        bbox.getCorner001(corners[1]);
        bbox.getCorner010(corners[2]);
        bbox.getCorner011(corners[3]);
        bbox.getCorner100(corners[4]);
        bbox.getCorner101(corners[5]);
        bbox.getCorner110(corners[6]);
        bbox.getCorner111(corners[7]);

        Vector2 min = new Vector2(999, 999);
        Vector2 max = new Vector2(-999, -999);

        for(int i = 0; i < 8; i++){
            Vector3 v = corners[i];
            camera.project(v);
            if(v.x < min.x)
                min.x = v.x;
            if(v.x > max.x)
                max.x = v.x;
            if(v.y < min.y)
                min.y = v.y;
            if(v.y > max.y)
                max.y = v.y;
        }
        rect.x = min.x;
        rect.y = min.y;
        rect.width = 1 + max.x - min.x;
        rect.height = 1 + max.y - min.y;

        Gdx.app.log("rect", "rect:" + rect.toString());

    }

    public void dispose() {
        sceneManager.dispose();
        environmentCubemap.dispose();
        diffuseCubemap.dispose();
        specularCubemap.dispose();
        brdfLUT.dispose();
    }
}

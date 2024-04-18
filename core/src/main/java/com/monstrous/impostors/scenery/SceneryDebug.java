package com.monstrous.impostors.scenery;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector3;
import com.monstrous.impostors.Settings;
import com.monstrous.impostors.terrain.Terrain;
import com.monstrous.impostors.terrain.TerrainChunk;


// Helper class to visualize chunk creation/disposal as debug overlay
//

public class SceneryDebug {
    private static int SIZE = 10; // pixels per chunk

    private Scenery scenery;

    private SpriteBatch batch;
    private Texture texture;
    private TextureRegion textureRegionChunk0;
    private TextureRegion textureRegionChunk1;
    private TextureRegion textureRegionChunk2;
    private TextureRegion textureRegionChunk3;
    private TextureRegion textureRegionChunkNotVisible;
    private TextureRegion textureRegionCam;

    public SceneryDebug( Scenery scenery ) {
        this.scenery = scenery;

        batch = new SpriteBatch();

        // use a pixmap to create different solid colour texture regions (1 pixel each)
        Pixmap pixmap = new Pixmap(6, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 1, 0f, 0.8f);
        pixmap.drawPixel(0,0);
        pixmap.setColor(0.2f, 0.7f, 0.3f, 0.8f);
        pixmap.drawPixel(1,0);
        pixmap.setColor(0.4f, 0.7f, 0.6f, 0.8f);
        pixmap.drawPixel(2,0);
        pixmap.setColor(0.1f, 0.4f, 1, 0.5f);
        pixmap.drawPixel(3,0);
        pixmap.setColor(0.5f,0.5f, 0.5f, 0.2f);
        pixmap.drawPixel(4,0);
        pixmap.setColor(0.7f,0.1f, 0.1f, 0.3f);
        pixmap.drawPixel(5,0);

        texture = new Texture(pixmap);
        textureRegionChunk0 = new TextureRegion(texture, 0,0,1,1);
        textureRegionChunk1 = new TextureRegion(texture, 1,0,1,1);
        textureRegionChunk2 = new TextureRegion(texture, 2,0,1,1);
        textureRegionChunk3 = new TextureRegion(texture, 3,0,1,1);
        textureRegionChunkNotVisible = new TextureRegion(texture, 4,0,1,1);
        textureRegionCam = new TextureRegion(texture, 5,0,1,1);
    }


    private Vector3 pos = new Vector3();

    public void debugRender(Vector3 playerPos, Vector3 camPos) {
        if(!Settings.debugSceneryChunkAllocation)
            return;

        int size = SIZE;  // pixels per chunk
        batch.begin();
        for(SceneryChunk chunk : scenery.chunks.values() ) {
            pos.set(chunk.getWorldPosition());
            pos.x /=SceneryChunk.CHUNK_SIZE;
            pos.z /=SceneryChunk.CHUNK_SIZE;
            convert(pos);
            if(chunk.lastSeen != scenery.lastCameraChange)
                batch.draw(textureRegionChunkNotVisible, pos.x, pos.y-SIZE, size-2, size-2);
            else switch(chunk.getLodLevel()) {
                case 0:              batch.draw(textureRegionChunk0, pos.x, pos.y - SIZE, size - 2, size - 2); break;
                case 1:              batch.draw(textureRegionChunk1, pos.x, pos.y - SIZE, size - 2, size - 2); break;
                case 2:              batch.draw(textureRegionChunk2, pos.x, pos.y - SIZE, size - 2, size - 2); break;
                case 3:              batch.draw(textureRegionChunk3, pos.x, pos.y - SIZE, size - 2, size - 2); break;
            }
        }


        // camera
        pos.set(camPos);
        pos.x /=SceneryChunk.CHUNK_SIZE;
        pos.z /=SceneryChunk.CHUNK_SIZE;
        convert(pos);
        batch.draw(textureRegionCam, pos.x, pos.y, 4, 4);

        batch.end();
    }

    // convert x,z from chunk units to screen pixels x,y
    private void convert(Vector3 pos){
        pos.x  = 400 + SIZE*pos.x;
        pos.y  = 400 - SIZE*pos.z;
    }


    public void dispose() {
        batch.dispose();
        texture.dispose();
    }
}

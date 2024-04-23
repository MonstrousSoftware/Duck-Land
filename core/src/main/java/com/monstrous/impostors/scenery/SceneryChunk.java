package com.monstrous.impostors.scenery;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.monstrous.impostors.PoissonDiskDistribution;
import com.monstrous.impostors.terrain.Terrain;

public class SceneryChunk implements Disposable {
    public static final float CHUNK_SIZE = 128;            // in world units


    private Array<Vector4> instancePositions;
    private Vector3 chunkPosition;              // world position of chunk centre
    public BoundingBox bbox;
    private int lodLevel;
    public int lastSeen;
    public int creationTime;
    public int key;
    public float distance;

    public SceneryChunk(int cx, int cz, int creationTime, int key, Terrain terrain, float separationDistance) {
        this.creationTime = creationTime;
        this.key = key;
        float x = cx*CHUNK_SIZE+CHUNK_SIZE/2;
        float z = cz*CHUNK_SIZE+CHUNK_SIZE/2;
        float h = terrain.getHeight(x, z);
        chunkPosition = new Vector3(x, h, z);   // world position in centre of chunk at terrain height
        bbox = new BoundingBox();
        bbox.set(new Vector3(x-CHUNK_SIZE/2, h-10, z-CHUNK_SIZE/2), new Vector3(x+CHUNK_SIZE/2, h+10, z+CHUNK_SIZE/2));

        // generate a random poisson distribution of instances over a rectangular area, meaning instances are never too close together
        MathUtils.random.setSeed(cx * 345 + cz * 56700);         // fix the random distribution to always be identical per chunk
        Rectangle area = new Rectangle(1, 1, CHUNK_SIZE, CHUNK_SIZE);
        Array<Vector2> points = PoissonDiskDistribution.generatePoissonDistribution(separationDistance, area);

        //instanceCount = points.size;

        // convert 2d points to 3d positions

        instancePositions = new Array<>();
        MathUtils.random.setSeed(cx * 345 + cz * 56700);         // fix the random distribution to always be identical
        for(Vector2 point : points ) {
            x = point.x + chunkPosition.x-CHUNK_SIZE/2;
            z = point.y + chunkPosition.z-CHUNK_SIZE/2;
            h = terrain.getHeight(x, z);
            if(h == 0)
                Gdx.app.log("height is 0", "x= "+x+" z= "+z);
            float angleY = MathUtils.random(0.0f, (float)Math.PI*2.0f);      // random rotation around Y (up) axis
            Vector4 position = new Vector4( x, h, z, angleY);               // world position, not chunk relative position
            instancePositions.add( position );
        }
    }

    public Vector3 getWorldPosition() {
        return chunkPosition;
    }

    public Array<Vector4> getPositions(){
        return instancePositions;
    }

    public int getLodLevel() {
        return lodLevel;
    }

    public void setLodLevel(int lodLevel) {
        this.lodLevel = lodLevel;
    }

    @Override
    public void dispose() {
        instancePositions.clear();
    }
}

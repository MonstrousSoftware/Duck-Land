package com.monstrous.impostors;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;



// Similar to standard LibGDX Decal
// does it compete for performance?

public class Impostor {



    // create an Impostor model, i.e. a quad of the right size with the texture region on it.
    //
    public static Model createImposterModel(TextureRegion textureRegion, ModelInstance instance){

        //we need to know the world size of the actual model
        BoundingBox boundingBox = new BoundingBox();
        instance.calculateBoundingBox(boundingBox);
        float halfWidth =  0.5f*boundingBox.getWidth();
        float height = boundingBox.getHeight();



        // use the impostor texture as material and use alpha blending
        Material material = new Material(
            TextureAttribute.createDiffuse(textureRegion.getTexture()),
            new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        );

        // Build a Model
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        
        MeshPartBuilder meshBuilder;
        meshBuilder = modelBuilder.part("impostor",GL20.GL_TRIANGLES,
            VertexAttributes.Usage.Position| VertexAttributes.Usage.TextureCoordinates,
            material);

        // set info for 4 corners of a quad
        VertexInfo[] vertexInfo = new VertexInfo[4];
        for(int i = 0; i < 4; i++)
            vertexInfo[i] = new VertexInfo();

        // we only set position and uv, no colour or normal vector
        // set (Vector3 pos, Vector3 nor, Color col, Vector2 uv)
        // note the v values are flipped to avoid an upside down texture
        vertexInfo[0].set( new Vector3(-halfWidth, 0, 0), null, null, new Vector2(textureRegion.getU(),textureRegion.getV2()) );
        vertexInfo[1].set( new Vector3(halfWidth, 0, 0), null, null, new Vector2(textureRegion.getU2(),textureRegion.getV2()) );
        vertexInfo[2].set( new Vector3(halfWidth, height, 0), null, null, new Vector2(textureRegion.getU2(),textureRegion.getV()) );
        vertexInfo[3].set( new Vector3(-halfWidth, height, 0), null, null, new Vector2(textureRegion.getU(),textureRegion.getV()) );

        // order: 00, 10, 11, 01
        meshBuilder.rect(vertexInfo[0], vertexInfo[1], vertexInfo[2], vertexInfo[3]);

        Model model = modelBuilder.end(); // makes the model
        return model;
    }
}

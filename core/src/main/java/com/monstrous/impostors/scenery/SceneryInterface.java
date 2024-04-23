package com.monstrous.impostors.scenery;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.utils.Array;
import net.mgsx.gltf.scene3d.scene.Scene;

public interface SceneryInterface {


    void update(float deltaTime, PerspectiveCamera cam, boolean forceUpdate);

    // need to be rendered with the instanced PBR shader
    Array<Scene> getScenes();

    // need to be rendered with the instanced decal shader
    Array<ModelInstance> getImpostors();

}

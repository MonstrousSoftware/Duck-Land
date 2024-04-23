package com.monstrous.impostors.scenery;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.utils.Array;
import net.mgsx.gltf.scene3d.scene.Scene;

public interface SceneryInterface {


    public void update(PerspectiveCamera cam, boolean forceUpdate);

    public Array<Scene> getScenes();

    // need to be rendered with the instanced decal shader
    public Array<ModelInstance> getImpostors();

}

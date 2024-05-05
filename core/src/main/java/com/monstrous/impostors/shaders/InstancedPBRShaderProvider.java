package com.monstrous.impostors.shaders;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Renderable;
import net.mgsx.gltf.scene3d.shaders.PBRShader;
import net.mgsx.gltf.scene3d.shaders.PBRShaderConfig;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;

public class InstancedPBRShaderProvider extends PBRShaderProvider {

    public InstancedPBRShaderProvider() {
        super(PBRShaderProvider.createDefaultConfig());
    }

    @Override
    protected PBRShader createShader(Renderable renderable, PBRShaderConfig config, String prefix){
        if( renderable.meshPart.mesh.isInstanced()) {
            prefix += "#define instanced\n";
        }
        config.vertexShader = Gdx.files.internal("shaders/pbr/pbr.vs.glsl").readString();
        return new InstancedPBRShader(renderable, config, prefix);
    }

}

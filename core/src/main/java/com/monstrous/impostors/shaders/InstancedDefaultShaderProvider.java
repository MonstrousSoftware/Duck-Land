package com.monstrous.impostors.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;

import static com.badlogic.gdx.graphics.g3d.shaders.DefaultShader.createPrefix;

public class InstancedDefaultShaderProvider extends DefaultShaderProvider {
    public DefaultShader.Config config;


    public InstancedDefaultShaderProvider(DefaultShader.Config config) {
        this.config = config == null ? new DefaultShader.Config() : config;
    }

    public InstancedDefaultShaderProvider() {
        this(null);
    }


    @Override
    protected DefaultShader createShader(Renderable renderable){

        config.vertexShader = Gdx.files.internal("shaders/default.vs.glsl").readString();

        String prefix = createPrefix(renderable,  config);
        if( renderable.meshPart.mesh.isInstanced()) {
            prefix += "#define instanced\n";
        }
        return new DefaultShader(renderable, this.config, prefix);
    }

}

package com.monstrous.impostors.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import net.mgsx.gltf.scene3d.shaders.PBRDepthShader;

public class InstancedPBRDepthShader extends PBRDepthShader {

    private boolean isInstancedShader;

    public InstancedPBRDepthShader(Renderable renderable, Config config, String prefix) {
        super(renderable, config, prefix);
        isInstancedShader = renderable.meshPart.mesh.isInstanced();
    }

    @Override
    public void begin(Camera camera, RenderContext context) {
        super.begin(camera, context);

        program.bind();
        program.getUniformLocation("ssbo");

        int ssboIndex = Gdx.gl30.glGetUniformBlockIndex(program.getHandle(),"ssbo");
        Gdx.gl30.glUniformBlockBinding(program.getHandle(), ssboIndex, 0);    // binding
    }

    @Override
    public boolean canRender(Renderable renderable) {
        if(renderable.meshPart.mesh.isInstanced() != isInstancedShader ) {
            return false;
        }
        return super.canRender(renderable);
    }
}

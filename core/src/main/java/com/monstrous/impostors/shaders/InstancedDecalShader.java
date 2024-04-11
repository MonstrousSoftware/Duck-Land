package com.monstrous.impostors.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class InstancedDecalShader extends BaseShader {

    //    Uniforms:
    //    uniform sampler2D u_texture;
    //    uniform mat4 u_projViewTrans;
    //
    //    Instance data:
    //    in mat4 i_worldTrans;

    @Override
    public void begin(Camera camera, RenderContext context) {
        program.bind();
        program.setUniformMatrix("u_projTrans", camera.combined);
        program.setUniformi("u_texture", 0);
        //                context.setDepthTest(GL32.GL_LESS);
    }

    @Override
    public void init() {
        ShaderProgram.prependVertexCode = "#version 300 es\n";
        ShaderProgram.prependFragmentCode = "#version 300 es\n";

        program = new ShaderProgram(Gdx.files.internal("shaders/decalinstanced.vert"),
                                    Gdx.files.internal("shaders/decalinstanced.frag"));
        if (!program.isCompiled()) {
            throw new GdxRuntimeException("Shader compile error: " + program.getLog());
        }
//        init(program, renderable);

        ShaderProgram.prependVertexCode = null;
        ShaderProgram.prependFragmentCode = null;
    }

    @Override
    public int compareTo(Shader other) {
        return 0;
    }

    @Override
    public boolean canRender(Renderable instance) {
        return true;
    }
}

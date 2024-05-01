package com.monstrous.impostors.shaders;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.monstrous.impostors.Settings;


public class InstancedDecalShaderProvider extends DefaultShaderProvider {

    DefaultShader.Config config;

    String vertexShader;
    String fragmentShader;

    static public class UVSize{     // userData structure to hold UV information
        float u, v;

        public UVSize(float u, float v) {
            this.u = u;
            this.v = v;
        }
    }

    public InstancedDecalShaderProvider(DefaultShader.Config config) {
        super(config);
        this.config = config;
        vertexShader = Gdx.files.internal("shaders/decalinstanced.vert").readString();
        fragmentShader = Gdx.files.internal("shaders/decalinstanced.frag").readString();
    }

    public InstancedDecalShaderProvider() {
            this(null);
    }



    @Override
    protected Shader createShader (final Renderable renderable) {

        return new DefaultShader(renderable) {

            //    Uniforms:
            //    uniform sampler2D u_texture;
            //    uniform mat4 u_projViewTrans;
            //
            //    Instance data:
            //    in vec4 i_offset;
            //
            // Note: we can use the same shader for decals with different textures and uvDimensions

            @Override
            public void init() {
                ShaderProgram.prependVertexCode = "#version 300 es\n";
                ShaderProgram.prependFragmentCode = "#version 300 es\n";

                program = new ShaderProgram( vertexShader, fragmentShader);
                if (!program.isCompiled()) {
                    throw new GdxRuntimeException("Shader compile error: " + program.getLog());
                }
                init(program, renderable);
                ShaderProgram.prependVertexCode = null;
                ShaderProgram.prependFragmentCode = null;
            }

            @Override
            public void begin(Camera camera, RenderContext context) {
                this.context = context;
                super.begin(camera, context);   // to set u_cameraPosition, etc.
                Color col = Settings.fogColor;
                program.setUniformf("u_fogColor", col.r, col.g, col.b, col.a);
                program.setUniformf("u_fogEquation", Settings.fogNear, Settings.fogFar,Settings.fogBase);
            }

            @Override
            public void render(Renderable renderable, Attributes combinedAttributes) {
                if(renderable.userData == null) throw new GdxRuntimeException("Missing UVsize, renderable.userData is null");

                final int unit = context.textureBinder.bind(((TextureAttribute)(renderable.material.get(TextureAttribute.Diffuse))).textureDescription);
                program.setUniformi("u_texture", unit);

                UVSize uvDimensions = (UVSize)(renderable.userData);
                program.setUniformf("u_step", uvDimensions.u, uvDimensions.v);

                super.render(renderable, combinedAttributes);
            }

            @Override
            public int compareTo(Shader other) {
                return 0;
            }

            @Override
            public boolean canRender(Renderable instance) {
                return instance.userData != null;
            }
        };
    }

}

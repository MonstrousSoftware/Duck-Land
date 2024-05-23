package com.monstrous.impostors.shaders;

import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DirectionalLightsAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;
import com.monstrous.impostors.Settings;
import net.mgsx.gltf.scene3d.attributes.CascadeShadowMapAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight;
import net.mgsx.gltf.scene3d.shaders.PBRShader;

public class InstancedPBRShader extends PBRShader {

    private boolean isInstancedShader;

    public InstancedPBRShader(Renderable renderable, Config config, String prefix) {
        super(renderable, config, prefix);
        isInstancedShader = renderable.meshPart.mesh.isInstanced();
    }

    @Override
    public boolean canRender(Renderable renderable) {
        if(renderable.meshPart.mesh.isInstanced() != isInstancedShader ) {
            return false;
        }
        return super.canRender(renderable);
    }

    protected int[] u_csmSamplers = new int[8];            // max cascades is 8
    protected int[] u_csmPCFClip = new int[8];
    protected int[] u_csmTransforms = new int[8];
    protected int u_ambientLight;        // MS: u_ambientLight should be protected not private in parent class

    @Override
    public void init(ShaderProgram program, Renderable renderable) {
        super.init(program, renderable);
//        u_mipmapScale = program.fetchUniformLocation("u_mipmapScale", false);
//
//        u_texCoord0Transform = program.fetchUniformLocation("u_texCoord0Transform", false);
//        u_texCoord1Transform = program.fetchUniformLocation("u_texCoord1Transform", false);
//
//        u_morphTargets1 = program.fetchUniformLocation("u_morphTargets1", false);
//        u_morphTargets2 = program.fetchUniformLocation("u_morphTargets2", false);

        u_ambientLight = program.fetchUniformLocation("u_ambientLight", false);

        int numCSM = Settings.numCascades;      // TMP
        for(int i=0 ; i< numCSM ; i++) {
            u_csmSamplers[i] = program.fetchUniformLocation("u_csmSamplers["+i+"]", true);
            u_csmTransforms[i] = program.fetchUniformLocation("u_csmTransforms["+i+"]", true);
            u_csmPCFClip[i] = program.fetchUniformLocation("u_csmPCFClip["+i+"]", true);
        }

//        u_csmSamplers = program.fetchUniformLocation("u_csmSamplers", false);
//        u_csmPCFClip = program.fetchUniformLocation("u_csmPCFClip", false);
//        u_csmTransforms = program.fetchUniformLocation("u_csmTransforms", false);
    }


    @Override
    protected void bindLights(Renderable renderable, Attributes attributes) {

        // XXX update color (to apply intensity) before default binding
        DirectionalLightsAttribute dla = attributes.get(DirectionalLightsAttribute.class, DirectionalLightsAttribute.Type);
        if(dla != null){
            for(DirectionalLight light : dla.lights){
                if(light instanceof DirectionalLightEx){
                    ((DirectionalLightEx) light).updateColor();
                }
            }
        }

        //super.bindLights(renderable, attributes);

        // XXX


        ColorAttribute ambiantLight = attributes.get(ColorAttribute.class, ColorAttribute.AmbientLight);
        if(ambiantLight != null){
            program.setUniformf(u_ambientLight, ambiantLight.color.r, ambiantLight.color.g, ambiantLight.color.b);
        }

        CascadeShadowMapAttribute csmAttrib = attributes.get(CascadeShadowMapAttribute.class, CascadeShadowMapAttribute.Type);
        if(csmAttrib != null && u_csmSamplers[0] >= 0){ // ?
            Array<DirectionalShadowLight> lights = csmAttrib.cascadeShadowMap.lights;
            for(int i=0 ; i<lights.size ; i++) {
                DirectionalShadowLight light = lights.get(i);
                float mapSize = light.getDepthMap().texture.getWidth();
                float pcf = 1.f / (2 * mapSize);
                float clip = 3.f / (2 * mapSize);

                int unit = context.textureBinder.bind(light.getDepthMap());

                program.setUniformi(u_csmSamplers[i], unit);
                program.setUniformMatrix(u_csmTransforms[i], light.getProjViewTrans());
                program.setUniformf(u_csmPCFClip[i], pcf, clip);


            }
        }
    }
}

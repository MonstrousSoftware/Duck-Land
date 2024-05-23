package com.monstrous.impostors.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.monstrous.impostors.screens.GameScreen;
import com.monstrous.impostors.Settings;
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute;
import net.mgsx.gltf.scene3d.scene.CascadeShadowMap;
import text.formic.Stringf;

import static com.badlogic.gdx.Application.ApplicationType.Desktop;

// Debug window for some in-game tweaking
// Typically, these modify members of the Settings class.

public class LightSettingsWindow extends Window {

    private final Skin skin;
    private final GameScreen screen;

    public LightSettingsWindow(String title, Skin skin, GameScreen screen) {
        super(title, skin);
        this.skin = skin;
        this.screen = screen;
        rebuild();
    }

    private void rebuild() {


        final Label ALValue = new Label("", skin);
        ALValue.setText(Stringf.format("%.1f", Settings.ambientLightLevel));
        final Slider alSlider = new Slider(0.0f, 2.0f, 0.1f, false, skin);
        alSlider.setValue(Settings.ambientLightLevel);
        alSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Settings.ambientLightLevel = alSlider.getValue();
                ALValue.setText(Stringf.format("%.1f", Settings.ambientLightLevel));
                //screen.setLighting();
                screen.sceneManager.setAmbientLight(Settings.ambientLightLevel);
            }
        });
        add(new Label("ambient light:", skin)).left();add(ALValue); row();
        add(alSlider).colspan(2).width(400f); row();



        final Label DLValue = new Label("", skin);
        DLValue.setText(Stringf.format("%.1f", Settings.directionalLightLevel));
        final Slider dlSlider = new Slider(0.0f, 5.0f, 0.1f, false, skin);
        dlSlider.setValue(Settings.directionalLightLevel);
        dlSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Settings.directionalLightLevel = dlSlider.getValue();
                DLValue.setText(Stringf.format("%.1f", Settings.directionalLightLevel));
                screen.light.intensity = Settings.directionalLightLevel;
            }
        });
        add(new Label("directional light:", skin)).left();add(DLValue); row();
        add(dlSlider).colspan(2).width(400f); row();

        //if (Gdx.app.getType() == Desktop) {
            final CheckBox CSMcheckBox = new CheckBox("cascaded shadow maps", skin);
            CSMcheckBox.setChecked(Settings.cascadedShadows);
            CSMcheckBox.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    Settings.cascadedShadows = CSMcheckBox.isChecked();
                    if (Settings.cascadedShadows) {
                        if (screen.csm != null)
                            screen.csm.dispose();
                        screen.csm = new CascadeShadowMap(Settings.numCascades);
                        screen.sceneManager.setCascadeShadowMap(screen.csm);
                    } else {
                        screen.sceneManager.setCascadeShadowMap(null);
                        screen.csm.dispose();
                        screen.csm = null;
                        screen.light.setViewport(Settings.shadowViewportSize, Settings.shadowViewportSize, 0f, 300f);
                    }
                }
            });
            add(CSMcheckBox).left();
            row();

            final Label CNValue = new Label("", skin);
            CNValue.setText(Settings.numCascades);
            final Slider cnSlider = new Slider(1.0f, 6.0f, 1f, false, skin);
            cnSlider.setValue(Settings.numCascades);
            cnSlider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    Settings.numCascades = (int) cnSlider.getValue();
                    CNValue.setText(Settings.numCascades);
                    if (screen.csm != null)
                        screen.csm.dispose();
                    screen.csm = new CascadeShadowMap(Settings.numCascades);
                    screen.sceneManager.setCascadeShadowMap(screen.csm);
                }
            });
            add(new Label("number of cascades:", skin)).left();
            add(CNValue);
            row();
            add(cnSlider).colspan(2).width(400f);
            row();


            final Label CSValue = new Label("", skin);
            CSValue.setText(Stringf.format("%.1f", Settings.cascadeSplitDivisor));
            final Slider csSlider = new Slider(1.0f, 16.0f, 0.1f, false, skin);
            csSlider.setValue(Settings.cascadeSplitDivisor);
            csSlider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    Settings.cascadeSplitDivisor = csSlider.getValue();
                    CSValue.setText(Stringf.format("%.1f", Settings.cascadeSplitDivisor));
                }
            });
            add(new Label("cascade split divisor:", skin)).left();
            add(CSValue);
            row();
            add(csSlider).colspan(2).width(400f);
            row();
       //}

        final Label biasValue = new Label("", skin);
        biasValue.setText( Settings.inverseShadowBias);
        final Slider biasSlider = new Slider(1.0f, 5000.0f, 10f, false, skin);
        biasSlider.setValue(Settings.inverseShadowBias);
        biasSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Settings.inverseShadowBias = (int) biasSlider.getValue();
                biasValue.setText( Settings.inverseShadowBias);
                if(Settings.usePBRshader)
                    screen.sceneManager.environment.set(new PBRFloatAttribute(PBRFloatAttribute.ShadowBias, 1f/Settings.inverseShadowBias));

            }
        });
        add(new Label("1/shadow bias:", skin)).left();add(biasValue); row();
        add(biasSlider).colspan(2).width(400f); row();

        final Label vpValue = new Label("", skin);
        vpValue.setText( Stringf.format("%.1f", Settings.shadowViewportSize));
        final Slider vpSlider = new Slider(10f, 8000.0f, 10f, false, skin);
        vpSlider.setValue(Settings.shadowViewportSize);
        vpSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Settings.shadowViewportSize = (int) vpSlider.getValue();
                vpValue.setText( Stringf.format("%.1f", Settings.shadowViewportSize));
                screen.light.setViewport(Settings.shadowViewportSize, Settings.shadowViewportSize, 0f, 300f);
            }
        });
        add(new Label("shadow viewport size:", skin)).left();add(vpValue); row();
        add(vpSlider).colspan(2).width(400f); row();

//        final Label farValue = new Label("", skin);
//        farValue.setText((int)Settings.fogFar);
//        final Slider farSlider = new Slider(0.0f, 9000.0f, 20f, false, skin);
//        farSlider.setValue(Settings.fogFar);
//        farSlider.addListener(new ChangeListener() {
//            @Override
//            public void changed(ChangeEvent event, Actor actor) {
//                Settings.fogFar = farSlider.getValue();
//                farValue.setText((int)Settings.fogFar);
//                Gdx.app.log("fog far", ""+Settings.fogFar);
//                screen.updateFogSettings();
//            }
//        });
//        add(new Label("fog far:", skin)).left();add(farValue); row();
//        add(farSlider).colspan(2).width(400f); row();
//
//        final Label expValue = new Label("", skin);
//        expValue.setText(Stringf.format("%.1f", Settings.fogExponent));
//        final Slider expSlider = new Slider(0.1f, 3.0f, 0.1f, false, skin);
//        expSlider.setValue(Settings.fogExponent);
//        expSlider.addListener(new ChangeListener() {
//            @Override
//            public void changed(ChangeEvent event, Actor actor) {
//                Settings.fogExponent = expSlider.getValue();
//                expValue.setText(Stringf.format("%.1f", Settings.fogExponent));
//                Gdx.app.log("fog exponent", ""+Settings.fogExponent);
//                screen.updateFogSettings();
//            }
//        });
//        add(new Label("fog exponent:", skin)).left();add(expValue).row();
//        add(expSlider).colspan(2).width(200f);       row();

//        final Slider SBslider = new Slider(0.0f, 0.01f, 0.0001f, false, skin);
//        SBslider.setValue(Settings.shadowBias);
//        SBslider.addListener(new ChangeListener() {
//            @Override
//            public void changed(ChangeEvent event, Actor actor) {
//                Settings.shadowBias = SBslider.getValue();
//                Gdx.app.log("bias", ""+Settings.shadowBias);
//                screen.gameView.adjustLighting();
//            }
//        });
//        final Label SBlabel = new Label("shadow bias", skin);
//        add(SBslider); add(SBlabel);        row();
//
//        final CheckBox LBcheckbox = new CheckBox("show light box", skin);
//        LBcheckbox.setChecked(Settings.showLightBox);
//        LBcheckbox.addListener(new ChangeListener() {
//            @Override
//            public void changed(ChangeEvent event, Actor actor) {
//                Settings.showLightBox = LBcheckbox.isChecked();
//            }
//        });
//        add(LBcheckbox).left();        row();
//
//        final CheckBox TCcheckbox = new CheckBox("terrain chunks allocation", skin);
//        TCcheckbox.setChecked(Settings.debugChunkAllocation);
//        TCcheckbox.addListener(new ChangeListener() {
//            @Override
//            public void changed(ChangeEvent event, Actor actor) {
//                Settings.debugChunkAllocation = TCcheckbox.isChecked();
//            }
//        });
//        add(TCcheckbox).left();        row();
//
//        final CheckBox RCcheckbox = new CheckBox("show colliders map (P)", skin);
//        RCcheckbox.setChecked(Settings.debugRockCollision);
//        RCcheckbox.addListener(new ChangeListener() {
//            @Override
//            public void changed(ChangeEvent event, Actor actor) {
//                Settings.debugRockCollision = RCcheckbox.isChecked();
//            }
//        });
//        add(RCcheckbox).left();        row();
//
//        final CheckBox PostCheckbox = new CheckBox("post-processing shader", skin);
//        PostCheckbox.setChecked(Settings.usePostShader);
//        PostCheckbox.addListener(new ChangeListener() {
//            @Override
//            public void changed(ChangeEvent event, Actor actor) {
//                Settings.usePostShader = PostCheckbox.isChecked();
//            }
//        });
//        add(PostCheckbox).left();        row();
//
//        if(Settings.multiSamplingFrameBufferAvailable) {
//            final CheckBox AAcheckbox = new CheckBox("multi-sample frame buffer", skin);
//            AAcheckbox.setChecked(Settings.useMultiSamplingFrameBuffer);
//            AAcheckbox.addListener(new ChangeListener() {
//                @Override
//                public void changed(ChangeEvent event, Actor actor) {
//                    Settings.useMultiSamplingFrameBuffer = AAcheckbox.isChecked();
//                }
//            });
//            add(AAcheckbox).left();
//        }
//        else
//            add(new Label("multi-sample frame buffer not available", skin)).left();
//        row();
//
//        final CheckBox CIcheckbox = new CheckBox("camera inverted", skin);
//        CIcheckbox.setChecked(Settings.cameraInverted);
//        CIcheckbox.addListener(new ChangeListener() {
//            @Override
//            public void changed(ChangeEvent event, Actor actor) {
//                Settings.cameraInverted = CIcheckbox.isChecked();
//                screen.gameView.cameraController.setCameraUpSideDown(Settings.cameraInverted);
//
//            }
//        });
//        add(CIcheckbox).left();        row();
//
//        final Slider FOVslider = new Slider(20.0f, 140f, 5f, false, skin);
//        FOVslider.setValue(Settings.cameraFieldOfView);
//        FOVslider.addListener(new ChangeListener() {
//            @Override
//            public void changed(ChangeEvent event, Actor actor) {
//                Settings.cameraFieldOfView = FOVslider.getValue();
//                Gdx.app.log("camera FOV", ""+Settings.cameraFieldOfView);
//                screen.gameView.getCamera().fieldOfView = Settings.cameraFieldOfView;
//            }
//        });
//        final Label FOVlabel = new Label("cam field of view", skin);
//        add(FOVslider); add(FOVlabel);        row();
//
//        final Slider CDslider = new Slider(5.0f, 200f, 2f, false, skin);
//        CDslider.setValue(Settings.cameraDistance);
//        CDslider.addListener(new ChangeListener() {
//            @Override
//            public void changed(ChangeEvent event, Actor actor) {
//                Settings.cameraDistance = CDslider.getValue();
//                Gdx.app.log("camera distance", ""+Settings.cameraDistance);
//                screen.gameView.cameraController.setDistance(Settings.cameraDistance);
//            }
//        });
//        final Label CDlabel = new Label("cam distance", skin);
//        add(CDslider); add(CDlabel);        row();


        pack();

    }
}

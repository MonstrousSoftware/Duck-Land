package com.monstrous.impostors.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.monstrous.impostors.GameScreen;
import com.monstrous.impostors.Settings;

// Debug window for some in-game tweaking
// Typically, these modify members of the Settings class.

public class FogSettingsWindow extends Window {

    private final Skin skin;
    private final GameScreen screen;

    public FogSettingsWindow(String title, Skin skin, GameScreen screen) {
        super(title, skin);
        this.skin = skin;
        this.screen = screen;
        rebuild();
    }

    private void rebuild() {


        final Slider nearSlider = new Slider(0.0f, 9000.0f, 20f, false, skin);
        nearSlider.setValue(Settings.fogNear);
        nearSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Settings.fogNear = nearSlider.getValue();
                Gdx.app.log("fog near", ""+Settings.fogNear);
                screen.updateFogSettings();
            }
        });
        add(nearSlider).width(400f); add(new Label("fog near", skin));        row();

        final Slider farSlider = new Slider(0.0f, 9000.0f, 20f, false, skin);
        farSlider.setValue(Settings.fogFar);
        farSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Settings.fogFar = farSlider.getValue();
                Gdx.app.log("fog far", ""+Settings.fogFar);
                screen.updateFogSettings();
            }
        });
        add(farSlider).width(400f); add(new Label("fog far", skin));        row();

        final Slider expSlider = new Slider(0.1f, 3.0f, 0.1f, false, skin);
        expSlider.setValue(Settings.fogExponent);
        expSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Settings.fogExponent = expSlider.getValue();
                Gdx.app.log("fog exponent", ""+Settings.fogExponent);
                screen.updateFogSettings();
            }
        });
        add(expSlider).width(200f); add(new Label("fog exponent", skin));        row();

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

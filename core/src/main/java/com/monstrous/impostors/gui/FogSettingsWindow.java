package com.monstrous.impostors.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.monstrous.impostors.screens.GameScreen;
import com.monstrous.impostors.Settings;
import text.formic.Stringf;

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


        final Label nearValue = new Label("", skin);
        nearValue.setText((int)Settings.fogNear);
        final Slider nearSlider = new Slider(0.0f, 9000.0f, 20f, false, skin);
        nearSlider.setValue(Settings.fogNear);
        nearSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Settings.fogNear = nearSlider.getValue();
                nearValue.setText((int)Settings.fogNear);
                Gdx.app.log("fog near", ""+Settings.fogNear);
                screen.updateFogSettings();
            }
        });
        add(new Label("fog near:", skin)).left();add(nearValue); row();
        add(nearSlider).colspan(2).width(400f); row();

        final Label farValue = new Label("", skin);
        farValue.setText((int)Settings.fogFar);
        final Slider farSlider = new Slider(0.0f, 9000.0f, 20f, false, skin);
        farSlider.setValue(Settings.fogFar);
        farSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Settings.fogFar = farSlider.getValue();
                farValue.setText((int)Settings.fogFar);
                Gdx.app.log("fog far", ""+Settings.fogFar);
                screen.updateFogSettings();
            }
        });
        add(new Label("fog far:", skin)).left();add(farValue); row();
        add(farSlider).colspan(2).width(400f); row();

        final Label expValue = new Label("", skin);
        expValue.setText(Stringf.format("%.1f", Settings.fogExponent));
        final Slider expSlider = new Slider(0.1f, 3.0f, 0.1f, false, skin);
        expSlider.setValue(Settings.fogExponent);
        expSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Settings.fogExponent = expSlider.getValue();
                expValue.setText(Stringf.format("%.1f", Settings.fogExponent));
                Gdx.app.log("fog exponent", ""+Settings.fogExponent);
                screen.updateFogSettings();
            }
        });
        add(new Label("fog exponent:", skin)).left();add(expValue).row();
        add(expSlider).colspan(2).width(200f);       row();

        pack();
    }
}

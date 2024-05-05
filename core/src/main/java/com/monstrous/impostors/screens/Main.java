package com.monstrous.impostors.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Version;
import com.monstrous.impostors.inputs.KeyBinding;

public class Main extends Game {
    @Override
    public void create() {
        Gdx.app.log("LibGDX version: ", Version.VERSION);
        KeyBinding.load();
        setScreen(new MenuScreen(this));
    }
}

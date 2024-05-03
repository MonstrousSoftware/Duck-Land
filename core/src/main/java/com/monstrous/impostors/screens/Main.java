package com.monstrous.impostors.screens;

import com.badlogic.gdx.Game;
import com.monstrous.impostors.inputs.KeyBinding;

public class Main extends Game {
    @Override
    public void create() {
        KeyBinding.load();
        setScreen(new MenuScreen(this));
    }
}

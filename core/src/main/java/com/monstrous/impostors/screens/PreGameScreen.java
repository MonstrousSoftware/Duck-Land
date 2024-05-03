package com.monstrous.impostors.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;


// interim screen

public class PreGameScreen extends ScreenAdapter {

    private Main game;
    private Viewport viewport;
    private Stage stage;      // from gdx-controllers-utils
    private Skin skin;
    private float time;

    public PreGameScreen(Main game) {
        this.game = game;
    }


    @Override
    public void show() {
        skin = new Skin(Gdx.files.internal("skin/uiskin.json"));
        stage = new Stage(new ScreenViewport());
        Table screenTable = new Table();
        screenTable.setFillParent(true);
        Label label = new Label("Generating...", skin);
        screenTable.add(label);
        stage.addActor(screenTable);
        time = 0;
    }

    @Override
    public void render(float deltaTime) {
        time+=deltaTime;
        if(time >  0.5f)
            game.setScreen(new GameScreen(game));
        ScreenUtils.clear(Color.BLACK);
        stage.act(deltaTime);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        // Resize your screen here. The parameters represent the new window size.
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        // Destroy screen's assets here.
        stage.dispose();
        skin.dispose();
    }


}

package com.monstrous.impostors.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.awt.*;


// main menu

public class MenuScreen extends ScreenAdapter {

    private Main game;
    private Stage stage;      // from gdx-controllers-utils
    private Skin skin;

    public MenuScreen(Main game) {
        this.game = game;
    }


    @Override
    public void show() {
        skin = new Skin(Gdx.files.internal("skin/uiskin.json"));
        stage = new Stage(new ScreenViewport());
        rebuild();
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float deltaTime) {
        ScreenUtils.clear(Color.BLACK);
        stage.act(deltaTime);
        stage.draw();
    }

    private void rebuild() {
        stage.clear();

        Table screenTable = new Table();
        screenTable.setFillParent(true);

        TextButton play = new TextButton("Show Demo", skin );
        TextButton keys = new TextButton("Keys", skin);
        TextButton quit = new TextButton("Quit", skin);

        float pad = 17f;
        screenTable.top();

        Table menu = new Table();
        menu.add(play).pad(pad).left().bottom().row();
        menu.add(keys).pad(pad).left().row();
        // hide quit on web
        if(!(Gdx.app.getType() == Application.ApplicationType.WebGL) )
            menu.add(quit).pad(pad).left().row();

        screenTable.add(menu).left().bottom().expand();
        screenTable.pack();

        screenTable.setColor(1,1,1,0);                   // set alpha to zero
        screenTable.addAction(Actions.fadeIn(2f));           // fade in
        stage.addActor(screenTable);

        play.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                game.setScreen(new PreGameScreen(game));
            }
        });

        keys.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                game.setScreen(new KeysScreen( game ));
            }
        });


        quit.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                Gdx.app.exit();
            }
        });

    }

    @Override
    public void resize(int width, int height) {
        // Resize your screen here. The parameters represent the new window size.
        stage.getViewport().update(width, height, true);
        rebuild();
    }

    @Override
    public void dispose() {
        // Destroy screen's assets here.
    }


}

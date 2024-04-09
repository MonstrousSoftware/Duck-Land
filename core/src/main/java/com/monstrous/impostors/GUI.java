package com.monstrous.impostors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class GUI {
    public Stage stage;
    private Skin skin;
    private Label lodLabel;
    private Label fpsLabel;


    public GUI() {
        stage = new Stage(new ScreenViewport());
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        rebuild();
    }

    private void rebuild() {
        stage.clear();

        Table screenTable = new Table();
        screenTable.setFillParent(true);
        screenTable.debug();

        Table statsTable = new Table();


        statsTable.add(new Label("FPS: ", skin)).left().pad(5);
        fpsLabel = new Label("", skin);
        statsTable.add(fpsLabel);
        statsTable.row();

        statsTable.add(new Label("LOD Level: ", skin)).left().pad(5);
        lodLabel = new Label("", skin);
        statsTable.add(lodLabel);
        statsTable.row();
        statsTable.top().left();
        statsTable.pack();

        screenTable.add(statsTable);
        screenTable.pack();

        stage.addActor(statsTable);
    }

    public void render(float deltaTime) {
        if(Settings.lodLevel < Settings.LOD_LEVELS)
            lodLabel.setText(Settings.lodLevel);
        else
            lodLabel.setText("Impostor");
        fpsLabel.setText( Gdx.graphics.getFramesPerSecond());

        stage.act(deltaTime);
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        rebuild();
    }


    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}

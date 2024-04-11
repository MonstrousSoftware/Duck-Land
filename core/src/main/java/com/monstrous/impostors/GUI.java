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
    private Label vertsLabel;
    private Label instancesLabel;
    private GameScreen screen;


    public GUI( GameScreen screen ) {
        this.screen = screen;

        stage = new Stage(new ScreenViewport());
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        rebuild();
    }

    private void rebuild() {
        stage.clear();

        String type = "window";

        Table screenTable = new Table();
        screenTable.setFillParent(true);

        screenTable.add(new Label("FPS: ", skin, type)).left().pad(5);
        fpsLabel = new Label("", skin, type);
        screenTable.add(fpsLabel).left();
        screenTable.row();

        screenTable.add(new Label("LOD Level: ", skin, type)).left().pad(5);
        lodLabel = new Label("", skin, type);
        screenTable.add(lodLabel).left();
        screenTable.row();

        screenTable.add(new Label("vertices: ", skin, type)).left().pad(5);
        vertsLabel = new Label("", skin, type);
        screenTable.add(vertsLabel).left();
        screenTable.row();

        screenTable.add(new Label("instances: ", skin, type)).left().pad(5);
        instancesLabel = new Label("", skin, type);
        screenTable.add(instancesLabel).left();
        screenTable.row();

        screenTable.bottom().left();
        screenTable.pack();

        stage.addActor(screenTable);
    }

    private void updateLabels(){
        if(Settings.lodLevel < Settings.LOD_LEVELS)
            lodLabel.setText(Settings.lodLevel);
        else
            lodLabel.setText("Impostor");
        vertsLabel.setText( screen.numVertices );
        instancesLabel.setText( screen.instanceCount );
        fpsLabel.setText( Gdx.graphics.getFramesPerSecond());
    }

    public void render(float deltaTime) {
        updateLabels();

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

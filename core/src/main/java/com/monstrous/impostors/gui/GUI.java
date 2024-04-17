package com.monstrous.impostors.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.monstrous.impostors.GameScreen;
import com.monstrous.impostors.Settings;

public class GUI {
    public Stage stage;
    private Skin skin;

    private Label fpsLabel;
    private Label[] vertsLabels;
    private Label[] instancesLabels;
    private Label totalInstancesLabel;
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


        screenTable.add(new Label("Total Instances: ", skin, type)).left().pad(5);
        totalInstancesLabel = new Label("", skin, type);
        screenTable.add(totalInstancesLabel).left();
        screenTable.row();


        screenTable.add(new Label("LOD Level: ", skin, type)).left().pad(5);
        for(int lod = 0; lod < Settings.LOD_LEVELS+1; lod++) {
            Label lodLabel = new Label(""+lod, skin, type);
            if(lod == Settings.LOD_LEVELS)
                lodLabel.setText("Impostor");
            screenTable.add(lodLabel).width(100).left();
        }
        screenTable.row();

        vertsLabels = new Label[Settings.LOD_LEVELS+1];
        screenTable.add(new Label("vertices: ", skin, type)).left().pad(5);
        for(int lod = 0; lod < Settings.LOD_LEVELS+1; lod++) {
            vertsLabels[lod] = new Label("", skin, type);
            screenTable.add(vertsLabels[lod]).left();
        }
        screenTable.row();

        instancesLabels = new Label[Settings.LOD_LEVELS+1];
        screenTable.add(new Label("instances: ", skin, type)).left().pad(5);
        for(int lod = 0; lod < Settings.LOD_LEVELS+1; lod++) {
            instancesLabels[lod] = new Label("", skin, type);
            screenTable.add(instancesLabels[lod]).left();
        }
        screenTable.row();

        screenTable.bottom().left();
        screenTable.pack();

        stage.addActor(screenTable);
    }

    private void updateLabels(){
        int total = 0;
        for(int lod = 0; lod < Settings.LOD_LEVELS+1; lod++) {
            vertsLabels[lod].setText(screen.scenery.statistics[lod].vertexCount);
            instancesLabels[lod].setText(screen.scenery.statistics[lod].instanceCount);
            total += screen.scenery.statistics[lod].instanceCount;
        }
        totalInstancesLabel.setText(total);
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

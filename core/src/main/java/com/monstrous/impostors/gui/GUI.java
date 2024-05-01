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
    private Label[] typeLabels;
    private Label[] instancesLabels;
    private Label totalInstancesLabel;
    private GameScreen screen;
    private FogSettingsWindow fogWindow;
    private LightSettingsWindow lightWindow;


    public GUI( GameScreen screen ) {
        this.screen = screen;

        stage = new Stage(new ScreenViewport());
        skin = new Skin(Gdx.files.internal("skin/uiskin.json"));
        fogWindow = new FogSettingsWindow("The Fogulator", skin, screen);
        lightWindow = new LightSettingsWindow("Light Settings", skin, screen);

        rebuild();
    }

    private void rebuild() {
        stage.clear();

        showFogMenu(Settings.showFogSettings);

        String labelType  = "default";

        Table screenTable = new Table();
        screenTable.setFillParent(true);

        screenTable.add(new Label("FPS: ", skin, labelType)).left().pad(5);
        fpsLabel = new Label("", skin, labelType);
        screenTable.add(fpsLabel).left();
        screenTable.row();


        screenTable.add(new Label("Total Instances: ", skin, labelType)).left().pad(5);
        totalInstancesLabel = new Label("", skin, labelType);
        screenTable.add(totalInstancesLabel).left();
        screenTable.row();


        // column headers
        screenTable.add(new Label("", skin, labelType)).left().pad(5);
        screenTable.add(new Label("", skin, labelType)).left().pad(5);
        for(int lod = 0; lod < Settings.LOD_LEVELS+1; lod++) {
            Label lodLabel = new Label("LOD"+lod, skin, labelType);
            if(lod == Settings.LOD_LEVELS)
                lodLabel.setText("Impostor");
            screenTable.add(lodLabel).width(100).left();
        }
        screenTable.row();

//        vertsLabels = new Label[Settings.LOD_LEVELS+1];
//        screenTable.add(new Label("vertices: ", skin, type)).left().pad(5);
//        for(int lod = 0; lod < Settings.LOD_LEVELS+1; lod++) {
//            vertsLabels[lod] = new Label("", skin, type);
//            screenTable.add(vertsLabels[lod]).left();
//        }
//        screenTable.row();

        typeLabels = new Label[screen.scenery.numTypes];
        instancesLabels = new Label[screen.scenery.numTypes * (Settings.LOD_LEVELS + 1)];
        for(int type = 0; type < screen.scenery.numTypes; type++) {
            typeLabels[type] = new Label(screen.scenery.statistics.getName(type), skin, labelType);
            screenTable.add(typeLabels[type]).left().pad(5);
            screenTable.add(new Label("instances: ", skin, labelType)).left().pad(5);
            for (int lod = 0; lod < Settings.LOD_LEVELS + 1; lod++) {
                instancesLabels[type*(Settings.LOD_LEVELS+1)+lod] = new Label("", skin, labelType);
                screenTable.add(instancesLabels[type*(Settings.LOD_LEVELS+1)+lod]).left();
            }
            screenTable.row();
        }

        screenTable.bottom().left();
        screenTable.pack();

        stage.addActor(screenTable);
    }

    private void updateLabels(){
        int total = 0;
        int index = 0;
        for(int type = 0; type < screen.scenery.numTypes; type++) {
            for (int lod = 0; lod < Settings.LOD_LEVELS + 1; lod++) {
                //vertsLabels[lod].setText(screen.scenery.statistics.getVertexCount(type, lod));
                instancesLabels[index++].setText(screen.scenery.statistics.getInstanceCount(type, lod));
                total += screen.scenery.statistics.getInstanceCount(type, lod);
            }
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

    public void showFogMenu(boolean mode){
        if(mode) {
            stage.addActor(fogWindow);
            fogWindow.setPosition(stage.getWidth()-fogWindow.getWidth(),
                    stage.getHeight()-fogWindow.getHeight());
        } else
            fogWindow.remove();
    }

    public void showLightMenu(boolean mode){
        if(mode) {
            stage.addActor(lightWindow);
            lightWindow.setPosition(stage.getWidth()-lightWindow.getWidth(),
                stage.getHeight()-lightWindow.getHeight());
        } else
            lightWindow.remove();
    }

    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}

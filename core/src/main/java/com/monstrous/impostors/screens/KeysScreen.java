package com.monstrous.impostors.screens;

import com.badlogic.gdx.*;
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
import com.monstrous.impostors.inputs.KeyBinding;

// key bindings menu
// shows key bindings and allows the user to modify them

public class KeysScreen extends InputAdapter implements Screen {

    private Main game;
    private Viewport viewport;
    private Stage stage;      // from gdx-controllers-utils
    private Skin skin;
    private TextButton pressedButton;
    private KeyBinding selectedBinding;
    private Label debugLabel;

    public KeysScreen(Main game) {
        this.game = game;
    }


    @Override
    public void show() {
        viewport = new ScreenViewport();
        skin = new Skin(Gdx.files.internal("skin/uiskin.json"));
        stage = new Stage(new ScreenViewport());
        InputMultiplexer im = new InputMultiplexer();
        im.addProcessor(stage);
        im.addProcessor(this);
        Gdx.input.setInputProcessor(im);
        selectedBinding = null;
    }

    @Override
    public void render(float deltaTime) {
        ScreenUtils.clear(Color.BLACK);
        stage.act(deltaTime);
        stage.draw();
    }

    private void rebuild(boolean fade) {
        stage.clear();

        Table screenTable = new Table();
        screenTable.setFillParent(true);

        Table keyTable = new Table();
        for(KeyBinding binding : KeyBinding.values()) {
            keyTable.add(new Label(binding.getDescription(), skin)).left();
            int keycode = binding.getKeyCode();
            String text = Input.Keys.toString(keycode);
            if(keycode == 0)    // unbound action
                text = "- -";
            TextButton button = new TextButton(text, skin);
            keyTable.add(button);
            keyTable.row();

            button.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    rebind(button, binding);
                }
            });


        }
        // Note: Input.Keys.toString() follows US keyboard layout in naming keys
        // It should really present the name following the regional keyboard setting.
        // Waiting for libGDX issue #6962 to be resolved.

        TextButton reset = new TextButton(" RESET ", skin);
        TextButton okay = new TextButton(" OK ", skin);

        debugLabel = new Label("To modify a key binding, click a button", skin);

        screenTable.top();
        screenTable.add(keyTable).pad(50).row();
        screenTable.add(debugLabel).pad(20).row();
        screenTable.add(reset).width(200).pad(10).row();
        screenTable.add(okay).width(200).pad(10).row();
        screenTable.pack();

        if(fade) {
            screenTable.setColor(1, 1, 1, 0);                   // set alpha to zero
            screenTable.addAction(Actions.fadeIn(2f));           // fade in
        }
        stage.addActor(screenTable);

        okay.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                KeyBinding.save();          // save changes to file
                game.setScreen(new MenuScreen(game));
            }
        });

        reset.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                for(KeyBinding binding : KeyBinding.values())
                    binding.resetKeyBinding();
                rebuild(false);  // update button labels
            }
        });

    }

    private void rebind(TextButton button, KeyBinding binding){
        pressedButton = button;
        pressedButton.setText("???");
        pressedButton.setColor(Color.RED);
        selectedBinding = binding;
        debugLabel.setText("Press the key to assign to this action (ESC to cancel)");
    }

    @Override
    public boolean keyDown(int keycode) {
        if(selectedBinding == null)
            return false;
        //debugLabel.setText("keycode : "+keycode);
        if(keycode != Input.Keys.ESCAPE) {
            selectedBinding.setKeyBinding(keycode);
            removeDupes(selectedBinding, keycode);
        }
        pressedButton.setText(Input.Keys.toString(selectedBinding.getKeyCode()));
        pressedButton.setColor(Color.WHITE);
        selectedBinding = null;

        return true;
    }

    // clear other keybindings to the same keycode
    private void removeDupes(KeyBinding changedBinding, int keycode ) {
        boolean changed = false;
        for(KeyBinding binding : KeyBinding.values()){
            if(binding == changedBinding)
                continue;
            if(binding.getKeyCode() == keycode){
                binding.setKeyBinding(0);
                changed = true;
            }
        }
        if(changed)
            rebuild(false);
    }


    @Override
    public void resize(int width, int height) {
        // Resize your screen here. The parameters represent the new window size.
        viewport.update(width, height, true);
        stage.getViewport().update(width, height, true);
        rebuild(true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        // Destroy screen's assets here.
        stage.dispose();
        skin.dispose();
    }


}

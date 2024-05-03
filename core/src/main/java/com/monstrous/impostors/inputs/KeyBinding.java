package com.monstrous.impostors.inputs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Preferences;
import com.monstrous.impostors.Settings;

public enum KeyBinding {
    FORWARD ("Move forward", Keys.W),
    BACK ("Move back", Keys.S),
    LEFT ("Turn left", Keys.A),
    RIGHT ("Turn right", Keys.D),
    TURBO ("Turbo boost", Keys.SPACE),
    UP ("Go up", Keys.E),
    DOWN ("Go down", Keys.Q),
    CYCLE_LOD ("Cycle LOD instances", Keys.TAB),
    INCREASE_LOD_DISTANCE ("Increase LOD distance", Keys.Z),
    DECREASE_LOD_DISTANCE ("Decrease LOD distance", Keys.X),
    TOGGLE_FULLSCREEN ("Toggle full screen", Keys.F),
    TERRAIN_OVERLAY ("Toggle terrain chunk overlay", Keys.T),
    SCENERY_OVERLAY ("Toggle scenery chunk overlay", Keys.P),
    SINGLE_INSTANCE ("Showcase single instance", Keys.M),
    FOG_MENU ("Toggle fog menu", Keys.Y),
    LIGHT_MENU ("Toggle light and shadows menu", Keys.L);

    private final String description;      // action
    private int defaultKeyCode;     // original code, used on reset
    private int keyCode;            // can be user configured

    KeyBinding(String description, int defaultKeyCode) {
        this.description = description;
        this.defaultKeyCode = defaultKeyCode;
        this.keyCode = defaultKeyCode;
    }

    public int getKeyCode(){
        return keyCode;
    }

    public void setKeyBinding(int keyCode ){
        this.keyCode = keyCode;
    }

    public void resetKeyBinding(){
        keyCode = defaultKeyCode;
    }

    public String getDescription() {
        return description;
    }

    // save key bindings to preferences file
    // call this after changing key bindings
    static public void save(){
        Preferences prefs = Gdx.app.getPreferences(Settings.title);
        for(KeyBinding binding : values()){
            // only save key binding that differ from the default
            //if(binding.keyCode != binding.defaultKeyCode)
                prefs.putInteger( String.valueOf(binding), binding.keyCode);
        }
        prefs.flush();  // save to file
    }

    // load key bindings from preferences file
    // call this once on startup
    static public void load() {
        Preferences prefs = Gdx.app.getPreferences(Settings.title);
        for(KeyBinding binding : values()){
            int keycode = prefs.getInteger(String.valueOf(binding), binding.defaultKeyCode);
            binding.keyCode = keycode;
        }
    }
}

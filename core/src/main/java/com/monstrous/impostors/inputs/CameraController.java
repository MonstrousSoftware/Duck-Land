package com.monstrous.impostors.inputs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntIntMap;
import com.monstrous.impostors.Settings;
import com.monstrous.impostors.terrain.Terrain;

public class CameraController extends InputAdapter {

    private final float WALK_SPEED = 150f;

    private final PerspectiveCamera camera;
    private final Terrain terrain;
    private float eyeHeight = 30f;
    private float speed = 0;
    protected final IntIntMap keys = new IntIntMap();
    protected float degreesPerPixel = 0.1f;
    protected final Vector3 tmp = new Vector3();
    protected final Vector3 tmp2 = new Vector3();
    protected final Vector3 tmp3 = new Vector3();
    private final Vector3 fwdHorizontal = new Vector3();

    public CameraController(PerspectiveCamera camera, Terrain terrain) {
        this.camera = camera;
        this.terrain = terrain;
    }


    public void update(float deltaTime) {
        fwdHorizontal.set(camera.direction).y = 0;
        fwdHorizontal.nor();

        if (keys.containsKey(KeyBinding.FORWARD.getKeyCode())) {
            speed = WALK_SPEED;
        }
        if (keys.containsKey(KeyBinding.BACK.getKeyCode())) {
            speed = -WALK_SPEED;
        }
        if (keys.containsKey(KeyBinding.TURBO.getKeyCode())) {
            speed = WALK_SPEED * 5f;
        } else if (!keys.containsKey(KeyBinding.FORWARD.getKeyCode()) &&
            !keys.containsKey(KeyBinding.TURBO.getKeyCode()) &&
            Math.abs(speed) > 0) {
            speed -= speed * 10f * deltaTime;
            if (Math.abs(speed) < 1f)
                speed = 0;
        }

        if (keys.containsKey(KeyBinding.UP.getKeyCode())) {
            eyeHeight += 30f * deltaTime;
        }
        if (keys.containsKey(KeyBinding.DOWN.getKeyCode())) {
            if (eyeHeight > 10f || Settings.singleInstance)
                eyeHeight -= 30f * deltaTime;
        }

        if (keys.containsKey(KeyBinding.LEFT.getKeyCode())) {
            camera.direction.rotate(camera.up, deltaTime * 30f);
        }
        if (keys.containsKey(KeyBinding.RIGHT.getKeyCode())) {
            camera.direction.rotate(camera.up, -deltaTime * 30f);
        }

        tmp.set(fwdHorizontal).scl(deltaTime * speed);
        camera.position.add(tmp);

        float ht = terrain.getHeight(camera.position.x, camera.position.z);
        camera.position.y = ht + eyeHeight;
        camera.update(true);
    }

    @Override
    public boolean keyDown(int keycode) {
        keys.put(keycode, keycode);
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        keys.remove(keycode, 0);
        return true;
    }


    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        float deltaX = -Gdx.input.getDeltaX() * degreesPerPixel;
        float deltaY = -Gdx.input.getDeltaY() * degreesPerPixel;
        if(Math.abs(deltaX) > 20 )
            return true;

        if(Settings.invertLook)
            deltaY = -deltaY;

        camera.direction.rotate(camera.up, deltaX);

        // avoid gimbal lock when looking straight up or down
        Vector3 oldPitchAxis = tmp.set(camera.direction).crs(camera.up).nor();
        Vector3 newDirection = tmp2.set(camera.direction).rotate(tmp, deltaY);
        Vector3 newPitchAxis = tmp3.set(tmp2).crs(camera.up);
        if (!newPitchAxis.hasOppositeDirection(oldPitchAxis))
            camera.direction.set(newDirection);

        return true;
    }
}

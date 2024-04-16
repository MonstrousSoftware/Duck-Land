
uniform mat4 u_projViewTrans;
uniform vec3 u_camPos;

// dedicated to the use of the decal atlas
uniform vec2     u_step;          // u width (fraction) horizontally per decal Y rotation, v height (fraction) horizontally per decal polar rotation

in vec3 a_position;
in vec2 a_texCoord0;

in vec4 i_offset;           // world position of instance (xyz) + y-rotation (w)

out vec2 texCoords;

// create a 3x3 rotation matrix to orient the vertex positions towards the camera
mat3 calcLookAtMatrix(vec3 origin, vec3 target) {
    vec3 worldUp = vec3(0.0, 1.0, 0.0);
    vec3 fwd =      normalize(target - origin);
    vec3 right =    normalize(cross(fwd, worldUp));
    vec3 up =       normalize(cross(right, fwd));

    return mat3(right, up, fwd);    // set columns
}

#define PI 3.1415926538

// get UV offset in the decal atlas texture for the decal at the closest angle
vec2 getUVoffset(vec3 camera, vec4 instance)
{
    vec3 fwd =      camera-instance.xyz;        // vector towards camera

    // angle in the horizontal plane (360 degrees)
    float angle = atan(fwd.z, fwd.x);
    angle += instance.w;           //  add model's rotation in the range [-Pi,Pi].
    angle -= 0.5*PI;                // turn to match definition of zero
    angle = mod(angle, 2.0*PI);

    float u_offset = floor( angle / (2.0*PI * u_step.x));     // index of texture column to use (0 .. N)
    u_offset *= u_step.x;

    float len = length( fwd.xz );
    float elevationAngle = 0.5*PI;
    if(len >= 0.1)
        elevationAngle = atan(fwd.y/ len);  // [-PI/2, PI/2]
    elevationAngle = max(elevationAngle, 0.0);    // [0, PI/2]

    float v_offset = floor( elevationAngle / (PI * 0.5 * u_step.y) );// index of texture row to use (0 .. M)
    v_offset = clamp(v_offset, 0.0, floor(1.0/u_step.y)-1.0);
    v_offset *= u_step.y;

    return vec2(u_offset, 0.000001*v_offset);
}



void main () {
    texCoords = a_texCoord0 + getUVoffset(u_camPos, i_offset);

    mat3 decalRotMatrix = calcLookAtMatrix( u_camPos, i_offset.xyz);

    gl_Position = u_projViewTrans *   vec4(decalRotMatrix * a_position + i_offset.xyz, 1.0);
}

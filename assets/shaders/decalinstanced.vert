

uniform mat4 u_projViewTrans;
uniform vec3 u_camPos;

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
#define STEPS 10.0
#define ELEV_STEP (PI / 6.0)

vec2 getUVoffset(vec3 origin, vec4 offset)
{
    vec3 fwd =      origin-offset.xyz;

    // angle in the horizontal plane
    float angle;
    if(abs(fwd.x) < 0.01 )
        angle = sign(fwd.z)*PI/2.0;
    else
        angle = atan(fwd.z, fwd.x);
    angle -= 0.5*PI;
    angle += offset.w;           //  in the range [-Pi,Pi].
    if(angle < 0.0)
        angle += 2.0*PI;
    float u =  floor(STEPS * angle / (2.0*PI));
    u /= STEPS;


    float len = length( fwd.xz );
    float elevationAngle = atan(fwd.y, len);

    float v = floor(elevationAngle / ELEV_STEP );
    v = clamp(v, 0.0, 5.0);
    //v /= 6.0;
    v = v * 299.0 / 2048.0;

//    float dist = distance(u_camPos.xz,i_worldTrans.xz);
//    angleX = atan(u_camPos.y - i_worldTrans.y,dist);
//    //float angleX = atan(rotationMatrix[1][2],rotationMatrix[2][2])-HALF_PI;
//    //float angleX = atan(-rotationMatrix[0][2],sqrt( (rotationMatrix[1][2] * rotationMatrix[1][2]) + (rotationMatrix[2][2] * rotationMatrix[2][2]) ))+HALF_PI;
//    angleY = acos(dot(normalize(vec2(i_worldTrans.x,u_camPos.x)),normalize(vec2(i_worldTrans.z,u_camPos.z))));
//
//    float tmpFloat1;
//    float tmpFloat2;
//
//    if (angleX > HALF_PI)
//    angleX = HALF_PI - (angleX - HALF_PI);
//    if (angleX < MINIMUM_ANGLE_RAD) {
//        tmpFloat1 = 0.0;
//        tmpStepX = 0.0;
//    }
//    else
//    {
//        tmpStepX = float(round((angleX - MINIMUM_ANGLE_RAD) / u_uvStepSize.x));
//        if (tmpStepX >= u_uvSteps.x) tmpStepX = u_uvSteps.x-1.0;
//        tmpFloat1 =  tmpStepX * u_uvSize.x;
//    }
//
//    tmpStepY = float(round((angleY) / u_uvStepSize.y));
//    if (tmpStepY >= u_uvSteps.y) tmpStepY = u_uvSteps.y-1.0;
//    tmpFloat2 =  tmpStepY * u_uvSize.y;

    return vec2(u+ a_texCoord0.x,v+ a_texCoord0.y);
}



void main () {
    texCoords = getUVoffset(u_camPos, i_offset);

    mat3 decalRotMatrix = calcLookAtMatrix( u_camPos, i_offset.xyz);

    gl_Position = u_projViewTrans *   vec4(decalRotMatrix * a_position + i_offset.xyz, 1.0);
}

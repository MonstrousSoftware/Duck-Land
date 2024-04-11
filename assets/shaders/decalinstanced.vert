



uniform mat4 u_projViewTrans;
uniform vec3 u_camPos;

in vec3 a_position;
in vec2 a_texCoord0;

in vec3 i_offset;

out vec2 texCoords;

mat3 calcLookAtMatrix(vec3 origin, vec3 target) {
    vec3 worldUp = vec3(0.0, 1.0, 0.0);
    vec3 fwd = normalize(target - origin);
    vec3 right = normalize(cross(fwd, worldUp));
    vec3 up = normalize(cross(right, fwd));

    return mat3(right, up, fwd);
}


void main () {
    texCoords = a_texCoord0;

    mat3 decalRotMatrix = calcLookAtMatrix(a_position, u_camPos);               // TO FIX

    vec3 pos =  i_offset +  a_position + 0.01*u_camPos;// * decalRotMatrix;

    gl_Position = u_projViewTrans * vec4(pos, 1.0);
}

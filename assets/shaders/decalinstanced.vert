

uniform mat4 u_projViewTrans;
in vec3 a_position;
in vec2 a_texCoord0;

//in mat4 i_worldTrans;
in vec3 i_offset;

out vec2 texCoords;

void main () {
    texCoords = a_texCoord0;

    vec3 pos = a_position + i_offset;

    gl_Position = u_projViewTrans * vec4(pos, 1.0);
}

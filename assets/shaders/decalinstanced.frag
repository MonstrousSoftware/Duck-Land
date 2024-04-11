
precision mediump float;

uniform sampler2D u_texture;
in vec2 texCoords;
out vec4 fragColor;


void main () {
    vec4 color = texture(u_texture, texCoords);
    if (color.a < 0.95) discard;

    fragColor = color;
}

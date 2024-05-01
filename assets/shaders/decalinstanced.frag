
precision highp float;

uniform sampler2D u_texture;
uniform vec4 u_cameraPosition;
uniform vec4 u_fogColor;

in vec2 texCoords;
in float v_fog;

out vec4 fragColor;

void main () {
    vec4 color = texture(u_texture, texCoords);
    if (color.a < 0.95) discard;

    color = mix(color, u_fogColor, v_fog);
    fragColor = color;
}


precision highp float;

uniform sampler2D u_texture;
uniform vec4 u_cameraPosition;
uniform vec4 u_fogColor;
uniform vec3 u_fogEquation; // (near, far, exponent)

in vec2 texCoords;
in vec3 v_position;

out vec4 fragColor;

void main () {
    vec4 color = texture(u_texture, texCoords);
    if (color.a < 0.95) discard;

    float eyeDistance = length(u_cameraPosition.xyz - v_position);
    float fog = (eyeDistance - u_fogEquation.x) / (u_fogEquation.y - u_fogEquation.x);
    fog = clamp(fog, 0.0, 1.0);
    fog = pow(fog, u_fogEquation.z);

    color = mix(color, u_fogColor, fog);

    fragColor = color;
}

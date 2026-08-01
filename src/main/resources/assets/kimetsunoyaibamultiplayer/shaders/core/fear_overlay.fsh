#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float Invert;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a < 0.01) {
        discard;
    }
    if (Invert > 0.5) {
        color.rgb = 1.0 - color.rgb;
    }
    fragColor = color;
}

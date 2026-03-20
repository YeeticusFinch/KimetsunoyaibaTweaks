#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    float luma = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    vec3 gray = vec3(luma);
    gray = clamp((gray - 0.5) * 1.2 + 0.5, 0.0, 1.0);
    fragColor = vec4(gray, color.a);
}

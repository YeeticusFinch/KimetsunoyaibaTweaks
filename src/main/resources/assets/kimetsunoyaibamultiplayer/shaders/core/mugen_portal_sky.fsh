#version 150

uniform sampler2D SkyNorth;
uniform sampler2D SkyEast;
uniform sampler2D SkySouth;
uniform sampler2D SkyWest;
uniform sampler2D SkyUp;
uniform sampler2D SkyDown;

uniform float PortalAlpha;

in vec3 worldViewRay;

out vec4 fragColor;

vec2 finishUv(vec2 uv) {
    uv = uv * 0.5 + 0.5;
    uv.y = 1.0 - uv.y;
    return uv;
}

vec4 sampleWorldCubemap(vec3 direction) {
    vec3 d = normalize(direction);
    vec3 a = abs(d);
    vec2 uv;

    if (a.x >= a.y && a.x >= a.z) {
        if (d.x > 0.0) {
            uv = vec2(-d.z, d.y) / a.x;
            return texture(SkyEast, finishUv(uv));
        }

        uv = vec2(d.z, d.y) / a.x;
        return texture(SkyWest, finishUv(uv));
    }

    if (a.y >= a.x && a.y >= a.z) {
        if (d.y > 0.0) {
            uv = vec2(d.x, -d.z) / a.y;
            return texture(SkyUp, finishUv(uv));
        }

        uv = vec2(d.x, d.z) / a.y;
        return texture(SkyDown, finishUv(uv));
    }

    if (d.z > 0.0) {
        uv = vec2(d.x, d.y) / a.z;
        return texture(SkySouth, finishUv(uv));
    }

    uv = vec2(-d.x, d.y) / a.z;
    return texture(SkyNorth, finishUv(uv));
}

void main() {
    vec4 sky = sampleWorldCubemap(worldViewRay);
    fragColor = vec4(sky.rgb, sky.a * PortalAlpha);
}

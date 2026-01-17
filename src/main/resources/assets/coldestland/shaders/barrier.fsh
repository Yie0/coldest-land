#version 150

#extension GL_OES_standard_derivatives : enable

in vec4 vertexColor;
in vec3 pos;
in float time;

out vec4 fragColor;

// [COLORS]
const vec3 C_DEEP      = vec3(0.00, 0.08, 0.16);
const vec3 C_MID       = vec3(0.10, 0.62, 0.67);
const vec3 C_ICE       = vec3(0.67, 0.67, 0.50);
const vec3 C_GLOW      = vec3(0.67, 1.00, 1.00);

// [FBM]
#define OCTAVES 3
const float FBM_SCALE   = 1.2;
const float FBM_SPEED   = 0.35;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}
float noise(in vec2 _st) {
    vec2 i = floor(_st);
    vec2 f = fract(_st);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(in vec2 _st) {
    float v = 0.0;
    float a = 0.5;
    vec2 shift = vec2(100.0);
    mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));
    for (int i = 0; i < OCTAVES; ++i) {
        v += a * noise(_st);
        _st = rot * _st * 2.0 + shift;
        a *= 0.5;
    }
    return v;
}

struct FBMResult {
    float f;
    vec2 q;
    vec2 r;
};

FBMResult getWarpedFBM(vec2 st, float t) {
    FBMResult res;

    res.q.x = fbm(st + 0.00 * t);
    res.q.y = fbm(st + vec2(1.0));

    res.r.x = fbm(st + 1.0 * res.q + vec2(1.7, 9.2) + FBM_SPEED * t);
    res.r.y = fbm(st + 1.0 * res.q + vec2(8.3, 2.8) + FBM_SPEED * 0.84 * t);

    res.f = fbm(st + res.r);

    return res;
}

void main() {
    vec3 fdx = dFdx(pos);
    vec3 fdy = dFdy(pos);
    vec3 normal = normalize(cross(fdx, fdy));
    vec3 blending = abs(normal);
    blending /= (blending.x + blending.y + blending.z);

    vec2 stX = pos.zy * FBM_SCALE;
    vec2 stY = pos.xz * FBM_SCALE;
    vec2 stZ = pos.xy * FBM_SCALE;

    FBMResult resX = getWarpedFBM(stX, time);
    FBMResult resY = getWarpedFBM(stY, time);
    FBMResult resZ = getWarpedFBM(stZ, time);

    float f = resX.f * blending.x + resY.f * blending.y + resZ.f * blending.z;
    vec2 q  = resX.q * blending.x + resY.q * blending.y + resZ.q * blending.z;
    vec2 r  = resX.r * blending.x + resY.r * blending.y + resZ.r * blending.z;

    vec3 color = mix(C_MID, C_ICE, clamp((f * f) * 4.0, 0.0, 1.0));
    color = mix(color, C_DEEP, clamp(length(q), 0.0, 1.0));
    color = mix(color, C_GLOW, clamp(length(r.x), 0.0, 1.0));

    float f2 = f*f;
    float fPow = f2 * f + 0.6 * f2 + 0.5 * f;
    vec3 finalRGB = mix(color, color * fPow, 0.55);

    float pulse = sin(time * 1.5) * 0.5 + 0.5;
    float core = 1.0 / (1.0 + length(pos) * 2.5);
    core *= pulse;
    finalRGB += C_GLOW * core * 0.2;

    float alpha = vertexColor.a * (0.6 + f * 0.4);
    alpha = clamp(alpha * (1.0 + sin(time * 0.3) * 0.1), 0.0, 1.0);

    fragColor = vec4(clamp(finalRGB * vertexColor.rgb * 1.2, 0.0, 1.0), alpha);
}
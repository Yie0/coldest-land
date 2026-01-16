#version 150

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;

out vec4 vertexColor;
out vec3 pos;
out float time;

void main() {
    vec3 localPos = Position;

    time = ModelOffset.x * 0.001 * 2.0;

    float t = time * 0.4;
    float amp1 = 0.070;
    float amp2 = 0.040;
    float amp3 = 0.024;
    float freq1 = 1.6;
    float freq2 = 2.8;
    float freq3 = 5.2;

    vec2 d1 = normalize(vec2(0.9, 0.4));
    vec2 d2 = normalize(vec2(-0.6, 0.8));
    vec2 d3 = normalize(vec2(0.3, -0.95));

    vec2 xz = localPos.xz;

    float w1 = sin(dot(xz, d1) * freq1 + t);
    float w2 = sin(dot(xz, d2) * freq2 + t * 0.85 * 2.0 + 1.2);
    float w3 = sin(dot(xz, d3) * freq3 + t * 1.25 * 2.0 + 2.7);

    float falloff = exp(-length(localPos.xy) * 0.12);

    float displacement =
    (amp1 * w1 + amp2 * w2 + amp3 * w3) * falloff;

    displacement += 0.010 * sin(t * 1.2 + length(xz) * 0.10);

    vec3 displaced = localPos + vec3(0.0, displacement, 0.0);

    gl_Position = ProjMat * ModelViewMat * vec4(displaced, 1.0);

    pos = displaced + ColorModulator.xyz;
    vertexColor = Color;
}

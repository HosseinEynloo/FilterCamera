precision highp float;

uniform vec3                iResolution;
uniform float               iGlobalTime;
uniform sampler2D           iChannel0;
varying vec2                texCoord;

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{	
	float stongth = 0.9;
	vec2 uv = fragCoord.xy;
	float waveu = sin((uv.y + iGlobalTime) * 20.0) * 0.6 * 0.10 * stongth;
	fragColor = texture2D(iChannel0, uv + vec2(waveu, 0));
}

void main() {
	mainImage(gl_FragColor, texCoord);
}
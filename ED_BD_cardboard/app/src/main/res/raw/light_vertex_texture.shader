uniform mat4 u_Model;
uniform mat4 u_MVP;
uniform mat4 u_MVMatrix;
uniform vec3 u_LightPos;

attribute vec4 a_Position;
//attribute vec4 a_Color;
attribute vec3 a_Normal;
attribute vec2 a_TexCoordinate; // Per-vertex texture coordinate information we will pass in.

//varying vec4 v_Color;
varying vec3 v_Grid;
varying vec2 v_TexCoordinate;   // This will be passed into the fragment shader.

void main() {
   v_Grid = vec3(u_Model * a_Position);

   vec3 modelViewVertex = vec3(u_MVMatrix * a_Position);
   vec3 modelViewNormal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));

   float distance = length(u_LightPos - modelViewVertex);
   vec3 lightVector = normalize(u_LightPos - modelViewVertex);
   float diffuse = max(dot(modelViewNormal, lightVector), 0.5);

   diffuse = diffuse * (1.0 / (1.0 + (0.00001 * distance * distance)));
   //v_Color = a_Color * diffuse;

   // Pass through the texture coordinate.
   v_TexCoordinate = a_TexCoordinate;

   gl_Position = u_MVP * a_Position;
}

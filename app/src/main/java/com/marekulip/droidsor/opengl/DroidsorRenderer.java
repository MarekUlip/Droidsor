package com.marekulip.droidsor.opengl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.marekulip.droidsor.sensorlogmanager.SensorData;
import com.marekulip.droidsor.sensorlogmanager.SensorsEnum;

/**
 * Renderer used to draw 3D cube which shows how data from sensors look in space
 */
public class DroidsorRenderer implements GLSurfaceView.Renderer
{
    private static final String TAG = "DroidsorRenderer";

    /**
     * Data from sensors which should be applied on the cube
     */
    private SensorData data;


    /**
     * Store the model matrix. This matrix is used to move models from object space (where each model can be thought
     * of being located at the center of the universe) to world space.
     */
    private float[] mModelMatrix = new float[16];

    /**
     * Store the view matrix. This can be thought of as camera. This matrix transforms world space to eye space;
     * it positions things relative to eye.
     */
    private float[] mViewMatrix = new float[16];

    /** Store the projection matrix. This is used to project the scene onto a 2D viewport. */
    private float[] mProjectionMatrix = new float[16];

    /** Allocate storage for the final combined matrix. This will be passed into the shader program. */
    private float[] mMVPMatrix = new float[16];

    /** Store model data in a float buffer. */
    private final FloatBuffer mCubePositions;
    private final FloatBuffer mCubeColors;

    /** This will be used to pass in the transformation matrix. */
    private int mMVPMatrixHandle;

    /** This will be used to pass in the modelview matrix. */
    private int mMVMatrixHandle;

    /** This will be used to pass in model position information. */
    private int mPositionHandle;

    /** This will be used to pass in model color information. */
    private int mColorHandle;

    /** How many bytes per float. */
    private final int mBytesPerFloat = 4;

    /** Size of the position data in elements. */
    private final int mPositionDataSize = 3;

    /** Size of the color data in elements. */
    private final int mColorDataSize = 4;

    /** This is a handle to per-vertex cube shading program. */
    private int mPerVertexProgramHandle;

    /**
     * Initialize the model data.
     */
    DroidsorRenderer()
    {
        // Define points for a cube.
        // X, Y, Z
        final float[] cubePositionData =
                {
                        // In OpenGL counter-clockwise winding is default. This means that when looking at a triangle,
                        // if the points are counter-clockwise the "front" is seen. If not then
                        // back is seen. OpenGL has an optimization where all back-facing triangles are culled, since they
                        // usually represent the backside of an object and aren't visible anyways.

                        // Front face screen
                        -1.10f, 1.75f, 0.251f,
                        -1.10f, -1.75f, 0.251f,
                        1.10f, 1.75f, 0.251f,
                        -1.10f, -1.75f, 0.251f,
                        1.10f, -1.75f, 0.251f,
                        1.10f, 1.75f, 0.251f,

                        // Front face
                        -1.25f, 2.25f, 0.25f,
                        -1.25f, -2.25f, 0.25f,
                        1.25f, 2.25f, 0.25f,
                        -1.25f, -2.25f, 0.25f,
                        1.25f, -2.25f, 0.25f,
                        1.25f, 2.25f, 0.25f,

                        // Right face
                        1.25f, 2.25f, 0.25f,
                        1.25f, -2.25f, 0.25f,
                        1.25f, 2.25f, -0.1f,
                        1.25f, -2.25f, 0.25f,
                        1.25f, -2.25f, -0.1f,
                        1.25f, 2.25f, -0.1f,

                        // Back face
                        1.25f, 2.25f, -0.1f,
                        1.25f, -2.25f, -0.1f,
                        -1.25f, 2.25f, -0.1f,
                        1.25f, -2.25f, -0.1f,
                        -1.25f, -2.25f, -0.1f,
                        -1.25f, 2.25f, -0.1f,

                        // Left face
                        -1.25f, 2.25f, -0.1f,
                        -1.25f, -2.25f, -0.1f,
                        -1.25f, 2.25f, 0.25f,
                        -1.25f, -2.25f, -0.1f,
                        -1.25f, -2.25f, 0.25f,
                        -1.25f, 2.25f, 0.25f,

                        // Top face
                        -1.25f, 2.25f, -0.1f,
                        -1.25f, 2.25f, 0.25f,
                        1.25f, 2.25f, -0.1f,
                        -1.25f, 2.25f, 0.25f,
                        1.25f, 2.25f, 0.25f,
                        1.25f, 2.25f, -0.1f,

                        // Bottom face
                        1.25f, -2.25f, -0.1f,
                        1.25f, -2.25f, 0.25f,
                        -1.25f, -2.25f, -0.1f,
                        1.25f, -2.25f, 0.25f,
                        -1.25f, -2.25f, 0.25f,
                        -1.25f, -2.25f, -0.1f,
                };

        // R, G, B, A
        final float[] cubeColorData =
                {
                        // Front face (screen)
                        0.25f, 1f, 1f, 1.0f,
                        0.5f, 0.8156f, 0.9411f, 1.0f,
                        0.5f, 0.8156f, 0.9411f, 1.0f,
                        0.5f, 0.8156f, 0.9411f, 1.0f,
                        0.5f, 0.8156f, 0.9411f, 1.0f,
                        0.5f, 0.8156f, 0.9411f, 1.0f,

                        // Front face (frame)
                        0.1f, 0.1f, 0.1f, 1.0f,
                        0.1f, 0.1f, 0.1f, 1.0f,
                        0.1f, 0.1f, 0.1f, 1.0f,
                        0.1f, 0.1f, 0.1f, 1.0f,
                        0.1f, 0.1f, 0.1f, 1.0f,
                        0.1f, 0.1f, 0.1f, 1.0f,

                        // Right face
                        0.1882f, 0.1882f, 0.1882f, 1.0f,
                        0.1882f, 0.1882f, 0.1882f, 1.0f,
                        0.1882f, 0.1882f, 0.1882f, 1.0f,
                        0.1882f, 0.1882f, 0.1882f, 1.0f,
                        0.1882f, 0.1882f, 0.1882f, 1.0f,
                        0.1882f, 0.1882f, 0.1882f, 1.0f,

                        // Back face
                        0.1f, 0.1f, 0.1f, 1.0f,
                        0.1f, 0.1f, 0.1f, 1.0f,
                        0.1f, 0.1f, 0.1f, 1.0f,
                        0.1f, 0.1f, 0.1f, 1.0f,
                        0.1f, 0.1f, 0.1f, 1.0f,
                        0.1f, 0.1f, 0.1f, 1.0f,

                        // Left face
                        0.1882f, 0.1882f, 0.1882f, 1.0f,
                        0.1882f, 0.1882f, 0.1882f, 1.0f,
                        0.1882f, 0.1882f, 0.1882f, 1.0f,
                        0.1882f, 0.1882f, 0.1882f, 1.0f,
                        0.1882f, 0.1882f, 0.1882f, 1.0f,
                        0.1882f, 0.1882f, 0.1882f, 1.0f,

                        // Top face
                        0.1882f, 0.1882f, 0.1882f, 1.0f,
                        0.1882f, 0.1882f, 0.1882f, 1.0f,
                        0.1882f, 0.1882f, 0.1882f, 1.0f,
                        0.1882f, 0.1882f, 0.1882f, 1.0f,
                        0.1882f, 0.1882f, 0.1882f, 1.0f,
                        0.1882f, 0.1882f, 0.1882f, 1.0f,

                        // Bottom face
                        0.1882f, 0.1882f, 0.1882f, 1.0f,
                        0.1882f, 0.1882f, 0.1882f, 1.0f,
                        0.1882f, 0.1882f, 0.1882f, 1.0f,
                        0.1882f, 0.1882f, 0.1882f, 1.0f,
                        0.1882f, 0.1882f, 0.1882f, 1.0f,
                        0.1882f, 0.1882f, 0.1882f, 1.0f,
                };

        // Initialize the buffers.
        mCubePositions = ByteBuffer.allocateDirect(cubePositionData.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mCubePositions.put(cubePositionData).position(0);

        mCubeColors = ByteBuffer.allocateDirect(cubeColorData.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mCubeColors.put(cubeColorData).position(0);


    }

    private String getVertexShader()
    {

        return "uniform mat4 u_MVPMatrix;      \n"		// A constant representing the combined model/view/projection matrix.
                + "uniform mat4 u_MVMatrix;       \n"		// A constant representing the combined model/view matrix.

                + "attribute vec4 a_Position;     \n"		// Per-vertex position information passed in.
                + "attribute vec4 a_Color;        \n"		// Per-vertex color information passed in.

                + "varying vec4 v_Color;          \n"		// This will be passed into the fragment shader.

                + "void main()                    \n" 	// The entry point for vertex shader.
                + "{                              \n"
                // Transform the vertex into eye space.
                + "   vec3 modelViewVertex = vec3(u_MVMatrix * a_Position);              \n"
                // Transform the normal's orientation into eye space.
                + "   v_Color = a_Color;                                       \n"
                // gl_Position is a special variable used to store the final position.
                // Multiply the vertex by the matrix to get the final point in normalized screen coordinates.
                + "   gl_Position = u_MVPMatrix * a_Position;                            \n"
                + "}                                                                     \n";
    }

    private String getFragmentShader()
    {

        return "precision mediump float;       \n"		// Set the default precision to medium.
                + "varying vec4 v_Color;          \n"		// This is the color from the vertex shader interpolated across the
                // triangle per fragment.
                + "void main()                    \n"		// The entry point for fragment shader.
                + "{                              \n"
                + "   gl_FragColor = v_Color;     \n"		// Pass the color directly through the pipeline.
                + "}                              \n";
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config)
    {
        // Set the background clear color to almost white.
        GLES20.glClearColor(0.85f, 0.85f, 0.85f, 0.0f);

        // Use culling to remove back faces.
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Enable depth testing
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Position the eye in front of the origin.
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = -0.5f;

        // Looking toward the distance
        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = -5.0f;

        // Set up vector. This is where head would be pointing were camera is held.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        // Set the view matrix. This matrix can be said to represent the camera position.
        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
        // view matrix.
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

        final String vertexShader = getVertexShader();
        final String fragmentShader = getFragmentShader();

        final int vertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        final int fragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        mPerVertexProgramHandle = createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                new String[] {"a_Position",  "a_Color", "a_Normal"});
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height)
    {
        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height);

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        final float ratio = (float) width / height;
        final float left = -ratio;
        final float right = ratio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        final float near = 1.0f;
        final float far = 10.0f;

        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
    }

    @Override
    public void onDrawFrame(GL10 glUnused)
    {
        if(data!=null) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            GLES20.glUseProgram(mPerVertexProgramHandle);

            // Set program handles for cube drawing.
            mMVPMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_MVPMatrix");
            mMVMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_MVMatrix");
            mPositionHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Position");
            mColorHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Color");

            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -5.0f);
            if(data.sensorType == SensorsEnum.INTERNAL_GYROSCOPE.sensorType){
                int enhancer = 20;
                Matrix.rotateM(mModelMatrix, 0, (float)data.values.x * enhancer, 1.0f, 0.0f, 0.0f);
                Matrix.rotateM(mModelMatrix, 0, (float)data.values.y * enhancer, 0.0f, 1.0f, 0.0f);
                Matrix.rotateM(mModelMatrix, 0, (float)data.values.z * enhancer, 0.0f, 0.0f, 1.0f);
            } else if(data.sensorType == SensorsEnum.INTERNAL_ACCELEROMETER.sensorType){
                Matrix.rotateM(mModelMatrix, 0, (float)(data.values.y  / 0.0981f)*0.9f, 1.0f, 0.0f, 0.0f);
                Matrix.rotateM(mModelMatrix, 0, (float)(data.values.x  / 0.0981f)*0.9f*-1, 0.0f, 1.0f, 0.0f);
            }
            drawCube();
        }
    }

    /**
     * Draws a cube.
     */
    private void drawCube()
    {
        // Pass in the position information
        mCubePositions.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
                0, mCubePositions);

        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Pass in the color information
        mCubeColors.position(0);
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
                0, mCubeColors);

        GLES20.glEnableVertexAttribArray(mColorHandle);

        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

        // Pass in the modelview matrix.
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        // Pass in the combined matrix.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // Pass in the light position in eye space.
        //GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);

        // Draw the cube.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 42);
    }


    /**
     * Helper function to compile a shader.
     *
     * @param shaderType The shader type.
     * @param shaderSource The shader source code.
     * @return An OpenGL handle to the shader.
     */
    private int compileShader(final int shaderType, final String shaderSource)
    {
        int shaderHandle = GLES20.glCreateShader(shaderType);

        if (shaderHandle != 0)
        {
            // Pass in the shader source.
            GLES20.glShaderSource(shaderHandle, shaderSource);

            // Compile the shader.
            GLES20.glCompileShader(shaderHandle);

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0)
            {
                Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shaderHandle));
                GLES20.glDeleteShader(shaderHandle);
                shaderHandle = 0;
            }
        }

        if (shaderHandle == 0)
        {
            throw new RuntimeException("Error creating shader.");
        }

        return shaderHandle;
    }

    /**
     * Helper function to compile and link a program.
     *
     * @param vertexShaderHandle An OpenGL handle to an already-compiled vertex shader.
     * @param fragmentShaderHandle An OpenGL handle to an already-compiled fragment shader.
     * @param attributes Attributes that need to be bound to the program.
     * @return An OpenGL handle to the program.
     */
    private int createAndLinkProgram(final int vertexShaderHandle, final int fragmentShaderHandle, final String[] attributes)
    {
        int programHandle = GLES20.glCreateProgram();

        if (programHandle != 0)
        {
            // Bind the vertex shader to the program.
            GLES20.glAttachShader(programHandle, vertexShaderHandle);

            // Bind the fragment shader to the program.
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);

            // Bind attributes
            if (attributes != null)
            {
                final int size = attributes.length;
                for (int i = 0; i < size; i++)
                {
                    GLES20.glBindAttribLocation(programHandle, i, attributes[i]);
                }
            }

            // Link the two shaders together into a program.
            GLES20.glLinkProgram(programHandle);

            // Get the link status.
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

            // If the link failed, delete the program.
            if (linkStatus[0] == 0)
            {
                Log.e(TAG, "Error compiling program: " + GLES20.glGetProgramInfoLog(programHandle));
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }

        if (programHandle == 0)
        {
            throw new RuntimeException("Error creating program.");
        }

        return programHandle;
    }

    public void setData(SensorData data){
        this.data = data;
    }


}

package com.example.dcrocco.sensorsmovement;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class MainActivity extends Activity  {

    //Sensores
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private Sensor senMagneticField;

    //MAGNETOMETRO Y ACELEROMETRO
    float[] mGravity;
    float[] mGeomagnetic;
    float pitch = 0;
    float roll = 0;
    float newPitch = 0;
    float newRoll = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senMagneticField = senSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        //CUBO Y MOVIMIENTO
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        GLSurfaceView view = new GLSurfaceView(this);
        view.setRenderer(new OpenGLRenderer());
        setContentView(view);
    }

    private float restrictAngle(float tmpAngle){
        while(tmpAngle>=180) tmpAngle-=360;
        while(tmpAngle<-180) tmpAngle+=360;
        return tmpAngle;
    }

    private float calculateFilteredAngle(float x, float y){
        final float alpha = 0.3f;
        float diff = x-y;

        //Nos aseguramos que la dif sea <= a 180
        diff = restrictAngle(diff);

        y += alpha*diff;
        //Nos aseguramos que Y quede entre [-180 y 180]
        y = restrictAngle(y);

        return y;
    }

    SensorEventListener listener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int acc) { }

        public void onSensorChanged(SensorEvent event) {

            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                mGravity = event.values;
            }

            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                mGeomagnetic = event.values;
            }

            //LUEGO DE TOMAR LOS VALORES DE LOS SENSORES, SETEAMOS LOS VALORES PITCH Y ROLL
            //PARA UTILIZARLOS EN LAS ROTACIONES
            if (mGravity != null && mGeomagnetic != null) {
                float R[] = new float[9];
                float I[] = new float[9];
                boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                if (success) {
                    float orientation[] = new float[3];
                    SensorManager.getOrientation(R, orientation);

                    newPitch= -(float) (( Math.toDegrees(orientation[1])));
                    newRoll = (float) (( Math.toDegrees(orientation[2])));

                    pitch = calculateFilteredAngle(newPitch, pitch);
                    roll = calculateFilteredAngle(newRoll, roll);

                }
            }
        }
    };


    public void onStop() {
        super.onStop();
        senSensorManager.unregisterListener(listener);

    }

    protected void onResume() {
        super.onResume();
        senSensorManager.registerListener(listener, senAccelerometer,
                SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(listener, senMagneticField,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    //CREAMOS UNA CLASE PARA DIBUJAR EL CUBO EN CADA FRAME, CON LOS VALORES ACTUALIZADOS DE
    //PITCH Y ROLL, DE ESTA FORM CREAMOS ROTACION EN EL CUBO
    class OpenGLRenderer implements GLSurfaceView.Renderer {

        private Cube mCube = new Cube();

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f);

            gl.glClearDepthf(1.0f);
            gl.glEnable(GL10.GL_DEPTH_TEST);
            gl.glDepthFunc(GL10.GL_LEQUAL);

            gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                    GL10.GL_NICEST);

        }

        @Override
        public void onDrawFrame(GL10 gl) {
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
            gl.glLoadIdentity();

            //ROTAMOS EL CUBO SEGUN LOS VALORES PITCH Y ROLL
            gl.glTranslatef(0.0f, 0.0f, -10.0f);
            gl.glRotatef(roll, 0f, 1f, 0f);
            gl.glRotatef(pitch, 1f, 0f, 0f);

            mCube.draw(gl);

            gl.glLoadIdentity();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            gl.glViewport(0, 0, width, height);
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            GLU.gluPerspective(gl, 45.0f, (float)width / (float)height, 0.1f, 100.0f);
            gl.glViewport(0, 0, width, height);

            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadIdentity();
        }
    }


    // UTILIZAMOS OPENGL PARA CREAR LA CLASE "CUBO" Y SUS ATRIBUTOS
    class Cube {

        private FloatBuffer mVertexBuffer;
        private FloatBuffer mColorBuffer;
        private ByteBuffer  mIndexBuffer;

        private float vertices[] = {
                -1.0f, -1.0f, -1.0f,
                1.0f, -1.0f, -1.0f,
                1.0f,  1.0f, -1.0f,
                -1.0f, 1.0f, -1.0f,
                -1.0f, -1.0f,  1.0f,
                1.0f, -1.0f,  1.0f,
                1.0f,  1.0f,  1.0f,
                -1.0f,  1.0f,  1.0f
        };

        //Seteamos los colres del cubo, RGB y opacidad
        private float colors[] = {
                0.0f,  1.0f,  0.0f,  1.0f,
                0.0f,  1.0f,  0.0f,  1.0f,
                1.0f,  0.5f,  0.0f,  1.0f,
                1.0f,  0.5f,  0.0f,  1.0f,
                1.0f,  0.0f,  0.0f,  1.0f,
                1.0f,  0.0f,  0.0f,  1.0f,
                0.0f,  0.0f,  1.0f,  1.0f,
                1.0f,  0.0f,  1.0f,  1.0f
        };

        private byte indices[] = {
                0, 4, 5, 0, 5, 1,
                1, 5, 6, 1, 6, 2,
                2, 6, 7, 2, 7, 3,
                3, 7, 4, 3, 4, 0,
                4, 7, 6, 4, 6, 5,
                3, 0, 1, 3, 1, 2
        };

        public Cube() {
            ByteBuffer byteBuf = ByteBuffer.allocateDirect(vertices.length * 4);
            byteBuf.order(ByteOrder.nativeOrder());
            mVertexBuffer = byteBuf.asFloatBuffer();
            mVertexBuffer.put(vertices);
            mVertexBuffer.position(0);

            byteBuf = ByteBuffer.allocateDirect(colors.length * 4);
            byteBuf.order(ByteOrder.nativeOrder());
            mColorBuffer = byteBuf.asFloatBuffer();
            mColorBuffer.put(colors);
            mColorBuffer.position(0);

            mIndexBuffer = ByteBuffer.allocateDirect(indices.length);
            mIndexBuffer.put(indices);
            mIndexBuffer.position(0);
        }

        public void draw(GL10 gl) {
            gl.glFrontFace(GL10.GL_CW);

            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
            gl.glColorPointer(4, GL10.GL_FLOAT, 0, mColorBuffer);

            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

            gl.glDrawElements(GL10.GL_TRIANGLES, 36, GL10.GL_UNSIGNED_BYTE,
                    mIndexBuffer);

            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
        }
    }


}

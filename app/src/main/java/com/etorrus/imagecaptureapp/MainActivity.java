package com.etorrus.imagecaptureapp;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraDevice;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    //Объявлям TextureView
    private TextureView mTextureView;
    //Этот прослушиватель может использоваться для уведомления, когда доступна текстура поверхности, связанная с этим видом текстуры.
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        //Вызывается, когда поверхность TextureViewSurfaceTexture готова к использованию.
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            //пока не привязан preview, будет показываться тост
            Toast.makeText(getApplication(), "TextureView доступен", Toast.LENGTH_SHORT).show();
        }
        //Вызывается, когда SurfaceTextureизменяется размер буфера.
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }
        //Вызывается, когда указанный объект SurfaceTextureдолжен быть уничтожен.
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }
        //Вызывается, когда указанное SurfaceTextureобновление обновляется updateTexImage().
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    //Класс CameraDevice представляет собой единую камеру, подключенную к устройству Android,
    // позволяющую осуществлять мелкозернистый контроль захвата изображения и последующей
    // обработки при высоких частотах кадров.
    private CameraDevice mCameraDevice;

    //Объекты обратного вызова для получения обновлений о состоянии устройства камеры.
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {

        //Метод, вызываемый при завершении открытия камеры.
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
        }

        //Этот метод называется, когда устройство камеры больше не доступно для использования.
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        //Метод вызван, когда устройство камеры обнаружило серьезную ошибку.
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Находим textureView и присваеваем mTextureView
        mTextureView = (TextureView) findViewById(R.id.textureView);
    }

    //onResume () вызывается всякий раз, когда вы возвращаетесь к активности из вызова или чего-то еще.
    //без него прога будет падать если свернуть ее и вернуться обратно
    @Override
    protected void onResume() {
        super.onResume();

        //если mTextureView не доступна, то сделать ее доступной
        if(mTextureView.isAvailable()) {

        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    // Activity находится в состоянии «Приостановлено» (программа свёрнута)
    @Override
    protected void onPause() {

        //Вызываем метод закрытия mCameraDevice, чтоб камерой могли пользоваться другие проги
        closeCamera();

        super.onPause();
    }

    /*Пока туплю, но вроде как, этот метод нужен для определения момента получения фокуса
    нашем приложением
    Срабатывает позже метода onCreate()*/
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        if(hasFocus) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE //При использовании других флагов макета нам нужен стабильный вид вложенных вложений
                    /*View хотел бы оставаться интерактивным при скрытии панели навигации SYSTEM_UI_FLAG_HIDE_NAVIGATION.
                    Если этот флаг не установлен, SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    будет принудительно очищена системой при любом взаимодействии с пользователем.
                    Поскольку этот флаг является модификатором SYSTEM_UI_FLAG_HIDE_NAVIGATION,
                    он имеет эффект только при использовании в сочетании с этим флагом.*/
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    //View хотел бы, чтобы его окно было выложено, как если бы оно было запрошено
                //| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    //View хотел бы, чтобы его окно было выложено, как если бы оно было запрошено
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    //полноэкранный режим
                //| View.SYSTEM_UI_FLAG_FULLSCREEN
                    //на устройствах, которые нарисовывают основные элементы навигации (Home, Back и т. п.)
                    // на экране, SYSTEM_UI_FLAG_HIDE_NAVIGATION заставят их исчезнуть
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    //Метод закрытия mCameraDevice
    private void closeCamera() {
        if(mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }
}

package com.etorrus.imagecaptureapp;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    //Объявлям TextureView
    private TextureView mTextureView;
    //Этот прослушиватель может использоваться для уведомления, когда доступна текстура поверхности, связанная с этим видом текстуры.
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        //Вызывается, когда поверхность TextureViewSurfaceTexture готова к использованию.
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

            //Для setupCamera в качестве аргументов идут высота и широта, ОТКУДА ОНИ БЕРУТСЯ НУЖНО ВЫЯСНИТЬ
            setupCamera(width, height);
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

    //Удобный класс для запуска нового потока, в котором есть looper.
    // Затем looper можно использовать для создания классов обработчиков.
    // Обратите внимание, что start () еще нужно вызвать.
    private HandlerThread mBackgroundHandlerThread;

    private Handler mBackgroundHandler;
    private String mCameraId;

    //SparseIntArray предназначен для большей эффективности памяти, чем использование HashMap для сопоставления целых чисел с целыми
    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        //Постоянная вращения: 0 градусов (естественная ориентация)
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        //Постоянная вращения: 90 градусов
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        //Постоянная вращения: 180 градусов
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        //Постоянная вращения: 270 градусов
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

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

        //Запускаем поток
        startBackgroundThread();

        //если mTextureView не доступна, то сделать ее доступной
        if(mTextureView.isAvailable()) {
            //если mTextureView доступна, то вызываем метод setupCamera и подставляем высоту и
            //широту такую же как у mTextureView
            setupCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    // Activity находится в состоянии «Приостановлено» (программа свёрнута)
    @Override
    protected void onPause() {

        //Вызываем метод закрытия mCameraDevice, чтоб камерой могли пользоваться другие проги
        closeCamera();

        //Останавливаем поток
        stopBackgroundThread();

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

    //Настройки камеры
    private void setupCamera(int width, int height) {

        //Диспетчер системных служб для обнаружения, характеризации и подключения CameraDevices.
        //Экземпляры этого класса должны быть получены Context.getSystemService(String)аргумента Context.CAMERA_SERVICE.
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        //один раз получил менеджер камеры камеры и объект менеджера камеры, мы можем получить список
        // идентификаторов камеры
        try {
            for(String cameraId : cameraManager.getCameraIdList()) {

                //Свойства, описывающие  CameraDevice.
                // Эти свойства фиксируются для данной CameraDevice и могут быть запрошены через CameraManager интерфейс getCameraCharacteristics(String).
                // CameraCharacteristics объекты неизменяемы.
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);

                //Если LENS_FACING_FRONT Устройство камеры находится в том же направлении, что и экран устройства.
                //то continue, т.к. я хочу использовать заднюю камеру (ТУТ ВОЗМОЖНО Я НАТУПИЛ)
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                //Присваеваем deviceOrientation - поворот экрана из его «естественной» ориентации.
                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();

                int totalRotation = sensorToDeviceRotation(cameraCharacteristics, deviceOrientation);
                boolean swapRotation = totalRotation == 90 || totalRotation == 270;
                int rotatedWidth = width;
                int rotatedHeight = height;
                if(swapRotation) {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }

                //cameraId это просто порядковый номер камеры
                mCameraId = cameraId;
                return;

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //Метод закрытия mCameraDevice
    private void closeCamera() {
        if(mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    //Метод запуска потока
    private void startBackgroundThread() {

        //Создаем новый поток
        mBackgroundHandlerThread = new HandlerThread("ImageCaptureApp");
        //Запускаем его
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }


    //Метод остановки потока
    private void stopBackgroundThread() {
        //Безопасно завершает работу looper нити обработчика.
        mBackgroundHandlerThread.quitSafely();
        try {

            //Ожидает, что эта thread умрет, этот метод унаследован от java.lang.Thread
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        //Угол по часовой стрелке, по которому необходимо поворачивать выходное изображение,
        // чтобы оно было вертикально на экране устройства в его естественной ориентации.
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        //ORIENTATIONS.get(deviceOrientation) возвращает int, deviceOrientation является ключом, или 0 если такое отображение не было сделано.
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        //Прибавляем 360 для того чтоб не получилось отрицательного числа, но это не точно
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }
}

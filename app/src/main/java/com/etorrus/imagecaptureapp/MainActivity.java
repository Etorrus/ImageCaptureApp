package com.etorrus.imagecaptureapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    //View хотел бы, чтобы его окно было выложено, как если бы оно было запрошено
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    //полноэкранный режим
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                    //на устройствах, которые нарисовывают основные элементы навигации (Home, Back и т. п.)
                    // на экране, SYSTEM_UI_FLAG_HIDE_NAVIGATION заставят их исчезнуть
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }
}

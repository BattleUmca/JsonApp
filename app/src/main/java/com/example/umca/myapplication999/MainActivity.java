package com.example.umca.myapplication999;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    public static final String MyUrl = "https://cadourionline.md/ws/checker.php?pass=";  //переменая с нашей сылкой
    public static final String MyPass = "CdRR3jb3gFaVLVtBIS5bAn8Ci097vq55p6Z9Ktcp";    // переменная с паролем к нашей сылке
    public static HashMap<Integer, Date> IdMap;                                      //в данной коллекции будут хранится наши заказы(номер заказ/дата получения заказа)
    public static ArrayList <Integer> lastOrders;

    public static NotificationManager nm; // создаем переменую для работы с NotificationManager
    private final int NOTIFICATION_ID = 127; //уникальная переменная для уведомлениий

    public static CameraManager mCameraManager = null;  //переменая для работы с камера менеджер
    public static String cameraId;
    public boolean tourchFlag=true;

    public static SoundPool mSoundPool;
    public static int mStreamId;

    public static Button button1;
   public static TextView textView;
   public static String OrderList;
    public static boolean visible=false;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.textField);
        button1=(Button) findViewById(R.id.button2) ;

       if(!visible) {button1.setVisibility(View.INVISIBLE);}
       else {button1.setVisibility(View.VISIBLE);textView.setText("Новые заказы:"+OrderList);}     //в случае если флаг видимости станет тру (прийдет уведомление) то показываем кнопку и список заказов
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    cancelNotification();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        });

        nm = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE); //инициализируем NotificationManage
        IdMap = new HashMap<>();
        lastOrders=new ArrayList<>();

        new ParseTask().execute();
    }

    private class ParseTask extends AsyncTask<Void, Boolean, String> {

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String resultJson = "";


        @Override
        protected String doInBackground(Void... params) {
            String str = "";
            JSONObject dataJsonObj = null;
            URL url = createUrl(MyUrl + MyPass);
            // получаем данные с внешнего ресурса
            while (true) {

                try {

                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    InputStream inputStream = urlConnection.getInputStream();
                    StringBuffer buffer = new StringBuffer();

                    reader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                    }

                    resultJson = buffer.toString();

                } catch (Exception e) {
                    e.printStackTrace();
                }


                try {

                    try {
                        String string = "{\"hasOrders\":true,\"ordersIds\":[\"14283\",\"14285\",\"14286\",\"14287\"]}";
                        dataJsonObj=new JSONObject(string);

                       // dataJsonObj = new JSONObject(resultJson);
                        DateControl();
                        str += dataJsonObj.get("hasOrders").toString();

                        if (str.equals("true")) {

                            /// если Джейсон масив содержит тру, получаем ид заказов
                            findId(dataJsonObj);
                            publishProgress(true); //запускаем метод для работы  Уй не прерывая поток

                        }


                    } catch (JSONException je) {

                    }
                    TimeUnit.SECONDS.sleep(10); //спим 10 минут

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }


        }

        protected void onProgressUpdate(Boolean... hasOrders) {


            OrderList=lastOrders.toString();


            if (hasOrders[0] == true) {
                visible=true;
                showNotification();                 //показываем уведомление, включаем музыку и фонарик


               cameraTourch();
                muzicLoop();                          // включаем музыку

            }

        }


        protected void onPostExecute(String strJson) {
        }

    }

    public void findId(JSONObject jsonObject) throws JSONException {
        int id = 0;
        int leng = 0;
        JSONArray idOrders = jsonObject.getJSONArray("ordersIds");
        leng = idOrders.length();
        lastOrders=new ArrayList<>();
        if (idOrders.length() > 0)

        {
            for (int j = 0; j < leng; j++) {


                id = Integer.parseInt(idOrders.get(j).toString());//получаем ид заказа
                if (!IdMap.containsKey(id))     // если заказа небыло в нашем списке то заносим и увелчиваем счетчик новых заказов

                {
                    IdMap.put(id, new Date(System.currentTimeMillis()));
                    lastOrders.add(id);


                }
            }


        }
    }

    //создаем сылку из нашей строки для доступа к Джейсон массиву
    public static URL createUrl(String link) {
        try {
            return new URL(link);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // проходимся по коллекции заказов и если заказу больше 1 дня удаляем его
    public void DateControl() {


        HashMap map = IdMap;
        Calendar calendar = Calendar.getInstance();
        Date finDate; // сдесь будем хранить даты из коллекции +1 день, для сравненя с текущей датой
        Date curentDate = new Date(System.currentTimeMillis()); //текущая дата
        int flag; // флаг в котором будет хранится разница между текущей датой и финальной


        Iterator<Map.Entry<Integer, Date>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Date> pair = iterator.next();
            Date value = pair.getValue();
            calendar.setTime(value);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            finDate = calendar.getTime();
            flag = curentDate.compareTo(finDate);
            if (flag >= 0) iterator.remove();


        }


    }

    // Создаем метод который будет кидать уведомления
    public void showNotification() {

        Notification.Builder builder = new Notification.Builder(getApplicationContext());//для настройки  параметров уведомления используем билдер, инициализируем его
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);// создаем интент, для обращения к нашему активити
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);//FLAG_CANCEL_CURRENT - указывает что надо закрыь ранее созданный интент, чтобы открыть новый.

        builder
                .setContentIntent(pendingIntent) //параметр который позволяет переходить в наше активити
                .setSmallIcon(R.mipmap.ic_launcher) //указываем маленькую иконку(при свернутом трее)
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.mipmap.ic_launcher)) // указываем большую иконку(при развернутом трее) путем преобразования маленькой в большую
                .setTicker("У вас новый заказ на сайте") /// устанавливаем заголовок уведомления при свернутом трее
                .setWhen(System.currentTimeMillis()) // в уведомлении указываем время получения уведомления
                .setAutoCancel(false) //автозакрытие уведомления после нажатия по нему
                .setContentTitle("Новый Заказ") // заголовок уведомления при развернутом трее(Тема)
                .setContentText("Нажмите чтобы открыть уведомление");// текст уведомления


        Notification notification = builder.build();// собираем уведомление(с помощью метода билд)


        nm.notify(NOTIFICATION_ID, notification); // показываем уведомление


    }

    public void cancelNotification() throws CameraAccessException {

        nm.cancelAll();// закрываем уведомление по ид
        mSoundPool.stop(mStreamId);  //останавливаем музыку
        tourchFlag=false;
        mCameraManager.setTorchMode(MainActivity.cameraId, false); // выключаем фонарик

        finish();



    }



    //Метод для зажигания фонарика
    public  void cameraTourch()    {
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE); // получаем экземпляр
        try {
            // Получения списка камер в устрйстве
            String[] cameraList = mCameraManager.getCameraIdList();
            //проходимся по списку устройств и включаем фонарик на первой камере(если он есть)
            for (String cameraID : cameraList) {


            }
            Boolean isFlashAvailable = getApplicationContext().getPackageManager()
                    .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
            if (isFlashAvailable) {


                    mCameraManager.setTorchMode(cameraList[0], true);



            }



        } catch (CameraAccessException e) {

            e.printStackTrace();

        }

    }
    public Boolean cameraSwitch()
    {
        boolean cameraFlag=tourchFlag;
        return cameraFlag;
    }

    // пишем метод для воспроизведения звукового файла в петле
    public void muzicLoop() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();


        mSoundPool = new SoundPool.Builder()
                .setAudioAttributes(audioAttributes)
                .setMaxStreams(1)
                .build();
        mStreamId = mSoundPool.load(this, R.raw.alarmclock, 1);      // загружаем файл в поток и получаем его ид
        mSoundPool.play(mStreamId, 1, 1, 1, -1, 1); // проигрываем музыку в петле
    }


}

package com.example.android.weather;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.R.attr.name;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.T;
import static com.example.android.weather.R.id.location;

public class MainActivity extends AppCompatActivity {
    final String API_KEY = "3p8mqv0gegi6yff3";

    TextView temperature_now,text;
    ListView listView;
    Activity activity;
    String theLocation;
    SwipeRefreshLayout refreshLayout;
    Toolbar toolbar;
    SQLiteDatabase db;
    Cursor cursor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        temperature_now = (TextView)findViewById(R.id.temperature_now);
        text = (TextView)findViewById(R.id.text);
        listView = (ListView)findViewById(R.id.list);
        toolbar = (Toolbar)findViewById(R.id.theToolBar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        theLocation= this.getPreferences(Context.MODE_PRIVATE).
                getString("theLocation","Beijing");
        toolbar.setTitle(theLocation);
        db = openOrCreateDatabase("weathers",MODE_PRIVATE,null);
        initDataBase();

        cursor = db.rawQuery("select * from weathers;",null);
        cursor.moveToFirst();
        temperature_now.setText(" "+cursor.getString(1)+"° ");
        text.setText(cursor.getString(2));

        listView.setAdapter(new WeatherAdapter(activity,getNextFromDataBase()));
        refreshLayout = (SwipeRefreshLayout)findViewById(R.id.Refresh_layout);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshToday();
                refreshNextDays();
            }
        });
        refreshToday();
        refreshNextDays();
    }
    private TodayWeather parseToday(String jsonString){
        TodayWeather todayWeather = new TodayWeather();

        try{
            JSONObject today = new JSONObject(jsonString);
            JSONArray results = today.getJSONArray("results");
            JSONObject location = results.getJSONObject(0).getJSONObject("location");
            JSONObject now = results.getJSONObject(0).getJSONObject("now");
            todayWeather.last_update = results.getJSONObject(0).getString("last_update");
            todayWeather.name = location.getString("name");
            todayWeather.code = now.getString("code");
            todayWeather.text = now.getString("text");
            todayWeather.temperature = now.getString("temperature");



        }catch (Exception e){
            Toast.makeText(getApplicationContext(),
                    e.getMessage(),
                    Toast.LENGTH_SHORT)
                    .show();
        }
        return todayWeather;
    }
    private ArrayList<DailyWeather> parseDays(String jsonString){
        ArrayList<DailyWeather> weathers = new ArrayList<>();
        try {
            JSONArray results = new JSONObject(jsonString).getJSONArray("results");
            JSONObject location = results.getJSONObject(0).getJSONObject("location");
            JSONArray daily = results.getJSONObject(0).getJSONArray("daily");
            String last_update = results.getJSONObject(0).getString("last_update");
            for (int i = 0; i < 3; i++) {
                JSONObject day = daily.getJSONObject(i);
                DailyWeather weather = new DailyWeather();
                weather.date = day.getString("date");
                weather.text_day = day.getString("text_day");
                weather.code_day = day.getString("code_day");
                weather.code_night = day.getString("code_night");
                weather.high = day.getString("high");
                weather.low = day.getString("low");
                weather.precip = day.getString("precip");
                weather.wind_direction = day.getString("wind_direction");
                weather.wind_direction_degree = day.getString("wind_direction_degree");
                weather.wind_scale  =day.getString("wind_scale");
                weather.wind_speed = day.getString("wind_speed");
                weather.week_day = getWeekDayFormInt(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)+i);
                weathers.add(weather);
            }

        }catch (Exception e){
            Toast.makeText(getApplicationContext(),
                    e.getMessage(),
                    Toast.LENGTH_SHORT)
                    .show();
        }

        return weathers;

    }
    private void refreshToday(){
        OkHttpClient client = new OkHttpClient();
        refreshLayout.setRefreshing(true);
        Request requestToday = new Request.Builder()
                .url("https://api.thinkpage.cn/v3/weather/now.json?key=" +
                        API_KEY +
                        "&location=" +
                        theLocation +
                        "&language=en&unit=c")
                .build();
        Call callToday = client.newCall(requestToday);
        callToday.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Toast.makeText(getApplicationContext(),
                        e.getMessage(),
                        Toast.LENGTH_SHORT)
                        .show();
            }
            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                final String s = response.body().string();
                final TodayWeather todayWeather =parseToday(s);
                activity.getPreferences(MODE_PRIVATE).edit().putString("theLocation",todayWeather.name).commit();

                db.execSQL("update weathers set temperature_now ='" +
                        todayWeather.temperature +
                        "',theText='" +
                        todayWeather.text +
                        "' where _id=0");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        temperature_now.setText(" "+todayWeather.temperature+"° ");
                        text.setText(todayWeather.text);
                        refreshLayout.setRefreshing(false);
                        toolbar.setTitle(todayWeather.name);
                    }
                });
            }
        });
    }
    private void refreshNextDays(){

        OkHttpClient client = new OkHttpClient();
        Request requestNext = new Request.Builder()
                .url("https://api.thinkpage.cn/v3/weather/daily.json?key=" +
                        API_KEY +
                        "&location=" +
                        theLocation +
                        "&language=en&unit=c&start=0&days=5")
                .build();

        Call callNext = client.newCall(requestNext);
        callNext.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Toast.makeText(getApplicationContext(),
                        e.getMessage(),
                        Toast.LENGTH_SHORT)
                        .show();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                final String s = response.body().string();
                final ArrayList<DailyWeather> weathers = parseDays(s);
                for (int i = 0; i < 3; i++) {
                    DailyWeather  weather = weathers.get(i);
                    db.execSQL("update weathers set high='" +
                            weather.high +
                            "',low='" +
                            weather.low +
                            "',theText='" +
                            weather.text_day +
                            "',weekDay='" +
                            weather.week_day +
                            "' where _id=" +
                            (i+1) +
                            "");
                }
                final WeatherAdapter adapter = new WeatherAdapter(activity,weathers);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listView.setAdapter(adapter);
                        refreshLayout.setRefreshing(false);
                    }
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNextDays();
        refreshToday();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case location:
                new MaterialDialog.Builder(this)
                        .title("Location")
                        .content("Input the location.")
                        .input(null, theLocation, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(MaterialDialog dialog, CharSequence input) {
                                theLocation = ""+input;
                                refreshToday();
                                refreshNextDays();
                            }
                        }).show();
        }

        return true;
    }

    private void initDataBase(){
        if (db.rawQuery("select * from sqlite_master",null).getCount()==1){
            db.execSQL("CREATE TABLE weathers(_id INTEGER,temperature_now TEXT,theText TEXT,high TEXT,low TEXT ,weekDay TEXT);");
            for (int i = 0; i < 4; i++) {
                db.execSQL("INSERT INTO weathers VALUES(" +
                        i +
                        ",'0','Unkown','0','0','Weekday');");

            }

        }
    }
    private String getWeekDayFormInt(int weekDay){
        switch (weekDay%7){
            case 1: return "Monday";
            case 2: return "Tuesday";
            case 3: return "Wednesday";
            case 4: return "Thursday";
            case 5: return "Friday";
            case 6: return "Saturday";
            case 0: return "Sunday";
        }
        return "";
    }
    private ArrayList<DailyWeather> getNextFromDataBase(){
        ArrayList<DailyWeather> weathers = new ArrayList<>();
        cursor.moveToFirst();
        for (int i = 0; i < 3; i++) {
            cursor.moveToNext();
            DailyWeather weather = new DailyWeather();
            weather.text_day = cursor.getString(2);
            weather.high = cursor.getString(3);
            weather.low = cursor.getString(4);
            weather.week_day = cursor.getString(5);
            weathers.add(weather);
        }
        return weathers;


    }

}

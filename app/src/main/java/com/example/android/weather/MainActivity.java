package com.example.android.weather;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.R.attr.name;

public class MainActivity extends AppCompatActivity {
    final String API_KEY = "3p8mqv0gegi6yff3";

    TextView location,temperature_now,text;
    ListView listView;
    Activity activity;
    SwipeRefreshLayout refreshLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;

        location = (TextView)findViewById(R.id.location);
        temperature_now = (TextView)findViewById(R.id.temperature_now);
        text = (TextView)findViewById(R.id.text);
        listView = (ListView)findViewById(R.id.list);
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
        Request requestToday = new Request.Builder()
                .url("https://api.thinkpage.cn/v3/weather/now.json?key=" +
                        API_KEY +
                        "&location=yantai&language=zh-Hans&unit=c")
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

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        location.setText(todayWeather.name);
                        temperature_now.setText(" "+todayWeather.temperature+"° ");
                        text.setText(todayWeather.text);
                        refreshLayout.setRefreshing(false);

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
                        "&location=yantai&language=zh-Hans&unit=c&start=0&days=5")
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
}

package com.example.android.weather;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;


/**
 * Created by x on 17-1-31.
 */

public class WeatherAdapter extends ArrayAdapter<DailyWeather> {

    public WeatherAdapter(Activity context, ArrayList<DailyWeather> weathers){
        super(context,0,weathers);

    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView = convertView;

        if (itemView==null){
            itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_layout,parent,false);
        }
        DailyWeather weather = getItem(position);
        TextView high_low = (TextView)itemView.findViewById(R.id.high_low);
        TextView text_day = (TextView)itemView.findViewById(R.id.text_day);
        TextView day_of_week = (TextView)itemView.findViewById(R.id.day_of_week);
        day_of_week.setText(weather.week_day);
        high_low.setText(weather.high+"°/"+weather.low+"° ");
        text_day.setText(weather.text_day);

        return itemView;
    }

}

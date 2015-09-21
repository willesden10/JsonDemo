package gab.jsondemo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;


//AsyncTask enables proper and easy use of the UI thread. This class allows to perform background operations
// and publish results on the UI thread without having to manipulate threads and/or handlers.
public class FetchData extends AsyncTask<String, Void, Boolean> {
    private static final String TAG = FetchData.class.toString();

    private Activity mActivity;
    private String mCountry,mCity, mTemp, mMin, mMax, mSpeed, mWeather, mWeatherDescription, mSunrise, mSunset, mIcon;
    private Bitmap mBitmap;

    FetchData(Activity activity){
        mActivity = activity;
    }

    @Override
    protected Boolean doInBackground(String... params) {

        //An URLConnection for HTTP (RFC 2616) used to send and receive data over the web. Data may be of any type and length.
        //This class may be used to send and receive streaming data whose length is not known in advance.
        HttpURLConnection httpURLConnection = null;
        BufferedReader bufferedReader = null;
        InputStream inputStreamBitmap = null;

        try {

            URL url = new URL(params[0]);

            httpURLConnection = (HttpURLConnection) url.openConnection();
            //Sets the request command which will be sent to the remote HTTP server. This method can only be called before the connection is made.
            httpURLConnection.setRequestMethod("GET");

            //Returns an InputStream for reading data from the resource pointed by this URLConnection.
            // It throws an UnknownServiceException by default. This method must be overridden by its subclasses.
            InputStream inputStream = httpURLConnection.getInputStream();

            //Wraps an existing Reader and buffers the input.
            //Expensive interaction with the underlying reader is minimized, since most (smaller) requests can be satisfied by accessing the buffer alone.
            // The drawback is that some extra space is required to hold the buffer and that copying takes place when filling that buffer,
            // but this is usually outweighed by the performance benefits.
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            //A modifiable sequence of characters for use in creating strings.
            //This class is intended as a direct replacement of StringBuffer for non-concurrent use;
            //unlike StringBuffer this class is not synchronized.
            StringBuilder stringBuilder = new StringBuilder();
            String line;

            while((line = bufferedReader.readLine()) != null){
                stringBuilder.append(line+"\n");
            }

            if(stringBuilder.length() == 0) return false;

            //if there hasn't been any exception buildJson get the data fields from a Json object.
            buildJson(stringBuilder.toString());

            //Getting the weather icon;
            URL urlBitmap = new URL("http://openweathermap.org/img/w/"+mIcon+".png");
            inputStreamBitmap = urlBitmap.openStream();
            mBitmap = BitmapFactory.decodeStream(inputStreamBitmap);

            return true;
        }

        catch(MalformedURLException exc){
            exc.printStackTrace(System.err);
        }
        catch(ProtocolException exc){
            exc.printStackTrace(System.err);
        }
        catch(JSONException exc){
            exc.printStackTrace();
        }
        catch(IOException exc){
            exc.printStackTrace(System.err);
        }



        finally {
            if(httpURLConnection != null)
                httpURLConnection.disconnect();
            if(bufferedReader != null){
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(inputStreamBitmap != null){
                try {
                    inputStreamBitmap.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean fetchOk) {

        if (fetchOk.booleanValue()) {
            TextView location = (TextView) mActivity.findViewById(R.id.location);
            location.setText(mCity+","+mCountry);

            TextView temp = (TextView) mActivity.findViewById(R.id.temp);
            temp.setText(mTemp);

            TextView min = (TextView) mActivity.findViewById(R.id.min);
            min.setText("min: "+mMin);

            TextView max = (TextView) mActivity.findViewById(R.id.max);
            max.setText("max: "+mMax);

            TextView wind = (TextView) mActivity.findViewById(R.id.wind);
            wind.setText("wind: "+mSpeed);

            ImageView icon = (ImageView) mActivity.findViewById(R.id.weather_icon);
            icon.setImageBitmap(mBitmap);

            TextView weather = (TextView) mActivity.findViewById(R.id.weather);
            weather.setText(mWeather);

            TextView weatherDescription = (TextView) mActivity.findViewById(R.id.weather_description);
            weatherDescription.setText(mWeatherDescription);

            TextView sunrise = (TextView) mActivity.findViewById(R.id.sunrise);
            sunrise.setText(mSunrise);

            TextView sunset = (TextView) mActivity.findViewById(R.id.sunset);
            sunset.setText(mSunset);
        }
    }

    private void buildJson(String jsonString) throws JSONException{

        JSONObject jsonObject = new JSONObject((jsonString));

        //Getting the country
        JSONObject jsonObjectSys = jsonObject.getJSONObject("sys");
        mCountry = jsonObjectSys.getString("country");

        //Getting the city
        mCity = jsonObject.getString("name");

        //Getting temperatures, they are rounded to integer and converted to string when adding \u2103
        JSONObject jsonObjectMain = jsonObject.getJSONObject("main");
        mTemp = Math.round(jsonObjectMain.getDouble("temp"))+"\u2103";
        mMin = Math.round(jsonObjectMain.getDouble("temp_min"))+"\u2103";
        mMax = Math.round(jsonObjectMain.getDouble("temp_max"))+"\u2103";

        //Getting wind speed
        JSONObject jsonObjectWind = jsonObject.getJSONObject("wind");
        double speed = jsonObjectWind.getDouble("speed") * 3.6; //  m/s * 3.6 = Km/h
        mSpeed = Math.round(speed)+"Km/h";

        //Gettin weather conditions
        JSONArray jsonArrayWeather = jsonObject.getJSONArray("weather");
        JSONObject jsonObjectWeather = jsonArrayWeather.getJSONObject(0);
        mWeather = jsonObjectWeather.getString("main");
        mWeatherDescription = jsonObjectWeather.getString("description");
        mIcon = jsonObjectWeather.getString("icon");

        //Getting Sunrise and Sunset
        //Formats and parses dates in a locale-sensitive manner.
        // Formatting turns a Date into a String, and parsing turns a String into a Date
        SimpleDateFormat simpleDateFormat = (SimpleDateFormat) DateFormat.getDateTimeInstance();
        simpleDateFormat.applyPattern("E kk:mm");

        double sunrise = jsonObjectSys.getDouble("sunrise");
        mSunrise = simpleDateFormat.format(sunrise*1000);

        simpleDateFormat.applyPattern("kk:mm");
        double sunset = jsonObjectSys.getDouble("sunset");
        mSunset = simpleDateFormat.format(sunset*1000);
    }
}

package com.example.translatortest;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.languageid.FirebaseLanguageIdentification;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CALL_PHONE;
import static android.Manifest.permission.MANAGE_OWN_CALLS;
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;

public class MainActivity extends AppCompatActivity {

    EditText sourceEt;
    TextView englishText;
    String sourceText;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private Intent intent;
    Button speak, sendSMS;
    int langPref;
    String greet;
    SharedPreferences sharedPreferences;
    RequestQueue queue;
    Set<String> commodities;
    String temp_max,temp_min,temperature;
    String fin;

    String state = "Gujarat";

    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;
    private static final int REQUEST_CODE = 101;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fetchLastLocation();
        getWeather("Mumbai");

        sharedPreferences = getSharedPreferences("MyPreferences", MODE_PRIVATE);

        commodities = new HashSet<>();
        sourceEt = (EditText) findViewById(R.id.source_et);
        englishText = (TextView) findViewById(R.id.textView);
        speak = findViewById(R.id.speak);
        sendSMS = findViewById(R.id.sms_act);

        sendSMS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SMSActivity.class));
            }
        });

        ActivityCompat.requestPermissions(this, new String[]{RECORD_AUDIO, CALL_PHONE, MANAGE_OWN_CALLS, READ_CONTACTS, ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION}, PackageManager.PERMISSION_GRANTED);

        final String langPrefText = sharedPreferences.getString("langCodeText", "hi");

        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(new Locale(langPrefText));
                }
            }
        }, "com.google.android.tts");

        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {

            }

            @Override
            public void onDone(String s) {
                speechRecognizer.startListening(intent);
            }

            @Override
            public void onError(String s) {

            }
        });

        langPref = sharedPreferences.getInt("langCode", 22);

        Log.d("Tag", "" + langPref);

        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, langPrefText);
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, langPrefText);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onError(int error) {
                Log.d("Error", "" + error);
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String string;
                if (matches != null) {
                    string = matches.get(0);
                    sourceEt.setText(string);
                    identifyLanguage();
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        });

        speak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //sourceText = "What is my name ?";
                //translateText(1, 11, langPref);
                speechRecognizer.startListening(intent);
            }
        });
    }


    public class downloadContents extends AsyncTask<String, Void, String>
    {
        @Override
        protected String doInBackground(String... strings) {
            URL url;
            HttpURLConnection urlConnection;
            try {
                StringBuilder result = new StringBuilder();
                url = new URL(strings[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(inputStream);
                int data = reader.read();
                while (data != -1) {
                    char i = (char) data;
                    result.append(i);
                    data = reader.read();
                }
                return result.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s)
        {
            super.onPostExecute(s);
            String res = s.split("\"extract\":")[1];
            fin = res.substring(1,res.lastIndexOf('.'));
        }


    }

    public double getMax(String state, String commodity) {
        Double max = 0.0;
        try {
            JSONArray jsonArray = new JSONArray(loadJSONFromAsset());
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    commodities.add(jsonObject.getString("commodity").toLowerCase());
                    if (jsonObject.getString("state").equalsIgnoreCase(state) && jsonObject.getString("commodity").equalsIgnoreCase(commodity)) {
                        max = Math.max(max, jsonObject.getDouble("max_price"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            Log.d("Set",commodities.toString());
            Log.d("Max", "" + max);
        } catch (Exception e) {
        }
        return max;
    }

    public void getWeather(String city) {
        String getApiUrl = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=3adbba153e556b8acd65cdc12de4e649";
        queue = MySingleton.getInstance(this.getApplicationContext())
                .getRequestQueue();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, getApiUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject main1 = response.getJSONObject("main");
                    temp_max = Double.toString(Math.round((Double.parseDouble(main1.getString("temp_max")) - 273) * 100.0) / 100.0);
                    temp_min = Double.toString(Math.round((Double.parseDouble(main1.getString("temp_min")) - 273) * 100.0) / 100.0);
                    temperature = Double.toString(Math.round((Double.parseDouble(main1.getString("temp")) - 273) * 100.0) / 100.0);
                    Log.d("Temperature", temp_max);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("", "Failed!");
            }
        });
        queue.add(jsonObjectRequest);
        getString(queue, getApiUrl);
    }

    public String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = getAssets().open("data.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public void identifyLanguage() {
        sourceText = sourceEt.getText().toString();

        FirebaseLanguageIdentification identifier = FirebaseNaturalLanguage.getInstance().getLanguageIdentification();
        identifier.identifyLanguage(sourceText).addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String s) {
                if (s.equals("und"))
                    Toast.makeText(MainActivity.this, "Language Not identified", Toast.LENGTH_SHORT);
                else {
                    getLanguagecode(s);
                }
            }
        });
    }

    private void getString(RequestQueue queue, String getApiUrl) {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, getApiUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                //display the contents of our url
                Log.d("Main", "onCreate: " + response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Main", "Failed to get info!");
            }
        });
        queue.add(stringRequest);
    }

    public void getLanguagecode(String s) {
        int langCode = 22;
        translateText(0, langPref, 11);
    }


    public void translateText(final int option, int sourceCode, int targetCode) {
        FirebaseTranslatorOptions options = new FirebaseTranslatorOptions.Builder().setSourceLanguage(sourceCode).setTargetLanguage(targetCode).build();
        final FirebaseTranslator translator = FirebaseNaturalLanguage.getInstance().getTranslator(options);
        FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder().build();
        translator.downloadModelIfNeeded(conditions).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                translator.translate(sourceText).addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String s) {
                        greet = s;
                        if (option == 0) {
                            englishText.setText(s);
                            s = s.toLowerCase();
                            List<String> items = Arrays.asList(s.split(" "));
                            String commodity = "";
                            int f = 0;
                            getMax("Maharashtra","Cotton");
                            for (String ss:items)
                            {
                                Log.d("SS",ss);
                                if(commodities.contains(ss.toLowerCase())) {
                                    f = 1;
                                    Log.d("In","here");
                                    commodity = ss;
                                    break;
                                }
                            }

                            if (items.contains("where") || items.contains("near")) {
                                Intent intent = new Intent(Intent.ACTION_VIEW,
                                        Uri.parse("google.navigation:q=" + s));
                                startActivity(intent);
                                sourceText = "Redirecting To Google Maps";
                                translateText(1,11,langPref);
                            } else if (items.contains("weather") || items.contains("atmosphere")) {
                                getWeather("mumbai");
                                sourceText = "Temperature is: "+temperature;
                                translateText(1,11,langPref);
                            } else if ((items.contains("prices") && f==1) || (items.contains("mandi"))) {
                                Log.d("State Commodity",state+commodity);
                                sourceText = "The Price available in nearest market is"+getMax(state,commodity);
                                translateText(1,11,langPref);
                            }else if (items.contains("urea")||items.contains("Urea")){
                                downloadContents task = new downloadContents();
                                try {
                                    task.execute("https://en.wikipedia.org/w/api.php?action=query&prop=extracts&format=json&exsentences=3&exintro=&explaintext=&exsectionformat=plain&titles="+"Urea");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                translateText(0,11,langPref);
                            }else if (items.contains("laws")||items.contains("law")||items.contains("protests")||items.contains("farmer's"))
                            {
                                String str = "expands the scope of trade areas of farmers' produce from select areas to \"any place of production, collection, aggregation\".\n" +
                                        "provides a legal framework for farmers to enter into pre-arranged contracts with buyers including mention of pricing.\nremoves foodstuff such as cereals, pulses, potato, onions, edible oilseeds, and oils, from the list of essential commodities, removing stockholding limits on agricultural items produced by Horticulture techniques except under \"extraordinary circumstances\"";
                                sourceText = str + "For more details visit !https://en.wikipedia.org/wiki/2020_Indian_agriculture_acts";
                                translateText(1,11,langPref);
                            }else {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse("https://www.google.com/search?q="+items.toString()));
                                startActivity(intent);
                            }
                        } else if (option == 1){
                            englishText.setText(s);
                            textToSpeech.speak(s, TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                    }
                });
            }
        });
    }

    private void fetchLastLocation()
    {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_CODE);
            return;
        }
        Task<Location> task =fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null)
                {
                    currentLocation=location;
                    //Toast.makeText(getApplicationContext(),currentLocation.getLatitude()+""+currentLocation.getLongitude(),Toast.LENGTH_LONG).show();
                    Geocoder geocoder = new Geocoder(getBaseContext(), Locale.getDefault());
                    try {
                    List<Address> addresses = geocoder.getFromLocation(currentLocation.getLatitude(),currentLocation.getLongitude(),1);
                    String add="";
                    if(addresses.size()>0){
                        for(int i = 0 ; i < addresses.size();i++){
                            add += addresses.get(i);
                        }
                        state = addresses.get(0).getAdminArea();
                        //Toast.makeText(MainActivity.this,addresses.get(0)+"here",Toast.LENGTH_SHORT).show();
                    }
                }
                catch (Exception e){
                    //Toast.makeText(MainActivity.this,""+e,Toast.LENGTH_SHORT).show();
                }
            }
            }
        });
    }
}
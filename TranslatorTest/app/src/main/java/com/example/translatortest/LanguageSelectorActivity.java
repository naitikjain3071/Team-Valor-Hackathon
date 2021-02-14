package com.example.translatortest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class LanguageSelectorActivity extends AppCompatActivity {

    SharedPreferences sharedPreferences;
    public final String myPref = "MyPreferences";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_selector);

        sharedPreferences = getSharedPreferences(myPref,MODE_PRIVATE);

        final ArrayList<String> language = new ArrayList<String>();
        final ArrayList<Integer> codes = new ArrayList<Integer>();
        final ArrayList<String> codeText = new ArrayList<>();
        language.add("Arabic");
        codes.add(1);
        codeText.add("ar");
        language.add("Bengali");
        codes.add(4);
        codeText.add("bn_IN");
        language.add("English");
        codes.add(11);
        codeText.add("en");
        language.add("Spanish");
        codes.add(13);
        codeText.add("es");
        language.add("Persian");
        codes.add(15);
        codeText.add("fa_");
        language.add("French");
        codes.add(17);
        codeText.add("fr_");
        language.add("Gujarati");
        codes.add(20);
        codeText.add("gu_IN");
        language.add("Hindi");
        codes.add(22);
        codeText.add("hi_IN");
//        language.add("Croatian");
//        codes.add(23);
//        language.add("Indonesian");
//        codes.add(26);
        language.add("Kannada");
        codes.add(31);
        codeText.add("kn_IN");
        language.add("Marathi");
        codes.add(36);
        codeText.add("mr_IN");
        language.add("Portuguese");
        codes.add(42);
        codeText.add("pt_");
        language.add("Russian");
        codes.add(44);
        codeText.add("ru_");
        language.add("Tamil");
        codes.add(50);
        codeText.add("ta_IN");
        language.add("Telegu");
        codes.add(51);
        codeText.add("te_IN");
        language.add("Urdu");
        codes.add(56);
        codeText.add("ur_IN");
//        language.add("Albanian");
//        codes.add(47);
//        codeText.add("")

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, language);
        ListView listView = (ListView) findViewById(R.id.samveg);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startActivity(new Intent(LanguageSelectorActivity.this,MainActivity.class));
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("langPref",language.get(position));
                editor.putInt("langCode",codes.get(position));
                editor.putString("langCodeText",codeText.get(position));
                editor.apply();
            }
        });
    }
}
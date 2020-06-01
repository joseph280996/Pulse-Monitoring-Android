package com.tonydicola.bletest.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private static String TAG="myTag";
    private JSONObject response;
    private JSONArray patientsJSONArray;
    private ArrayList<Patient> dataList = new ArrayList<Patient>();
    private ListView listView;
    private PatientListAdapter adapter;
    private Button addPatientBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.landing_layout);
        GetPatients getPatients = new GetPatients(this);
        getPatients.execute();
        addPatientBtn = (Button) findViewById(R.id.add_patient);
        addPatientBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, CreatePatient.class);
                startActivity(intent);
            }
        });

        listView = (ListView) findViewById(R.id.ListView01);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, LandingPage.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    class GetPatients extends AsyncTask<String, Void, String> {
        private Context mContext;
        GetPatients(Context context) {
            this.mContext = context;
        }
        @Override
        protected String doInBackground(String... params) {
            String result = "";
            try {
                URL url = new URL("http://165.227.254.178/patients");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                StringBuffer sb = new StringBuffer();
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String read;
                while((read = br.readLine())!=null){
                    sb.append(read);
                }
                br.close();
                result=sb.toString();
            } catch (IOException e) {
                Log.e(TAG, "Error: " + e.toString());
            }
            return result;
        }

        @Override
        protected void onPostExecute(String data){
            super.onPostExecute(data);
            try {
                response = new JSONObject(data);
                patientsJSONArray = response.getJSONArray("patients");
                if (patientsJSONArray != null) {
                    for (int i = 0; i < patientsJSONArray.length(); i++) {
                        String firstName = patientsJSONArray.getJSONObject(i).getString("firstName");
                        String lastName = patientsJSONArray.getJSONObject(i).getString("lastName");
                        String DOB = patientsJSONArray.getJSONObject(i).has("DOB") ? patientsJSONArray.getJSONObject(i).getString("DOB"): "";
                        Patient currPatient = new Patient(firstName, lastName, DOB);
                        dataList.add(currPatient);
                    }
                }
                adapter = new PatientListAdapter(MainActivity.this, 0, dataList);
                listView.setAdapter(adapter);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }
}

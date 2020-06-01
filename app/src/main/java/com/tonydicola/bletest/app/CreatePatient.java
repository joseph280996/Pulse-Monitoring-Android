package com.tonydicola.bletest.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class CreatePatient extends Activity {
    private static String TAG="myTag";
    TextView firstNameTextView, lastNameTextView, DOBTextView;
    private JSONObject response;
    private JSONObject newPatient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_patient_layout);
        firstNameTextView = (TextView) findViewById(R.id.firstName);
        lastNameTextView = (TextView) findViewById(R.id.lastName);
        DOBTextView = (TextView) findViewById(R.id.DOB);
        Button createPatientBtn = (Button) findViewById(R.id.createPatientBtn);
        final CreatePatientTask task = new CreatePatientTask(CreatePatient.this);
        createPatientBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Log.v(TAG, "clicked");
                task.execute();
            }
        });

    }

    public class CreatePatientTask extends AsyncTask<String, Void, String> {
        private Context mContext;
        CreatePatientTask(Context context) {
            this.mContext = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            Log.v(TAG, "doInBackground is called");
            String urlString = "http://165.227.254.178/patients"; // URL to call
            String result = "";
            JSONObject jsonData = new JSONObject();
            String firstName = firstNameTextView.getText().toString();
            String dateOfBirth = DOBTextView.getText().toString();
            String lastName = lastNameTextView.getText().toString();

            Log.v(TAG, "Begin Try catch");
            try {
                Log.v(TAG, "encode data");
                jsonData.put("firstName", firstName);
                jsonData.put("lastName", lastName);
                jsonData.put("DOB", dateOfBirth);
                Log.v(TAG, jsonData.toString());
                Log.v(TAG, "Begin POST connection to "+urlString);
                URL url = new URL(urlString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.addRequestProperty("Content-Length", Integer.toString(jsonData.toString().length()));
                urlConnection.addRequestProperty("Content-Type", "application/json; charset=utf-8");
                urlConnection.setRequestMethod("POST");
                OutputStream out = urlConnection.getOutputStream();
                out.write(jsonData.toString().getBytes("utf-8"), 0, jsonData.toString().getBytes("utf-8").length);
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                StringBuilder sb = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String read;
                while((read = br.readLine())!=null){
                    sb.append(read);
                }
                br.close();
                result=sb.toString();
                Log.v(TAG, "Finished transferring data");
            } catch (Exception e) {
                result = "";
            }
            return result;
        }

        @Override
        protected void onPostExecute(String data) {
            super.onPostExecute(data);
            Log.v(TAG, data);
            try {
                if (!data.isEmpty()) {
                    response = new JSONObject(data);
                    newPatient = response.getJSONObject("patient");
                }
                Intent intent = new Intent();
                intent.setClass(CreatePatient.this, MainActivity.class);
                CreatePatient.this.startActivity(intent);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }
}

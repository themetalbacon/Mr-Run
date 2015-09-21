package com.example.projecttest;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.projecttest.R;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class main_menu extends ActionBarActivity{

    public static ImageView yellow = null;
    public static ImageView red = null;
    public static ImageView blue = null;
    public static ImageView green = null;
    public static ImageView grey = null;
    public static ImageView orange = null;



    JSONParser jsonParser = new JSONParser();

    private static final String LOGIN_URL = "http://mrrundb.netau.net/colour.php";

    private static final String TAG_SUCCESS = "success";
    private static final String TAG_MESSAGE = "message";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);

        yellow = (ImageView) this.findViewById(R.id.yellowButton);
        green = (ImageView) this.findViewById(R.id.greenButton);
        blue = (ImageView) this.findViewById(R.id.blueButton);
        red = (ImageView) this.findViewById(R.id.redButton);
        orange = (ImageView) this.findViewById(R.id.orangeButton);
        grey = (ImageView) this.findViewById(R.id.greyButton);

        View.OnClickListener screenClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                switch (v.getId()) {
                    case R.id.yellowButton:
                        login.myColour = "Yellow";
                        break;
                    case R.id.redButton:
                        login.myColour = "Red";
                        break;
                    case R.id.greenButton:
                        login.myColour = "Green";
                        break;
                    case R.id.blueButton:
                        login.myColour = "Blue";
                        break;
                    case R.id.greyButton:
                        login.myColour = "Grey";
                        break;
                    case R.id.orangeButton:
                        login.myColour = "Orange";
                        break;
                    default:
                        break;
                }

                if (login.username.equals("guest")) {
                    startActivity(new Intent(main_menu.this, Client.class));
                }
                else {
                    new addColour().execute();
                }
            }
        };

        yellow.setOnClickListener(screenClickListener);
        red.setOnClickListener(screenClickListener);
        green.setOnClickListener(screenClickListener);
        blue.setOnClickListener(screenClickListener);
        orange.setOnClickListener(screenClickListener);
        grey.setOnClickListener(screenClickListener);


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void nextPage () {
        startActivity(new Intent(main_menu.this, Client.class));

    }

    class addColour extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected String doInBackground(String... args) {
            int success;
            try {
                // Building Parameters
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("username", login.username));
                params.add(new BasicNameValuePair("colour", login.myColour));

                Log.d("request!", "starting");
                // getting product details by making HTTP request
                JSONObject json = jsonParser.makeHttpRequest(
                        LOGIN_URL, "POST", params);

                // json success tag
                success = json.getInt(TAG_SUCCESS);
                if (success == 1) {
                    Log.d("Added colour", json.toString());
                    //upon successful login, save username:
                    // Async json success tag
                    success = json.getInt(TAG_SUCCESS);
                    if (success == 1) {
                        Intent i = new Intent(main_menu.this, Client.class);
                        finish();
                        startActivity(i);
                        return json.getString(TAG_MESSAGE);
                    } else {
                        Log.d("Login Failure!", json.getString(TAG_MESSAGE));
                        return json.getString(TAG_MESSAGE);
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;

        }

        protected void onPostExecute(String file_url) {
            // dismiss the dialog once product deleted
            if (file_url != null) {
                Toast.makeText(main_menu.this, file_url, Toast.LENGTH_LONG).show();
            }

        }
    }

}

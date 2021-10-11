package com.example.econtree;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class SecondActivity extends AppCompatActivity {


    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> urls = new ArrayList<>();
    ArrayAdapter arrayAdapter;

    SQLiteDatabase NYTstorage;







    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        ListView listview = (ListView) findViewById(R.id.listView2);

        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);

        listview.setAdapter(arrayAdapter);

        ImageView NytView = (ImageView) findViewById(R.id.nytImage);

        NytView.animate().alpha(0f).setDuration(2000);



        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                Intent intent = new Intent(getApplicationContext(), PageActivity1.class);
                intent.putExtra("URLS", urls.get(i));


                startActivity(intent);
            }
        });


        NYTstorage = this.openOrCreateDatabase("NYTArticles", MODE_PRIVATE, null);

        NYTstorage.execSQL("CREATE TABLE IF NOT EXISTS NYTarticles (TITLES VARCHAR, URLS VARCHAR)");

        updateListView();



        DownloadTask task = new DownloadTask();


        try {
            task.execute("https://api.nytimes.com/svc/mostpopular/v2/viewed/1.json?api-key=XWb2BUa6YOBFHKgVIcTSAgWvUm70M3C0").get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }







    }

    public void updateListView() {
        Cursor c = NYTstorage.rawQuery("SELECT * FROM NYTarticles", null);

        int contentIndex = c.getColumnIndex("URLS");
        int titleIndex = c.getColumnIndex("TITLES");

        if (c.moveToFirst()) {
            titles.clear();
            urls.clear();

            do {
                titles.add(c.getString(titleIndex));
                urls.add(c.getString(contentIndex));
            } while (c.moveToNext());

            arrayAdapter.notifyDataSetChanged();


        }
    }


    public class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {


            String result = "";

            URL url;

            HttpURLConnection urlConnection = null;

            try {
                url = new URL(strings[0]);

                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream in = urlConnection.getInputStream();

                InputStreamReader reader = new InputStreamReader(in);

                int data = reader.read();

                while (data != -1) {

                    char current = (char) data;

                    result += current;

                    data = reader.read();

                }

               // Log.i("Results", result);

                JSONObject jsonObject1 = new JSONObject(result);

                String APIContent = jsonObject1.getString("results");

                JSONArray jsonArray = new JSONArray(APIContent);

                NYTstorage.execSQL("DELETE FROM NYTarticles");



                for (int i = 0; i < 20; i++) {

                    String JSONholder = jsonArray.getString(i);

                    JSONObject jsonObject2 = new JSONObject(JSONholder);


                    if (!jsonObject2.isNull("url") && !jsonObject2.isNull("title")) {


                        String articleTitle = jsonObject2.getString("title");

                        String articleURL = jsonObject2.getString("url");

                        articleURL = articleURL.substring(0, 23) + "." + articleURL.substring(23, articleURL.length());


                        String sql = "INSERT INTO NYTarticles (TITLES,URLS) VALUES (?,?)";

                        SQLiteStatement statement = NYTstorage.compileStatement(sql);


                        statement.bindString(1, articleTitle);
                        statement.bindString(2, articleURL);

                        statement.execute();


                    }


                }

















            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();

        }
    }
}
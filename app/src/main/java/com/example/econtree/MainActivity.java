package com.example.econtree;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> urls = new ArrayList<>();
    ArrayAdapter arrayAdapter;


    SQLiteDatabase storageDB;




    public void MainToSecond() {

        Intent intent = new Intent(getApplicationContext(), SecondActivity.class);

        startActivity(intent);

    }





    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.main_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }



    @Override
    public boolean onOptionsItemSelected( MenuItem item) {
        super.onOptionsItemSelected(item);

       if (item.getItemId() == R.id.nyTimes) {

           MainToSecond();

       }


        return false;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listview = (ListView) findViewById(R.id.listView2);

        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);

        listview.setAdapter(arrayAdapter);

        ImageView GView = (ImageView) findViewById(R.id.guardianView);

        GView.animate().alpha(0f).setDuration(2000);




        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                Intent intent = new Intent(getApplicationContext(), PageActivity1.class);
                intent.putExtra("URLS", urls.get(i));


                startActivity(intent);
            }
        });




        storageDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE,null);

        storageDB.execSQL("CREATE TABLE IF NOT EXISTS GuardianArticles2 (TITLES VARCHAR, URLS VARCHAR)");



          updateListView();




        DownloadTask task = new DownloadTask();

        try {
            task.execute("https://content.guardianapis.com/search?from-date=2020-12-15&order-by=newest&page-size=25&q=Economics%20Business%20Politics&api-key=f0b7a2b5-2922-4009-801a-281bac86bdc6").get();
        } catch (Exception e) { e.printStackTrace(); }


    }

    public void updateListView() {
        Cursor c = storageDB.rawQuery("SELECT * FROM GuardianArticles2", null);

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

        //downloading content from the API page char by char

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

              //  Log.i("URL data", result);


                // JSON Object accessed by using response  and then results as a whole JSON Objects, now turned into whole into JSON Array.

                JSONObject jsonObject = new JSONObject(result);

                String APIcontentBarrier = jsonObject.getString("response");

                JSONObject jsonObject2 = new JSONObject(APIcontentBarrier);

                String APIContentBarrier2 = jsonObject2.getString("results");

                JSONArray jsonArray = new JSONArray(APIContentBarrier2);

                //  Log.i("Content" , jsonArray.getString(2));


                  storageDB.execSQL("DELETE FROM GuardianArticles2");


                  int numberOfitems = 20;

                  if (jsonArray.length() < 20) {

                      numberOfitems = jsonArray.length();

                  }




                  for (int i = 0; i < numberOfitems; i++) {



                      // Time to use contents of JSONArray and extract out JSON Objects of interest

                      String JSONholder = jsonArray.getString(i);

                      JSONObject jsonObject3 = new JSONObject(JSONholder);

                      if (!jsonObject3.isNull("webUrl") && !jsonObject3.isNull("webTitle")) {

                          String articleTitle = jsonObject3.getString("webTitle");

                          String articleURL = jsonObject3.getString("webUrl");

                         // Log.i("URL title", articleTitle + articleURL);

                          String sql = "INSERT INTO GuardianArticles2 (TITLES, URLS) VALUES (?, ?)";

                          SQLiteStatement statement = storageDB.compileStatement(sql);

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
            Toast.makeText(MainActivity.this, "Greetings from Faizan Syed, BN0501697", Toast.LENGTH_LONG).show();
        }
    }
}


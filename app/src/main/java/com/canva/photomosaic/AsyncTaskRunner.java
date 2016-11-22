package com.canva.photomosaic;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by Anuja on 11/17/16.
 */
public class AsyncTaskRunner extends AsyncTask<String  , String, Bitmap > {

    public AsyncResponse delegate = null ;

    public AsyncTaskRunner(AsyncResponse delegate){
        this.delegate = delegate ;
    }





    ArrayList<Bitmap> bitmapArray = new ArrayList<Bitmap>();
    static OkHttpClient client = new OkHttpClient();


    @Override
    protected Bitmap doInBackground(String... strings) {
        final String IMGUR_CLIENT_ID = "...";
        final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");
        Map<String, String> options = new HashMap<>();
        options.put("color", strings[0]);
        RequestBody formBody = new FormBody.Builder()
                .add("image", "Your message")
                .build();
        Request request = new Request.Builder()
                .url("http://10.0.2.2:8765/color/32/32/"+ strings[0])
                .post(formBody)
                .build();
        Response response = null;
        final Bitmap[] bitmap = {null};
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream inputStream = response.body().byteStream();
                bitmap[0] = BitmapFactory.decodeStream(inputStream);
            }
        });
        // Headers responseHeaders = response.headers();
        // for (int i = 0; i < responseHeaders.size(); i++) {
        //     System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));


        //bitmapArray.add(bitmap);
        // imgView.setImageBitmap(bitmap);

        // }

        //System.out.println(response.body().string());

        // Do something with the response.
        return bitmap[0];
    }

    @Override
    protected void onPostExecute(Bitmap result) {

        if(delegate!=null)
        {
            delegate.processFinish(result);
        }
        else
        {
        }
    }

}

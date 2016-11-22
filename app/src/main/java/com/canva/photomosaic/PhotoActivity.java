package com.canva.photomosaic;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketImpl;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.http.HttpMethod;


public class PhotoActivity extends AppCompatActivity implements AsyncResponse {

    private final int RESULT_LOAD_IMAGE = 1;
    ImageView imgView;
    Button btnGallery, btnMosaic;
    ArrayList<Bitmap> noOfTiles;
    static int flagSet = 0 ;
    Uri selectedImage;
    static OkHttpClient client = new OkHttpClient();
    ArrayList<Bitmap> bitmapArray = new ArrayList<Bitmap>();


    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client2;
    static int xCoord1 = 0 ;
    static int yCoord1 = 0 ;

    //Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        imgView = (ImageView) findViewById(R.id.imageToConvert);
        btnGallery = (Button) findViewById(R.id.bGallery);
        btnMosaic = (Button) findViewById(R.id.mosaicImage);
        if (savedInstanceState != null) {
            Bitmap bitmap = savedInstanceState.getParcelable("BitmapImage");
            imgView.setImageBitmap(bitmap);
        }

        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickImageFromGallery();
            }
        });

        btnMosaic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                splitImageToTiles();
            }
        });

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client2 = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void splitImageToTiles() {

        BitmapDrawable drawable = (BitmapDrawable) imgView.getDrawable();
        Bitmap bitmap = drawable.getBitmap();
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int tileHeight = 32;
        int tileWidth = 32;


        int noOfTileRows = height / tileHeight;
        int noOfTileColumns = width / tileWidth;

        Toast.makeText(this, "Splitting Image" + width + "  " + height + " " + noOfTileRows + " " + noOfTileColumns, Toast.LENGTH_LONG).show();

        noOfTiles = new ArrayList<Bitmap>(noOfTileRows);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);

        int yCoord = 0;
        for (int x = 0; x < noOfTileRows; x++) {
            int xCoord = 0;
            for (int y = 0; y < noOfTileColumns; y++) {
                int averageColor = findAverageColor(Bitmap.createBitmap(scaledBitmap, xCoord, yCoord, tileWidth, tileHeight));
                String hexColor = String.format("%06X", (0xFFFFFF & averageColor));

                Log.d("RGB is ", hexColor + " ");
                AsyncTaskRunner runner = new AsyncTaskRunner(this);
                runner.execute(hexColor);
                //createSocketConnection(hexColor);

                if(x == noOfTileRows-1 && y == noOfTileColumns-1)
                    flagSet = 1;
                noOfTiles.add(Bitmap.createBitmap(scaledBitmap, xCoord, yCoord, tileWidth, tileHeight));
                xCoord += tileWidth;
            }
            yCoord += tileHeight;
        }
    }

    @Override
    public void processFinish(Bitmap bitmapFinal) {
        //bitmapArray.add(bitmapFinal);

        int yCoord = 0 ;


        BitmapDrawable drawable = (BitmapDrawable) imgView.getDrawable();
        Bitmap bitmap = drawable.getBitmap();
    /*ByteArrayOutputStream stream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);*/
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);
        bitmapArray.add(Bitmap.createBitmap(bitmapFinal,xCoord1 , yCoord1, 32, 32));
        xCoord1 = xCoord1+32 ;


        if(flagSet == 1)
            mergeImage(bitmapArray,32,32);
    }

    void mergeImage(ArrayList<Bitmap> imageChunks, int width, int height) {

        BitmapDrawable drawable = (BitmapDrawable) imgView.getDrawable();
        Bitmap bitmapImg = drawable.getBitmap();
        int widthImg = bitmapImg.getWidth();
        int heightImg = bitmapImg.getHeight();

        int tileHeight = 32;
        int tileWidth = 32;


        int noOfTileRows = heightImg / tileHeight;
        int noOfTileColumns = widthImg / tileWidth;
        // create a bitmap of a size which can hold the complete image after merging
        Bitmap bitmap = Bitmap.createBitmap(widthImg, heightImg, Bitmap.Config.ARGB_4444);

        // create a canvas for drawing all those small images
        Canvas canvas = new Canvas(bitmap);
        int count = 0;
        Bitmap currentChunk = imageChunks.get(0);

        //Array of previous row chunks bottom y coordinates
        int[] yCoordinates = new int[noOfTileColumns];
        Arrays.fill(yCoordinates, 0);

        for (int y = 0; y < noOfTileRows; ++y) {
            int xCoord = 0;
            for (int x = 0; x < noOfTileColumns; ++x) {
                currentChunk = imageChunks.get(count);
                canvas.drawBitmap(currentChunk, xCoord, yCoordinates[x], null);
                xCoord += currentChunk.getWidth();
                yCoordinates[x] += currentChunk.getHeight();
                count++;
            }
        }

    /*
     * The result image is shown in a new Activity
     */

        imgView.setImageBitmap(bitmap);

        /*Intent intent = new Intent(PhotoActivity.this, MergedImage.class);
        intent.putExtra("merged_image", bitmap);
        startActivity(intent);
        finish();*/
    }


    private void createSocketConnection(String color) {
        Map<String, String> options = new HashMap<>();
        options.put("color", color);
        RequestBody formBody = new FormBody.Builder()
                .add("message", "Your message")
                .build();
        Request request = new Request.Builder()
                .url("http://10.0.2.2:8765/color/32/32")
                .post(formBody)
                .build();

        try {
            Response response = client.newCall(request).execute();

            // Do something with the response.
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*client.(API.SEARCH_AUTO_SUGGEST)
                .setParams(options)
                .setMethod(HttpMethod.GET)
                .setListener(new Listener<AutoSuggestResponse>() {
                    @Override
                    public void onFailure(Throwable t) {
                        if (suggestionAdapter != null) {
                            suggestionAdapter.clear();
                            suggestionAdapter.updateValues(suggestionHelper.getMatchingSuggestions(query));
                            suggestionAdapter.notifyDataSetChanged();
                            animateListHeight();
                        }
                    }

                    @Override
                    public void onResponse(NetworkResponse<AutoSuggestResponse> autoSuggestResponseNetworkResponse) {
                        autoSuggestProducts.clear();
                        autoSuggestProductUrls.clear();
                        autoSuggestProductPdp.clear();
                        SearchFrameLayout.this.mResponse = autoSuggestResponseNetworkResponse.getBody();
                        autoSuggestSubCategories = mResponse.getProducts();
                        *//*for (int i = 0; autoSuggestSubCategories != null && i < autoSuggestSubCategories.size(); i++) {
                            autoSuggestProducts.add(autoSuggestSubCategories.get(i).getValue());
                            autoSuggestProductUrls.add(autoSuggestSubCategories.get(i).getImage());
                            autoSuggestProductPdp.add(autoSuggestSubCategories.get(i).getUrl());
                        }*//*
                        SearchFrameLayout.flagOpenFirst = 0;
                        suggestionAdapter.updatePdpUrl(autoSuggestProductPdp);
                        suggestionAdapter.updateImageUrls(autoSuggestProductUrls);
                        suggestionAdapter.updateProductValues(autoSuggestProducts);
                        suggestionAdapter.updateValuesFromQuery(mResponse.getTerms(), query, suggestionHelper.getMatchingSuggestions(query));
                        suggestionAdapter.notifyDataSetChanged();
                        animateListHeight();
                    }
                }).execute();
    }
*/
    }

    private int findAverageColor(Bitmap bitmap) {
        if (null == bitmap) return Color.TRANSPARENT;

        int redBucket = 0;
        int greenBucket = 0;
        int blueBucket = 0;
        int alphaBucket = 0;

        boolean hasAlpha = bitmap.hasAlpha();
        int pixelCount = bitmap.getWidth() * bitmap.getHeight();
        int[] pixels = new int[pixelCount];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int y = 0, h = bitmap.getHeight(); y < h; y++) {
            for (int x = 0, w = bitmap.getWidth(); x < w; x++) {
                int color = pixels[x + y * w]; // x + y * width
                redBucket += (color >> 16) & 0xFF; // Color.red
                greenBucket += (color >> 8) & 0xFF; // Color.greed
                blueBucket += (color & 0xFF); // Color.blue
                if (hasAlpha) alphaBucket += (color >>> 24); // Color.alpha
            }
        }

        return Color.rgb(
                redBucket / pixelCount,
                greenBucket / pixelCount,
                blueBucket / pixelCount);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        BitmapDrawable drawable = (BitmapDrawable) imgView.getDrawable();
        Bitmap bitmap = drawable.getBitmap();
        outState.putParcelable("BitmapImage", bitmap);
        super.onSaveInstanceState(outState);

    }

   /* @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        bitmap = savedInstanceState.getParcelable("BitmapImage");
    }*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        switch (requestCode) {

            case RESULT_LOAD_IMAGE:
                if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {

                    Uri uri = data.getData();

                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        ImageView imageView = (ImageView) findViewById(R.id.imageToConvert);
                        imageView.setImageBitmap(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        }
    }


    void pickImageFromGallery() {

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), RESULT_LOAD_IMAGE);
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client2.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Photo Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.canva.photomosaic/http/host/path")
        );
        AppIndex.AppIndexApi.start(client2, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Photo Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.canva.photomosaic/http/host/path")
        );
        AppIndex.AppIndexApi.end(client2, viewAction);
        client2.disconnect();
    }
}

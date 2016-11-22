package com.canva.photomosaic;

import android.graphics.Bitmap;

/**
 * Created by Anuja on 11/17/16.
 */
public interface AsyncResponse {
    void processFinish(Bitmap output);
}
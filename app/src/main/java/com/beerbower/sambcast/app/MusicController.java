package com.beerbower.sambcast.app;

import android.content.Context;
import android.widget.MediaController;

/**
 * Created by Nicholas on 12/28/2015.
 */
public class MusicController extends MediaController {

    public MusicController(Context c) {
        super(c);
    }

    @Override
    public void hide() {}
}

package com.example.treadtracksproto;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;

/**
 * Created by Conan on 4/21/2014.
 */

/**
 * Container to store song metadata from the MediaStore.
 */
public class SongItem {
    private Context context;
    private String title;
    private String artist;
    private String filepath;
    private String albumid;

    public SongItem(Context context, String title, String artist, String filepath, String album) {
        this.context = context;
        this.title = title;
        this.artist = artist;
        this.filepath = filepath;
        this.albumid = album;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public String getFilepath() {
        return filepath;
    }

    public Bitmap getAlbumArt(){
        Bitmap art;
        String projection[] = {MediaStore.Audio.Albums.ALBUM_ART};
        String selection = MediaStore.Audio.Albums._ID + "=" + this.albumid;
        Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(uri,projection,selection,null,null);
        cursor.moveToFirst();
        art = BitmapFactory.decodeFile(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART)));
        cursor.close();

        return art;
    }
}
package com.example.treadtracksproto;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Created by Conan on 4/21/2014.
 */

/**
 * Custom holder to populate a ListView with song title and album artist.
 */
public class SongAdapter extends ArrayAdapter<SongItem> {
    Context context;
    int layoutResourceId;
    SongItem[] songs = null;

    public SongAdapter(Context context, int layoutResourceId, SongItem[] songs){
        super(context, layoutResourceId, songs);
        this.context = context;
        this.layoutResourceId = layoutResourceId;
        this.songs = songs;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        SongHolder holder;
        if(row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new SongHolder();
            holder.title = (TextView) row.findViewById(R.id.title);
            holder.artist = (TextView) row.findViewById(R.id.artist);

            row.setTag(holder);

        } else {
                holder = (SongHolder)row.getTag();
        }

        SongItem song = songs[position];
        holder.title.setText(song.getTitle());
        holder.artist.setText(song.getArtist());

        return row;
    }

    public SongItem getSongItem(int position){
        return songs[position];
    }
}
class SongHolder {
    TextView title;
    TextView artist;
}

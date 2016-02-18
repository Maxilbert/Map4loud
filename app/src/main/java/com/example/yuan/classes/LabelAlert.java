package com.example.yuan.classes;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

import com.example.yuan.map4loud.R;

/**
 * Created by yuan on 2/18/16.
 */
public class LabelAlert {

    Context context = null;

    public LabelAlert(Context c){
        this.context = c;
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle("Label noise to UNLOCK MORE")
                .setIcon(R.drawable.diamond)
                .setMessage("Label your noise sample to get more sound coins, and then you can unlock acoustic data of more areas.");
        setPositiveButton(builder);
        setNegativeButton(builder);
        builder.create().show();
    }

    private void setPositiveButton(AlertDialog.Builder builder){
        builder.setPositiveButton("Go to label", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(context, "labelling", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setNegativeButton(AlertDialog.Builder builder){
        builder.setNegativeButton("Not this time", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //mSlidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });
    }
}

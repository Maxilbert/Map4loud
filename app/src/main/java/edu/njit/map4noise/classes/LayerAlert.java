package edu.njit.map4noise.classes;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.yuan.map4noise.R;

/**
 * Created by yuan on 2/20/16.
 */
public class LayerAlert {

    Context context = null;
    View view = null;
    AlertDialog.Builder builder = null;

    public LayerAlert (Context c, View v) {
        this.context = c;
        this.view = v;
        ViewGroup parent = ((ViewGroup) v.getParent());
        if(parent != null)
            parent.removeView(v);
        builder = new AlertDialog.Builder(context)
                .setTitle("What noise info do you want?")
                .setIcon(R.drawable.layers)
                .setView(v);
        setPositiveButton(builder);
        //setNegativeButton(builder);
        builder.setCancelable(false);
        builder.create().show();
    }

    private void setPositiveButton(AlertDialog.Builder builder){
        builder.setPositiveButton("Back to Map", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(context, "Setting changed", Toast.LENGTH_SHORT).show();
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

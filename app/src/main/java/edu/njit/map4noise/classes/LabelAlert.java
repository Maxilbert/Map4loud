package edu.njit.map4noise.classes;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

import com.example.yuan.map4noise.R;

/**
 * Created by yuan on 2/18/16.
 */
public class LabelAlert {

    Context context = null;
    String label = null;

    public LabelAlert(Context c, String classification){
        this.context = c;
        label = classification;
        if(label != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setTitle("Label noise to UNLOCK MORE")
                    .setIcon(R.drawable.diamond)
                    .setMessage("We think you are around a \"" + label + "\". Is that correct? " +
                            "Label your noise sample to get more sound coins, " +
                            "and then you can unlock acoustic data of more areas.");
            setPositiveButton(builder);
            setNegativeButton(builder);
            builder.setCancelable(false);
            builder.create().show();
        }
    }

    public LabelAlert(Context c){
        this(c, null);
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

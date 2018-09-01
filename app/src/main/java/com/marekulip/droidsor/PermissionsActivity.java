package com.marekulip.droidsor;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

@TargetApi(Build.VERSION_CODES.M)
public class PermissionsActivity extends AppCompatActivity {

    private LinearLayout layout;
    public static final String[] requiredPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initGUI();
    }

    /**
     * Checks if all required permissions are allowed. It is not necessary to have allowed all permission to run app. They are only needed for some features for example acquiring gps position.
     * @param context Context that will be used to check the permission.
     * @return true if all permissions are set. False if some is still missing.
     */
    public static boolean hasAllPermissions(Context context){
        for(String s : requiredPermissions){
            if(ContextCompat.checkSelfPermission(context,s) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initGUI();
        }
    }

    /**
     * Inits or restarts gui with buttons for each not yet allowed permission.
     */
    private void initGUI(){
        setContentView(R.layout.activity_permissions);
        layout = findViewById(R.id.permission_layout);
        boolean isEveryPresent = true;
        // iterate required permissions
        for(String s : requiredPermissions){
            if(checkSelfPermission(s) != PackageManager.PERMISSION_GRANTED){
                // if permission is not allowed create button for it
                isEveryPresent = false;
                layout.addView(createButtonForPermission(s));
            }
        }
        if(!isEveryPresent){
            // if some permission is not allowed also create button to allow all.
            // button is created even for one permission
            Button but = new Button(this);
            but.setText(R.string.allow_all);
            but.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            but.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requestPermissions(requiredPermissions,2);
                }
            });
            layout.addView(but);
        }else {
            // Else just tell user that everything is fine
            TextView tv = findViewById(R.id.explainer);
            tv.setText(R.string.all_permissions_allowed);
            tv.setVisibility(View.VISIBLE);
            findViewById(R.id.explain_but).setVisibility(View.GONE);
        }
    }

    /**
     * Creates {@link Button} for specified permission
     * @param permission String representation of permission for which the button should be created. String should be provided from {@link Manifest.permission} class.
     * @return usable button.
     */
    private Button createButtonForPermission(final String permission){
        Button but = new Button(this);
        switch (permission){
            case Manifest.permission.RECORD_AUDIO: but.setText(R.string.record_audio);
            break;
            case Manifest.permission.ACCESS_FINE_LOCATION: but.setText(R.string.location_permission);
            break;
            case Manifest.permission.WRITE_EXTERNAL_STORAGE: but.setText(R.string.write_to_external_storage);
            break;
        }
        but.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermissions(new String[]{permission},1);
            }
        });
        return but;
    }

    /**
     * Action for button to display or hide permission explanations
     * @param view button to which this action is attached
     */
    public void explainPermissions(View view){
        handleExplanation(false,view);
    }

    /**
     * Explains or hides permission explanations
     * @param stop True to hide, false to show
     * @param view button that called this method
     */
    private void handleExplanation(boolean stop, final View view){
        TextView tv = findViewById(R.id.explainer);

        Button but = (Button)view;
        if(stop){
            tv.setVisibility(View.GONE);
            but.setText(R.string.explain_permissions);
            but.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleExplanation(false,view);
                }
            });
        } else{
            tv.setVisibility(View.VISIBLE);
            but.setText(R.string.ok);
            but.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleExplanation(true,view);
                }
            });
        }
    }
}

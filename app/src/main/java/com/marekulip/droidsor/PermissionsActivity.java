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

    private void initGUI(){
        setContentView(R.layout.activity_permissions);
        layout = findViewById(R.id.permission_layout);
        boolean isEveryPresent = true;
        for(String s : requiredPermissions){
            if(checkSelfPermission(s) != PackageManager.PERMISSION_GRANTED){
                isEveryPresent = false;
                layout.addView(createButtonForPermission(s));
            }
        }
        if(!isEveryPresent){
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
            TextView tv = findViewById(R.id.explainer);
            tv.setText(R.string.all_permissions_allowed);
            tv.setVisibility(View.VISIBLE);
            findViewById(R.id.explain_but).setVisibility(View.GONE);
        }
    }

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

    public void explainPermissions(View view){
        handleExplanation(false,view);
    }

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

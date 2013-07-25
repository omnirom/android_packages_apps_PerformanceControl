package com.brewcrewfoo.performance.activities;

/**
 * Created by h0rn3t on 21.07.2013.
 */
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.util.ActivityThemeChangeInterface;
import com.brewcrewfoo.performance.util.Constants;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class FlasherActivity extends Activity implements Constants, ActivityThemeChangeInterface {
    TextView flasherInfo;
    TextView deviceName;
    TextView deviceModel;
    TextView deviceBoard;
    Button chooseBtn;
    SharedPreferences mPreferences;
    private boolean mIsLightTheme;
    private String part;
    private String tip;
    private String model;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme();
        setContentView(R.layout.flasher);

        Intent intent1=getIntent();
        tip=intent1.getStringExtra("mod");

        flasherInfo=(TextView)findViewById(R.id.flashinfo);
        deviceName=(TextView)findViewById(R.id.name);
        deviceModel=(TextView)findViewById(R.id.model);
        deviceBoard=(TextView)findViewById(R.id.board);
        chooseBtn=(Button) findViewById(R.id.chooseBtn);

        model=Build.MODEL;
        deviceModel.setText(model);
        deviceBoard.setText(Build.MANUFACTURER);
        deviceName.setText(Build.DEVICE);//Build.PRODUCT

        if(getPart(model)){
            if(tip.equalsIgnoreCase("kernel")){
                flasherInfo.setText("boot.img "+getString(R.string.flash_info,part)+" "+tip.toUpperCase());
                chooseBtn.setText(getString(R.string.btn_choose,"boot.img"));
            }
            else{
                flasherInfo.setText("recovery.img "+getString(R.string.flash_info,part)+" "+tip.toUpperCase());
                chooseBtn.setText(getString(R.string.btn_choose,"recovery.img"));
            }
            chooseBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    //------------
                    try{
                        Intent intent2 = new Intent(FlasherActivity.this, FileChooser.class);
                        intent2.putExtra("mod",tip);
                        intent2.putExtra("part",part);
                        startActivity(intent2);
                        //finish();
                    }
                    catch(Exception e){
                        Log.e(TAG,"Error launching filechooser activity");
                    }
                }
            });
        }
        else{
            chooseBtn.setVisibility(View.GONE);
        }
    }


    private static String getValue(String tag, org.w3c.dom.Element element) {
        NodeList nodes = element.getElementsByTagName(tag).item(0).getChildNodes();
        Node node = (Node) nodes.item(0);
        return node.getNodeValue();
    }

    private Boolean getPart(String m){
        Boolean gasit=false;
        InputStream is;
        try {
            if (new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/PerformanceControl/devices.xml").exists()){
                is = new BufferedInputStream(new FileInputStream(Environment.getExternalStorageDirectory().getAbsolutePath()+"/PerformanceControl/devices.xml"));
            }
            else{
                is = getResources().openRawResource(R.raw.devices);
            }
            DocumentBuilder builder=DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc=builder.parse(is, null);
            doc.getDocumentElement().normalize();
            NodeList nList=doc.getElementsByTagName("device");
            for (int k = 0; k < nList.getLength(); k++) {
                Node node = nList.item(k);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    org.w3c.dom.Element element = (org.w3c.dom.Element) node;
                    final String models[]=getValue("model", element).split(",");
                    for (String mi : models) {
                        if(mi.equalsIgnoreCase(mi)){
                            part=getValue(tip, element);
                            gasit=true;
                        }
                    }
                    if(gasit) {
                        Log.i(TAG,tip+" partition = "+part);
                        break;
                    }
                }
            }
            is.close();
        }
        catch (Exception e) {
            Log.e(TAG,"Error reading devices.xml");
            gasit=false;
            e.printStackTrace();
        }
        return gasit;
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.flasher_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.flash_kernel) {
            tip="kernel";
            if(getPart(this.model)){
                if(tip.equalsIgnoreCase("kernel")){
                    flasherInfo.setText("boot.img "+getString(R.string.flash_info,part)+" "+tip.toUpperCase());
                    chooseBtn.setText(getString(R.string.btn_choose,"boot.img"));
                }
                else{
                    flasherInfo.setText("recovery.img "+getString(R.string.flash_info,part)+" "+tip.toUpperCase());
                    chooseBtn.setText(getString(R.string.btn_choose,"recovery.img"));
                }
                chooseBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        //------------
                        try{
                            Intent intent2 = new Intent(FlasherActivity.this, FileChooser.class);
                            intent2.putExtra("mod",tip);
                            intent2.putExtra("part",part);
                            startActivity(intent2);
                        }
                        catch(Exception e){
                            Log.e(TAG,"Error launching filechooser activity");
                        }
                    }
                });
            }
            else{
                chooseBtn.setVisibility(View.GONE);
            }

        }
        if (item.getItemId() == R.id.flash_recovery) {
            tip="recovery";
            if(getPart(model)){
                if(tip.equalsIgnoreCase("kernel")){
                    flasherInfo.setText("boot.img "+getString(R.string.flash_info,part)+" "+tip.toUpperCase());
                    chooseBtn.setText(getString(R.string.btn_choose,"boot.img"));
                }
                else{
                    flasherInfo.setText("recovery.img "+getString(R.string.flash_info,part)+" "+tip.toUpperCase());
                    chooseBtn.setText(getString(R.string.btn_choose,"recovery.img"));
                }
                chooseBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        //------------
                        try{
                            Intent intent2 = new Intent(FlasherActivity.this, FileChooser.class);
                            intent2.putExtra("mod",tip);
                            intent2.putExtra("part",part);
                            startActivity(intent2);
                        }
                        catch(Exception e){
                            Log.e(TAG,"Error launching filechooser activity");
                        }
                    }
                });
            }
            else{
                chooseBtn.setVisibility(View.GONE);
            }

        }
        return true;
    }

    @Override
    public boolean isThemeChanged() {
        final boolean is_light_theme = mPreferences.getBoolean(PREF_USE_LIGHT_THEME, false);
        return is_light_theme != mIsLightTheme;
    }

    @Override
    public void setTheme() {
        final boolean is_light_theme = mPreferences.getBoolean(PREF_USE_LIGHT_THEME, false);
        mIsLightTheme = is_light_theme;
        setTheme(is_light_theme ? R.style.Theme_Light : R.style.Theme_Dark);
    }
}

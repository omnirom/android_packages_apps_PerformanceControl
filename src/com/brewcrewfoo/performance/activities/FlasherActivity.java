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

        String model=Build.MODEL;
        deviceModel.setText(model);
        deviceBoard.setText(Build.MANUFACTURER);
        deviceName.setText(Build.DEVICE);//Build.PRODUCT

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
                    if(getValue("model", element).equalsIgnoreCase(model)){
                        part=getValue(tip, element);
                        Log.i(TAG,tip+" partition = "+part);
                        gasit=true;
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
        final String bf;
        if(tip.equalsIgnoreCase("kernel")){
            bf="boot.img";
        }
        else{
            bf="recovery.img";
        }
        chooseBtn=(Button) findViewById(R.id.chooseBtn);
        chooseBtn.setText(getString(R.string.btn_choose,bf));
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

        if(gasit){
            flasherInfo.setText(bf+" "+getString(R.string.flash_info,part)+" "+tip.toUpperCase());
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


    @Override
    public void onResume() {
        super.onResume();
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

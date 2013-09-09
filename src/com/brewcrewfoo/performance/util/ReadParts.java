package com.brewcrewfoo.performance.util;

import android.app.Activity;
import android.os.Environment;
import android.util.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import com.brewcrewfoo.performance.R;

/**
 * Created by h0rn3t on 09.09.2013.
 */
public class ReadParts extends Activity implements Constants {
    private static String _part = null;
    private static String _model = null;
    private static String _tip = null;
    final private static String extfpath="/PerformanceControl/devices.xml";

    public static void set_model(String m){
        _model=m;
    }
    public static void set_tip(String t){
        _tip=t;
    }
    public static String get_part(){
        return _part;
    }
    private static String getValue(String tag, org.w3c.dom.Element element) {
        NodeList nodes = element.getElementsByTagName(tag).item(0).getChildNodes();
        Node node = (Node) nodes.item(0);
        return node.getNodeValue();
    }

    public Boolean isPart(){
        Boolean gasit=false;
        InputStream f;
        final String fn= Environment.getExternalStorageDirectory().getAbsolutePath()+extfpath;
        try {
            if (new File(fn).exists()){
                f = new BufferedInputStream(new FileInputStream(fn));
                Log.i(TAG,"external /PerformanceControl/devices.xml in use");
            }
            else{
                f = getApplicationContext().getResources().openRawResource(R.raw.devices);
            }
            DocumentBuilder builder= DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc=builder.parse(f, null);
            doc.getDocumentElement().normalize();
            NodeList nList=doc.getElementsByTagName("device");
            for (int k = 0; k < nList.getLength(); k++) {
                Node node = nList.item(k);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    org.w3c.dom.Element element = (org.w3c.dom.Element) node;
                    final String models[]=getValue("model", element).split(",");
                    for (String mi : models) {
                        if(mi.equalsIgnoreCase(_model)){
                            _part=getValue(_tip, element);
                            gasit=true;
                        }
                    }
                    if(gasit) {
                        Log.d(TAG,_tip+" partition detected: "+_part);
                        break;
                    }
                }
            }
            f.close();
        }
        catch (Exception e) {
            Log.e(TAG,"Error reading devices.xml");
            gasit=false;
            e.printStackTrace();
        }
        return gasit;
    }


}

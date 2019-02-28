package app.com.detectionapp.PrommeInfo.dealMsgFromServer;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.StringRes;
import android.util.Log;
import android.util.Xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * author : test
 * date : 2019/2/24 18:56
 * description :将msg 数组 dump 到本地
 */
public class dumpMsgListToLocalFactory {

    ArrayList<messageLocal> _messages = new ArrayList<>();//要转的message list
    String filename ;//存的文件名
    Activity activity;

    private static final String TAG = "dumpMsgListToLocalFacto";

    public dumpMsgListToLocalFactory(ArrayList<messageLocal> _messages, String filename, Activity activity) {
        this._messages = _messages;
        this.filename = filename;
        this.activity = activity;
    }

   public void dumpMsgListToLocalFile()
   {
       try
       {
            File file = new File(activity.getFilesDir().getAbsolutePath() + "/"+filename);

            DocumentBuilderFactory dfact = DocumentBuilderFactory.newInstance();
            DocumentBuilder build = dfact.newDocumentBuilder();
            Document doc = build.newDocument();

           Element root = doc.createElement("messages");
           doc.appendChild(root);
           Element details = doc.createElement("details");
           root.appendChild(details);

           for(messageLocal messageLocal : _messages)
           {
               Element perMessage = doc.createElement("perMessage");
               details.appendChild(perMessage);

               Element index = doc.createElement("index");
               index.appendChild(doc.createTextNode(String.valueOf(messageLocal.getId())));
               perMessage.appendChild(index);

               Element appName = doc.createElement("appName");
               appName.appendChild(doc.createTextNode(messageLocal.getAppName()));
               perMessage.appendChild(appName);

               Element appType = doc.createElement("appType");
               appType.appendChild(doc.createTextNode(messageLocal.getAppType()));
               perMessage.appendChild(appType);

               Element detectionTime = doc.createElement("detectionTime");
               detectionTime.appendChild(doc.createTextNode(messageLocal.getDetectionTime()));
               perMessage.appendChild(detectionTime);

               Element systemMessage = doc.createElement("systemMessage");
               systemMessage.appendChild(doc.createTextNode(messageLocal.getSystemMessage()));
               perMessage.appendChild(systemMessage);
           }

           TransformerFactory tranFactory = TransformerFactory.newInstance();
           Transformer aTransformer = tranFactory.newTransformer();

           aTransformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");

           aTransformer.setOutputProperty(
                   "{http://xml.apache.org/xslt}indent-amount", "4");
           aTransformer.setOutputProperty(OutputKeys.INDENT, "yes");


           DOMSource source = new DOMSource(doc);

           FileWriter fos = new FileWriter(file);
           StreamResult result = new StreamResult(fos);
           aTransformer.transform(source, result);

           Log.d(TAG, "dumpMsgListToLocalFile: jiangzhe has dump to xml file success");
       }catch (Throwable e)
       {
           Log.d(TAG, "dumpMsgListToLocalFile: jiangzhe " + e.getMessage());
       }
   }
}

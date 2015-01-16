package cn.edu.ustc.weiking;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class PhoneContacts extends Activity {
    ListView listView_phoneContacts;
    private String usrName = "";
    ProgressDialog pd;
    public List<Map<String, String>> list_phoneCon = new ArrayList<Map<String, String>>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.display);
	this.setTitle("通讯录");
	listView_phoneContacts = (ListView) PhoneContacts.this
		.findViewById(R.id.listView_display);
	getInfoFromMain();
	processThread();
    }

    public void getInfoFromMain() {
	Bundle bundle = getIntent().getExtras();
	usrName = bundle.getString("UsrName");
    }

    private final Handler handler = new Handler() {
	@Override
	public void handleMessage(Message msg) {
	    super.handleMessage(msg);
	    pd.dismiss();
	    SimpleAdapter listItemAdapter = new SimpleAdapter(
		    PhoneContacts.this,
		    list_phoneCon,// 数据源
		    R.layout.listview, new String[] { "name", "num" },
		    new int[] { R.id.textView_Time, R.id.textView_TraStatus });
	    listView_phoneContacts.setAdapter(listItemAdapter);
	}
    };

    public void processThread() {
	pd = ProgressDialog.show(PhoneContacts.this, "Please Wait",
		"数据处理中，请等待...");
	new Thread() {
	    @Override
	    public void run() {
		// TODO 加上耗时操作
		String msg = getPhoneCon();
		deelWithMsg(msg);
		handler.sendEmptyMessage(0);
	    }
	}.start();
    }

    public String getPhoneCon() {
	String msg = "";
	Socket mSocket = null;
	try {
	    mSocket = new Socket(InfoBackUpActivity.SOCKET_IP,
		    InfoBackUpActivity.SOCKET_PORT);
	    PrintWriter out = new PrintWriter(new BufferedWriter(
		    new OutputStreamWriter(mSocket.getOutputStream())), true);
	    String uploadMsg = "getPhoneCon@%" + usrName + "#";
	    out.println(uploadMsg);
	    // 接受服务器的信息
	    BufferedReader br = new BufferedReader(new InputStreamReader(
		    mSocket.getInputStream()));
	    String mstr = br.readLine();
	    msg = mstr;
	    Log.v("PhoneContacts", "getMsg " + mstr);
	    out.close();
	    br.close();
	    mSocket.close();
	} catch (Exception e) {
	    Log.v("InfoDisplay", e.toString());
	}
	return msg;
    }

    public void deelWithMsg(String msg) {
	String[] strArr = msg.split("&");
	for (int i = 0; i < strArr.length; i++) {
	    String[] strArr1 = strArr[i].split("@%");
	    if (strArr1[0].equals("PHONECON")) {
		String[] strPhoneCon = strArr1[1].split("#");
		for (int j = 0; j < strPhoneCon.length; j++) {
		    String[] strArr2 = strPhoneCon[j].split(";");
		    HashMap<String, String> map = new HashMap<String, String>();
		    String traMsg = "";
		    traMsg += "ID：" + strArr2[0] + "	电话：" + strArr2[2]
			    + "	存储位置：" + strArr2[3];
		    map.put("name", "姓名：" + strArr2[1]);
		    map.put("num", traMsg);
		    list_phoneCon.add(map);
		}
	    }
	}
	Log.v("phonecontacts", "finish msg");
    }

}
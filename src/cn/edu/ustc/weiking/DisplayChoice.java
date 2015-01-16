package cn.edu.ustc.weiking;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DisplayChoice extends Activity {
    private String usrName = "";
    private ProgressDialog pd;
    private String[] callLogStr;
    private String[] smsLogStr;
    private String[] traLogStr;
    LinearLayout callLogLinearLayout;
    LinearLayout smsLogLinearLayout;
    LinearLayout traLogLinearLayout;
    TextView callLogCountTextView;
    TextView smsLogCountTextView;
    TextView traLogCountTextView;
    private final int[] msgCount = { 0, 0, 0 };

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.displaychoice);
	this.setTitle("备忘信息展示");
	getWidget();
	registListener();
	getInfoFromMain();
	processThread();
    }

    public void getWidget() {
	callLogLinearLayout = (LinearLayout) DisplayChoice.this
		.findViewById(R.id.linearLayout_callLog);
	smsLogLinearLayout = (LinearLayout) DisplayChoice.this
		.findViewById(R.id.linearLayout_smsLog);
	traLogLinearLayout = (LinearLayout) DisplayChoice.this
		.findViewById(R.id.linearLayout_traLog);
	callLogCountTextView = (TextView) DisplayChoice.this
		.findViewById(R.id.textView_callLogCount);
	smsLogCountTextView = (TextView) DisplayChoice.this
		.findViewById(R.id.textView_smsLogCount);
	traLogCountTextView = (TextView) DisplayChoice.this
		.findViewById(R.id.textView_traLogCount);
    }

    public void registListener() {
	callLogLinearLayout.setOnClickListener(new View.OnClickListener() {

	    @Override
	    public void onClick(View v) {
		// TODO Auto-generated method stub
		Bundle bundle = new Bundle();
		bundle.putString("msgKind", "CALLLOG");
		bundle.putStringArray("msgValue", callLogStr);
		Intent intent = new Intent(DisplayChoice.this, Display.class);
		intent.putExtras(bundle);
		startActivity(intent);
	    }
	});
	smsLogLinearLayout.setOnClickListener(new View.OnClickListener() {

	    @Override
	    public void onClick(View v) {
		// TODO Auto-generated method stub
		Bundle bundle = new Bundle();
		bundle.putString("msgKind", "SMSLOG");
		bundle.putStringArray("msgValue", smsLogStr);
		Intent intent = new Intent(DisplayChoice.this, Display.class);
		intent.putExtras(bundle);
		startActivity(intent);
	    }
	});
	traLogLinearLayout.setOnClickListener(new View.OnClickListener() {

	    @Override
	    public void onClick(View v) {
		// TODO Auto-generated method stub
		Bundle bundle = new Bundle();
		bundle.putString("msgKind", "TRALOG");
		bundle.putStringArray("msgValue", traLogStr);
		Intent intent = new Intent(DisplayChoice.this, Display.class);
		intent.putExtras(bundle);
		startActivity(intent);
	    }
	});
    }

    public void getInfoFromMain() {
	Bundle bundle = getIntent().getExtras();
	usrName = bundle.getString("UsrName");
    }

    public void processThread() {
	pd = ProgressDialog.show(DisplayChoice.this, "Please Wait",
		"数据下载中，请等待...");
	new Thread() {
	    @Override
	    public void run() {
		// TODO 加上耗时操作
		String msg = getInfo();
		deelWithMsg(msg);
		handler.sendEmptyMessage(0);
	    }
	}.start();
    }

    public void deelWithMsg(String msg) {

	String[] strArr = msg.split("&");
	for (int i = 0; i < strArr.length; i++) {
	    String[] strArr1 = strArr[i].split("@%");
	    if (strArr1[0].equals("CALLLOG")) {
		Log.v("display", "deel with calllog");
		String[] strCallLog = strArr1[1].split("!%");
		msgCount[0] = strCallLog.length;
		callLogStr = strCallLog;
	    } else if (strArr1[0].equals("SMSLOG")) {
		Log.v("display", "deel with smslog");
		String[] strSMSLog = strArr1[1].split("!%");
		msgCount[1] = strSMSLog.length;
		smsLogStr = strSMSLog;
	    } else if (strArr1[0].equals("TRALOG")) {
		Log.v("display", "deel with tralog");
		String[] strTraLog = strArr1[1].split("#");
		msgCount[2] = strTraLog.length;
		traLogStr = strTraLog;
	    }
	}
    }

    private final Handler handler = new Handler() {
	@Override
	public void handleMessage(Message msg) {
	    super.handleMessage(msg);
	    pd.dismiss();
	    callLogCountTextView.setText("总计与" + msgCount[0] + "人的通话记录");
	    smsLogCountTextView.setText("总计与" + msgCount[1] + "人的短信记录");
	    traLogCountTextView.setText("总计" + msgCount[2] + "条流量统计记录");
	}
    };

    public String getInfo() {
	String msg = "";
	Socket mSocket = null;
	try {
	    mSocket = new Socket(InfoBackUpActivity.SOCKET_IP,
		    InfoBackUpActivity.SOCKET_PORT);
	    PrintWriter out = new PrintWriter(new BufferedWriter(
		    new OutputStreamWriter(mSocket.getOutputStream())), true);
	    String uploadMsg = "getInfo@%" + usrName + "#";
	    out.println(uploadMsg);
	    // 接受服务器的信息
	    BufferedReader br = new BufferedReader(new InputStreamReader(
		    mSocket.getInputStream()));
	    String mstr = br.readLine();
	    msg = mstr;
	    Log.v("InfoDisplay", "getMsg " + mstr);
	    out.close();
	    br.close();
	    mSocket.close();
	} catch (Exception e) {
	    Log.v("InfoDisplay", e.toString());
	}
	return msg;
    }

}
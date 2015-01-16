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
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import cn.edu.ustc.weiking.TreeViewAdapter.TreeNode;

public class InfoDisplay extends Activity {
    /** Called when the activity is first created. */
    ExpandableListView expandableList_callLog;
    ExpandableListView expandableList_smsLog;
    ListView listView_traLog;
    TreeViewAdapter adapter_callLog;
    TreeViewAdapter adapter_smsLog;
    Bundle bundle;
    private ProgressDialog pd;
    private String usrName = "";
    // Sample data set. children[i] contains the children (String[]) for
    // groups[i].
    public List<String> groups_callLog = new ArrayList<String>();
    public Map<String, List<callLogClass>> children_callLog = new HashMap<String, List<callLogClass>>();
    public List<String> groups_smsLog = new ArrayList<String>();
    public Map<String, List<smsLogClass>> children_smsLog = new HashMap<String, List<smsLogClass>>();
    public List<Map<String, String>> list_traLog = new ArrayList<Map<String, String>>();

    public class callLogClass {
	String callNum;
	String callDate;
	String callTime;
	int callType;
    }

    public class smsLogClass {
	int id;
	String smsNum;
	String smsDate;
	int smsType;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.infodisplay);
	this.setTitle("备忘信息展示");
	getInfoFromMain();
	adapter_callLog = new TreeViewAdapter(this);
	adapter_smsLog = new TreeViewAdapter(this);
	expandableList_callLog = (ExpandableListView) InfoDisplay.this
		.findViewById(R.id.expandableListView_callLog);
	expandableList_smsLog = (ExpandableListView) InfoDisplay.this
		.findViewById(R.id.expandableListView_smsLog);
	listView_traLog = (ListView) InfoDisplay.this
		.findViewById(R.id.listView_traLog);
	processThread();
    }

    public void getInfoFromMain() {
	bundle = getIntent().getExtras();
	usrName = bundle.getString("UsrName");
    }

    public void sendInfoToMain() {
	Intent intentForResult = new Intent();
	setResult(RESULT_OK, intentForResult);
	Log.v("uo", "sendSucceed");
    }

    @Override
    public void onBackPressed() {
	sendInfoToMain(); // 向主界面发送消息
	super.onBackPressed();
    }

    private final Handler handler = new Handler() {
	@Override
	public void handleMessage(Message msg) {
	    super.handleMessage(msg);
	    pd.dismiss();
	    expandableList_callLog.setAdapter(adapter_callLog);
	    expandableList_smsLog.setAdapter(adapter_smsLog);
	    SimpleAdapter listItemAdapter = new SimpleAdapter(
		    InfoDisplay.this,
		    list_traLog,// 数据源
		    R.layout.listview, new String[] { "time", "traStatus" },
		    new int[] { R.id.textView_Time, R.id.textView_TraStatus });
	    listView_traLog.setAdapter(listItemAdapter);

	}
    };

    private void processThread() {
	pd = ProgressDialog.show(InfoDisplay.this, "Please Wait",
		"数据处理中，请等待...");
	new Thread() {
	    @Override
	    public void run() {
		// TODO 加上耗时操作
		String msg = getInfo();
		deelWithMsg(msg);
		prepareDisplay();
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
		for (int j = 0; j < strCallLog.length; j++) {
		    List<callLogClass> temp_list = new ArrayList<callLogClass>();
		    temp_list.clear();
		    String[] strArr2 = strCallLog[j].split("#");
		    // TODO 将名字存入parents组
		    groups_callLog.add(strArr2[0]);
		    for (int k = 1; k < strArr2.length; k++) {
			String[] strArr3 = strArr2[k].split(";");
			// TODO 将这些存入children组
			callLogClass temp_class = new callLogClass();
			temp_class.callNum = strArr3[0];
			temp_class.callDate = strArr3[1];
			temp_class.callTime = strArr3[2];
			temp_class.callType = Integer.parseInt(strArr3[3]);
			temp_list.add(temp_class);
		    }
		    children_callLog.put(strArr2[0], temp_list);
		}
	    } else if (strArr1[0].equals("SMSLOG")) {
		Log.v("display", "deel with smslog");

		String[] strSMSLog = strArr1[1].split("!%");
		for (int j = 0; j < strSMSLog.length; j++) {
		    List<smsLogClass> temp_list = new ArrayList<smsLogClass>();
		    temp_list.clear();
		    String[] strArr2 = strSMSLog[j].split("#");
		    // TODO 将名字存入parents组
		    groups_smsLog.add(strArr2[0]);
		    for (int k = 1; k < strArr2.length; k++) {
			String[] strArr3 = strArr2[k].split(";");
			// TODO 将这些存入children组
			smsLogClass temp_class = new smsLogClass();
			temp_class.id = Integer.parseInt(strArr3[0]);
			temp_class.smsNum = strArr3[1];
			temp_class.smsDate = strArr3[2];
			temp_class.smsType = Integer.parseInt(strArr3[3]);
			temp_list.add(temp_class);
		    }
		    children_smsLog.put(strArr2[0], temp_list);
		}
	    } else if (strArr1[0].equals("TRALOG")) {
		Log.v("display", "deel with tralog");
		String[] strTraLog = strArr1[1].split("#");
		for (int j = 0; j < strTraLog.length; j++) {
		    String[] strArr2 = strTraLog[j].split(";");
		    HashMap<String, String> map = new HashMap<String, String>();
		    String traMsg = "";
		    traMsg += "移动流量：" + strArr2[1] + "；总流量：" + strArr2[2];
		    map.put("time", "采样时间：" + strArr2[0]);
		    map.put("traStatus", traMsg);
		    list_traLog.add(map);
		}
	    }

	}
	Log.v("display", "finish msg");
    }

    public void prepareDisplay() {
	adapter_callLog.RemoveAll();
	adapter_callLog.notifyDataSetChanged();
	adapter_smsLog.RemoveAll();
	adapter_smsLog.notifyDataSetChanged();
	List<TreeNode> treeNode_callLog = adapter_callLog.GetTreeNode();
	for (int i = 0; i < groups_callLog.size(); i++) {
	    TreeViewAdapter.TreeNode node = new TreeViewAdapter.TreeNode();
	    String name = "" + groups_callLog.get(i);
	    Log.v("test display", "get from list  " + name);

	    node.parent = name;
	    List<callLogClass> temp_list = new ArrayList<callLogClass>();
	    temp_list = children_callLog.get(name);
	    for (int ii = 0; ii < temp_list.size(); ii++) {
		String strTemp = "Num:" + temp_list.get(ii).callNum + "  Date:"
			+ temp_list.get(ii).callDate + "  Time:"
			+ temp_list.get(ii).callTime;
		if (temp_list.get(ii).callType == 1) {
		    strTemp += "  Type:Incoming";
		} else if (temp_list.get(ii).callType == 2) {
		    strTemp += "  Type:Outgoing";
		} else if (temp_list.get(ii).callType == 3) {
		    strTemp += "  Type:Missed";
		}
		node.children.add(strTemp);
	    }
	    treeNode_callLog.add(node);
	}
	adapter_callLog.UpdateTreeNode(treeNode_callLog);
	List<TreeNode> treeNode_smsLog = adapter_smsLog.GetTreeNode();
	for (int i = 0; i < groups_smsLog.size(); i++) {
	    TreeViewAdapter.TreeNode node = new TreeViewAdapter.TreeNode();
	    node.parent = groups_smsLog.get(i);
	    List<smsLogClass> temp_list = new ArrayList<smsLogClass>();
	    temp_list = children_smsLog.get(groups_smsLog.get(i));
	    for (int ii = 0; ii < temp_list.size(); ii++) {
		String strTemp = "ID:" + temp_list.get(ii).id + "  Num:"
			+ temp_list.get(ii).smsNum + "  Date:"
			+ temp_list.get(ii).smsDate;
		if (temp_list.get(ii).smsType == 1) {
		    strTemp += "  Type:Incoming";
		} else if (temp_list.get(ii).smsType == 2) {
		    strTemp += "  Type:Outgoing";
		}
		node.children.add(strTemp);
	    }
	    treeNode_smsLog.add(node);
	}
	adapter_smsLog.UpdateTreeNode(treeNode_smsLog);
	/*
	 * expandableList_callLog.setAdapter(adapter_callLog);
	 * expandableList_smsLog.setAdapter(adapter_smsLog);
	 * 
	 * listView_traLog = new ListView(InfoDisplay.this);
	 * listView_traLog.setAdapter(new ArrayAdapter<String>(InfoDisplay.this,
	 * R.id.listView_traLog, list_traLog)); setContentView(listView_traLog);
	 */
    }

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
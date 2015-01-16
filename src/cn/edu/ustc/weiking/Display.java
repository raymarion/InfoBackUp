package cn.edu.ustc.weiking;

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
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import cn.edu.ustc.weiking.TreeViewAdapter.TreeNode;

public class Display extends Activity {
    private String[] msg;
    private String msgKind = "";
    private ProgressDialog pd;
    TreeViewAdapter adapter_callLog;
    TreeViewAdapter adapter_smsLog;
    ExpandableListView expandableList_display;
    ListView listView_display;
    public List<String> groups_callLog = new ArrayList<String>();
    public List<String> groups_smsLog = new ArrayList<String>();
    public Map<String, List<callLogClass>> children_callLog = new HashMap<String, List<callLogClass>>();
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
	setContentView(R.layout.display);
	this.setTitle("信息展示");
	expandableList_display = (ExpandableListView) Display.this
		.findViewById(R.id.expandableListView_display);
	listView_display = (ListView) Display.this
		.findViewById(R.id.listView_display);
	getInfoFromMain();
	processThread();
    }

    public void getInfoFromMain() {
	Bundle bundle = getIntent().getExtras();
	msgKind = bundle.getString("msgKind");
	msg = bundle.getStringArray("msgValue");
    }

    private final Handler handler = new Handler() {
	@Override
	public void handleMessage(Message msg) {
	    super.handleMessage(msg);
	    pd.dismiss();
	    if (msgKind.equals("TRALOG")) {
		SimpleAdapter listItemAdapter = new SimpleAdapter(
			Display.this,
			list_traLog,// 数据源
			R.layout.listview,
			new String[] { "time", "traStatus" }, new int[] {
				R.id.textView_Time, R.id.textView_TraStatus });
		listView_display.setAdapter(listItemAdapter);
	    } else if (msgKind.equals("CALLLOG")) {
		expandableList_display.setAdapter(adapter_callLog);
	    } else if (msgKind.equals("SMSLOG")) {
		expandableList_display.setAdapter(adapter_smsLog);
	    }
	}
    };

    public void processThread() {
	pd = ProgressDialog.show(Display.this, "Please Wait", "数据处理中，请等待...");
	new Thread() {
	    @Override
	    public void run() {
		// TODO 加上耗时操作
		deelWithMsg(msg);
		prepareDisplay();
		handler.sendEmptyMessage(0);
	    }
	}.start();
    }

    public void deelWithMsg(String[] msg) {
	if (msgKind.equals("CALLLOG")) {
	    for (int j = 0; j < msg.length; j++) {
		List<callLogClass> temp_list = new ArrayList<callLogClass>();
		temp_list.clear();
		String[] strArr2 = msg[j].split("#");
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
	} else if (msgKind.equals("SMSLOG")) {
	    for (int j = 0; j < msg.length; j++) {
		List<smsLogClass> temp_list = new ArrayList<smsLogClass>();
		temp_list.clear();
		String[] strArr2 = msg[j].split("#");
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
	} else if (msgKind.equals("TRALOG")) {
	    for (int j = 0; j < msg.length; j++) {
		String[] strArr2 = msg[j].split(";");
		HashMap<String, String> map = new HashMap<String, String>();
		String traMsg = "";
		traMsg += "移动流量：" + strArr2[1] + "；总流量：" + strArr2[2];
		map.put("time", "采样时间：" + strArr2[0]);
		map.put("traStatus", traMsg);
		list_traLog.add(map);
	    }
	}
	Log.v("display", "finish msg");
    }

    public void prepareDisplay() {
	if (msgKind.equals("CALLLOG")) {
	    adapter_callLog = new TreeViewAdapter(this);

	    adapter_callLog.RemoveAll();
	    adapter_callLog.notifyDataSetChanged();
	    List<TreeNode> treeNode_callLog = adapter_callLog.GetTreeNode();
	    for (int i = 0; i < groups_callLog.size(); i++) {
		TreeViewAdapter.TreeNode node = new TreeViewAdapter.TreeNode();
		String name = "" + groups_callLog.get(i);
		Log.v("test display", "get from list  " + name);

		node.parent = name;
		List<callLogClass> temp_list = new ArrayList<callLogClass>();
		temp_list = children_callLog.get(name);
		for (int ii = 0; ii < temp_list.size(); ii++) {
		    String strTemp = "号码:" + temp_list.get(ii).callNum
			    + "  时间:" + temp_list.get(ii).callDate + "  通话时长:"
			    + temp_list.get(ii).callTime;
		    if (temp_list.get(ii).callType == 1) {
			strTemp += "  类型:来电";
		    } else if (temp_list.get(ii).callType == 2) {
			strTemp += "  类型:拨出";
		    } else if (temp_list.get(ii).callType == 3) {
			strTemp += "  类型:未接";
		    }
		    node.children.add(strTemp);
		}
		treeNode_callLog.add(node);
	    }
	    adapter_callLog.UpdateTreeNode(treeNode_callLog);
	} else if (msgKind.equals("SMSLOG")) {
	    adapter_smsLog = new TreeViewAdapter(this);
	    adapter_smsLog.RemoveAll();
	    adapter_smsLog.notifyDataSetChanged();
	    List<TreeNode> treeNode_smsLog = adapter_smsLog.GetTreeNode();
	    for (int i = 0; i < groups_smsLog.size(); i++) {
		TreeViewAdapter.TreeNode node = new TreeViewAdapter.TreeNode();
		node.parent = groups_smsLog.get(i);
		List<smsLogClass> temp_list = new ArrayList<smsLogClass>();
		temp_list = children_smsLog.get(groups_smsLog.get(i));
		for (int ii = 0; ii < temp_list.size(); ii++) {
		    String strTemp = "ID:" + temp_list.get(ii).id + "  号码:"
			    + temp_list.get(ii).smsNum + "  时间:"
			    + temp_list.get(ii).smsDate;
		    if (temp_list.get(ii).smsType == 1) {
			strTemp += "  类型:接收";
		    } else if (temp_list.get(ii).smsType == 2) {
			strTemp += "  类型:发送";
		    }
		    node.children.add(strTemp);
		}
		treeNode_smsLog.add(node);
	    }
	    adapter_smsLog.UpdateTreeNode(treeNode_smsLog);
	}
    }
}
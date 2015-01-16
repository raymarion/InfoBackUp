package cn.edu.ustc.weiking;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

public class UploadOption extends Activity {
    /** Called when the Activity is first created */
    /** 参数设置 */
    CheckBox isAutoUploadCheckBox;
    CheckBox isUploadCallLogCheckBox;
    CheckBox isUploadSMSLogCheckBox;
    CheckBox isUploadAPPLogCheckBox;
    CheckBox isUploadNetworkTrafficStatsLocCheckBox;
    CheckBox isUploadPhoneContactsCheckBox;
    LinearLayout pickTimeLinearLayout;
    TextView uploadCycTextView;
    Bundle bundle;
    boolean isAuto;
    boolean isUpload[];
    long uploadCyc = 360000;
    int uploadHour = 6;
    int uploadMinute = 0;
    InfoCollectService info;

    /** 获得组件 */
    public void getWidget() {
	isAutoUploadCheckBox = (CheckBox) findViewById(R.id.isAutoUploadCheckBox);
	pickTimeLinearLayout = (LinearLayout) findViewById(R.id.linearLayout_PickTime);
	uploadCycTextView = (TextView) findViewById(R.id.textView_UploadCyc);
	if (isAuto) {
	    isAutoUploadCheckBox.setChecked(true);
	    // isAutoUploadCheckBox.setText("自动上传（上传周期：" + uploadHour + "小时"
	    // + uploadMinute + "分钟）");
	    uploadCycTextView.setText("上传周期：" + uploadHour + "小时"
		    + uploadMinute / 10 + "" + uploadMinute % 10 + "分钟");
	} else {
	    isAutoUploadCheckBox.setChecked(false);
	    uploadCyc = -1;
	}

	isUploadCallLogCheckBox = (CheckBox) findViewById(R.id.isUploadCallLogCheckBox);
	if (isUpload[0])
	    isUploadCallLogCheckBox.setChecked(true);
	else
	    isUploadCallLogCheckBox.setChecked(false);

	isUploadSMSLogCheckBox = (CheckBox) findViewById(R.id.isUploadSMSLogCheckBox);
	if (isUpload[1])
	    isUploadSMSLogCheckBox.setChecked(true);
	else
	    isUploadSMSLogCheckBox.setChecked(false);

	isUploadNetworkTrafficStatsLocCheckBox = (CheckBox) findViewById(R.id.isUploadNetworkTrafficStatsLocCheckBox);
	if (isUpload[2])
	    isUploadNetworkTrafficStatsLocCheckBox.setChecked(true);
	else
	    isUploadNetworkTrafficStatsLocCheckBox.setChecked(false);

	isUploadAPPLogCheckBox = (CheckBox) findViewById(R.id.isUploadAPPLogCheckBox);
	if (isUpload[3])
	    isUploadAPPLogCheckBox.setChecked(true);
	else
	    isUploadAPPLogCheckBox.setChecked(false);

	isUploadPhoneContactsCheckBox = (CheckBox) findViewById(R.id.isUploadPhoneContactsCheckBox);
	if (isUpload[4])
	    isUploadPhoneContactsCheckBox.setChecked(true);
	else
	    isUploadPhoneContactsCheckBox.setChecked(false);
    }

    /** 获得主界面传递的信息 */
    public void getInfoFromMain() {
	bundle = getIntent().getExtras();
	isAuto = bundle.getBoolean("isAutoCheckValue");
	isUpload = bundle.getBooleanArray("isUploadCheckValue");
	uploadCyc = bundle.getLong("uploadCycValue");
	Log.v("uploadOption", "cyc: " + uploadCyc);
	uploadHour = (int) (uploadCyc / 3600000);
	uploadMinute = (int) ((uploadCyc / 60000) % 60);
    }

    /** 向主界面传递信息 */
    public void sendInfoToMain() {
	Intent intentForResult = new Intent();
	intentForResult.putExtra("isAutoCheckValue", isAuto);
	intentForResult.putExtra("isUploadCheckValue", isUpload);
	intentForResult.putExtra("uploadCycValue", uploadCyc); // 暂时定为手动上传
	setResult(RESULT_OK, intentForResult);
	Log.v("uo", "sendSucceed");
    }

    /** 为控件添加监听 */
    public void regiestListener() {
	isAutoUploadCheckBox
		.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
		    @Override
		    public void onCheckedChanged(CompoundButton buttonView,
			    boolean isChecked) {
			if (isChecked) {
			    isAuto = true;
			    // onPickTime();
			} else {
			    isAuto = false;
			}
		    }
		});
	pickTimeLinearLayout.setOnClickListener(new View.OnClickListener() {

	    @Override
	    public void onClick(View v) {
		// TODO Auto-generated method stub
		onPickTime();
	    }
	});

	isUploadCallLogCheckBox
		.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
		    @Override
		    public void onCheckedChanged(CompoundButton buttonView,
			    boolean isChecked) {
			// TODO Auto-generated method stub
			if (isChecked) {
			    isUpload[0] = true;
			} else {
			    isUpload[0] = false;
			}
		    }
		});
	isUploadSMSLogCheckBox
		.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
		    @Override
		    public void onCheckedChanged(CompoundButton buttonView,
			    boolean isChecked) {
			// TODO Auto-generated method stub
			if (isChecked) {
			    isUpload[1] = true;
			} else {
			    isUpload[1] = false;
			}
		    }
		});
	isUploadAPPLogCheckBox
		.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
		    @Override
		    public void onCheckedChanged(CompoundButton buttonView,
			    boolean isChecked) {
			// TODO Auto-generated method stub
			if (isChecked) {
			    isUpload[2] = true;
			} else {
			    isUpload[2] = false;
			}
		    }
		});
	isUploadNetworkTrafficStatsLocCheckBox
		.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
		    @Override
		    public void onCheckedChanged(CompoundButton buttonView,
			    boolean isChecked) {
			// TODO Auto-generated method stub
			if (isChecked) {
			    isUpload[3] = true;
			} else {
			    isUpload[3] = false;
			}
		    }
		});
	isUploadPhoneContactsCheckBox
		.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
		    @Override
		    public void onCheckedChanged(CompoundButton buttonView,
			    boolean isChecked) {
			// TODO Auto-generated method stub
			if (isChecked) {
			    isUpload[4] = true;
			} else {
			    isUpload[4] = false;
			}
		    }
		});
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.uploadoption);
	getInfoFromMain(); // 获得信息必须在getWidget()函数之前调用
	getWidget();
	regiestListener();
    }

    @Override
    public void onBackPressed() {
	sendInfoToMain(); // 向主界面发送消息
	super.onBackPressed();
    }

    @Override
    public void onDestroy() {
	super.onDestroy();
    }

    public void onPickTime() {
	LayoutInflater inflater = (LayoutInflater) getApplicationContext()
		.getSystemService(LAYOUT_INFLATER_SERVICE);
	View view = inflater.inflate(R.layout.timepicker, null);
	AlertDialog.Builder builder = new AlertDialog.Builder(UploadOption.this);
	builder.setView(view);
	final TimePicker tp = (TimePicker) view.findViewById(R.id.timePicker);
	tp.setIs24HourView(true);
	tp.setCurrentHour(uploadHour);
	tp.setCurrentMinute(uploadMinute);
	builder.setTitle("设置上传周期").setIcon(android.R.drawable.ic_dialog_info)
		.setCancelable(false)
		.setPositiveButton("确定", new DialogInterface.OnClickListener() {

		    @Override
		    public void onClick(DialogInterface dialog, int which) {
			// TODO Auto-generated method stub
			uploadHour = tp.getCurrentHour();
			uploadMinute = tp.getCurrentMinute();
			if (uploadHour < 0) {
			    timeTooSmall(UploadOption.this);
			} else {
			    uploadCyc = (uploadHour * 60 + uploadMinute) * 60000;
			    // isAutoUploadCheckBox.setText("自动上传（上传周期："
			    // + uploadHour + "小时" + uploadMinute + "分钟）");
			    uploadCycTextView.setText("上传周期：" + uploadHour
				    + "小时" + uploadMinute / 10 + ""
				    + uploadMinute % 10 + "分钟");
			}
		    }
		}).create().show();
    }

    public void timeTooSmall(Context context) {
	AlertDialog.Builder dialog = new AlertDialog.Builder(context);
	dialog.setTitle("错误").setIcon(android.R.drawable.ic_dialog_info)
		.setMessage("上传周期过小，请重新设置！").setCancelable(false)
		.setPositiveButton("确定", new DialogInterface.OnClickListener() {

		    @Override
		    public void onClick(DialogInterface dialog, int which) {
			// TODO Auto-generated method stub
			dialog.cancel();
			onPickTime();
		    }
		}).create().show();
    }
}
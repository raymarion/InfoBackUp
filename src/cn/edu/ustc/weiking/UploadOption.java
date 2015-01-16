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
    /** �������� */
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

    /** ������ */
    public void getWidget() {
	isAutoUploadCheckBox = (CheckBox) findViewById(R.id.isAutoUploadCheckBox);
	pickTimeLinearLayout = (LinearLayout) findViewById(R.id.linearLayout_PickTime);
	uploadCycTextView = (TextView) findViewById(R.id.textView_UploadCyc);
	if (isAuto) {
	    isAutoUploadCheckBox.setChecked(true);
	    // isAutoUploadCheckBox.setText("�Զ��ϴ����ϴ����ڣ�" + uploadHour + "Сʱ"
	    // + uploadMinute + "���ӣ�");
	    uploadCycTextView.setText("�ϴ����ڣ�" + uploadHour + "Сʱ"
		    + uploadMinute / 10 + "" + uploadMinute % 10 + "����");
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

    /** ��������洫�ݵ���Ϣ */
    public void getInfoFromMain() {
	bundle = getIntent().getExtras();
	isAuto = bundle.getBoolean("isAutoCheckValue");
	isUpload = bundle.getBooleanArray("isUploadCheckValue");
	uploadCyc = bundle.getLong("uploadCycValue");
	Log.v("uploadOption", "cyc: " + uploadCyc);
	uploadHour = (int) (uploadCyc / 3600000);
	uploadMinute = (int) ((uploadCyc / 60000) % 60);
    }

    /** �������洫����Ϣ */
    public void sendInfoToMain() {
	Intent intentForResult = new Intent();
	intentForResult.putExtra("isAutoCheckValue", isAuto);
	intentForResult.putExtra("isUploadCheckValue", isUpload);
	intentForResult.putExtra("uploadCycValue", uploadCyc); // ��ʱ��Ϊ�ֶ��ϴ�
	setResult(RESULT_OK, intentForResult);
	Log.v("uo", "sendSucceed");
    }

    /** Ϊ�ؼ���Ӽ��� */
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
	getInfoFromMain(); // �����Ϣ������getWidget()����֮ǰ����
	getWidget();
	regiestListener();
    }

    @Override
    public void onBackPressed() {
	sendInfoToMain(); // �������淢����Ϣ
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
	builder.setTitle("�����ϴ�����").setIcon(android.R.drawable.ic_dialog_info)
		.setCancelable(false)
		.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {

		    @Override
		    public void onClick(DialogInterface dialog, int which) {
			// TODO Auto-generated method stub
			uploadHour = tp.getCurrentHour();
			uploadMinute = tp.getCurrentMinute();
			if (uploadHour < 0) {
			    timeTooSmall(UploadOption.this);
			} else {
			    uploadCyc = (uploadHour * 60 + uploadMinute) * 60000;
			    // isAutoUploadCheckBox.setText("�Զ��ϴ����ϴ����ڣ�"
			    // + uploadHour + "Сʱ" + uploadMinute + "���ӣ�");
			    uploadCycTextView.setText("�ϴ����ڣ�" + uploadHour
				    + "Сʱ" + uploadMinute / 10 + ""
				    + uploadMinute % 10 + "����");
			}
		    }
		}).create().show();
    }

    public void timeTooSmall(Context context) {
	AlertDialog.Builder dialog = new AlertDialog.Builder(context);
	dialog.setTitle("����").setIcon(android.R.drawable.ic_dialog_info)
		.setMessage("�ϴ����ڹ�С�����������ã�").setCancelable(false)
		.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {

		    @Override
		    public void onClick(DialogInterface dialog, int which) {
			// TODO Auto-generated method stub
			dialog.cancel();
			onPickTime();
		    }
		}).create().show();
    }
}
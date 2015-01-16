package cn.edu.ustc.weiking;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

public class InfoBackUpActivity extends Activity {
	/** Called when the activity is first created. */
	/** �������� */
	private final int UPLOAD_PARA = 10; // ��uploadOption������Ϣ��requestCode
	private final int INFODISPLAY_PARA = 11;
	public final static String SOCKET_IP = "202.38.95.146";//the IP address in USTC NIC blade Server.
	public final static int SOCKET_PORT = 1500;
	Button uploadOptionButton; // �ϴ�ѡ�ť
	Button getInfoButton; // ��ȡ�Լ�ͳ����Ϣ��ť
	Button manualUploadButton; // �ֶ��ϴ���ť
	Button startServiceButton; // ������Ϣ�ռ�����ť
	Button uploadPhoneConButton;
	Button downloadPhoneConButton;
	ServiceInterface mServiceInterface;
	InfoCollectService mInfoCollectService;// �ɼ���Ϣ����
	GetMessageService mGetMessageService; // ��ȡ��Ϣ����
	ProgressDialog pd;
	ProgressDialog pdForCon;

	private boolean isAuto = false; // �Ƿ��Զ��ϴ�
	private boolean isUpload[] = { true, true, true, false, false }; // �Ƿ��ϴ�ͨ����¼�����ż�¼��������Ϣ��λ�á�Ӧ�ó���ͨѶ¼
	private long uploadCyc = 21600000;
	private long uploadTime = 0;
	private long uploadPhoneTime = 0;
	private String usrName = "";
	private long timeRemeberUsr = 0;
	private boolean isRemeberUsr = false;
	private boolean isEverWork = false;
	private boolean isLogoutFinish = true;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		InfoCollectService.registerIntent(this);
		GetMessageService.registerIntent(this);

		try {
			Log.v("main11111", "1");
			getConfig();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long time = System.currentTimeMillis(); // ����ɱ���ʱ��

		getWidget(); // ������
		regiestListener(); // ��Ӽ���
		if (!isWorked(InfoBackUpActivity.this)
				&& (usrName.equals("") || (time > timeRemeberUsr + 604800000))) {
			// Ϊ�˲����ܷ���֪ͨ���������            ???????????????????????????????????????????????
			onLogin();
			// startServiceButton.performClick();
		} else {
			isRemeberUsr = true;
			Log.v("main11111", "2");

			Intent intent = new Intent(InfoBackUpActivity.this,
					InfoCollectService.class);
			bindService(intent, serviceConnection, 0); // ��Service
			Intent intent2 = new Intent(InfoBackUpActivity.this,
					GetMessageService.class);
			bindService(intent2, serviceConnection2, 0);
			Log.v("main111111", " 1" + usrName);
		}
	}

	@Override
	public void onBackPressed() {
		if (isWorked(InfoBackUpActivity.this)) {
			Log.v("main", "unbind");
			unbindService(serviceConnection);
		} else {
			onLogout();
		}

		super.onBackPressed();
	}

	@Override
	public void onDestroy() {
		try {
			saveConfig();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while (!isLogoutFinish) {
		}
		;
		super.onDestroy();
	}

	/**
	 * ��ȡ������Ϣ
	 * 
	 * @throws IOException
	 */
	public void getConfig() throws IOException {
		FileInputStream inStream = null;
		try {
			inStream = this.openFileInput("infoConfig.txt");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		byte[] bytes = new byte[100];
		int length = inStream.read(bytes);
		if (length != -1) {
			String str = "";
			for (int i = 0, j = 0; i < length; i++) {
				if (bytes[i] != ' ') {
					str += (char) bytes[i];
				} else {
					switch (j) {
					case 0:
						if (Integer.parseInt(str) == 1)
							isAuto = true;
						else
							isAuto = false;
						str = "";
						break;
					case 1:
						if (Integer.parseInt(str) == 1)
							isUpload[0] = true;
						else
							isUpload[0] = false;
						str = "";
						break;
					case 2:
						if (Integer.parseInt(str) == 1)
							isUpload[1] = true;
						else
							isUpload[1] = false;
						str = "";
						break;
					case 3:
						if (Integer.parseInt(str) == 1)
							isUpload[2] = true;
						else
							isUpload[2] = false;
						str = "";
						break;
					case 4:
						if (Integer.parseInt(str) == 1)
							isUpload[3] = true;
						else
							isUpload[3] = false;
						str = "";
						break;
					case 5:
						if (Integer.parseInt(str) == 1)
							isUpload[4] = true;
						else
							isUpload[4] = false;
						str = "";
						break;
					case 6:
						uploadCyc = Long.parseLong(str);
						str = "";
						break;
					case 7:
						uploadTime = Long.parseLong(str);
						str = "";
						break;
					case 8:
						uploadPhoneTime = Long.parseLong(str);
						break;
					case 9:
						usrName = str;
						str = "";
						break;
					case 10:
						timeRemeberUsr = Long.parseLong(str);
						str = "";
						break;
					default:
						break;
					}
					j++;
				}
			}
		}
		inStream.close();
		Log.v("main", "get success");
	}

	/**
	 * ����������Ϣ
	 * 
	 * @throws IOException
	 */
	public void saveConfig() throws IOException {
		File mFile = new File("infoConfig.txt");
		if (mFile.exists()) {
			mFile.delete();
		}
		FileOutputStream outStream = null;
		try {
			outStream = this.openFileOutput("infoConfig.txt",
					Context.MODE_PRIVATE);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			Log.v("main", "out null");
			return;
		}
		String buffer = "";
		if (isAuto) {
			buffer = buffer + "1 ";
		} else {
			buffer = buffer + "0 ";
		}
		for (int i = 0; i < 5; i++) {
			if (isUpload[i]) {
				buffer += "1 ";
			} else {
				buffer += "0 ";
			}
		}
		buffer += uploadCyc + " " + uploadTime + " " + uploadPhoneTime + " ";
		if (isRemeberUsr) {
			buffer += usrName + " " + timeRemeberUsr + " ";
		}
		byte[] bytes = buffer.getBytes();
		outStream.write(bytes);
		outStream.close();
		Log.v("main", "save success");
	}

	/** ��½���� */
	public void onLogin() {
		LayoutInflater inflater = (LayoutInflater) getApplicationContext()
				.getSystemService(LAYOUT_INFLATER_SERVICE);
		final View view = inflater.inflate(R.layout.login, null);
		AlertDialog.Builder builder = new AlertDialog.Builder(
				InfoBackUpActivity.this);
		builder.setView(view);
		final EditText name = (EditText) view
				.findViewById(R.id.editText_usrName);
		final EditText password = (EditText) view
				.findViewById(R.id.editText_Password);
		final CheckBox isRemeberUsrCheckBox = (CheckBox) view
				.findViewById(R.id.checkBox_remeberUsr);
		isRemeberUsrCheckBox
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						// TODO Auto-generated method stub
						if (isChecked) {
							isRemeberUsr = true;
							long time = System.currentTimeMillis();
							if (time > timeRemeberUsr + 604800000)
								timeRemeberUsr = time;
						} else {
							isRemeberUsr = false;
						}
					}
				});
		builder.setTitle("�û���½")
				.setCancelable(false)
				.setPositiveButton("��½", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(final DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						final String strName = name.getText().toString();
						final String strPassword = password.getText()
								.toString();
						Log.v("main", "login: " + strName);
						Log.v("main", "password: " + strPassword);
						if (strName.equals("")) {
							usrIllegal(InfoBackUpActivity.this, 0);
							dialog.cancel();
						} else {
							dialog.cancel();
							pdForCon = ProgressDialog.show(
									InfoBackUpActivity.this, "Please Wait",
									"��¼�У���ȴ�...");
							new Thread() {
								@Override
								public void run() {
									int nResult = 0;
									Socket mSocket = null;
									try {
										InetSocketAddress add = new InetSocketAddress(
												SOCKET_IP, SOCKET_PORT);
										mSocket = new Socket();
										mSocket.connect(add, 10000); // �ȴ���ʱ������IOException
										PrintWriter out = new PrintWriter(
												new BufferedWriter(
														new OutputStreamWriter(
																mSocket.getOutputStream())),
												true);
										String msg = "login@%" + strName + "#"
												+ strPassword + "#";
										out.println(msg);
										// ���ܷ���������Ϣ
										BufferedReader br = new BufferedReader(
												new InputStreamReader(mSocket
														.getInputStream()));
										String mstr = br.readLine();
										nResult = Integer.parseInt(mstr);
										out.close();
										br.close();
										mSocket.close();
									} catch (UnknownHostException e) {
										Log.v("LocalServiceSendError",
												e.toString());
									} catch (IOException e) {
										Log.v("LocalServiceSendError",
												e.toString());
										nResult = -1;
										// networkFalse(InfoBackUpActivity.this);
									} catch (Exception e) {
										Log.v("LocalServiceSendError",
												e.toString());
									}
									Message msg = handlerForCon.obtainMessage();
									msg.arg1 = 0;
									String str = "" + nResult + "#" + strName;
									msg.obj = str;
									handlerForCon.sendMessage(msg);
								}
							}.start();

						}
					}
				})
				.setNeutralButton("ע��", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.cancel();
						onRegist();
					}
				})
				.setNegativeButton("�˳�", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.cancel();
						InfoBackUpActivity.this.onBackPressed();
					}
				}).create().show();
	}

	/** ע����� */
	public void onRegist() {
		LayoutInflater inflater = (LayoutInflater) getApplicationContext()
				.getSystemService(LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.regist, null);
		AlertDialog.Builder builder = new AlertDialog.Builder(
				InfoBackUpActivity.this);
		builder.setView(view);
		final EditText name = (EditText) view
				.findViewById(R.id.editText_registName);
		final EditText password = (EditText) view
				.findViewById(R.id.editText_registPassword);
		final EditText passwordCon = (EditText) view
				.findViewById(R.id.editText_registPasswordCon);
		builder.setTitle("���û�ע��")
				.setCancelable(false)
				.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(final DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						final String strName = name.getText().toString();
						final String strPassword = password.getText()
								.toString();
						final String strPasswordCon = passwordCon.getText()
								.toString();
						if (strName.equals("")) {
							usrIllegal(InfoBackUpActivity.this, 1);
							dialog.cancel();
						} else {
							Log.v("main", "name: " + strName);
							Log.v("main", "password: " + strPassword);
							Log.v("main", "passwordCon: " + strPasswordCon);
							if (!strPassword.equals(strPasswordCon)
									|| strPassword.equals("")) {
								passwordNotSame(InfoBackUpActivity.this);
							} else {
								dialog.cancel();
								pdForCon = ProgressDialog.show(
										InfoBackUpActivity.this, "Please Wait",
										"ע���У���ȴ�...");
								new Thread() {
									@Override
									public void run() {
										int nResult = 0;
										Socket mSocket = null;
										try {
											InetSocketAddress add = new InetSocketAddress(
													SOCKET_IP, SOCKET_PORT);
											mSocket = new Socket();
											mSocket.connect(add, 10000); // �ȴ���ʱ������IOException
											PrintWriter out = new PrintWriter(
													new BufferedWriter(
															new OutputStreamWriter(
																	mSocket.getOutputStream())),
													true);
											String msg = "regist@%" + strName
													+ "#" + strPassword + "#"
													+ strPasswordCon + "#";
											out.println(msg);
											// ���ܷ���������Ϣ
											BufferedReader br = new BufferedReader(
													new InputStreamReader(
															mSocket.getInputStream()));
											String mstr = br.readLine();
											nResult = Integer.parseInt(mstr);

											out.close();
											br.close();
											mSocket.close();
										} catch (UnknownHostException e) {
											Log.v("main",
													"Error: " + e.toString());
										} catch (IOException e) {
											Log.v("main",
													"Error: " + e.toString());
											nResult = -1;
											// networkFalse(InfoBackUpActivity.this);
										} catch (Exception e) {
											Log.v("LocalServiceSendError",
													e.toString());
										}
										Message msg = handlerForCon
												.obtainMessage();
										msg.arg1 = 1;
										String str = "" + nResult + "#";
										msg.obj = str;
										handlerForCon.sendMessage(msg);
									}
								}.start();
							}
						}
					}
				})
				.setNeutralButton("���ص�¼",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								// TODO Auto-generated method stub
								dialog.cancel();
								onLogin();
							}
						})
				.setNegativeButton("�˳�", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.cancel();
						InfoBackUpActivity.this.onBackPressed();
					}
				}).create().show();
	}

	/** �˳���¼ */
	public void onLogout() {
		pdForCon = ProgressDialog.show(InfoBackUpActivity.this, "Please Wait",
				"�����˳��У���ȴ�...");
		isLogoutFinish = false;
		new Thread() {
			@Override
			public void run() {
				Socket mSocket = null;
				int result = 0;
				try {
					InetSocketAddress add = new InetSocketAddress(SOCKET_IP,
							SOCKET_PORT);
					mSocket = new Socket();
					mSocket.connect(add, 10000); //
					PrintWriter out = new PrintWriter(new BufferedWriter(
							new OutputStreamWriter(mSocket.getOutputStream())),
							true);
					String msg = "logout@%" + usrName + "#";
					out.println(msg);
					// ���ܷ���������Ϣ
					BufferedReader br = new BufferedReader(
							new InputStreamReader(mSocket.getInputStream()));
					// String mstr = br.readLine();
				} catch (UnknownHostException e) {
					Log.v("main", "Error: " + e.toString());
				} catch (IOException e) {
					Log.v("main", "Error: " + e.toString());
					// networkFalse(InfoBackUpActivity.this);
					result = -1;
				} catch (Exception e) {
					Log.v("LocalServiceSendError", e.toString());
				}
				Message msg = handlerForCon.obtainMessage();
				msg.arg1 = 2;
				String str = "" + result + "#";
				msg.obj = str;
				handlerForCon.sendMessage(msg);
			}
		}.start();
	}

	/** ���粻���� */
	public void networkFalse(Context context) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle("����").setIcon(android.R.drawable.ic_dialog_info)
				.setMessage("���粻���ã����Ժ����ԣ�").setCancelable(false)
				.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.cancel();
						InfoBackUpActivity.this.onBackPressed();
					}
				}).create().show();
	}

	/** ��¼ʱ������� */
	public void passwordWrong(Context context) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle("����").setIcon(android.R.drawable.ic_dialog_info)
				.setMessage("�������").setCancelable(false)
				.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.cancel();
						onLogin();
					}
				}).create().show();
	}

	/** ��¼ʱ�û��������� */
	public void usrNotFound(Context context) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle("����").setIcon(android.R.drawable.ic_dialog_info)
				.setMessage("�û������ڣ�����ע�ᣡ").setCancelable(false)
				.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.cancel();
						onRegist();
					}
				}).create().show();
	}

	/** �û��ѵ�¼ */
	public void usrAlreadyLogin(Context context) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle("����").setIcon(android.R.drawable.ic_dialog_info)
				.setMessage("�û��ѵ�¼�������ظ���¼��").setCancelable(false)
				.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.cancel();
						onLogin();
					}
				}).create().show();
	}

	/** �û�������Ϊ�� */
	public void usrIllegal(Context context, final int flag) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle("����").setIcon(android.R.drawable.ic_dialog_info)
				.setMessage("�û�������Ϊ�գ�").setCancelable(false)
				.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.cancel();
						if (flag == 0) {
							onLogin();
						} else {
							onRegist();
						}
					}
				}).create().show();
	}

	/** ע��ʱ�����������벻��ͬ */
	public void passwordNotSame(Context context) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle("����").setIcon(android.R.drawable.ic_dialog_info)
				.setMessage("�����������������룡").setCancelable(false)
				.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.cancel();
						onRegist();
					}
				}).create().show();
	}

	/** ע��ɹ� */
	public void registSuccess(Context context) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle("�ɹ���").setIcon(android.R.drawable.ic_dialog_info)
				.setMessage("ע��ɹ������¼��").setCancelable(false)
				.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.cancel();
						onLogin();
					}
				}).create().show();
	}

	/** �û����Ѵ��� */
	public void registNameUsed(Context context) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle("����").setIcon(android.R.drawable.ic_dialog_info)
				.setMessage("�û����Ѵ��ڣ����������룡").setCancelable(false)
				.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.cancel();
						onRegist();
					}
				}).create().show();
	}

	/** ��÷���ֵ */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case UPLOAD_PARA:
			switch (resultCode) {
			case RESULT_OK: // ˼������ô�趨һ��ͨ�õĲ����������Ƿ��ͷ��ͽ��շ�����ʽ����100
				boolean isAuto1 = data.getBooleanExtra("isAutoCheckValue",
						false);
				boolean[] isUpload1 = data
						.getBooleanArrayExtra("isUploadCheckValue");
				long uploadCyc1 = data.getLongExtra("uploadCycValue", -1);
				if (isAuto1) {
					manualUploadButton.setEnabled(false);
				} else {
					manualUploadButton.setEnabled(true);
					manualUploadButton.setOnClickListener(manualUpload);
				}
				if (isAuto != isAuto1 || isUpload != isUpload1
						|| uploadCyc != uploadCyc1) {
					isAuto = isAuto1;
					isUpload = isUpload1;
					uploadCyc = uploadCyc1;
					mInfoCollectService.updateOption(isAuto, isUpload,
							uploadCyc);
				}
				break;
			default:
				Toast.makeText(this, "���󷵻�ֵ[" + resultCode + "]",
						Toast.LENGTH_LONG).show();
			}
			break;
		case INFODISPLAY_PARA:
			break;
		default:
			Toast.makeText(this, "���󷵻�ֵ[" + requestCode + "]",
					Toast.LENGTH_LONG).show();
		}
	}

	/** ������ */
	public void getWidget() {
		uploadOptionButton = (Button) findViewById(R.id.uploadOptionButton);
		getInfoButton = (Button) findViewById(R.id.getInfoButton);
		manualUploadButton = (Button) findViewById(R.id.manualUploadButton);
		startServiceButton = (Button) findViewById(R.id.startServiceButton);
		if (isWorked(InfoBackUpActivity.this)) {
			startServiceButton.setText("�ر���Ϣ�ռ�����");
		} else {
			startServiceButton.setText("������Ϣ�ռ�����");
		}
		uploadPhoneConButton = (Button) findViewById(R.id.uploadPhoneButton);

		downloadPhoneConButton = (Button) findViewById(R.id.downloadPhoneConButton);
	}

	/** Ϊ��ť��Ӽ�����C++�г��¼��� */
	public void regiestListener() {
		uploadOptionButton.setOnClickListener(onClickUploadOption);
		startServiceButton.setOnClickListener(onClickStartService);
		if (isAuto) { // ����Զ��ϴ������ð�ť���ɵ��
			// manualUploadButton.setClickable(false);
			manualUploadButton.setEnabled(false);
		} else {
			manualUploadButton.setOnClickListener(manualUpload);
		}
		getInfoButton.setOnClickListener(getInfo);
		if (uploadPhoneTime == 0) {
			uploadPhoneConButton.setText("����ͨѶ¼");
		} else {

			SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			Date date = new Date(uploadPhoneTime);
			String strDate = sfd.format(date);
			uploadPhoneConButton.setText("����ͨѶ¼���ϴα���ʱ��"
					+ strDate.substring(0, strDate.indexOf(" ")) + ")");
		}
		uploadPhoneConButton.setOnClickListener(onClickUploadPhoneCon);
		downloadPhoneConButton.setOnClickListener(onClickDownloadPhoneCon);
	}

	/** �ж�InfoCollectService Service�Ƿ�������״̬ */
	public boolean isWorked(Context context) {
		ActivityManager myManager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE); // ��ȡ����
		ArrayList<RunningServiceInfo> runningService = (ArrayList<RunningServiceInfo>) myManager
				.getRunningServices(30); // ��ȡ�������еķ���
		for (int i = 0; i < runningService.size(); i++) {
			if (runningService.get(i).service.getClassName().toString()
					.equals("cn.edu.ustc.weiking.InfoCollectService")) {
				Log.v("MainActivity", "ServiceIsWorked");
				return true;
			}
		}
		Log.v("MainActivity", "ServiceIsNotWorking");
		return false;
	}

	ServiceConnection serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			// TODO Auto-generated method stub
			Log.v("MainActivity", "�Ѱ󶨵�service");
			mInfoCollectService = ((InfoCollectService.ServiceBinder) arg1)
					.getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			mInfoCollectService = null;
			Log.v("MainActivity", "���ѶϿ�");
		}

	};
	ServiceConnection serviceConnection2 = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			Log.v("MainActivity", "bind get message service");
			mGetMessageService = ((GetMessageService.ServiceBinder) service)
					.getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			
			mGetMessageService = null;
		}

	};

	// ��ȡͨѶ¼
	public String getPhoneContacts() {
		String msg = "";
		final String[] PHONES_PROJECTION = new String[] { Phone.DISPLAY_NAME,
				Phone.NUMBER, Phone.CONTACT_ID };
		Cursor phoneCursor = getContentResolver().query(Phone.CONTENT_URI,
				PHONES_PROJECTION, null, null, null);
		if (phoneCursor != null) {
			while (phoneCursor.moveToNext()) {
				String phoneNumber = phoneCursor.getString(1); // �õ��ֻ�����
				if (TextUtils.isEmpty(phoneNumber)) // ���ֻ�����Ϊ���ֶ� ������ǰѭ��
					continue;
				String contactName = phoneCursor.getString(0); // �õ���ϵ������
				Long contactid = Long.parseLong(phoneCursor.getString(2)); // �õ���ϵ��ID
				msg += "ContactId: " + contactid + ";ContactName: "
						+ contactName + ";ContactNum: " + phoneNumber
						+ ";ContactPos: " + "Phone" + "&";
			}
		}
		phoneCursor.close();
		Cursor simCursor = getContentResolver().query(
				Uri.parse("content://icc/adn"), PHONES_PROJECTION, null, null,
				null);
		if (simCursor != null) {
			while (simCursor.moveToNext()) {
				String phoneNumber = simCursor.getString(1); // �õ��ֻ�����
				if (TextUtils.isEmpty(phoneNumber)) // ���ֻ�����Ϊ�յĻ���Ϊ���ֶ� ������ǰѭ��
					continue;
				String contactName = simCursor.getString(0); // �õ���ϵ������
				Long contactid = (long) 0; // �õ���ϵ��ID,SIM���޷���ȡId
				msg += "ContactId: " + contactid + ";ContactName: "
						+ contactName + ";ContactNum: " + phoneNumber
						+ ";ContactPos: " + "SIM" + "&";
			}
		}
		simCursor.close();
		return msg;
	}

	public void uploadPhoneContacts() {
		Socket mSocket = null;
		try {
			String msg = getPhoneContacts();
			mSocket = new Socket(SOCKET_IP, SOCKET_PORT);
			PrintWriter out = new PrintWriter(new BufferedWriter(
					new OutputStreamWriter(mSocket.getOutputStream())), true);
			msg = "phoneContacts@%" + usrName + "#" + msg;
			out.println(msg);
			// ���շ���������Ϣ
			BufferedReader br = new BufferedReader(new InputStreamReader(
					mSocket.getInputStream()));
			String mstr = br.readLine();
			Log.v("LocalService", "upload phone contacts " + mstr);
			out.close();
			br.close();
			mSocket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			Log.v("LocalServiceSendError", e.toString());
		}
	}

	/** ����ϴ�ѡ�ť */
	public Button.OnClickListener onClickUploadOption = new Button.OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Bundle bundle = new Bundle(); // ׼�����������ݸ�UploadOptionActivity
			bundle.putBoolean("isAutoCheckValue", isAuto);
			bundle.putBooleanArray("isUploadCheckValue", isUpload);
			bundle.putLong("uploadCycValue", uploadCyc);
			Intent intent = new Intent(InfoBackUpActivity.this,
					UploadOption.class);
			intent.putExtras(bundle); // ��intent������Ҫ���ݵĲ���
			startActivityForResult(intent, UPLOAD_PARA);
			Log.v("MainActivity", "OnClickUploadOption");
		}
	};
	/** ��������Ϣ��ť */
	public Button.OnClickListener getInfo = new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			Bundle bundle = new Bundle();
			bundle.putString("UsrName", usrName);
			Intent intent = new Intent(InfoBackUpActivity.this,
					DisplayChoice.class);
			intent.putExtras(bundle);
			startActivityForResult(intent, INFODISPLAY_PARA);
			Log.v("MainActivity", "OnClickInfoDisplay");
		}
	};
	/** ����ֶ��ϴ���ť */
	public Button.OnClickListener manualUpload = new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			Log.v("upload", "onclick");
			mInfoCollectService.uploadInfoFromMain();
		}
	};
	/** �����������ť */
	public Button.OnClickListener onClickStartService = new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Log.v("MainActivity", "OnClickService");
			if (!isWorked(InfoBackUpActivity.this)) { // �жϷ����Ƿ��Ѿ�����
				Bundle bundle = new Bundle();
				bundle.putBoolean("isAutoCheckValue", isAuto);
				bundle.putBooleanArray("isUploadCheckValue", isUpload);
				bundle.putLong("uploadCycValue", uploadCyc);
				bundle.putLong("lastUploadTimeValue", uploadTime);
				bundle.putString("usrNameValue", usrName);
				Intent intent = new Intent(InfoBackUpActivity.this,
						InfoCollectService.class);
				intent.putExtras(bundle);
				startService(intent); // ��������
				bindService(intent, serviceConnection, 0); // ��Service
				Log.v("MainActivity", "OnClickStartService");
				startServiceButton.setText("�ر���Ϣ�ռ�����");
				// ���濪����ȡ��Ϣ����
				Bundle bundle2 = new Bundle();
				bundle2.putString("usrNameValue", usrName);
				Intent intent2 = new Intent(InfoBackUpActivity.this,
						GetMessageService.class);
				intent2.putExtras(bundle2);
				startService(intent2);
				bindService(intent2, serviceConnection2, 0);
			} else {
				uploadTime = mInfoCollectService.getLastTime();
				Intent intent = new Intent(InfoBackUpActivity.this,
						InfoCollectService.class);
				stopService(intent); // ��������
				unbindService(serviceConnection);
				Log.v("MainActivity", "OnClickStopService");
				startServiceButton.setText("������Ϣ�ռ�����");
				// ����رջ�ȡ��Ϣ����
				Intent intent2 = new Intent(InfoBackUpActivity.this,
						GetMessageService.class);
				stopService(intent2); // ��������
				unbindService(serviceConnection2);
			}
		}
	};

	/** �������ͨѶ¼ */
	public Button.OnClickListener onClickUploadPhoneCon = new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			processThread();
		}
	};
	/** �������ͨѶ¼���� */
	public Button.OnClickListener onClickDownloadPhoneCon = new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			Bundle bundle = new Bundle();
			bundle.putString("UsrName", usrName);
			Intent intent = new Intent(InfoBackUpActivity.this,
					PhoneContacts.class);
			intent.putExtras(bundle);
			startActivity(intent);
		}
	};

	public void processThread() {
		pd = ProgressDialog.show(InfoBackUpActivity.this, "Please Wait",
				"ͨѶ¼�����У���ȴ�...");
		new Thread() {
			@Override
			public void run() {
				// TODO ���Ϻ�ʱ����
				uploadPhoneContacts();
				handler.sendEmptyMessage(0);
			}
		}.start();
	}

	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			pd.dismiss();
			uploadPhoneTime = System.currentTimeMillis();
			if (uploadPhoneTime == 0) {
				uploadPhoneConButton.setText("����ͨѶ¼");
			} else {
				SimpleDateFormat sfd = new SimpleDateFormat(
						"yyyy-MM-dd hh:mm:ss");
				Date date = new Date(uploadPhoneTime);
				String strDate = sfd.format(date);
				uploadPhoneConButton.setText("����ͨѶ¼���ϴα���ʱ��"
						+ strDate.substring(0, strDate.indexOf(" ")) + ")");
			}
		}
	};
	private final Handler handlerForCon = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			pdForCon.dismiss();
			switch (msg.arg1) {
			case 0: // �û���¼��handler
				String str0 = msg.obj.toString();
				String[] strArr0 = str0.split("#");
				int nResult0 = Integer.parseInt(strArr0[0]);
				String strName = strArr0[1];
				switch (nResult0) {
				case 0:
					usrName = strName;
					isEverWork = true;
					Log.v("usrName", usrName);
					if (!isWorked(InfoBackUpActivity.this)) {
						startServiceButton.performClick();
					}
					break;
				case 4:
					// �������
					passwordWrong(InfoBackUpActivity.this);
					break;
				case 3:
					// �û������ڣ���ע��
					usrNotFound(InfoBackUpActivity.this);
					break;
				case 5:
					// XXX �û��ѵ�¼
					//usrAlreadyLogin(InfoBackUpActivity.this);
					usrName = strName;
					isEverWork = true;
					Log.v("usrName", usrName);
					if (!isWorked(InfoBackUpActivity.this)) {
						startServiceButton.performClick();
					}
					break;
				case -1:
					networkFalse(InfoBackUpActivity.this);
					break;
				default:
					break;
				}
				break;
			case 1: // �û�ע���handler
				String str1 = msg.obj.toString();
				String[] strArr1 = str1.split("#");
				int nResult1 = Integer.parseInt(strArr1[0]);
				switch (nResult1) {
				case 0:
					registSuccess(InfoBackUpActivity.this);
					break;
				case 2:
					registNameUsed(InfoBackUpActivity.this);
					break;
				case -1:
					networkFalse(InfoBackUpActivity.this);
				default:
					break;
				}
				break;
			case 2: // �û��˳���handler
				String str2 = msg.obj.toString();
				String[] strArr2 = str2.split("#");
				int nResult2 = Integer.parseInt(strArr2[0]);
				switch (nResult2) {
				case -1:
					networkFalse(InfoBackUpActivity.this);
					break;
				default:
					break;
				}
				isLogoutFinish = true;
				break;
			default:
				break;
			}

		}
	};

}
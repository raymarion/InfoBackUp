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
	/** 参数设置 */
	private final int UPLOAD_PARA = 10; // 向uploadOption传送信息的requestCode
	private final int INFODISPLAY_PARA = 11;
	public final static String SOCKET_IP = "202.38.95.146";
	public final static int SOCKET_PORT = 1500;
	Button uploadOptionButton; // 上传选项按钮
	Button getInfoButton; // 获取自己统计信息按钮
	Button manualUploadButton; // 手动上传按钮
	Button startServiceButton; // 开启信息收集服务按钮
	Button uploadPhoneConButton;
	Button downloadPhoneConButton;
	ServiceInterface mServiceInterface;
	InfoCollectService mInfoCollectService;// 采集信息服务
	GetMessageService mGetMessageService; // 获取信息服务
	ProgressDialog pd;
	ProgressDialog pdForCon;

	private boolean isAuto = false; // 是否自动上传
	private boolean isUpload[] = { true, true, true, false, false }; // 是否上传通话记录、短信记录、流量信息和位置、应用程序、通讯录
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
		long time = System.currentTimeMillis(); // 换算成北京时间

		getWidget(); // 获得组件
		regiestListener(); // 添加监听
		if (!isWorked(InfoBackUpActivity.this)
				&& (usrName.equals("") || (time > timeRemeberUsr + 604800000))) {
			// 为了测试能否在通知栏打开浏览器            ???????????????????????????????????????????????
			onLogin();
			// startServiceButton.performClick();
		} else {
			isRemeberUsr = true;
			Log.v("main11111", "2");

			Intent intent = new Intent(InfoBackUpActivity.this,
					InfoCollectService.class);
			bindService(intent, serviceConnection, 0); // 绑定Service
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
	 * 获取配置信息
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
	 * 保存配置信息
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

	/** 登陆界面 */
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
		builder.setTitle("用户登陆")
				.setCancelable(false)
				.setPositiveButton("登陆", new DialogInterface.OnClickListener() {

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
									"登录中，请等待...");
							new Thread() {
								@Override
								public void run() {
									int nResult = 0;
									Socket mSocket = null;
									try {
										InetSocketAddress add = new InetSocketAddress(
												SOCKET_IP, SOCKET_PORT);
										mSocket = new Socket();
										mSocket.connect(add, 10000); // 等待超时，进入IOException
										PrintWriter out = new PrintWriter(
												new BufferedWriter(
														new OutputStreamWriter(
																mSocket.getOutputStream())),
												true);
										String msg = "login@%" + strName + "#"
												+ strPassword + "#";
										out.println(msg);
										// 接受服务器的信息
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
				.setNeutralButton("注册", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.cancel();
						onRegist();
					}
				})
				.setNegativeButton("退出", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.cancel();
						InfoBackUpActivity.this.onBackPressed();
					}
				}).create().show();
	}

	/** 注册界面 */
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
		builder.setTitle("新用户注册")
				.setCancelable(false)
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {

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
										"注册中，请等待...");
								new Thread() {
									@Override
									public void run() {
										int nResult = 0;
										Socket mSocket = null;
										try {
											InetSocketAddress add = new InetSocketAddress(
													SOCKET_IP, SOCKET_PORT);
											mSocket = new Socket();
											mSocket.connect(add, 10000); // 等待超时，进入IOException
											PrintWriter out = new PrintWriter(
													new BufferedWriter(
															new OutputStreamWriter(
																	mSocket.getOutputStream())),
													true);
											String msg = "regist@%" + strName
													+ "#" + strPassword + "#"
													+ strPasswordCon + "#";
											out.println(msg);
											// 接受服务器的信息
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
				.setNeutralButton("返回登录",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								// TODO Auto-generated method stub
								dialog.cancel();
								onLogin();
							}
						})
				.setNegativeButton("退出", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.cancel();
						InfoBackUpActivity.this.onBackPressed();
					}
				}).create().show();
	}

	/** 退出登录 */
	public void onLogout() {
		pdForCon = ProgressDialog.show(InfoBackUpActivity.this, "Please Wait",
				"程序退出中，请等待...");
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
					// 接受服务器的信息
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

	/** 网络不可用 */
	public void networkFalse(Context context) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle("错误").setIcon(android.R.drawable.ic_dialog_info)
				.setMessage("网络不可用，请稍后再试！").setCancelable(false)
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.cancel();
						InfoBackUpActivity.this.onBackPressed();
					}
				}).create().show();
	}

	/** 登录时密码错误 */
	public void passwordWrong(Context context) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle("错误！").setIcon(android.R.drawable.ic_dialog_info)
				.setMessage("密码错误！").setCancelable(false)
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.cancel();
						onLogin();
					}
				}).create().show();
	}

	/** 登录时用户名不存在 */
	public void usrNotFound(Context context) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle("错误！").setIcon(android.R.drawable.ic_dialog_info)
				.setMessage("用户不存在，请先注册！").setCancelable(false)
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.cancel();
						onRegist();
					}
				}).create().show();
	}

	/** 用户已登录 */
	public void usrAlreadyLogin(Context context) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle("错误！").setIcon(android.R.drawable.ic_dialog_info)
				.setMessage("用户已登录，请勿重复登录！").setCancelable(false)
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.cancel();
						onLogin();
					}
				}).create().show();
	}

	/** 用户名不能为空 */
	public void usrIllegal(Context context, final int flag) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle("错误！").setIcon(android.R.drawable.ic_dialog_info)
				.setMessage("用户名不能为空！").setCancelable(false)
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {

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

	/** 注册时两次输入密码不相同 */
	public void passwordNotSame(Context context) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle("错误！").setIcon(android.R.drawable.ic_dialog_info)
				.setMessage("密码有误，请重新输入！").setCancelable(false)
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.cancel();
						onRegist();
					}
				}).create().show();
	}

	/** 注册成功 */
	public void registSuccess(Context context) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle("成功！").setIcon(android.R.drawable.ic_dialog_info)
				.setMessage("注册成功，请登录！").setCancelable(false)
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.cancel();
						onLogin();
					}
				}).create().show();
	}

	/** 用户名已存在 */
	public void registNameUsed(Context context) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle("错误！").setIcon(android.R.drawable.ic_dialog_info)
				.setMessage("用户名已存在，请重新输入！").setCancelable(false)
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.cancel();
						onRegist();
					}
				}).create().show();
	}

	/** 获得返回值 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case UPLOAD_PARA:
			switch (resultCode) {
			case RESULT_OK: // 思考：怎么设定一个通用的参数，而不是发送方和接收方都显式地是100
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
				Toast.makeText(this, "错误返回值[" + resultCode + "]",
						Toast.LENGTH_LONG).show();
			}
			break;
		case INFODISPLAY_PARA:
			break;
		default:
			Toast.makeText(this, "错误返回值[" + requestCode + "]",
					Toast.LENGTH_LONG).show();
		}
	}

	/** 获得组件 */
	public void getWidget() {
		uploadOptionButton = (Button) findViewById(R.id.uploadOptionButton);
		getInfoButton = (Button) findViewById(R.id.getInfoButton);
		manualUploadButton = (Button) findViewById(R.id.manualUploadButton);
		startServiceButton = (Button) findViewById(R.id.startServiceButton);
		if (isWorked(InfoBackUpActivity.this)) {
			startServiceButton.setText("关闭信息收集服务");
		} else {
			startServiceButton.setText("启动信息收集服务");
		}
		uploadPhoneConButton = (Button) findViewById(R.id.uploadPhoneButton);

		downloadPhoneConButton = (Button) findViewById(R.id.downloadPhoneConButton);
	}

	/** 为按钮添加监听（C++中称事件） */
	public void regiestListener() {
		uploadOptionButton.setOnClickListener(onClickUploadOption);
		startServiceButton.setOnClickListener(onClickStartService);
		if (isAuto) { // 如果自动上传，设置按钮不可点击
			// manualUploadButton.setClickable(false);
			manualUploadButton.setEnabled(false);
		} else {
			manualUploadButton.setOnClickListener(manualUpload);
		}
		getInfoButton.setOnClickListener(getInfo);
		if (uploadPhoneTime == 0) {
			uploadPhoneConButton.setText("备份通讯录");
		} else {

			SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			Date date = new Date(uploadPhoneTime);
			String strDate = sfd.format(date);
			uploadPhoneConButton.setText("备份通讯录（上次备份时间"
					+ strDate.substring(0, strDate.indexOf(" ")) + ")");
		}
		uploadPhoneConButton.setOnClickListener(onClickUploadPhoneCon);
		downloadPhoneConButton.setOnClickListener(onClickDownloadPhoneCon);
	}

	/** 判断InfoCollectService Service是否处于运行状态 */
	public boolean isWorked(Context context) {
		ActivityManager myManager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE); // 获取服务
		ArrayList<RunningServiceInfo> runningService = (ArrayList<RunningServiceInfo>) myManager
				.getRunningServices(30); // 获取正在运行的服务
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
			Log.v("MainActivity", "已绑定到service");
			mInfoCollectService = ((InfoCollectService.ServiceBinder) arg1)
					.getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			mInfoCollectService = null;
			Log.v("MainActivity", "绑定已断开");
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

	// 读取通讯录
	public String getPhoneContacts() {
		String msg = "";
		final String[] PHONES_PROJECTION = new String[] { Phone.DISPLAY_NAME,
				Phone.NUMBER, Phone.CONTACT_ID };
		Cursor phoneCursor = getContentResolver().query(Phone.CONTENT_URI,
				PHONES_PROJECTION, null, null, null);
		if (phoneCursor != null) {
			while (phoneCursor.moveToNext()) {
				String phoneNumber = phoneCursor.getString(1); // 得到手机号码
				if (TextUtils.isEmpty(phoneNumber)) // 当手机号码为空字段 跳过当前循环
					continue;
				String contactName = phoneCursor.getString(0); // 得到联系人名称
				Long contactid = Long.parseLong(phoneCursor.getString(2)); // 得到联系人ID
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
				String phoneNumber = simCursor.getString(1); // 得到手机号码
				if (TextUtils.isEmpty(phoneNumber)) // 当手机号码为空的或者为空字段 跳过当前循环
					continue;
				String contactName = simCursor.getString(0); // 得到联系人名称
				Long contactid = (long) 0; // 得到联系人ID,SIM卡无法获取Id
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
			// 接收服务器的信息
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

	/** 点击上传选项按钮 */
	public Button.OnClickListener onClickUploadOption = new Button.OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Bundle bundle = new Bundle(); // 准备将参数传递给UploadOptionActivity
			bundle.putBoolean("isAutoCheckValue", isAuto);
			bundle.putBooleanArray("isUploadCheckValue", isUpload);
			bundle.putLong("uploadCycValue", uploadCyc);
			Intent intent = new Intent(InfoBackUpActivity.this,
					UploadOption.class);
			intent.putExtras(bundle); // 给intent附加上要传递的参数
			startActivityForResult(intent, UPLOAD_PARA);
			Log.v("MainActivity", "OnClickUploadOption");
		}
	};
	/** 点击获得信息按钮 */
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
	/** 点击手动上传按钮 */
	public Button.OnClickListener manualUpload = new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			Log.v("upload", "onclick");
			mInfoCollectService.uploadInfoFromMain();
		}
	};
	/** 点击开启服务按钮 */
	public Button.OnClickListener onClickStartService = new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Log.v("MainActivity", "OnClickService");
			if (!isWorked(InfoBackUpActivity.this)) { // 判断服务是否已经开启
				Bundle bundle = new Bundle();
				bundle.putBoolean("isAutoCheckValue", isAuto);
				bundle.putBooleanArray("isUploadCheckValue", isUpload);
				bundle.putLong("uploadCycValue", uploadCyc);
				bundle.putLong("lastUploadTimeValue", uploadTime);
				bundle.putString("usrNameValue", usrName);
				Intent intent = new Intent(InfoBackUpActivity.this,
						InfoCollectService.class);
				intent.putExtras(bundle);
				startService(intent); // 开启服务
				bindService(intent, serviceConnection, 0); // 绑定Service
				Log.v("MainActivity", "OnClickStartService");
				startServiceButton.setText("关闭信息收集服务");
				// 下面开启获取信息服务
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
				stopService(intent); // 结束服务
				unbindService(serviceConnection);
				Log.v("MainActivity", "OnClickStopService");
				startServiceButton.setText("启动信息收集服务");
				// 下面关闭获取信息服务
				Intent intent2 = new Intent(InfoBackUpActivity.this,
						GetMessageService.class);
				stopService(intent2); // 结束服务
				unbindService(serviceConnection2);
			}
		}
	};

	/** 点击备份通讯录 */
	public Button.OnClickListener onClickUploadPhoneCon = new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			processThread();
		}
	};
	/** 点击下载通讯录备份 */
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
				"通讯录备份中，请等待...");
		new Thread() {
			@Override
			public void run() {
				// TODO 加上耗时操作
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
				uploadPhoneConButton.setText("备份通讯录");
			} else {
				SimpleDateFormat sfd = new SimpleDateFormat(
						"yyyy-MM-dd hh:mm:ss");
				Date date = new Date(uploadPhoneTime);
				String strDate = sfd.format(date);
				uploadPhoneConButton.setText("备份通讯录（上次备份时间"
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
			case 0: // 用户登录的handler
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
					// 密码错误
					passwordWrong(InfoBackUpActivity.this);
					break;
				case 3:
					// 用户不存在，请注册
					usrNotFound(InfoBackUpActivity.this);
					break;
				case 5:
					// XXX 用户已登录
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
			case 1: // 用户注册的handler
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
			case 2: // 用户退出的handler
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
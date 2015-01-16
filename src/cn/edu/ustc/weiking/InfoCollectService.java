package cn.edu.ustc.weiking;

/**引入包*/
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.text.format.DateFormat;
import android.util.Log;

public class InfoCollectService extends Service {
	/** 参数设置 */
	private long cycMillisecond = 0; // 上传周期
	private boolean isAuto = false; // 是否自动上传
	private boolean isUpload[] = { false, false, false, false, false }; // 上传选项
	private static long time; // 记录上次采集时间，以后可能换一种方式记录，这样的方式在服务从新开始的时候time会重置
	// private TrafficStats trafficStats;
	private String usrName = "";
	private float mobileKBs = 0;
	private float totalKBs = 0;
	private String traTime = "";
	private final IBinder binder = new ServiceBinder();
	private Timer timerCollect1 = null; // 计时器
	private Timer timerCollect2 = null;
	private Timer timerUpload = null;
	private boolean isGPSOn = false;

	private Map<String, Integer> mapProcess = null; // 采集应用程序所用的MAP

	private static InfoBackUpActivity mainActivity;
	private WakeLock mWakeLock = null; // 电源锁
	private int appCount = 0;
	private int appCountId = 0;

	LocationManager locMgr; // 位置信息
	Criteria criteria;
	String setProvider = "gps";

	private DBAdapter dbAdapter;
	private String uploadMsg = "";
	private boolean uploadFinish = false;
	NotificationManager notificationManager;
	Notification notification;
	private static int NOTIFICATIONID = 10101;

	private enum DBSTATUS {
		INITSTATUS, READING, WRITING
	};

	private DBSTATUS dbStatus = DBSTATUS.INITSTATUS;

	/** 申请电源锁 */
	private void acquireWakeLock() {
		if (null == mWakeLock) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
					| PowerManager.ON_AFTER_RELEASE, "LocalServiceWakeLock");
			if (null != mWakeLock) {
				mWakeLock.acquire();
			}
			Log.v("LocalService", "wakelockon");
		}
	}

	/** 释放电源锁 */
	private void releaseWakeLock() {
		if (null != mWakeLock) {
			mWakeLock.release();
			mWakeLock = null;
			Log.v("LocalService", "wakelockdown");
		}
	}

	/** 获取主界面的context */
	static void registerIntent(Context context) {
		mainActivity = (InfoBackUpActivity) context;
	}

	final LocationListener locationListener = new LocationListener() {

		@Override
		public void onLocationChanged(Location location) {
			// TODO Auto-generated method stub
			// showLocation(location, 1); //因为每次到时间就会自动再获取一次，所以可以不需要这个
		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			showLocation(null, 2);
		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub

		}

	};

	/** 计时完成循环任务 */
	private void startTimer() { // timer的计时可能出现错误，在断开usb且关屏的情况下
		Log.v("LocalService", "start timer");
		timerCollect1 = new Timer();
		timerCollect2 = new Timer();
		timerUpload = new Timer();
		TimerTask trafficTimerTask = new TimerTask() {
			@Override
			public void run() {
				while (dbStatus != DBSTATUS.INITSTATUS) {
				}
				;
				dbStatus = DBSTATUS.WRITING;
				dbAdapter.open(); // 打开数据库
				getNetworkTrafficStats(); // 获取流量信息
				String strLocation = getLocation(); // 获取位置信息
				dbAdapter
						.insertTitle(traTime, mobileKBs, totalKBs, strLocation); // 插入信息
				dbAdapter.close(); // 关闭数据库
				dbStatus = DBSTATUS.INITSTATUS;
			}
		};
		TimerTask uploadTimerTask = new TimerTask() {
			@Override
			public void run() {
				uploadInfo(isUpload);
			}
		};
		TimerTask getApp = new TimerTask() {
			@Override
			public void run() {
				getRunningProcess();
				Log.v("test count", "appCount:" + appCount);
				if (appCount == 30) { // 30分钟一次统计程序运行时间占有率
					while (dbStatus != DBSTATUS.INITSTATUS) {
					}
					;
					dbStatus = DBSTATUS.WRITING;
					dbAdapter.open();
					SimpleDateFormat formatter = new SimpleDateFormat(
							"yyyy-MM-dd kk:mm:ss");
					Date curDate = new Date(System.currentTimeMillis());// 获取当前时间
					String appTime = formatter.format(curDate);
					for (Map.Entry<String, Integer> entry : mapProcess
							.entrySet()) {
						Log.v("test count", "" + entry.getValue());
						dbAdapter.insertTitle(appCountId, entry.getKey(),
								entry.getValue() / (float) appCount, appTime);
					}
					initMap();
					dbAdapter.close();
					dbStatus = DBSTATUS.INITSTATUS;
					appCount = 0;
					appCountId++;
				}
			}
		};
		if (isUpload[2]) {
			timerCollect1.schedule(trafficTimerTask, 180000, 180000); // 流量统计准备设计半小时一次
		}
		if (isUpload[3]) {
			timerCollect2.schedule(getApp, 10000, 10000); // 应用程序信息设计一分钟统计一次
		}
		if (isAuto && cycMillisecond > 360000) { // 自动上传周期小于一小时的话不自动上传
			timerUpload.schedule(uploadTimerTask, cycMillisecond,
					cycMillisecond);
		}
	}

	/** 停止计时循环 */
	private void stopTimer() {
		timerCollect1.cancel();
		timerCollect2.cancel();
		timerUpload.cancel();
	}

	/** 主界面点击手动上传按钮后调用上传函数 */
	public void uploadInfoFromMain() {
		uploadInfo(isUpload);
	}

	public void uploadInfo(final boolean[] isUploadIn) {
		Log.v("LocalService", "test for 外部调用");

		new Thread() {
			@Override
			public void run() {
				Log.v("LocalService", "in new Thread");
				uploadMsg = ""; // uploadMsg是要上传的信息，前5个字符标识上传的信息中是否存在对应的信息
				for (int i = 0; i < 5; i++) {
					if (isUploadIn[i]) {
						uploadMsg += "1";
					} else {
						uploadMsg += "0";
					}
				}
				uploadMsg += "#"; // 每种类型的Msg用'#'标识隔开
				{ // 通话记录需要上传
					uploadMsg += "CALLLOG&";
					if (isUploadIn[0]) {
						Log.v("testLocalService", "in getCallLog");
						getCallLog();
					}
					uploadMsg += "#";
				}
				{ // 短信记录需要上传
					uploadMsg += "SMSLOG&";
					if (isUploadIn[1]) {
						Log.v("testLocalService", "in getSMSLog");
						getSMSLog();
					}
					uploadMsg += "#";
				}
				while (dbStatus != DBSTATUS.INITSTATUS) {
				}
				;
				dbStatus = DBSTATUS.READING;
				dbAdapter.open();
				Cursor mCursor = null;
				{ // 应用程序记录需要上传
					uploadMsg += "APPLOG&";
					if (isUploadIn[2]) {
						mCursor = dbAdapter.getAllTitles(DBAdapter.TABLE_APP);
						Log.v("LocalService",
								"countAPP : " + mCursor.getCount());
						if (mCursor.moveToFirst()) {
							do {
								deelWithTitle(mCursor, 1);
							} while (mCursor.moveToNext());
						}
					}
					uploadMsg += "#";
				}
				{ // 流量信息需要上传（包含位置信息）
					uploadMsg += "TRASTATS$";
					if (isUploadIn[3]) {
						mCursor = dbAdapter
								.getAllTitles(DBAdapter.TABLE_TRAFFIC);
						Log.v("LocalSerivce",
								"countTra : " + mCursor.getCount());
						if (mCursor.moveToFirst()) {
							do {
								deelWithTitle(mCursor, 2);
							} while (mCursor.moveToNext());
						}
					}
					uploadMsg += "#";
				}
				/*
				 * { uploadMsg += "PHONECONTACT$"; if (isUploadIn[4]) {
				 * Log.v("LocalSerivce", "in get phonecontact");
				 * getPhoneContacts(); } uploadMsg += "#"; }
				 */
				dbAdapter.deleteAllTitle(DBAdapter.TABLE_APP);
				dbAdapter.deleteAllTitle(DBAdapter.TABLE_TRAFFIC);
				dbAdapter.close();
				dbStatus = DBSTATUS.INITSTATUS;

				sendInfo(uploadMsg); // 上传信息
				uploadMsg = "";
				uploadFinish = true;
			}

		}.start();
	}

	public void deelWithTitle(Cursor c, int flag) {
		switch (flag) {
		case 1:
			Log.v("uploadInfo",
					"id: " + c.getString(0) + ";appCountId: " + c.getString(1)
							+ ";appName: " + c.getString(2) + ";appOcc: "
							+ c.getString(3) + ";appTime: " + c.getString(4));
			uploadMsg += "id: " + c.getString(0) + ";appCountId: "
					+ c.getString(1) + ";appName: " + c.getString(2)
					+ ";appOcc: " + c.getString(3) + ";appTime: "
					+ c.getString(4) + "&"; // "&"表示一条记录的完结
			break;
		case 2:
			Log.v("uploadInfo",
					"id: " + c.getString(0) + ";traTime: " + c.getString(1)
							+ ";traStatsMobile: " + c.getString(2)
							+ ";traStatsTotal: " + c.getString(3)
							+ ";traLocation: " + c.getString(4));
			uploadMsg += "id: " + c.getString(0) + ";traTime: "
					+ c.getString(1) + ";traStatsMobile: " + c.getString(2)
					+ ";traStatsTotal: " + c.getString(3) + ";traLocation: "
					+ c.getString(4) + "&"; // "&"表示一条记录的完结
			break;
		default:
			Log.v("uploadInfo", "error flag");
		}
	}

	/** 从主界面获取信息 */
	public void getInfoFromMain(Intent intent) {
		Bundle bundle;
		bundle = intent.getExtras();
		isAuto = bundle.getBoolean("isAutoCheckValue");
		isUpload = bundle.getBooleanArray("isUploadCheckValue");
		cycMillisecond = bundle.getLong("uploadCycValue");
		time = bundle.getLong("lastUploadTimeValue");
		usrName = bundle.getString("usrNameValue");
		Log.v("LocalService", "isAuto:" + isAuto);
		Log.v("LovalService", "isUpload:" + isUpload[0] + ";" + isUpload[1]
				+ ";" + isUpload[2] + ";" + isUpload[3] + ";" + isUpload[4]
				+ ";");
		Log.v("LocalService", "cycMillisecond:" + cycMillisecond);
		Log.v("LocalServer", "time: " + time);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v("LocalService", "Recevice start id " + startId + ": " + intent);
		// TODO 接收启动时传递的信息
		getInfoFromMain(intent);

		startTimer();
		// setNotification();
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) { // 必须实现的函数
		Log.v("LocalService", "onBind");
		return binder;
	}

	class ServiceBinder extends Binder implements ServiceInterface {
		public InfoCollectService getService() { // 返回服务
			return InfoCollectService.this;
		}

		@Override
		public void show() {
			// TODO Auto-generated method stub
			Log.v("LocalSerivce", "通过绑定调用");
		}
	}

	@Override
	public void onCreate() { // 创建Service时执行
		super.onCreate();
		Log.v("LocalService", "Service onCreate"); // 标记开始运行
		Log.v("LocalService", "lastTime: " + time);
		dbAdapter = new DBAdapter(this); // 创建新的数据库对象
		prepareLocation();
		acquireWakeLock();
		initMap();
	}

	@Override
	public void onDestroy() {
		stopTimer();
		releaseWakeLock();
		notificationManager.cancel(R.drawable.ic_launcher);
		super.onDestroy();
		Log.v("LocalService", "Service onDestroy"); // 测试是否Destroy
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
		Log.v("LocalService", "Service onRebind");
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Log.v("LocalService", "Service onStart");
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.v("LocalService", "Service onUnbind");
		return super.onUnbind(intent);
	}

	/** 获取通话记录 */
	public void getCallLog() {
		String strCallNum = ""; // 电话号码
		String strCallName = ""; // 联系人
		int type; // 通讯类型0,1,2分别表示incoming, outgoing or missed
		long callTime; // 通话持续时间
		Date date; // 通讯时间
		String strDate = "";
		ContentResolver cr = getContentResolver();
		final Cursor cursor = cr.query(CallLog.Calls.CONTENT_URI,
				new String[] { CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME,
						CallLog.Calls.TYPE, CallLog.Calls.DATE,
						CallLog.Calls.DURATION }, CallLog.Calls.DATE + ">"
						+ time, null, CallLog.Calls.DEFAULT_SORT_ORDER);
		for (int i = 0; i < cursor.getCount(); i++) {
			cursor.moveToPosition(i);
			strCallNum = cursor.getString(0);
			strCallName = cursor.getString(1);
			type = cursor.getInt(2);
			SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			date = new Date(Long.parseLong(cursor.getString(3))); // 将Jan.1.1970开始的minisecond换算成现在的时间
			strDate = sfd.format(date);
			callTime = cursor.getInt(4);
			/** 打印出读取的日期 */
			Log.v("CallLog", "Tell: " + strCallNum);
			Log.v("CallLog", "Name: " + strCallName);
			Log.v("CallLog", "CallTime: " + strDate);
			Log.v("CallLog", "Time: " + callTime);
			Log.v("CallLog", "Type: " + type);
			uploadMsg += "Tell: " + strCallNum + ";Name: " + strCallName
					+ ";CallTime: " + strDate + ";Time: " + callTime
					+ ";Type: " + type + "&";
			// if (i == 0) {
			// time = Long.parseLong(cursor.getString(3));
			// }
		}
	}

	/** 读取短信信息，但不读取短信内容 */
	public void getSMSLog() {
		String strSMSNum = ""; // 短信号码
		String strSMSName = ""; // 短信对象
		String strDate = ""; // 短信时间（格式化）
		Date date; // 短信时间
		int id = 0; // 短信ID
		int type = 0; // 短信类型：0未读；1已读
		Map<String, String> personMap = new HashMap<String, String>();
		ContentResolver cr = getContentResolver();
		final Cursor cursor = cr.query(Uri.parse("content://sms/"),
				new String[] { "_id", "address", "date", "type" }, "date>"
						+ time, null, "date desc");

		for (int i = 0; i < cursor.getCount(); i++) {
			cursor.moveToPosition(i);
			id = cursor.getInt(0);
			strSMSNum = cursor.getString(1);
			if (personMap.containsKey(strSMSNum)) {
				strSMSName = personMap.get(strSMSNum);
			} else {
				strSMSName = getPeopleNameFromPerson(strSMSNum);
				personMap.put(strSMSNum, strSMSName);
			}
			// strSMSName = cursor.getString(2);
			SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			date = new Date(Long.parseLong(cursor.getString(2)));
			strDate = sfd.format(date);
			type = cursor.getInt(3);
			/** 打印出读取的信息 */
			Log.v("SMSLog", "SMSId: " + id);
			Log.v("SMSLog", "SMSNum: " + strSMSNum);
			Log.v("SMSLog", "Name: " + strSMSName);
			Log.v("SMSLog", "Time: " + strDate);
			Log.v("SMSLog", "Type: " + type);
			uploadMsg += "SMSId: " + id + ";SMSNum: " + strSMSNum + ";Name: "
					+ strSMSName + ";Time: " + strDate + ";Type: " + type + "&";
			// if (i == 0) {
			// time = Long.parseLong(cursor.getString(3));
			// }
		}
	}

	private String getPeopleNameFromPerson(String address) {
		String strPerson = "null";
		Uri personUri = Uri.withAppendedPath(
				ContactsContract.PhoneLookup.CONTENT_FILTER_URI, address);
		Cursor cur = getContentResolver().query(personUri,
				new String[] { PhoneLookup.DISPLAY_NAME }, null, null, null);
		if (cur.moveToFirst()) {
			int nameIdx = cur.getColumnIndex(PhoneLookup.DISPLAY_NAME);
			strPerson = cur.getString(nameIdx);
		}
		cur.close();
		return strPerson;
	}

	public void prepareLocation() {
		locMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
		criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE); // 高精度
		criteria.setAltitudeRequired(true); // 显示海拔
		criteria.setBearingRequired(true); // 显示方向
		criteria.setSpeedRequired(true); // 显示速度
		criteria.setCostAllowed(false); // 不允许有花费
		criteria.setPowerRequirement(Criteria.POWER_LOW); // 低功耗
		setProvider = locMgr.getBestProvider(criteria, true);
		try {
			locMgr.requestLocationUpdates(setProvider, 60000, 0,
					locationListener);
			isGPSOn = true;
		} catch (Exception e) {
			Log.v("localService", "Error: " + e.toString());
		}
	}

	/** 获取位置信息 */
	public String getLocation() {
		// 已知问题是，即使有位置信息，也只有打开一次地图软件后才可以有位置信息的缓存，才可以读取到位置信息
		Location recentLoc;
		if (isGPSOn) {
			recentLoc = locMgr.getLastKnownLocation(setProvider);
			return showLocation(recentLoc, 1);
		} else {
			return showLocation(null, 2);
		}
	}

	// 展示位置信息
	public String showLocation(Location location, int flag) {
		String locationString = "null";
		if (location != null) {
			Date d = new Date();
			d.setTime(location.getTime());// UTC时间,转北京时间+8小时
			String GpsTime = DateFormat.format("yyyy-MM-dd kk:mm:ss", d)
					.toString();
			locationString = "latitude:" + location.getLatitude()
					+ ",longtitude:" + location.getLongitude() + ",altitude:"
					+ location.getAltitude() + ",bearing:"
					+ location.getBearing() + ",speed:" + location.getSpeed()
					+ ",gpstime:" + GpsTime;
			Log.v("Location",
					"纬度:" + location.getLatitude() + "经度:"
							+ location.getLongitude() + "海拔:"
							+ location.getAltitude() + "方向:"
							+ location.getBearing() + "速度:"
							+ location.getSpeed() + "GPS时间:" + GpsTime);
		} else {
			if (flag == 1) {
				// locationString = "null";
				Log.v("LocalService", "没有位置信息");
			} else if (flag == 2) {
				// locationString = "null";
				Log.v("LocalService", "GPS服务已关闭");
			}
		}
		return locationString;
	}

	// 流量统计
	public void getNetworkTrafficStats() {
		long mobileRxBytes = TrafficStats.getMobileRxBytes(); // 手机上网接收流量
		long mobileTxBytes = TrafficStats.getMobileTxBytes(); // 手机上网发送流量
		long totalRxBytes = TrafficStats.getTotalRxBytes(); // 总上网接收流量（包括wifi）
		long totalTxBytes = TrafficStats.getTotalTxBytes(); // 总上网发送流量（包括wifi）
		mobileKBs = (mobileRxBytes + mobileTxBytes) / (float) 1024;
		if (mobileKBs < 0)
			mobileKBs = 0;
		totalKBs = (totalRxBytes + totalTxBytes) / (float) 1024;
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		Date curDate = new Date(System.currentTimeMillis());// 获取当前时间
		traTime = formatter.format(curDate);
	}

	public void initMap() {
		// Log.v("LocalService", "initMao");
		if (mapProcess == null) {
			mapProcess = new HashMap<String, Integer>();
		} else {
			mapProcess.clear();
		}
	}

	/** 把运行时间存放在map里 */
	public void getRunningProcess() {
		appCount++;
		Log.v("LocalService", "count:" + appCount);

		PackagesInfo pi = new PackagesInfo(this);
		ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE); // 获取正在运行的服务
		List<RunningAppProcessInfo> runInfo = am.getRunningAppProcesses(); // 获取正在运行的应用
		PackageManager pm = this.getPackageManager(); // //获取包管理器，在这里主要通过包名获取程序的程序名
		for (RunningAppProcessInfo ra : runInfo) {
			if (ra.processName.equals("system")
					|| ra.processName.equals("com.android.phone")) {
				continue; // 系统应用
			}
			if (pi.getInfo(ra.processName) == null) {
				continue; // 某些系统进程
			}
			String strName = "";
			strName = pi.getInfo(ra.processName).loadLabel(pm).toString();
			if (mapProcess.containsKey(strName)) {
				Integer i = mapProcess.get(strName);
				i++;
				mapProcess.put(strName, i); // 直接put会更新对应key的value
			} else {
				mapProcess.put(strName, 1);
			}

		}
	}

	public long getLastTime() {
		return time;
	}

	public void sendInfo(String msg) {
		Socket mSocket = null;
		time = System.currentTimeMillis();
		Log.v("LocalService", "try to send msg");
		try {
			mSocket = new Socket(InfoBackUpActivity.SOCKET_IP,
					InfoBackUpActivity.SOCKET_PORT);
			PrintWriter out = new PrintWriter(new BufferedWriter(
					new OutputStreamWriter(mSocket.getOutputStream())), true);
			msg = "info@%" + usrName + "#" + msg;
			out.println(msg);
			// 接受服务器的信息
			BufferedReader br = new BufferedReader(new InputStreamReader(
					mSocket.getInputStream()));
			String mstr = br.readLine();
			Log.v("LocalService", "getMsg " + mstr);
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

	public void updateOption(boolean auto, boolean[] upload, long uploadc) {
		// TODO 改变选项的时候上传信息，也许应该另起一线程吧
		stopTimer();
		uploadInfo(isUpload);
		// uploadFinish = false;
		isAuto = auto;
		isUpload = upload;
		cycMillisecond = uploadc;
		startTimer();
	}

	public void setNotification() {
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notification = new Notification(R.drawable.ic_launcher, "用户行为信息采集系统",
				System.currentTimeMillis());
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_NO_CLEAR;

		PendingIntent contentIntent = PendingIntent.getActivity(mainActivity,
				0, mainActivity.getIntent(), 0);
		notification.setLatestEventInfo(mainActivity, "行为信息采集正在进行......",
				"用户行为信息采集系统", contentIntent);
		notificationManager.notify(R.drawable.ic_launcher, notification);
	}
}
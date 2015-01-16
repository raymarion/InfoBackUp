package cn.edu.ustc.weiking;

/**�����*/
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
	/** �������� */
	private long cycMillisecond = 0; // �ϴ�����
	private boolean isAuto = false; // �Ƿ��Զ��ϴ�
	private boolean isUpload[] = { false, false, false, false, false }; // �ϴ�ѡ��
	private static long time; // ��¼�ϴβɼ�ʱ�䣬�Ժ���ܻ�һ�ַ�ʽ��¼�������ķ�ʽ�ڷ�����¿�ʼ��ʱ��time������
	// private TrafficStats trafficStats;
	private String usrName = "";
	private float mobileKBs = 0;
	private float totalKBs = 0;
	private String traTime = "";
	private final IBinder binder = new ServiceBinder();
	private Timer timerCollect1 = null; // ��ʱ��
	private Timer timerCollect2 = null;
	private Timer timerUpload = null;
	private boolean isGPSOn = false;

	private Map<String, Integer> mapProcess = null; // �ɼ�Ӧ�ó������õ�MAP

	private static InfoBackUpActivity mainActivity;
	private WakeLock mWakeLock = null; // ��Դ��
	private int appCount = 0;
	private int appCountId = 0;

	LocationManager locMgr; // λ����Ϣ
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

	/** �����Դ�� */
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

	/** �ͷŵ�Դ�� */
	private void releaseWakeLock() {
		if (null != mWakeLock) {
			mWakeLock.release();
			mWakeLock = null;
			Log.v("LocalService", "wakelockdown");
		}
	}

	/** ��ȡ�������context */
	static void registerIntent(Context context) {
		mainActivity = (InfoBackUpActivity) context;
	}

	final LocationListener locationListener = new LocationListener() {

		@Override
		public void onLocationChanged(Location location) {
			// TODO Auto-generated method stub
			// showLocation(location, 1); //��Ϊÿ�ε�ʱ��ͻ��Զ��ٻ�ȡһ�Σ����Կ��Բ���Ҫ���
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

	/** ��ʱ���ѭ������ */
	private void startTimer() { // timer�ļ�ʱ���ܳ��ִ����ڶϿ�usb�ҹ����������
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
				dbAdapter.open(); // �����ݿ�
				getNetworkTrafficStats(); // ��ȡ������Ϣ
				String strLocation = getLocation(); // ��ȡλ����Ϣ
				dbAdapter
						.insertTitle(traTime, mobileKBs, totalKBs, strLocation); // ������Ϣ
				dbAdapter.close(); // �ر����ݿ�
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
				if (appCount == 30) { // 30����һ��ͳ�Ƴ�������ʱ��ռ����
					while (dbStatus != DBSTATUS.INITSTATUS) {
					}
					;
					dbStatus = DBSTATUS.WRITING;
					dbAdapter.open();
					SimpleDateFormat formatter = new SimpleDateFormat(
							"yyyy-MM-dd kk:mm:ss");
					Date curDate = new Date(System.currentTimeMillis());// ��ȡ��ǰʱ��
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
			timerCollect1.schedule(trafficTimerTask, 180000, 180000); // ����ͳ��׼����ư�Сʱһ��
		}
		if (isUpload[3]) {
			timerCollect2.schedule(getApp, 10000, 10000); // Ӧ�ó�����Ϣ���һ����ͳ��һ��
		}
		if (isAuto && cycMillisecond > 360000) { // �Զ��ϴ�����С��һСʱ�Ļ����Զ��ϴ�
			timerUpload.schedule(uploadTimerTask, cycMillisecond,
					cycMillisecond);
		}
	}

	/** ֹͣ��ʱѭ�� */
	private void stopTimer() {
		timerCollect1.cancel();
		timerCollect2.cancel();
		timerUpload.cancel();
	}

	/** ���������ֶ��ϴ���ť������ϴ����� */
	public void uploadInfoFromMain() {
		uploadInfo(isUpload);
	}

	public void uploadInfo(final boolean[] isUploadIn) {
		Log.v("LocalService", "test for �ⲿ����");

		new Thread() {
			@Override
			public void run() {
				Log.v("LocalService", "in new Thread");
				uploadMsg = ""; // uploadMsg��Ҫ�ϴ�����Ϣ��ǰ5���ַ���ʶ�ϴ�����Ϣ���Ƿ���ڶ�Ӧ����Ϣ
				for (int i = 0; i < 5; i++) {
					if (isUploadIn[i]) {
						uploadMsg += "1";
					} else {
						uploadMsg += "0";
					}
				}
				uploadMsg += "#"; // ÿ�����͵�Msg��'#'��ʶ����
				{ // ͨ����¼��Ҫ�ϴ�
					uploadMsg += "CALLLOG&";
					if (isUploadIn[0]) {
						Log.v("testLocalService", "in getCallLog");
						getCallLog();
					}
					uploadMsg += "#";
				}
				{ // ���ż�¼��Ҫ�ϴ�
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
				{ // Ӧ�ó����¼��Ҫ�ϴ�
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
				{ // ������Ϣ��Ҫ�ϴ�������λ����Ϣ��
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

				sendInfo(uploadMsg); // �ϴ���Ϣ
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
					+ c.getString(4) + "&"; // "&"��ʾһ����¼�����
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
					+ c.getString(4) + "&"; // "&"��ʾһ����¼�����
			break;
		default:
			Log.v("uploadInfo", "error flag");
		}
	}

	/** ���������ȡ��Ϣ */
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
		// TODO ��������ʱ���ݵ���Ϣ
		getInfoFromMain(intent);

		startTimer();
		// setNotification();
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) { // ����ʵ�ֵĺ���
		Log.v("LocalService", "onBind");
		return binder;
	}

	class ServiceBinder extends Binder implements ServiceInterface {
		public InfoCollectService getService() { // ���ط���
			return InfoCollectService.this;
		}

		@Override
		public void show() {
			// TODO Auto-generated method stub
			Log.v("LocalSerivce", "ͨ���󶨵���");
		}
	}

	@Override
	public void onCreate() { // ����Serviceʱִ��
		super.onCreate();
		Log.v("LocalService", "Service onCreate"); // ��ǿ�ʼ����
		Log.v("LocalService", "lastTime: " + time);
		dbAdapter = new DBAdapter(this); // �����µ����ݿ����
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
		Log.v("LocalService", "Service onDestroy"); // �����Ƿ�Destroy
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

	/** ��ȡͨ����¼ */
	public void getCallLog() {
		String strCallNum = ""; // �绰����
		String strCallName = ""; // ��ϵ��
		int type; // ͨѶ����0,1,2�ֱ��ʾincoming, outgoing or missed
		long callTime; // ͨ������ʱ��
		Date date; // ͨѶʱ��
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
			date = new Date(Long.parseLong(cursor.getString(3))); // ��Jan.1.1970��ʼ��minisecond��������ڵ�ʱ��
			strDate = sfd.format(date);
			callTime = cursor.getInt(4);
			/** ��ӡ����ȡ������ */
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

	/** ��ȡ������Ϣ��������ȡ�������� */
	public void getSMSLog() {
		String strSMSNum = ""; // ���ź���
		String strSMSName = ""; // ���Ŷ���
		String strDate = ""; // ����ʱ�䣨��ʽ����
		Date date; // ����ʱ��
		int id = 0; // ����ID
		int type = 0; // �������ͣ�0δ����1�Ѷ�
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
			/** ��ӡ����ȡ����Ϣ */
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
		criteria.setAccuracy(Criteria.ACCURACY_FINE); // �߾���
		criteria.setAltitudeRequired(true); // ��ʾ����
		criteria.setBearingRequired(true); // ��ʾ����
		criteria.setSpeedRequired(true); // ��ʾ�ٶ�
		criteria.setCostAllowed(false); // �������л���
		criteria.setPowerRequirement(Criteria.POWER_LOW); // �͹���
		setProvider = locMgr.getBestProvider(criteria, true);
		try {
			locMgr.requestLocationUpdates(setProvider, 60000, 0,
					locationListener);
			isGPSOn = true;
		} catch (Exception e) {
			Log.v("localService", "Error: " + e.toString());
		}
	}

	/** ��ȡλ����Ϣ */
	public String getLocation() {
		// ��֪�����ǣ���ʹ��λ����Ϣ��Ҳֻ�д�һ�ε�ͼ�����ſ�����λ����Ϣ�Ļ��棬�ſ��Զ�ȡ��λ����Ϣ
		Location recentLoc;
		if (isGPSOn) {
			recentLoc = locMgr.getLastKnownLocation(setProvider);
			return showLocation(recentLoc, 1);
		} else {
			return showLocation(null, 2);
		}
	}

	// չʾλ����Ϣ
	public String showLocation(Location location, int flag) {
		String locationString = "null";
		if (location != null) {
			Date d = new Date();
			d.setTime(location.getTime());// UTCʱ��,ת����ʱ��+8Сʱ
			String GpsTime = DateFormat.format("yyyy-MM-dd kk:mm:ss", d)
					.toString();
			locationString = "latitude:" + location.getLatitude()
					+ ",longtitude:" + location.getLongitude() + ",altitude:"
					+ location.getAltitude() + ",bearing:"
					+ location.getBearing() + ",speed:" + location.getSpeed()
					+ ",gpstime:" + GpsTime;
			Log.v("Location",
					"γ��:" + location.getLatitude() + "����:"
							+ location.getLongitude() + "����:"
							+ location.getAltitude() + "����:"
							+ location.getBearing() + "�ٶ�:"
							+ location.getSpeed() + "GPSʱ��:" + GpsTime);
		} else {
			if (flag == 1) {
				// locationString = "null";
				Log.v("LocalService", "û��λ����Ϣ");
			} else if (flag == 2) {
				// locationString = "null";
				Log.v("LocalService", "GPS�����ѹر�");
			}
		}
		return locationString;
	}

	// ����ͳ��
	public void getNetworkTrafficStats() {
		long mobileRxBytes = TrafficStats.getMobileRxBytes(); // �ֻ�������������
		long mobileTxBytes = TrafficStats.getMobileTxBytes(); // �ֻ�������������
		long totalRxBytes = TrafficStats.getTotalRxBytes(); // ��������������������wifi��
		long totalTxBytes = TrafficStats.getTotalTxBytes(); // ��������������������wifi��
		mobileKBs = (mobileRxBytes + mobileTxBytes) / (float) 1024;
		if (mobileKBs < 0)
			mobileKBs = 0;
		totalKBs = (totalRxBytes + totalTxBytes) / (float) 1024;
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		Date curDate = new Date(System.currentTimeMillis());// ��ȡ��ǰʱ��
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

	/** ������ʱ������map�� */
	public void getRunningProcess() {
		appCount++;
		Log.v("LocalService", "count:" + appCount);

		PackagesInfo pi = new PackagesInfo(this);
		ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE); // ��ȡ�������еķ���
		List<RunningAppProcessInfo> runInfo = am.getRunningAppProcesses(); // ��ȡ�������е�Ӧ��
		PackageManager pm = this.getPackageManager(); // //��ȡ������������������Ҫͨ��������ȡ����ĳ�����
		for (RunningAppProcessInfo ra : runInfo) {
			if (ra.processName.equals("system")
					|| ra.processName.equals("com.android.phone")) {
				continue; // ϵͳӦ��
			}
			if (pi.getInfo(ra.processName) == null) {
				continue; // ĳЩϵͳ����
			}
			String strName = "";
			strName = pi.getInfo(ra.processName).loadLabel(pm).toString();
			if (mapProcess.containsKey(strName)) {
				Integer i = mapProcess.get(strName);
				i++;
				mapProcess.put(strName, i); // ֱ��put����¶�Ӧkey��value
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
			// ���ܷ���������Ϣ
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
		// TODO �ı�ѡ���ʱ���ϴ���Ϣ��Ҳ��Ӧ������һ�̰߳�
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
		notification = new Notification(R.drawable.ic_launcher, "�û���Ϊ��Ϣ�ɼ�ϵͳ",
				System.currentTimeMillis());
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_NO_CLEAR;

		PendingIntent contentIntent = PendingIntent.getActivity(mainActivity,
				0, mainActivity.getIntent(), 0);
		notification.setLatestEventInfo(mainActivity, "��Ϊ��Ϣ�ɼ����ڽ���......",
				"�û���Ϊ��Ϣ�ɼ�ϵͳ", contentIntent);
		notificationManager.notify(R.drawable.ic_launcher, notification);
	}
}
package cn.edu.ustc.weiking;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBAdapter {

    // 流量和位置信息数据库
    private static final String TRAFFIC_TABLE_NAME = "traTable";
    private static final String TRAFFIC_ROWID = "_id";
    private static final String TRAFFIC_TIME = "traTime"; // 统计时间
    private static final String TRAFFIC_TRAFFICSTATS_MOBILE = "traStatsMobile"; // 收集上网流量
    private static final String TRAFFIC_TRAFFICSTATS_TOTAL = "traStatsTotal"; // 总流量
    private static final String TRAFFIC_LOCATION = "traLocation"; // 位置信息

    // 应用程序统计数据库
    private static final String APP_TABLE_NAME = "appTable";
    private static final String APP_ROWID = "_id";
    private static final String APP_COUNTID = "appCountId"; // 应用程序统计标识
    private static final String APP_NAME = "appName"; // 应用程序名称
    private static final String APP_OCCUPANCY = "appOcc"; // 应用程序在时间节点内的占有率
    private static final String APP_TIME = "appTime"; // 统计时间

    private static final String TAG = "DBAdapter";
    private static final String DATABASE_NAME = "InfoCollect";
    // private static final String DATABASE_TABLE = "infoCollect";
    private static final int DATABASE_VERSION = 1;

    public static final int TABLE_APP = 1;
    public static final int TABLE_TRAFFIC = 2;

    private static final String DATABASE_CREATE_ONE = "create table "
	    + APP_TABLE_NAME + " (" + APP_ROWID
	    + " integer primary key autoincrement, " + APP_COUNTID
	    + " integer, " + APP_NAME + " text not null, " + APP_OCCUPANCY
	    + " float, " + APP_TIME + " text not null);";
    private static final String DATABASE_CREATE_TWO = "create table "
	    + TRAFFIC_TABLE_NAME + " (" + TRAFFIC_ROWID
	    + " integer primary key autoincrement, " + TRAFFIC_TIME
	    + " text not null, " + TRAFFIC_TRAFFICSTATS_MOBILE + " float, "
	    + TRAFFIC_TRAFFICSTATS_TOTAL + " float, " + TRAFFIC_LOCATION
	    + " text not null);";

    private final Context context;
    private final DatabaseHelper DBHelper;
    private SQLiteDatabase db;

    public DBAdapter(Context ctx) {
	this.context = ctx;
	DBHelper = new DatabaseHelper(context);
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
	DatabaseHelper(Context context) {
	    super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	// 创建数据库
	public void onCreate(SQLiteDatabase db) {
	    db.execSQL(DATABASE_CREATE_ONE);
	    db.execSQL(DATABASE_CREATE_TWO);
	}

	@Override
	// 更新数据库
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	    Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
		    + newVersion + ", which will destroy all old data");
	    db.execSQL("DROP TABLE IF EXISTS " + APP_TABLE_NAME);
	    db.execSQL("DROP TABLE IF EXISTS " + TRAFFIC_TABLE_NAME);
	    onCreate(db);
	}
    }

    // ---打开数据库---
    public DBAdapter open() throws SQLException {
	db = DBHelper.getWritableDatabase();
	Log.v("DB", db.getPath());
	return this;
    }

    // ---关闭数据库---
    public void close() {
	DBHelper.close();
    }

    // ---向数据库中插入一个标题---
    public long insertTitle(int appCountId, String appName, float appOcc,
	    String appTime) {
	ContentValues args = new ContentValues();
	args.put(APP_COUNTID, appCountId);
	args.put(APP_NAME, appName);
	args.put(APP_OCCUPANCY, appOcc);
	args.put(APP_TIME, appTime);
	return db.insert(APP_TABLE_NAME, null, args);
    }

    // ---向数据库中插入一个标题---
    public long insertTitle(String traTime, float traStatsMobile,
	    float traStatsTotal, String traLocation) {
	ContentValues args = new ContentValues();
	args.put(TRAFFIC_TIME, traTime);
	args.put(TRAFFIC_TRAFFICSTATS_MOBILE, traStatsMobile);
	args.put(TRAFFIC_TRAFFICSTATS_TOTAL, traStatsTotal);
	args.put(TRAFFIC_LOCATION, traLocation);
	return db.insert(TRAFFIC_TABLE_NAME, null, args);
    }

    // ---删除一个指定标题---
    public boolean deleteTitle(int table, long rowId) {
	switch (table) {
	case TABLE_APP:
	    return db.delete(APP_TABLE_NAME, APP_ROWID + "=" + rowId, null) > 0;
	case TABLE_TRAFFIC:
	    return db.delete(TRAFFIC_TABLE_NAME, TRAFFIC_ROWID + "=" + rowId,
		    null) > 0;
	default:
	    return false;
	}
    }

    // ---删除所有标题---
    public int deleteAllTitle(int table) {
	switch (table) {
	case TABLE_APP:
	    return db.delete(APP_TABLE_NAME, null, null);
	case TABLE_TRAFFIC:
	    return db.delete(TRAFFIC_TABLE_NAME, null, null);
	default:
	    return 0;
	}
    }

    // ---检索所有标题---
    public Cursor getAllTitles(int table) {
	switch (table) {
	case TABLE_APP:
	    return db.query(APP_TABLE_NAME, new String[] { APP_ROWID,
		    APP_COUNTID, APP_NAME, APP_OCCUPANCY, APP_TIME }, null,
		    null, null, null, null);
	case TABLE_TRAFFIC:
	    return db.query(TRAFFIC_TABLE_NAME, new String[] { TRAFFIC_ROWID,
		    TRAFFIC_TIME, TRAFFIC_TRAFFICSTATS_MOBILE,
		    TRAFFIC_TRAFFICSTATS_TOTAL, TRAFFIC_LOCATION }, null, null,
		    null, null, null);
	default:
	    return null;
	}
    }

    // ---检索一个指定标题---
    public Cursor getTitle(int table, long rowId) throws SQLException {
	Cursor mCursor;
	switch (table) {
	case TABLE_APP:
	    mCursor = db.query(true, APP_TABLE_NAME, new String[] { APP_ROWID,
		    APP_COUNTID, APP_NAME, APP_OCCUPANCY, APP_TIME }, APP_ROWID
		    + "=" + rowId, null, null, null, null, null);
	case TABLE_TRAFFIC:
	    mCursor = db.query(true, TRAFFIC_TABLE_NAME, new String[] {
		    TRAFFIC_ROWID, TRAFFIC_TIME, TRAFFIC_TRAFFICSTATS_MOBILE,
		    TRAFFIC_TRAFFICSTATS_TOTAL, TRAFFIC_LOCATION },
		    TRAFFIC_ROWID + "=" + rowId, null, null, null, null, null);
	default:
	    mCursor = null;
	}
	if (mCursor != null) {
	    mCursor.moveToFirst();
	}
	return mCursor;
    }

    // ---更新一个标题---
    public boolean updateTitle(long rowId, int appCountId, String appName,
	    float appOcc, String appTime) {
	ContentValues args = new ContentValues();
	args.put(APP_COUNTID, appCountId);
	args.put(APP_NAME, appName);
	args.put(APP_OCCUPANCY, appOcc);
	args.put(APP_TIME, appTime);
	return db.update(APP_TABLE_NAME, args, APP_ROWID + "=" + rowId, null) > 0;
    }

    public boolean updateTitle(long rowId, String traTime,
	    float traStatsMobile, float traStatsTotal, String traLocation) {
	ContentValues args = new ContentValues();
	args.put(TRAFFIC_TIME, traTime);
	args.put(TRAFFIC_TRAFFICSTATS_MOBILE, traStatsMobile);
	args.put(TRAFFIC_TRAFFICSTATS_TOTAL, traStatsTotal);
	args.put(TRAFFIC_LOCATION, traLocation);
	return db.update(TRAFFIC_TABLE_NAME, args, TRAFFIC_ROWID + "=" + rowId,
		null) > 0;
    }
}

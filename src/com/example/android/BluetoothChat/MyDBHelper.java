package com.example.android.BluetoothChat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDBHelper extends SQLiteOpenHelper {
	private final static String DATABASE_NAME = "leaf_addr.db";
	private final static int DATABASE_VERSION = 1;
	private final static String TABLE_NAME = "leaf_addr";
	public final static String LEAF_ID = "_id";
	public final static String LEAF_NAME = "name";
	public final static String LEAF_ADDR = "address";
	public final static String LEAF_GROUP = "lgroup";

	Context context;

	SQLiteDatabase leafBD;

	public MyDBHelper(Context context) {
		// TODO Auto-generated constructor stub
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	// 创建table
	@Override
	public void onCreate(SQLiteDatabase db) {
		// String sql = "CREATE TABLE " + TABLE_NAME + " (" + LEAF_ID
		// + " INTEGER primary key autoincrement, "
		// + LEAF_NAME + " text, "
		// + LEAF_GROUP + " text, "
		// + LEAF_ADDR + " text);";
		// db.execSQL(sql);
		db.execSQL("DROP TABLE IF EXISTS person");
		// 创建person表
		db.execSQL("CREATE TABLE leaf_addr "
				+ "(_id INTEGER PRIMARY KEY AUTOINCREMENT, " + "name VARCHAR, "
				+ "address VARCHAR, " + "lgroup VARCHAR)");
	}

	public void openDataBase(MyDBHelper helper) {
		leafBD = helper.getWritableDatabase();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
		db.execSQL(sql);
		onCreate(db);
	}

	public Cursor select() {
		Cursor cursor = leafBD.query(TABLE_NAME, null, null, null, null, null,
				null);
		return cursor;
	}

	public Cursor selectSet() {
		String[] columns = { LEAF_ID, LEAF_NAME, LEAF_ADDR, LEAF_GROUP};
		String[] parms = { "00" };
		Cursor result = leafBD.query(TABLE_NAME, columns, "address<>?", parms, null,
				null, null);
		return result;
	}

	// 增加操作
	public long insert(String leafName, String leafAddr, String leafGroup) {
		/* ContentValues */
		ContentValues cv = new ContentValues();
		cv.put(LEAF_NAME, leafName);
		cv.put(LEAF_ADDR, leafAddr);
		cv.put(LEAF_GROUP, leafGroup);
		long row = leafBD.insert(TABLE_NAME, null, cv);
		return row;
	}

	// 删除操作
	public void delete(int id) {
		String where = LEAF_ID + " = ?";
		String[] whereValue = { Integer.toString(id) };
		leafBD.delete(TABLE_NAME, where, whereValue);
	}

	// 修改操作
	public void update(int id, String leafName, String leafAddr,
			String leafGroup) {
		String where = LEAF_ID + " = ?";
		String[] whereValue = { Integer.toString(id) };

		ContentValues cv = new ContentValues();
		cv.put(LEAF_NAME, leafName);
		cv.put(LEAF_ADDR, leafAddr);
		cv.put(LEAF_GROUP, leafGroup);
		leafBD.update(TABLE_NAME, cv, where, whereValue);
	}
}
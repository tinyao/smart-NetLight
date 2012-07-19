package com.example.android.BluetoothChat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class SceneDBHelper extends SQLiteOpenHelper {
	private final static String DATABASE_NAME = "scene.db";
	private final static int DATABASE_VERSION = 1;
	private final static String TABLE_NAME = "scene_table";
	public final static String SCENE_ID = "_id";
	public final static String SCENE_NAME = "name";
	public final static String SCENE_A = "a_lux";
	public final static String SCENE_B = "b_lux";
	public final static String SCENE_C = "c_lux";
	public final static String SCENE_D = "d_lux";

	Context context;

	SQLiteDatabase sceneDb;

	public SceneDBHelper(Context context) {
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
		db.execSQL("CREATE TABLE scene_table "
				+ "(_id INTEGER PRIMARY KEY AUTOINCREMENT, " 
				+ "name VARCHAR, "
				+ "a_lux VARCHAR, " 
				+ "b_lux VARCHAR, "
				+ "c_lux VARCHAR, "
				+ "d_lux VARCHAR)");
	}

	public void openDataBase(SceneDBHelper helper) {
		sceneDb = helper.getWritableDatabase();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
		db.execSQL(sql);
		onCreate(db);
	}

	public Cursor select() {
		Cursor cursor = sceneDb.query(TABLE_NAME, null, null, null, null, null,
				null);
		return cursor;
	}

	// 增加操作
	public long insert(String sceneName, String sceneA, String sceneB, String sceneC, String sceneD) {
		/* ContentValues */
		ContentValues cv = new ContentValues();
		cv.put(SCENE_NAME, sceneName);
		cv.put(SCENE_A, sceneA);
		cv.put(SCENE_B, sceneB);
		cv.put(SCENE_C, sceneC);
		cv.put(SCENE_D, sceneD);
		long row = sceneDb.insert(TABLE_NAME, null, cv);
		return row;
	}

	// 删除操作
	public void delete(int id) {
		String where = SCENE_ID + " = ?";
		String[] whereValue = { Integer.toString(id) };
		sceneDb.delete(TABLE_NAME, where, whereValue);
	}

	// 修改操作
	public void update(int id, String sceneName, String sceneA, String sceneB, String sceneC, String sceneD) {
		String where = SCENE_ID + " = ?";
		String[] whereValue = { Integer.toString(id) };

		ContentValues cv = new ContentValues();
		cv.put(SCENE_NAME, sceneName);
		cv.put(SCENE_A, sceneA);
		cv.put(SCENE_B, sceneB);
		cv.put(SCENE_C, sceneC);
		cv.put(SCENE_D, sceneD);
		sceneDb.update(TABLE_NAME, cv, where, whereValue);
	}
}
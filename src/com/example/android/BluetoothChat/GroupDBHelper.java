package com.example.android.BluetoothChat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

 
public class GroupDBHelper extends SQLiteOpenHelper {
    private final static String DATABASE_NAME = "group_addr.db";
    private final static int DATABASE_VERSION = 1;
    private final static String TABLE_NAME = "group_addr";
    public final static String GROUP_ID = "_id";
    public final static String GROUP_NAME = "name";
    public final static String GROUP_ADDR = "address";
    public final static String GROUP_LEAF = "leaf";
    
    Context context;
    
    SQLiteDatabase groupDB;
 
    public GroupDBHelper(Context context) {
        // TODO Auto-generated constructor stub
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
 
    
    // 创建table
    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_NAME + " (" + GROUP_ID
                + " INTEGER primary key autoincrement, " + GROUP_NAME
                + " text, " + GROUP_ADDR
                + " text, "+ GROUP_LEAF + " text);";
        db.execSQL(sql);
    }
    
    
    public void openDataBase(GroupDBHelper helper){
    	groupDB = helper.getWritableDatabase();  
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
        db.execSQL(sql);
        onCreate(db);
    }
 
    public Cursor select() {
        Cursor cursor = groupDB
                .query(TABLE_NAME, null, null, null, null, null, null);
        return cursor;
    }
 
    // 增加操作
    public long insert(String groupName, String groupAddr, String leaf) {
        /* ContentValues */
        ContentValues cv = new ContentValues();
        cv.put(GROUP_NAME, groupName);
        cv.put(GROUP_ADDR, groupAddr);
        cv.put(GROUP_LEAF, leaf);
        long row = groupDB.insert(TABLE_NAME, null, cv);
        return row;
    }
 
    // 删除操作
    public void delete(int id) {
        String where = GROUP_ID + " = ?";
        String[] whereValue = { Integer.toString(id) };
        groupDB.delete(TABLE_NAME, where, whereValue);
    }
 
    // 修改操作
    public void update(int id, String groupName, String groupAddr, String leaf) {
        String where = GROUP_ID + " = ?";
        String[] whereValue = { Integer.toString(id) };
 
        ContentValues cv = new ContentValues();
        cv.put(GROUP_NAME, groupName);
        cv.put(GROUP_ADDR, groupAddr);
        cv.put(GROUP_LEAF, leaf);
        groupDB.update(TABLE_NAME, cv, where, whereValue);
    }
}
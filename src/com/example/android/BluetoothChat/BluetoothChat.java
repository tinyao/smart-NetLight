/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.BluetoothChat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * This is the main Activity that displays the current chat session.
 */
public class BluetoothChat extends Activity {

	// ViewPager是google SDk中自带的一个附加包的一个类，可以用来实现屏幕间的切换。
	// android-support-v4.jar
	private ViewPager mPager;// 页卡内容
	private List<View> listViews; // Tab页面列表
	private ImageView cursor;// 动画图片
	private int offset = 0;// 动画图片偏移量
	private int currIndex = 0;// 当前页卡编号
	private int bmpW;// 动画图片宽度

	private View layout1 = null;
	private View layout2 = null;
	private View layout3 = null;

	private boolean initBool = true;
	
	private boolean notConnectToasted = false;
	
	// Debugging
	private static final String TAG = "BluetoothChat";
	private static final boolean D = true;

	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	public static final int MESSAGE_ERROR_TOAST = 6;
	
	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	// Layout Views
	private TextView mTitle;
	private ListView mConversationView;
	private EditText mOutEditText;
	private Button mSendButton;
	private Button connectBtn;
	private ToggleButton showDebugBtn;
	private ImageView phoneConImg;

	// Layout panel
	private Bitmap bmp;
	private Matrix matrix = new Matrix();
	private Bitmap resizedBitmap;

	private SeekBar seek1, seek2, seek3, seek4; // the lamp seekbar
	private View pointer; // dashboard poioter
	private Animation am; // pointer rotate animation
	private TextView per1, per2, per3; // dashboard power percent
	private TextView sensorVal, sensorTargetVal;
	private TextView leafSelect;
	private View panel_1, panel_2, panel_3;
	private ToggleButton tab_1, tab_2, tab_3;

	private ToggleButton adjustBtn;
	private Button factorBtn, targetLuxBtn, fetchFactorBtn;
	private TextView lampA, lampB, lampC, lampD;
	private ImageView refreshBtn;
	
	private Spinner sceneSpinner;
	private EditText sceneNameEdt;
	private Button recoverSceneBtn, addSceneBtn;
	
	// Layout panel 3
	private Spinner operateSpinner;
	private View addrPanel;
	private View groupPanel;
	private View fadePanel;

	private SeekBar groupSeek;
	
	private ListView leafList, groupLeafList;
	private Spinner leafAddrSpinner, groupSpinner;
	private EditText leafNameEdt, groupNameEdt;
	private Button leafNameBtn, leafAddrSet, groupJoinSet;

	private Spinner fadeTimeSpinner, fadeRateSpinner;
	private Button fadeTimeBtn, fadeRateBtn;
	
	private TextView LogInfo;
	private ImageButton clearLogBtn, clearDebugLog;
	private ScrollView logScroll;
	private Cursor leafcursor, groupcursor, sceneCursor;
	
	private String curCtrlLeafAddr;	//Hex String
	private String curCtrlLeafAddrNormal;

	private int flag = -1; // wait for the respond
	private String responseCheck;
	private int toGroupId;
	private boolean isInSeekPanel = false;
	private String fadeTime="00", fadeRate="01";

	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Array adapter for the conversation thread
	private ArrayAdapter<String> mConversationArrayAdapter;
	// String buffer for outgoing messages
	private StringBuffer mOutStringBuffer;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	private BluetoothChatService mChatService = null;

	private SharedPreferences sp;
	private MyDBHelper helper;
	private GroupDBHelper groupHelper;
	private SceneDBHelper sceneHelper;
	
	private int leafCurrent = -1;
	private int groupCurrent = -1;
	private String groupJoinLeaf = "";
	private boolean queryLuxA, queryLuxB, queryLuxC, queryLuxD;
	private String targetHexLux = "";
	private ArrayList<String> sceneLuxArray = new ArrayList<String>(); 
	
	private AlarmReceiver alarmReceiver;
	private Stack sendStack;

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (D)
			Log.e(TAG, "+++ ON CREATE +++");

		// byte[] tt = hexStringToBytes("0fef");
		// printHexString(tt);

		// Set up the window layout
		// requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.main);
		
		Toast.makeText(this, "DEMO", Toast.LENGTH_SHORT).show();
		
		InitImageView();
		InitViewPager();

		// getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
		// R.layout.custom_title);
		sp = this.getSharedPreferences(PrefConfig.SHARED_PREF_NAME, 0);
		initViewById();

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		
	}

	/**
	 * 初始化ViewPager
	 */
	private void InitViewPager() {
		mPager = (ViewPager) findViewById(R.id.vPager);
		listViews = new ArrayList<View>();
		LayoutInflater mInflater = getLayoutInflater();
		layout1 = mInflater.inflate(R.layout.lay1, null);
		layout2 = mInflater.inflate(R.layout.lay_display, null);
		layout3 = mInflater.inflate(R.layout.lay3, null);

		listViews.add(layout1);
		listViews.add(layout2);
		listViews.add(layout3);
		mPager.setAdapter(new MyPagerAdapter(listViews));
		mPager.setCurrentItem(0);
		mPager.setOnPageChangeListener(new MyOnPageChangeListener());

		// Set up the custom title
		mTitle = (TextView) layout1.findViewById(R.id.detect_connect);
		mTitle.setText(R.string.app_name);
		mTitle = (TextView) layout1.findViewById(R.id.detect_connect);
	}

	/**
	 * 初始化动画
	 */
	private void InitImageView() {
		cursor = (ImageView) findViewById(R.id.cursor);
		bmpW = BitmapFactory.decodeResource(getResources(), R.drawable.a)
				.getWidth();// 获取图片宽度
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int screenW = dm.widthPixels;// 获取分辨率宽度
		offset = (screenW / 3 - bmpW) / 2;// 计算偏移量
		Matrix matrix = new Matrix();
		matrix.postTranslate(offset, 0);
		cursor.setImageMatrix(matrix);// 设置动画初始位置
	}

	private void initViewById() {
		// TODO Auto-generated method stub
		
		connectBtn = (Button)layout1.findViewById(R.id.connect_bt_btn);
		showDebugBtn = (ToggleButton)layout1.findViewById(R.id.show_debug_btn);
		phoneConImg = (ImageView)layout1.findViewById(R.id.phone_pic);
		connectBtn.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent serverIntent = new Intent(BluetoothChat.this, DeviceListActivity.class);
				startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			}
			
		});
		showDebugBtn.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				// TODO Auto-generated method stub
				if(arg1){
					View view = (View)layout1.findViewById(R.id.debug_view);
					view.setVisibility(View.VISIBLE);
					phoneConImg.setVisibility(View.GONE);
				}else{
					View view = (View)layout1.findViewById(R.id.debug_view);
					view.setVisibility(View.GONE);
					phoneConImg.setVisibility(View.VISIBLE);
				}
				
			}
			
		});
		
		pointer = (View) layout2.findViewById(R.id.pointer_view);
		seek1 = (SeekBar) layout2.findViewById(R.id.seekBar1);
		seek2 = (SeekBar) layout2.findViewById(R.id.seekBar2);
		seek3 = (SeekBar) layout2.findViewById(R.id.seekBar3);
		seek4 = (SeekBar) layout2.findViewById(R.id.seekBar4);

		per1 = (TextView) layout2.findViewById(R.id.percent_1);
		per2 = (TextView) layout2.findViewById(R.id.percent_2);
		per3 = (TextView) layout2.findViewById(R.id.percent_3);
		
		sensorVal = (TextView) layout2.findViewById(R.id.sensorVal);
		sensorTargetVal = (TextView) layout2.findViewById(R.id.sensorTargetVal);
		leafSelect = (TextView) layout2.findViewById(R.id.leafSelect);
		
		seek1.setOnSeekBarChangeListener(listener);
		seek2.setOnSeekBarChangeListener(listener);
		seek3.setOnSeekBarChangeListener(listener);
		seek4.setOnSeekBarChangeListener(listener);
		
		adjustBtn = (ToggleButton)layout2.findViewById(R.id.auto_adjust);
		factorBtn = (Button)layout2.findViewById(R.id.factor_set);
		targetLuxBtn = (Button) layout2.findViewById(R.id.targetLux);
		fetchFactorBtn = (Button) layout2.findViewById(R.id.fetch_factor);
		lampA = (TextView)layout2.findViewById(R.id.lamp_a_lux);
		lampB = (TextView)layout2.findViewById(R.id.lamp_b_lux);
		lampC = (TextView)layout2.findViewById(R.id.lamp_c_lux);
		lampD = (TextView)layout2.findViewById(R.id.lamp_d_lux);
		refreshBtn = (ImageView)layout2.findViewById(R.id.refresh);
		
		sceneSpinner = (Spinner) layout2.findViewById(R.id.scene_spinner);
		sceneNameEdt = (EditText) layout2.findViewById(R.id.scene_edt);
		recoverSceneBtn = (Button) layout2.findViewById(R.id.recover_scene);
		addSceneBtn = (Button) layout2.findViewById(R.id.add_scene);
		
//		adjustBtn.setOnClickListener(lay3btnListener);
		factorBtn.setOnClickListener(lay3btnListener);
		targetLuxBtn.setOnClickListener(lay3btnListener);
		fetchFactorBtn.setOnClickListener(lay3btnListener);
		leafSelect.setOnClickListener(lay3btnListener);
		refreshBtn.setOnClickListener(lay3btnListener);
		recoverSceneBtn.setOnClickListener(lay3btnListener);
		addSceneBtn.setOnClickListener(lay3btnListener);
		
		
		tab_1 = (ToggleButton) layout2.findViewById(R.id.ctrl_tab_1);
		tab_2 = (ToggleButton) layout2.findViewById(R.id.ctrl_tab_2);
		tab_3 = (ToggleButton) layout2.findViewById(R.id.ctrl_tab_3);

		panel_1 = (View) layout2.findViewById(R.id.panel_1);
		panel_2 = (View) layout2.findViewById(R.id.panel_2);
		panel_3 = (View) layout2.findViewById(R.id.panel_3);

		tab_1.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				tab_1.setChecked(true);
				tab_2.setChecked(false);
				tab_3.setChecked(false);
				panel_1.setVisibility(View.GONE);
				panel_2.setVisibility(View.VISIBLE);
				panel_3.setVisibility(View.GONE);

				if(!detectConnected()) return;
				
				// 离开单独调光面板，将fade恢复到此前的值
				if(isInSeekPanel){
					isInSeekPanel = false;
					listl.clear();
					listl.addLast(new SendMsgTread(BluetoothChat.this, "a3" + fadeTime, 0, -1));
					listl.addLast(new SendMsgTread(BluetoothChat.this, curCtrlLeafAddr + "2e", 200, PrefConfig.SET_FADE_TIME));
					listl.addLast(new SendMsgTread(BluetoothChat.this, curCtrlLeafAddr + "2e", 400, PrefConfig.SET_FADE_TIME));
					listl.addLast(new SendMsgTread(BluetoothChat.this, "a3" + fadeRate, 600, -1));
					listl.addLast(new SendMsgTread(BluetoothChat.this, curCtrlLeafAddr + "2f", 800, PrefConfig.SET_FADE_RATE));
					listl.addLast(new SendMsgTread(BluetoothChat.this, curCtrlLeafAddr + "2f", 1000, PrefConfig.SET_FADE_RATE));
					while(!listl.isEmpty()){
						listl.removeFirst().start();
					}
					Log.d("DEBUG", "recover the fadetime and faderate");
				}
			}
			
		});
		
		tab_2.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				tab_1.setChecked(false);
				tab_2.setChecked(true);
				tab_3.setChecked(false);
				panel_1.setVisibility(View.VISIBLE);
				panel_2.setVisibility(View.GONE);
				panel_3.setVisibility(View.GONE);
				isInSeekPanel = true; //
				
				listl.clear();
				
				if(!detectConnected()) return;
				
				// 查询当前的fade值，在收到回复后，继续执行任务，将fadeTime和fadeRate值设为0，1
				listl.addLast(new SendMsgTread(BluetoothChat.this, curCtrlLeafAddr + "a5", 0, PrefConfig.QUERY_FADE));
				listl.removeFirst().start();
			}
			
		});
		
		tab_3.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				tab_1.setChecked(false);
				tab_2.setChecked(false);
				tab_3.setChecked(true);
				panel_1.setVisibility(View.GONE);
				panel_2.setVisibility(View.GONE);
				panel_3.setVisibility(View.VISIBLE);
				
				if(!detectConnected()) return;
				
				// 离开单独调光面板，将fade恢复到此前的值
				if(isInSeekPanel){
					isInSeekPanel = false;
					listl.clear();
					listl.addLast(new SendMsgTread(BluetoothChat.this, "a3" + fadeTime, 0, -1));
					listl.addLast(new SendMsgTread(BluetoothChat.this, curCtrlLeafAddr + "2e", 200, PrefConfig.SET_FADE_TIME));
					listl.addLast(new SendMsgTread(BluetoothChat.this, curCtrlLeafAddr + "2e", 400, PrefConfig.SET_FADE_TIME));
					listl.addLast(new SendMsgTread(BluetoothChat.this, "a3" + fadeRate, 600, -1));
					listl.addLast(new SendMsgTread(BluetoothChat.this, curCtrlLeafAddr + "2f", 800, PrefConfig.SET_FADE_RATE));
					listl.addLast(new SendMsgTread(BluetoothChat.this, curCtrlLeafAddr + "2f", 1000, PrefConfig.SET_FADE_RATE));
					while(!listl.isEmpty()){
						listl.removeFirst().start();
					}
					Log.d("DEBUG", "recover the fadetime and faderate");
				}
				fillSceneToSpinner();
			}
			
		});
		
		adjustBtn.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				ImageView autoImg = (ImageView) layout2.findViewById(R.id.is_auto_on);
				if(!isChecked){
					autoAdjust();
					autoImg.setImageDrawable(BluetoothChat.this.getResources().getDrawable(R.drawable.factor_set));
					Log.d("DEBUG", "已开启自动调光");
				}else{
					stopAutoAdjust();
					autoImg.setImageDrawable(BluetoothChat.this.getResources().getDrawable(R.drawable.factor_unset));
					Log.d("DEBUG", "已停止自动调光");
				}
			}
			
		});

		// layout3
		operateSpinner = (Spinner) layout3.findViewById(R.id.operatons_spi);
		addrPanel = (View) layout3.findViewById(R.id.short_addr_panel);
		groupPanel = (View) layout3.findViewById(R.id.group_set_panel);
		fadePanel = (View) layout3.findViewById(R.id.fade_ctrl_panel);

		leafList = (ListView) layout3.findViewById(R.id.leaf_list);
		leafAddrSpinner = (Spinner) layout3
				.findViewById(R.id.leaf_addr_spinner);
		leafNameEdt = (EditText) layout3.findViewById(R.id.leaf_name_edt);
		leafNameBtn = (Button) layout3.findViewById(R.id.leaf_name_ok_btn);
		leafAddrSet = (Button) layout3.findViewById(R.id.leaf_set_btn);

		groupLeafList = (ListView) layout3.findViewById(R.id.group_leaf_list);
		groupSpinner = (Spinner) layout3.findViewById(R.id.group_spinner);
		groupNameEdt = (EditText) layout3.findViewById(R.id.group_name_edt);
		groupJoinSet = (Button) layout3.findViewById(R.id.group_join_btn);

		fadeTimeSpinner = (Spinner) layout3.findViewById(R.id.fade_time_spinner);
		fadeRateSpinner = (Spinner) layout3.findViewById(R.id.fade_rate_spinner);
		fadeTimeBtn = (Button) layout3.findViewById(R.id.fade_time_btn);
		fadeRateBtn = (Button) layout3.findViewById(R.id.fade_rate_btn);
		
		fadeTimeBtn.setOnClickListener(lay3btnListener);
		fadeRateBtn.setOnClickListener(lay3btnListener);

		groupSeek = (SeekBar) layout3.findViewById(R.id.group_seek);
		groupSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				
				if(mChatService.getState() != BluetoothChatService.STATE_CONNECTED){
					return;
				}
				
				listl.addLast(new SendMsgTread(BluetoothChat.this,"80"+convertInt2Hex(progress),10,-1));
				listl.removeFirst().start();
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				listl.clear();
				if(!detectConnected()) return;
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		LogInfo = (TextView) layout3.findViewById(R.id.loginfo);
		clearLogBtn = (ImageButton) layout3.findViewById(R.id.clear_log);
		clearDebugLog = (ImageButton) layout1.findViewById(R.id.clear_debug_log);
		logScroll = (ScrollView) layout3.findViewById(R.id.log_info_scroll);

		initDatabase();

		operateSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// TODO Auto-generated method stub
				switch (arg2) {
				case 0:
					addrPanel.setVisibility(View.VISIBLE);
					groupPanel.setVisibility(View.GONE);
					fadePanel.setVisibility(View.GONE);
					break;
				case 1:
					addrPanel.setVisibility(View.GONE);
					groupPanel.setVisibility(View.VISIBLE);
					fadePanel.setVisibility(View.GONE);
					break;
				case 2:
					addrPanel.setVisibility(View.GONE);
					groupPanel.setVisibility(View.GONE);
					fadePanel.setVisibility(View.VISIBLE);
					break;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}

		});
		
		leafAddrSpinner.setOnItemSelectedListener(spinnerSelectedListener);
		groupSpinner.setOnItemSelectedListener(spinnerSelectedListener);
		sceneSpinner.setOnItemSelectedListener(spinnerSelectedListener);
		
		leafNameBtn.setOnClickListener(lay3btnListener);
		leafAddrSet.setOnClickListener(lay3btnListener);
		groupJoinSet.setOnClickListener(lay3btnListener);
		clearLogBtn.setOnClickListener(lay3btnListener);
		
		leafList.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
					long id) {
				// TODO Auto-generated method stub
				leafcursor.moveToPosition(pos);
				String leafName = leafcursor.getString(leafcursor.getColumnIndex(MyDBHelper.LEAF_NAME));
				int saddr = Integer.valueOf(
						leafcursor.getString(leafcursor.getColumnIndex(MyDBHelper.LEAF_ADDR)));
				if(saddr != 0){
					leafAddrSpinner.setSelection((saddr - 1)/2);
				}else{
					Toast.makeText(BluetoothChat.this, "该从机还未设置地址！", Toast.LENGTH_SHORT).show();
				}
				leafNameEdt.setText(leafName);
				leafCurrent = pos; // 设置当前操作的从机position
			}
			
		});
		

	}

	OnSeekBarChangeListener listener = new OnSeekBarChangeListener() {

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
			
			if(!detectConnected()) return;
			
			flag = -1;
			
			listl.clear();//设置光照前，清空此前残留任务
			
			switch(seekBar.getId()){
			case R.id.seekBar1:
				listl.addLast(new SendMsgTread(BluetoothChat.this,"a3"+"01",10,-1));
				break;
			case R.id.seekBar2:
				listl.addLast(new SendMsgTread(BluetoothChat.this,"a3"+"02",10,-1));
				break;
			case R.id.seekBar3:
				listl.addLast(new SendMsgTread(BluetoothChat.this,"a3"+"03",10,-1));
				break;
			case R.id.seekBar4:
				listl.addLast(new SendMsgTread(BluetoothChat.this,"a3"+"04 ",10,-1));
				break;
			}
			
			listl.removeFirst().start();
		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			// TODO Auto-generated method stub
			
			Message msg = panelHandler.obtainMessage();
			msg.what = 0; // pointer rotate with the corresponding angle
			msg.arg1 = progress;
			panelHandler.sendMessage(msg);	//动画效果
			
			if(mChatService.getState() != BluetoothChatService.STATE_CONNECTED){
				return;
			}
			
			//发送调光
			String s = convertInt2Hex((int)(progress*(255/100.0)));
			Log.d("FALG", "flag:  " + flag);
			
			if(flag != -1){
				return;
			}
			
			String adjustAddr = convertInt2Hex(Integer.valueOf(curCtrlLeafAddrNormal) - 1);
			
			switch(seekBar.getId()){
			case R.id.seekBar1:
				listl.addLast(new SendMsgTread(BluetoothChat.this, adjustAddr+s, 10, -1));
				lampA.setText("" + progress + "%");
				break;
			case R.id.seekBar2:
				listl.addLast(new SendMsgTread(BluetoothChat.this, adjustAddr+s, 10, -1));
				lampB.setText("" + progress + "%");
				break;
			case R.id.seekBar3:
				listl.addLast(new SendMsgTread(BluetoothChat.this, adjustAddr+s, 10, -1));
				lampC.setText("" + progress + "%");
				break;
			case R.id.seekBar4:
				listl.addLast(new SendMsgTread(BluetoothChat.this, adjustAddr+s, 10, -1));
				lampD.setText("" + progress + "%");
				break;
			}
			
			new Thread(){
				public void run(){
					try {
						sleep(200);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(!listl.isEmpty()){
						// single adjust
						Log.d("DEBUG", "----------adjust--------");
						listl.removeFirst().start();
					}
				}
			}.start();
			
		}
	};

	Handler panelHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);

			switch (msg.what) {
			case 0:
				int sum_angle = (seek1.getProgress() + seek2.getProgress()
						+ seek3.getProgress() + seek4.getProgress()) / 4;

				// 动画设定(指定旋转动画) (startAngle, endAngle, rotateX, rotateY) 88 17
				am = new RotateAnimation(200 * (sum_angle - 0.25f) / 100,
						200 * sum_angle / 100, dip2px(BluetoothChat.this, 53),
						dip2px(BluetoothChat.this, 11));

				am.setFillAfter(true);

				pointer.startAnimation(am);

				per1.setText("" + sum_angle / 100);
				per2.setText("" + sum_angle % 100 / 10);
				per3.setText("" + sum_angle % 100 % 10);

				Log.d("DEBUG", "-----rotate-----");
				
				double powerV = (int)((sum_angle*4/100.0 * 256 / 256.0 * 3) * 10) / 10.0;
				TextView powerTv = (TextView)layout2.findViewById(R.id.power_value);
				powerTv.setText( powerV + "W");
				
//				listl.clear();
				
				break;
			case PrefConfig.UPDATE_FACTOR:
				ImageView factorImg = (ImageView)layout2.findViewById(R.id.is_factor_set);
				factorImg.setImageDrawable(BluetoothChat.this.getResources().getDrawable(R.drawable.factor_set));
				break;
			}

		}

	};

	/**
	 * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
	 */
	public static int dip2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}

	/**
	 * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
	 */
	public static int px2dip(Context context, float pxValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (pxValue / scale + 0.5f);
	}

	@Override
	public void onStart() {
		super.onStart();
		if (D)
			Log.e(TAG, "++ ON START ++");

		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the chat session
		} else {
			if (mChatService == null)
				setupChat();
		}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		if (D)
			Log.e(TAG, "+ ON RESUME +");

		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity
		// returns.
		if (mChatService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
				// Start the Bluetooth chat services
				mChatService.start();
			}
		}
		
		IntentFilter filter = new IntentFilter();
        filter.addAction("com.tinyao.alarm");
        alarmReceiver = new AlarmReceiver();
        this.registerReceiver(alarmReceiver, filter);
		
	}

	private void setupChat() {
		Log.d(TAG, "setupChat()");

		// Initialize the array adapter for the conversation thread
		mConversationArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.message);
		mConversationView = (ListView) layout1.findViewById(R.id.in);
		mConversationView.setAdapter(mConversationArrayAdapter);

		// Initialize the compose field with a listener for the return key
		mOutEditText = (EditText) layout1.findViewById(R.id.edit_text_out);
		mOutEditText.setOnEditorActionListener(mWriteListener);

		// Initialize the send button with a listener that for click events
		mSendButton = (Button) layout1.findViewById(R.id.button_send);
		mSendButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Send a message using content of the edit text widget
				TextView view = (TextView) layout1
						.findViewById(R.id.edit_text_out);
				String message = view.getText().toString();
				view.setText("");
				sendMessage(message);
			}
		});

		// Initialize the BluetoothChatService to perform bluetooth connections
		mChatService = new BluetoothChatService(this, mHandler);

		// Initialize the buffer for outgoing messages
		mOutStringBuffer = new StringBuffer("");
		
		sendStack = new Stack();

	}

	@Override
	public synchronized void onPause() {
		super.onPause();
		if (D)
			Log.e(TAG, "- ON PAUSE -");
	}

	@Override
	public void onStop() {
		super.onStop();
		if (D)
			Log.e(TAG, "-- ON STOP --");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Stop the Bluetooth chat services
		if (mChatService != null)
			mChatService.stop();
		if (D)
			Log.e(TAG, "--- ON DESTROY ---");
		
//		IntentFilter filter = new IntentFilter();
//        filter.addAction("com.tinyao.alarm");
//
//        this.unregisterReceiver(alarmReceiver);
	}

	private void ensureDiscoverable() {
		if (D)
			Log.d(TAG, "ensure discoverable");
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	/**
	 * Sends a message.
	 * 
	 * @param message
	 *            A string of text to send.
	 */
	private void sendMessage(String message) {
		// Check that we're actually connected before trying anything
		if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
			Message msg = mHandler.obtainMessage();
			msg.what = MESSAGE_ERROR_TOAST;
			msg.obj = getResources().getString(R.string.not_connected);
			mHandler.sendMessage(msg);
			return;
		}

		// Check that there's actually something to send
		if (message.length() > 0) {
			// Get the message bytes and tell the BluetoothChatService to write

			byte[] send = hexStringToBytes(message);
			// printHexString(send);
			mChatService.write(send);
//			Log.d("DEBUG", "" + send);

			// Reset out string buffer to zero and clear the edit text field
			mOutStringBuffer.setLength(0);
//			mOutEditText.setText(mOutStringBuffer);
//			mOutEditText.setText("");
		}
	}

	
	private boolean detectConnected(){
		// Check that we're actually connected before trying anything
		if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
				Message msg = mHandler.obtainMessage();
				msg.what = MESSAGE_ERROR_TOAST;
				msg.obj = getResources().getString(R.string.not_connected);
				mHandler.sendMessage(msg);
				return false;
		}
		return true;
	}
	
	/**
	 * Convert hex string to byte[]
	 * 
	 * @param hexString
	 *            the hex string
	 * @return byte[]
	 */
	public static byte[] hexStringToBytes(String hexString) {
		if (hexString == null || hexString.equals("")) {
			return null;
		}
		hexString = hexString.toUpperCase();
		int length = hexString.length() / 2;
		char[] hexChars = hexString.toCharArray();
		byte[] d = new byte[length];
		for (int i = 0; i < length; i++) {
			int pos = i * 2;
			d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
		}
		return d;
	}

	/**
	 * Convert char to byte
	 * 
	 * @param c
	 *            char
	 * @return byte
	 */
	private static byte charToByte(char c) {
		return (byte) "0123456789ABCDEF".indexOf(c);
	}

	// 将指定byte数组以16进制的形式打印到控制台
	public static String BytesToString(byte[] b) {
		String result = "";
		for (int i = 0; i < b.length; i++) {
			String hex = Integer.toHexString(b[i] & 0xFF);
			if (hex.length() == 1) {
				hex = '0' + hex;
			}
			result = result + hex.toUpperCase();
		}
		return result;
	}

	// The action listener for the EditText widget, to listen for the return key
	private TextView.OnEditorActionListener mWriteListener = new TextView.OnEditorActionListener() {
		public boolean onEditorAction(TextView view, int actionId,
				KeyEvent event) {
			// If the action is a key-up event on the return key, send the
			// message
			if (actionId == EditorInfo.IME_NULL
					&& event.getAction() == KeyEvent.ACTION_UP) {
				String message = view.getText().toString();
				sendMessage(message);
			}
			if (D)
				Log.i(TAG, "END onEditorAction");
			return true;
		}
	};

	// The Handler that gets information back from the BluetoothChatService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				if (D)
					Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothChatService.STATE_CONNECTED:
					mTitle.setText(R.string.title_connected_to);
					mTitle.append(mConnectedDeviceName);
					mConversationArrayAdapter.clear();
					
					Log.d("DEBUG", curCtrlLeafAddr + "---connected---" + curCtrlLeafAddrNormal);
					// 连接成功，设置地址，保证首次操作非查询
					if(Integer.valueOf(curCtrlLeafAddrNormal) < 16){
						curCtrlLeafAddr = "0" + curCtrlLeafAddr;
					}
					setShortAddress(curCtrlLeafAddr, curCtrlLeafAddrNormal);
					queryToInitPanel();
					clearDebugLog.setVisibility(View.VISIBLE);
					break;
				case BluetoothChatService.STATE_CONNECTING:
					mTitle.setText(R.string.title_connecting);
					break;
				case BluetoothChatService.STATE_LISTEN:
				case BluetoothChatService.STATE_NONE:
					mTitle.setText(R.string.title_not_connected);
					break;
				}
				break;
			case MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				// construct a string from the buffer

				// String writeMessage = new String(writeBuf);
				String writeMessage = BytesToString(writeBuf);
				mConversationArrayAdapter.add("Me:  " + writeMessage);
				LogInfo.append("\n> " + "send cmd: " + writeMessage);
				break;
			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				// construct a string from the valid bytes in the buffer
				String readMessage = new String(readBuf, 0, msg.arg1);
				Log.d("DEBUG", "receive: " + readMessage);
				Log.d("DEBUG", "receive:----"  + String.valueOf(Byte.valueOf(readBuf[0])));
				int rec = Integer.valueOf(String.valueOf(Byte.valueOf(readBuf[0])));
				
				if(rec < 0){
					rec = 256 + rec;
				}
				
				if(readMessage.length() > 0){
					mConversationArrayAdapter.add(mConnectedDeviceName + ":  "
							+ "0x" + convertInt2Hex(rec));
					LogInfo.append("\n" + "receive str: " + "0x" + convertInt2Hex(rec));
					dealMsgResponse(rec);
				}
				
				//下一个任务
				if(!listl.isEmpty())
					listl.removeFirst().start();
				
				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			case MESSAGE_ERROR_TOAST:
				Toast.makeText(getApplicationContext(),
						String.valueOf(msg.obj), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// Get the BLuetoothDevice object
				BluetoothDevice device = mBluetoothAdapter
						.getRemoteDevice(address);
				// Attempt to connect to the device
				mChatService.connect(device);
			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a chat session
				setupChat();
			} else {
				// User did not enable Bluetooth or an error occured
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.scan:
			// Launch the DeviceListActivity to see devices and do scan
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			return true;
		case R.id.discoverable:
			// Ensure this device is discoverable by others
			ensureDiscoverable();
			return true;
		}
		return false;
	}

	/**
	 * 页卡切换监听
	 */
	public class MyOnPageChangeListener implements OnPageChangeListener {

		int one = offset * 2 + bmpW;// 页卡1 -> 页卡2 偏移量
		int two = one * 2;// 页卡1 -> 页卡3 偏移量

		@Override
		public void onPageSelected(int arg0) {
			Animation animation = null;
			switch (arg0) {
			case 0:
				if (currIndex == 1) {
					animation = new TranslateAnimation(one, 0, 0, 0);
				} else if (currIndex == 2) {
					animation = new TranslateAnimation(two, 0, 0, 0);
				}
				break;
			case 1:
				if (currIndex == 0) {
					animation = new TranslateAnimation(offset, one, 0, 0);
				} else if (currIndex == 2) {
					animation = new TranslateAnimation(two, one, 0, 0);
				}
				break;
			case 2:
				if (currIndex == 0) {
					animation = new TranslateAnimation(offset, two, 0, 0);
				} else if (currIndex == 1) {
					animation = new TranslateAnimation(one, two, 0, 0);
				}
				break;
			}
			currIndex = arg0;
			animation.setFillAfter(true);// True:图片停在动画结束位置
			animation.setDuration(300);
			cursor.startAnimation(animation);
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}

		@Override
		public void onPageScrollStateChanged(int arg0) {
		}
	}

	/* layout3 Button Click Listener */
	public View.OnClickListener lay3btnListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch (v.getId()) {
			case R.id.refresh:
				// 震动
				Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				long[] pattern = { 60, 60 }; // 停止 开启 停止 开启
				vibrator.vibrate(pattern, -1);
				queryLux(10, true);
				break;
			case R.id.factor_set:
				resetFactor();
				break;
			case R.id.leaf_name_ok_btn:	// 更新从机名称
				int updateId = Integer.
					valueOf(leafcursor.getString(leafcursor.getColumnIndexOrThrow(MyDBHelper.LEAF_ID)));
				String addr = leafcursor.getString(leafcursor.getColumnIndexOrThrow(MyDBHelper.LEAF_ADDR));
				String group = leafcursor.getString(leafcursor.getColumnIndexOrThrow(MyDBHelper.LEAF_GROUP));
				helper.update(updateId, 
						leafNameEdt.getText().toString(), addr, group);
				fillData2LeafList();
				break;
			case R.id.leaf_set_btn:		// 设置从机地址
				if(!detectConnected()) return;
				String address = leafAddrSpinner.getSelectedItem().toString();// 获取地址str
				// 设置shortAddress，成功后将载数据库中更新从机（insert/update）
				String hexAddr = Integer.toHexString(Integer.valueOf(address));
				if(Integer.valueOf(address) < 16){
					hexAddr = "0" + hexAddr;
				}
				Log.d("DEBUG", "send Addr: " + "0x" + hexAddr);
				setShortAddress(hexAddr, address);
				break;
			case R.id.group_join_btn:	//设置组成员 
				if(!detectConnected()) return;
				String groupAddress = groupcursor.getString(
						groupcursor.getColumnIndexOrThrow(GroupDBHelper.GROUP_ADDR));
				SparseBooleanArray sba = groupLeafList.getCheckedItemPositions();
				// 待处理组Id
				String gId = groupcursor.getString(
						groupcursor.getColumnIndexOrThrow(GroupDBHelper.GROUP_ID));
				setGroupMembers(gId, groupAddress, getLeavesAddressArray(sba));;
				break;
			case R.id.targetLux:
				if(!detectConnected()) return;
				setTargetLux();
				break;
			case R.id.fetch_factor:
				fetchExistFactor();
				break;
			case R.id.leafSelect:
				AlertDialog.Builder builder = new AlertDialog.Builder(BluetoothChat.this);
				final Cursor addrleafCursor = helper.selectSet();
				if(addrleafCursor.getCount() == 0){
					Toast.makeText(BluetoothChat.this, "您还没有设置灯地址，转到设置面板开始设置...s", Toast.LENGTH_SHORT).show();
					break;
				}
				SimpleCursorAdapter adapter = new SimpleCursorAdapter(BluetoothChat.this,
						R.layout.leaf_select_item, addrleafCursor,
						new String[] { MyDBHelper.LEAF_NAME, MyDBHelper.LEAF_ADDR},
						new int[] { R.id.item_leaf_name, R.id.item_leaf_addr });
				builder.setAdapter(adapter, new DialogInterface.OnClickListener(){

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						// 选中当前操作组
						addrleafCursor.moveToPosition(which);
						String name = addrleafCursor.getString(addrleafCursor.getColumnIndexOrThrow(MyDBHelper.LEAF_NAME));
						String addr = addrleafCursor.getString(addrleafCursor.getColumnIndexOrThrow(MyDBHelper.LEAF_ADDR));
						String hexAddr = Integer.toHexString(Integer.valueOf(addr));
						if(Integer.valueOf(addr) < 16){
							hexAddr = "0" + hexAddr;
						}
						setShortAddress(hexAddr, addr);
						Log.d("DEBUG", "select leaf----------" + hexAddr);
						sp.edit().putString("cureent_leaf_name", name)
							.putString("current_leaf_addr", addr).commit();
						curCtrlLeafAddr = hexAddr;
						leafSelect.setText(name);
					}
					
				});
				AlertDialog dialog = builder.create();
				dialog.setCanceledOnTouchOutside(true);
				dialog.show();
				break;
			case R.id.recover_scene:
				//恢复场景设置
				recoverScene();
				break;
			case R.id.add_scene:
				if(!detectConnected()) return;
				String name = sceneNameEdt.getText().toString();
				addCurrentLux2Scene(name);
				break;
			case R.id.fade_time_btn:
				if(!detectConnected()) return;
				String hexFadeT = convertInt2Hex(Integer.valueOf(fadeTimeSpinner.getSelectedItem().toString()));
				setFadeTime(hexFadeT);
				break;
			case R.id.fade_rate_btn:
				if(!detectConnected()) return;
				String hexFadeR = convertInt2Hex(Integer.valueOf(fadeRateSpinner.getSelectedItem().toString()));
				setFadeRate(hexFadeR);
				break;
			case R.id.clear_log:
				LogInfo.setText("");
				break;
			}
		}


	};
	
	
	public void setFadeTime(String fadeT){
		listl.clear();
		listl.addLast(new SendMsgTread(BluetoothChat.this, "a3" + fadeT, 0, -1));
		listl.addLast(new SendMsgTread(BluetoothChat.this, curCtrlLeafAddr + "2e", 200, PrefConfig.SET_FADE_TIME));
		listl.addLast(new SendMsgTread(BluetoothChat.this, curCtrlLeafAddr + "2e", 400, PrefConfig.SET_FADE_TIME));
		while(!listl.isEmpty()){
			listl.removeFirst().start();
		}
	}
	
	public void setFadeRate(String luxVal){
		listl.clear();
		listl.addLast(new SendMsgTread(BluetoothChat.this, "a3" + luxVal, 0, -1));
		listl.addLast(new SendMsgTread(BluetoothChat.this, curCtrlLeafAddr + "2f", 200, PrefConfig.SET_FADE_RATE));
		listl.addLast(new SendMsgTread(BluetoothChat.this, curCtrlLeafAddr + "2f", 400, PrefConfig.SET_FADE_RATE));
		while(!listl.isEmpty()){
			listl.removeFirst().start();
		}
	}

	/* layout3 Spinner Selected Listener */
	public AdapterView.OnItemSelectedListener spinnerSelectedListener = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> arg0, View v, int position,
				long arg3) {
			// TODO Auto-generated method stub
			switch (arg0.getId()) {
			case R.id.group_spinner:
				Log.d("DEBUG", "select--group" + "");
				groupcursor.moveToPosition(position);
				String gName = groupcursor.getString(
						groupcursor.getColumnIndexOrThrow(GroupDBHelper.GROUP_NAME));
				groupNameEdt.setText(gName);
//				String gId = groupcursor.getString(
//						groupcursor.getColumnIndexOrThrow(GroupDBHelper.GROUP_ID));
////				refreshLeafListWithGroup(gId);
				
				String leaves = groupcursor.getString(
						groupcursor.getColumnIndexOrThrow(GroupDBHelper.GROUP_LEAF));
				if(!leaves.equals("")){
					String[] leafID = leaves.split(",");
					Log.d("DEBUG", "select--" + leaves);
					for(String pos:leafID)
						groupLeafList.setItemChecked(Integer.valueOf(pos), true);
				}
				groupCurrent = position;
				break;
			case R.id.scene_spinner:
				Log.d("DEBUG", "select--scene" + position);
				sceneCursor.moveToPosition(position);
				sceneLuxArray.clear();
				String luxa = sceneCursor.getString(sceneCursor.getColumnIndexOrThrow(SceneDBHelper.SCENE_A));
				String luxb = sceneCursor.getString(sceneCursor.getColumnIndexOrThrow(SceneDBHelper.SCENE_B));
				String luxc = sceneCursor.getString(sceneCursor.getColumnIndexOrThrow(SceneDBHelper.SCENE_C));
				String luxd = sceneCursor.getString(sceneCursor.getColumnIndexOrThrow(SceneDBHelper.SCENE_D));
				sceneLuxArray.add(0, luxa);
				sceneLuxArray.add(1, luxb);
				sceneLuxArray.add(2, luxc);
				sceneLuxArray.add(3, luxd);
				Toast.makeText(BluetoothChat.this, 
						"A: "+luxa +
						"\tB: "+luxb +
						"\nC: "+luxc +
						"\tD: "+luxd,
						Toast.LENGTH_SHORT).show();
				break;
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// TODO Auto-generated method stub
			
		}

	};

	/*
	 * 初始化数据库
	 */
	public void initDatabase(){
		//从机leaf数据库
		helper = new MyDBHelper(this);
		helper.openDataBase(helper);
		
		groupHelper = new GroupDBHelper(this);
		groupHelper.openDataBase(groupHelper);
		
		sceneHelper = new SceneDBHelper(this);
		sceneHelper.openDataBase(sceneHelper);
		
		if(sp.getBoolean(PrefConfig.FIRST_RUN, true)){
			String leafTotal = "";
			for(int i=0; i<16; i++){ //初始化16个从机，地址未设置
				helper.insert("从机" + i, "00", "0");
				if(i == 0) 
					leafTotal = "" + 0;
				else
					leafTotal = leafTotal + "," + i;
			}
			
			for(int j=0; j<5; j++){
				groupHelper.insert("群组" + j, "" + (96 + j), "");
			}
			
			sp.edit().putBoolean(PrefConfig.FIRST_RUN, false).commit();
		}
		
		fillData2LeafList();
		
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
	            this, R.array.short_addr_array, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		leafAddrSpinner.setAdapter(adapter);
	}
	
	
	public void fillSceneToSpinner(){
		sceneCursor = sceneHelper.select();
		SimpleCursorAdapter sceneAdapter = new SimpleCursorAdapter(BluetoothChat.this,
				android.R.layout.simple_spinner_item, sceneCursor,
				new String[] { SceneDBHelper.SCENE_NAME},
				new int[] { android.R.id.text1});
		sceneAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		sceneSpinner.setAdapter(sceneAdapter);
		if(sceneCursor.getCount() == 0){
			recoverSceneBtn.setEnabled(false);
		}
	}
	
	/*
	 * ==========================================================================
	 */
	/* ================================发送指令==================================== */
	/*
	 * ==========================================================================
	 * =
	 */

	/**
	 * 设置从机地址，并写入flash
	 * 
	 * @param time
	 *            睡眠时长millsecond
	 * @param cmd
	 *            指令
	 */
	protected void setShortAddress(final String hexAddr, final String addr) {
		// TODO Auto-generated method stub
		new Thread() {
			public void run() {
				try {
					sendMessage("A3" + hexAddr);
					sleep(200);
					sendMessage("ff80");
					sleep(150);
					sendMessage("ff80");
					sleep(150);
					sendMessage("ff82");
					sleep(150);
					sendMessage("ff82");
					// 查询是否设置成功
					sleep(200);
					sendMessage("ff9f");
					flag = PrefConfig.QUERY_SHORT_ADDR_SET; // 等待响应，响应类型
					responseCheck = addr;	// 用于检测与返回对比，判断设置是否成功，address是Hex字符串
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	public void setGroupMembers(String groupId, String groupAddress, ArrayList<HashMap<String, Object>> array){
		
		for(HashMap<String, Object> map : array){
			String address = String.valueOf(map.get("address"));//十进制
			groupJoinLeaf = String.valueOf(map.get("leaf_name"));
			String hexAddr = convertInt2Hex(Integer.valueOf(address));//十六进制地址
			String groupHexAddr = convertInt2Hex(Integer.valueOf(groupAddress));
			if(map.get("operate").equals("join")){
				addGroupMember(groupId, groupHexAddr, hexAddr);
			}else if(map.get("operate").equals("delete")){
				removeGroupMember(groupId, groupHexAddr, hexAddr);
			}
		}
		
		
	}
	
	
	public void addGroupMember(final String groupId, final String groupAddress, final String hexAddr){
		
		Log.d("DEBUG", "----groupId----" + groupId + "---");
		
		new Thread() {
			public void run() {
				try {
					sendMessage(hexAddr + groupAddress);
					sleep(200);
					sendMessage(hexAddr + groupAddress);
					sleep(200);
					sendMessage("ff82");
					sleep(150);
					sendMessage("ff82");
					
					if(Integer.valueOf(groupId) <= 8){
						// 查询是否设置成功, //是否载0～7内
						sleep(200);
						sendMessage(hexAddr + "c0"); 
						flag = PrefConfig.QUERY_LEAF_GROUP_1; // 等待响应，响应类型
						toGroupId = Integer.valueOf(groupId) - 1;
					}else{
						// 查询是否设置成功, //是否载8～f内
						sleep(200);
						sendMessage(hexAddr + "c1");
						flag = PrefConfig.QUERY_LEAF_GROUP_2; // 等待响应，响应类型
						toGroupId = Integer.valueOf(groupId) - 1;
					}
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();
		
	}
	
	public void removeGroupMember(final String groupId, String groupAddress, String hexAddr){
		
	}
	
	public ArrayList<HashMap<String, Object>> getLeavesAddressArray(SparseBooleanArray array){
		
		ArrayList<HashMap<String, Object>> addrList = new ArrayList<HashMap<String, Object>>();
		
		HashMap<String, Object> map; 
		
		Log.d("DEBUG", "groupCurrent---" + groupCurrent);
		groupcursor.moveToPosition(groupCurrent);
		String leaves = groupcursor.getString(groupcursor.getColumnIndexOrThrow(GroupDBHelper.GROUP_LEAF));
		Log.d("DEBUG", "leaves---" + leaves);
		
		if(!leaves.equals("")){
			String[] leafArray = leaves.split(",");
			
			//用于从组中删除从机
			for(String leafId:leafArray){
				if(!array.get(Integer.valueOf(leafId))){
					//原先在组内，现在不在组内的
					map = new HashMap<String, Object>();
					leafcursor.moveToPosition(Integer.valueOf(leafId));
					String addr = leafcursor.getString(leafcursor.getColumnIndexOrThrow(MyDBHelper.LEAF_ADDR));
					String leafName = leafcursor.getString(leafcursor.getColumnIndexOrThrow(MyDBHelper.LEAF_NAME));
					map.put("position", Integer.valueOf(leafId));
					map.put("address", addr);
					map.put("leaf_name", leafName);
					map.put("operate", "delete");
					addrList.add(map);
				}
			}
		}
		
		//添加到组内的从机
		for(int j=0; j<array.size(); j++){
			if(array.get(j)){
				map = new HashMap<String, Object>();
				leafcursor.moveToPosition(j);
				String addr = leafcursor.getString(leafcursor.getColumnIndexOrThrow(MyDBHelper.LEAF_ADDR));
				String leafName = leafcursor.getString(leafcursor.getColumnIndexOrThrow(MyDBHelper.LEAF_NAME));
				map.put("position", Integer.valueOf(j));
				map.put("address", addr);
				map.put("leaf_name", leafName);
				map.put("operate", "join");
				addrList.add(map);
			}
		}
		
		
		for(HashMap<String, Object> mapUnit : addrList){
			Log.d("DEBUG", "-----" + mapUnit);
		}
		
		return addrList;
	}

	
	public void autoAdjust() {
		// TODO Auto-generated method stub
		
//		listl.addLast(new SendMsgTread(this, curCtrlLeafAddr+"EF", 0, -1));
		listl.addLast(new SendMsgTread(this, curCtrlLeafAddr+"EE", 0, -1));
		startQueryLux();
		listl.removeFirst().start();
//		listl.removeFirst().start();
		
	}
	
	public void stopAutoAdjust(){
		listl.clear();
		listl.addLast(new SendMsgTread(this, curCtrlLeafAddr+"F0", 0, -1));
		listl.removeFirst().start();
	}

	
	public void startQueryLux() {
		// TODO Auto-generated method stub
		AlarmManager alarms = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		int alarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP;
		
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, 
				new Intent("com.tinyao.alarm"), 0);
		alarms.setRepeating(alarmType, 2000, 4000, pendingIntent);
		
		sp.edit().putInt("alarm_times", 30).commit();//闹钟五次
	}
	
	/**
	 * 
	 */
	private void saveLeafAfterSet() {
		// TODO Auto-generated method stub
		String leafName = leafNameEdt.getText().toString();
		String leafAddr = leafAddrSpinner.getSelectedItem().toString();
		String leafGroup = leafAddrSpinner.getSelectedItem().toString();
		helper.insert(leafName, leafAddr, leafGroup);
	}

	public void fillData2LeafList() {
		leafcursor = helper.select();
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				R.layout.leaf_list_single_item, leafcursor,
				new String[] { MyDBHelper.LEAF_NAME },
				new int[] { android.R.id.text1 });
		leafList.setAdapter(adapter);
		leafList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		
		SimpleCursorAdapter groupLeafAdapter = new SimpleCursorAdapter(this,
				R.layout.leaf_list_multiple_item, leafcursor,
				new String[] { MyDBHelper.LEAF_NAME },
				new int[] { android.R.id.text1 });
		groupLeafList.setAdapter(groupLeafAdapter);
		groupLeafList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		
		groupcursor = groupHelper.select();
		SimpleCursorAdapter existGroupAdapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_spinner_item, groupcursor,
				new String[] { GroupDBHelper.GROUP_NAME},
				new int[] { android.R.id.text1});
		existGroupAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		groupSpinner.setAdapter(existGroupAdapter);
		
		leafSelect.setText(sp.getString("cureent_leaf_name", "< 选择 >"));
		curCtrlLeafAddrNormal = sp.getString("cureent_leaf_addr", "03");
		curCtrlLeafAddr = Integer.toHexString(Integer.valueOf(curCtrlLeafAddrNormal));
		
		Log.d("DEBUG", curCtrlLeafAddrNormal + "---" + curCtrlLeafAddr);
		
		fillSceneToSpinner();
		
	}
	
	public void refreshLeafListWithGroup(String groupId){
		
		
		
	}
	
	/**
	 * 
	 * @param leafAddr
	 * @param GroupId
	 */
	protected void addLeafToGroup(String leafAddr, String GroupId) {

	}
	
	protected void dealMsgResponse(int res) {
		// TODO Auto-generated method stub
		Log.d("FLAG", "DEAL MSG flag -----" + flag);
		
		switch (flag) {
		case -1:
			break;
		case PrefConfig.QUERY_SHORT_ADDR_SET: // 设置从机短地址
			LogInfo.append("\n> " + "receive str: " + "0x" + convertInt2Hex(res));
			if (res == Integer.valueOf(responseCheck)) {
				LogInfo.append("\n> " + getResources().getString(R.string.short_address_set_ok));
				if(leafCurrent == -1) break;
				leafcursor.moveToPosition(leafCurrent);
				int id = Integer.valueOf(
						leafcursor.getString(leafcursor.getColumnIndexOrThrow(MyDBHelper.LEAF_ID)));
				String lname = leafcursor.getString(leafcursor.getColumnIndexOrThrow(MyDBHelper.LEAF_NAME));
				String group = leafcursor.getString(leafcursor.getColumnIndexOrThrow(MyDBHelper.LEAF_GROUP));
				helper.update(id, lname, responseCheck, group);
			} else {
				LogInfo.append("\n> " + getResources().getString( R.string.short_address_set_fail));
				// Toast.makeText(this,
				// getResources().getString(R.string.short_address_set_fail),
				// Toast.LENGTH_SHORT).show();
			}
			
			break;
		case PrefConfig.QUERY_LEAF_GROUP_1:
//			LogInfo.append("\n> " + "receive str: " + "0x" + convertInt2Hex(res));
			String binRes1 = Integer.toBinaryString(res);
			if(toGroupId < 8){
				char is = 0;
				if(binRes1.length() >= toGroupId)
					is = binRes1.charAt(toGroupId);
				Log.d("DEBUG", "binary is: " + is);
				if(is == '1'){
					Log.d("DEBUG", "nsuccess: add " + groupJoinLeaf + " to group " + toGroupId);
					LogInfo.append("\nsuccess: " + "add " + groupJoinLeaf + " to group " + toGroupId);
				}else{
					Log.d("DEBUG", "fail: add " + groupJoinLeaf + " to group " + toGroupId);
					LogInfo.append("\nfail: " + "add " + groupJoinLeaf + " to group " + toGroupId);
				}
			}
			break;
		case PrefConfig.QUERY_LEAF_GROUP_2:
//			LogInfo.append("\n> " + "receive str: " + "0x" + convertInt2Hex(res));
			String binRes2 = Integer.toBinaryString(res);
			if(toGroupId > 8){
				char is = binRes2.charAt(toGroupId - 7);
				if(is == 1){
					Log.d("DEBUG", "nsuccess: add " + groupJoinLeaf + " to group " + toGroupId);
					LogInfo.append("\nsuccess: " + "add " + groupJoinLeaf + " to group " + toGroupId);
				}else{
					Log.d("DEBUG", "fail: add " + groupJoinLeaf + " to group " + toGroupId);
					LogInfo.append("\nfail: " + "add " + groupJoinLeaf + " to group " + toGroupId);
				}
			}
			break;
		case PrefConfig.QUERY_LAMP_LUX_A:
			Log.d("lamp-----------", "" + res);
			//这里把光照值做处理映射到0～100
			int lux1 =(int) (res/255.0 * 100);
			
			lampA.setText("" + lux1 + "%");
			seek1.setProgress(lux1);
			break;
		case PrefConfig.QUERY_LAMP_LUX_B:
			Log.d("lamp-----------", "" + res);
			//这里把光照值做处理映射到0～100
			int lux2 =(int) (res/255.0 * 100);
			lampB.setText("" + lux2 + "%");
			seek2.setProgress(lux2);
			break;
		case PrefConfig.QUERY_LAMP_LUX_C:
			Log.d("lamp-----------", "" + res);
			//这里把光照值做处理映射到0～100
			int lux3 =(int) (res/255.0 * 100);
			lampC.setText("" + lux3 + "%");
			seek3.setProgress(lux3);
			break;
		case PrefConfig.QUERY_LAMP_LUX_D:
			Log.d("lamp-----------", "" + res);
			//这里把光照值做处理映射到0～100
			int lux4 =(int) (res/255.0 * 100);
			lampD.setText("" + lux4 + "%");
			seek4.setProgress(lux4);
			
			Message msg = panelHandler.obtainMessage();
			msg.what = 0; // pointer rotate with the corresponding angle
			panelHandler.sendMessage(msg);
			break;
		case PrefConfig.QUERY_SENSOR_LUX:
			sensorVal.setText("" + res*4);
			break;
		case PrefConfig.QUERY_STATE:
			//res
			String state = Integer.toBinaryString(res);
			while(state.length()<8){
				state = "0" + state;
			}
			Log.d("DEBUG", "state----" + state);
			setPanelState(state);
			break;
		case PrefConfig.QUERY_FADE:
			Log.d("DEBUG", "query_fade");
			if(isInSeekPanel){
				listl.clear();
				listl.addLast(new SendMsgTread(BluetoothChat.this, "a3" + "00", 0, -1));
				listl.addLast(new SendMsgTread(BluetoothChat.this, curCtrlLeafAddr + "2e", 200, PrefConfig.SET_FADE_TIME));
				listl.addLast(new SendMsgTread(BluetoothChat.this, curCtrlLeafAddr + "2e", 400, PrefConfig.SET_FADE_TIME));
				listl.addLast(new SendMsgTread(BluetoothChat.this, curCtrlLeafAddr + "2f", 600, PrefConfig.SET_FADE_RATE));
				listl.addLast(new SendMsgTread(BluetoothChat.this, curCtrlLeafAddr + "2f", 800, PrefConfig.SET_FADE_RATE));
				while(!listl.isEmpty()){
					listl.removeFirst().start();
				}
				String fadeVal = Integer.toBinaryString(res);
				Log.d("DEBUG", "query_fade" + fadeVal);
				String fadeT = "0",fadeR = "1";
				if(fadeVal.length() > 4){
					fadeT = fadeVal.substring(0, fadeVal.length()-4);
					fadeR = fadeVal.substring(fadeVal.length()-4, fadeVal.length());
				}else{
					fadeR = fadeVal.substring(0, fadeVal.length());
				}
				Log.d("DEBUG", "fadeT: " + fadeT + " fadeR: " + fadeR);
				fadeTime = convertBinaryStr2HexStr(fadeT);//记录当前fade值
				fadeRate = convertBinaryStr2HexStr(fadeR);
				Log.d("DEBUG", "fadeTime: " + fadeTime + " fadeRate: " + fadeRate);
			}
			break;
		case PrefConfig.QUERY_TARGET_LUX:
			Log.d("DEBUG", "========== query target lux ==========" + res);
			if(convertInt2Hex(res).equals(targetHexLux)){
				Toast.makeText(this, "理想照度值" + (res*4) + "设置成功！", Toast.LENGTH_SHORT).show();
			}else{
				Toast.makeText(this, "理想照度值" + (res*4) + "设置失败，请重试！", Toast.LENGTH_SHORT).show();
			}
			sensorTargetVal.setText("" + res*4);
			targetHexLux = "";
			break;
		case PrefConfig.QUERY_TARGET_LUX_INIT:
			Log.d("DEBUG", "========== query target lux init==========" + res);
			sensorTargetVal.setText("" + res*4);
			break;
		case PrefConfig.QUERY_FACTOR_A:
			Log.d("DEBUG", "========== query factor A ==========" + res);
			sp.edit().putFloat("factor_a", (float)(res*4/200.0)).commit();
			break;
		case PrefConfig.QUERY_FACTOR_B:
			Log.d("DEBUG", "========== query factor B ==========" + res);
			sp.edit().putFloat("factor_b", (float)(res*4/200.0)).commit();
			break;
		case PrefConfig.QUERY_FACTOR_C:
			Log.d("DEBUG", "========== query factor C ==========" + res);
			sp.edit().putFloat("factor_c", (float)(res*4/200.0)).commit();
			break;
		case PrefConfig.QUERY_FACTOR_D:
			Log.d("DEBUG", "========== query factor D ==========" + res);
			sp.edit().putFloat("factor_d", (float)(res*4/200.0)).commit();
			
			Toast.makeText(this, 
					"影响因子\nA灯：" + sp.getFloat("factor_a", 1) + 
					"\t\tB灯：" + sp.getFloat("factor_b", 1) +
					"\nC灯：" + sp.getFloat("factor_c", 1) +
					"\t\tD灯:" + sp.getFloat("factor_d", 1), 
					Toast.LENGTH_LONG).show();
			break;
		}

		logScroll.post(new Runnable() {
			public void run() {
				logScroll.fullScroll(ScrollView.FOCUS_DOWN);
			}
		});
		
		
	}

	private String convertInt2Hex(int num) {
		// TODO Auto-generated method stub
		String result = Integer.toHexString(num);
		if(num<16){
			result = "0" + result;
		}
		return result;
	}
	
	private String convertBinaryStr2HexStr(String binStr){
		int intVal = 0;
		char[] binC = binStr.toCharArray();
		for(int i=0; i<binC.length; i++){
			if(binC[i] == '1')
				intVal =intVal +  (int) Math.pow(2, binC.length-1-i) * 1;
		}
		return convertInt2Hex(intVal);
	}

	public class AlarmReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			
			Log.d("DEBUG", "-----query-----");
			if(mChatService.getState() == BluetoothChatService.STATE_CONNECTED){
				
				queryLux(00, true);
				
				int cur = sp.getInt("alarm_times", 0);
				Log.d("DEBUG", "--cur--" + cur);
				
				if(cur == 0){
					//BluetoothChat.this.unregisterReceiver(alarmReceiver);
					AlarmManager alarms = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
					int alarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP;
					
					PendingIntent pendingIntent = PendingIntent.getBroadcast(BluetoothChat.this, 0, 
							new Intent("com.tinyao.alarm"), 0);
					alarms.cancel(pendingIntent);
				}else{
					sp.edit().putInt("alarm_times", cur-1).commit();
				}
				
			}
			
		}

	}

	/**
	 * 查询灯照度
	 * @param runRightnow 
	 */
	public void queryLux(int initSleep, boolean runRightnow){
		
		Log.d("DEBUG", "query lux -----------");
		
		listl.clear();//清空此前的任务队列
		
		listl.addLast(new SendMsgTread(this, curCtrlLeafAddr + "FA", initSleep, PrefConfig.QUERY_LAMP_LUX_A));
		listl.addLast(new SendMsgTread(this, curCtrlLeafAddr + "FB", 10, PrefConfig.QUERY_LAMP_LUX_B));
		listl.addLast(new SendMsgTread(this, curCtrlLeafAddr + "FC", 10, PrefConfig.QUERY_LAMP_LUX_C));
		listl.addLast(new SendMsgTread(this, curCtrlLeafAddr + "FD", 10, PrefConfig.QUERY_LAMP_LUX_D));
		listl.addLast(new SendMsgTread(this, curCtrlLeafAddr + "F5", 10, PrefConfig.QUERY_SENSOR_LUX));
		listl.addLast(new SendMsgTread(this, curCtrlLeafAddr + "F4", 10, PrefConfig.QUERY_STATE));
		listl.addLast(new SendMsgTread(this, curCtrlLeafAddr + "FE", 10, PrefConfig.QUERY_TARGET_LUX_INIT));
		
		if(runRightnow)
			listl.removeFirst().start();
		
	}
	
	/**
	 * 
	 */
	protected void queryToInitPanel() {
		// TODO Auto-generated method stub
		queryLux(10, false);
	}
	
	/**
	 * 
	 */
	protected void recoverScene(){
		String adjustAddr = convertInt2Hex(Integer.valueOf(curCtrlLeafAddrNormal) - 1);
		final String luxa = adjustAddr + convertInt2Hex(Integer.valueOf(sceneLuxArray.get(0))*255/100);
		final String luxb = adjustAddr + convertInt2Hex(Integer.valueOf(sceneLuxArray.get(1))*255/100);
		final String luxc = adjustAddr + convertInt2Hex(Integer.valueOf(sceneLuxArray.get(2))*255/100);
		final String luxd = adjustAddr + convertInt2Hex(Integer.valueOf(sceneLuxArray.get(3))*255/100);
		
		Log.d("DEBUG", "recover: " + luxa + "-" + luxb + "-" + luxc + "-" + luxd);
		
		new Thread(){
			
			public void run(){
				
				try {
					listl.clear();
					sendMessage("a3" + "01");
					sleep(250);
					sendMessage(luxa);
					sleep(200);
					sendMessage("a3" + "02");
					sleep(250);
					sendMessage(luxb);
					sleep(200);
					sendMessage("a3" + "03");
					sleep(250);
					sendMessage(luxc);
					sleep(200);
					sendMessage("a3" + "04");
					sleep(250);
					sendMessage(luxd);
					sleep(300);
					queryLux(0, true);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			
		}.start();
		
	}
	
	/**
	 * 设置理想照度
	 */
	public void setTargetLux(){
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("感应器目标照度");
		LayoutInflater mInflater = getLayoutInflater();
		View tartgetView = mInflater.inflate(R.layout.target_edt, null);
		builder.setView(tartgetView);
		final EditText targetEdt = (EditText) tartgetView.findViewById(R.id.target_edit);
		builder.setPositiveButton("确定", new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				int tint = Integer.valueOf(targetEdt.getText().toString());
				if(tint<1024 && tint>=0){
					String tstr = convertInt2Hex(tint/4);
					targetHexLux = tstr;
					listl.clear();
					// 设置理想照度
					listl.addLast(new SendMsgTread(BluetoothChat.this, "c3" + tstr, 0, -1));
					listl.addLast(new SendMsgTread(BluetoothChat.this, curCtrlLeafAddr + "F2", 200, -1));
					
					// 查询理想照度 用于判断是否设置成功
					listl.addLast(new SendMsgTread(BluetoothChat.this, curCtrlLeafAddr + "FE", 400, PrefConfig.QUERY_TARGET_LUX));
					while(!listl.isEmpty()){
						listl.removeFirst().start();
					}
				}else{
					Toast.makeText(BluetoothChat.this, "输入格式错误", Toast.LENGTH_SHORT).show();
				}
			}
			
		});
		builder.setNegativeButton("取消", new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				
			}
			
		});
		builder.create().show();
		
	}
	
	/**
	 * 查询目标照度
	 */
	public void queryTargetLux(int initSleep){
		listl.addLast(new SendMsgTread(this, curCtrlLeafAddr + "FE", initSleep, PrefConfig.QUERY_TARGET_LUX_INIT));
	}
	
	public void fetchExistFactor(){
		listl.clear();
		listl.addLast(new SendMsgTread(this, curCtrlLeafAddr + "E1", 0, PrefConfig.QUERY_FACTOR_A));
		listl.addLast(new SendMsgTread(this, curCtrlLeafAddr + "E2", 0, PrefConfig.QUERY_FACTOR_B));
		listl.addLast(new SendMsgTread(this, curCtrlLeafAddr + "E3", 0, PrefConfig.QUERY_FACTOR_C));
		listl.addLast(new SendMsgTread(this, curCtrlLeafAddr + "E4", 0, PrefConfig.QUERY_FACTOR_D));
		listl.removeFirst().start();
	}
	
	
	public void addCurrentLux2Scene(String sceneName){
		String luxA = lampA.getText().toString().split("%")[0];
		String luxB = lampB.getText().toString().split("%")[0];
		String luxC = lampC.getText().toString().split("%")[0];
		String luxD = lampD.getText().toString().split("%")[0];
		sceneHelper.insert(sceneName, luxA, luxB, luxC, luxD);
		fillSceneToSpinner();
	}
	
	
	/**
	 * 重置影响因子
	 */
	public void resetFactor(){
		listl.addLast(new SendMsgTread(this, curCtrlLeafAddr + "EF", 200, PrefConfig.UPDATE_FACTOR));
		if(!listl.isEmpty())
			listl.removeFirst().start();
		Message msg = panelHandler.obtainMessage();
		msg.what = PrefConfig.UPDATE_FACTOR;
		panelHandler.sendMessage(msg);
	}

	private void setPanelState(String state){
		char[] st = state.toCharArray();
		ImageView autoImg = (ImageView) layout2.findViewById(R.id.is_auto_on);
		if(st[7] == '1'){ //auto
//			adjustBtn.setChecked(false);
			autoImg.setImageDrawable(BluetoothChat.this.getResources().getDrawable(R.drawable.factor_set));
		}else{
//			adjustBtn.setChecked(true);
			autoImg.setImageDrawable(BluetoothChat.this.getResources().getDrawable(R.drawable.factor_unset));
			
		}
		
		ImageView factorImg = (ImageView)layout2.findViewById(R.id.is_factor_set);
		if(st[5] == '1'){
			factorImg.setImageDrawable(BluetoothChat.this.getResources().getDrawable(R.drawable.factor_unset));
		}else{
			factorImg.setImageDrawable(BluetoothChat.this.getResources().getDrawable(R.drawable.factor_set));
		}
		
		ImageView personImg = (ImageView)layout2.findViewById(R.id.is_person_in);
		if(st[4] == '1'){
			personImg.setImageDrawable(BluetoothChat.this.getResources().getDrawable(R.drawable.factor_set));
		}else{
			personImg.setImageDrawable(BluetoothChat.this.getResources().getDrawable(R.drawable.factor_unset));
		}
		
	}
	
	LinkedList<SendMsgTread> listl = new LinkedList<SendMsgTread>();
	
	public class SendMsgTread extends Thread{
		
		private String msg;
		private int sleepTime;
		private int type;
		
		public SendMsgTread(Context con, String sendMsg, int sleeptime, int type){
			msg = sendMsg;
			sleepTime = sleeptime;
			this.type = type;
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				sleep(sleepTime);
				sendMessage(msg);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			super.run();
		}

		@Override
		public synchronized void start() {
			// TODO Auto-generated method stub
			flag = type;
			super.start();
		}
		
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if(keyCode == event.KEYCODE_BACK){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("提示");
			builder.setMessage("您确定要退出？");
			builder.setPositiveButton("确定", new DialogInterface.OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					finish();
				}
				
			});
			builder.setNegativeButton("取消", new DialogInterface.OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					
				}
				
			});
			builder.create().show();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	
	
	
}


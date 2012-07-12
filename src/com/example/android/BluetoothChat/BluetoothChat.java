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
import java.util.List;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.util.Log;
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

	// Layout panel
	private Bitmap bmp;
	private Matrix matrix = new Matrix();
	private Bitmap resizedBitmap;

	private SeekBar seek1, seek2, seek3, seek4; // the lamp seekbar
	private View pointer; // dashboard poioter
	private Animation am; // pointer rotate animation
	private TextView per1, per2, per3; // dashboard power percent
	private View panel_1, panel_2, panel_3;
	private ToggleButton tab_1;

	// Layout panel 3
	private Spinner operateSpinner;
	private View addrPanel;
	private View fadePanel;

	private ListView leafList, groupLeafList;
	private Spinner leafAddrSpinner, groupSpinner;
	private EditText leafNameEdt, groupNameEdt;
	private Button leafNameBtn, leafAddrSet, groupJoinSet;

	private Spinner fadeTimeSpinner, fadeRateSpinner;
	private Button fadeTimeBtn, fadeRateBtn;

	private TextView LogInfo;
	private Button clearLogBtn;
	private ScrollView logScroll;
	private Cursor leafcursor;

	private int flag = -1; // wait for the respond
	private String responseCheck;

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
	
	private int leafCurrent = -1;

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
		ImageView refresh = (ImageView) layout2.findViewById(R.id.refresh);
		pointer = (View) layout2.findViewById(R.id.pointer_view);
		seek1 = (SeekBar) layout2.findViewById(R.id.seekBar1);
		seek2 = (SeekBar) layout2.findViewById(R.id.seekBar2);
		seek3 = (SeekBar) layout2.findViewById(R.id.seekBar3);
		seek4 = (SeekBar) layout2.findViewById(R.id.seekBar4);

		per1 = (TextView) layout2.findViewById(R.id.percent_1);
		per2 = (TextView) layout2.findViewById(R.id.percent_2);
		per3 = (TextView) layout2.findViewById(R.id.percent_3);

		seek1.setOnSeekBarChangeListener(listener);
		seek2.setOnSeekBarChangeListener(listener);
		seek3.setOnSeekBarChangeListener(listener);
		seek4.setOnSeekBarChangeListener(listener);

		tab_1 = (ToggleButton) layout2.findViewById(R.id.ctrl_tab_1);

		panel_1 = (View) layout2.findViewById(R.id.panel_1);
		panel_2 = (View) layout2.findViewById(R.id.panel_2);
		panel_3 = (View) layout2.findViewById(R.id.panel_3);

		tab_1.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				if (!tab_1.isChecked()) {
					panel_1.setVisibility(View.VISIBLE);
					panel_2.setVisibility(View.GONE);
				} else {
					panel_1.setVisibility(View.GONE);
					panel_2.setVisibility(View.VISIBLE);
				}
			}

		});

		// layout3
		operateSpinner = (Spinner) layout3.findViewById(R.id.operatons_spi);
		addrPanel = (View) layout3.findViewById(R.id.short_and_group_panel);
		fadePanel = (View) layout3.findViewById(R.id.fade_ctrl_panel);

		leafList = (ListView) layout3.findViewById(R.id.leaf_list);
		leafAddrSpinner = (Spinner) layout3
				.findViewById(R.id.leaf_addr_spinner);
		leafNameEdt = (EditText) layout3.findViewById(R.id.leaf_name_edt);
		leafNameBtn = (Button) layout3.findViewById(R.id.leaf_name_ok_btn);
		leafAddrSet = (Button) layout3.findViewById(R.id.leaf_set_btn);

		groupLeafList = (ListView) layout3.findViewById(R.id.group_leaf_list);
		groupSpinner = (Spinner) layout3.findViewById(R.id.group_spinner);
		groupNameEdt = (EditText) layout3.findViewById(R.id.leaf_name_edt);
		groupJoinSet = (Button) layout3.findViewById(R.id.group_join_btn);

		fadeTimeSpinner = (Spinner) layout3.findViewById(R.id.fade_time_spinner);
		fadeRateSpinner = (Spinner) layout3.findViewById(R.id.fade_rate_spinner);
		fadeTimeBtn = (Button) layout3.findViewById(R.id.fade_time_btn);
		fadeRateBtn = (Button) layout3.findViewById(R.id.fade_rate_btn);

		LogInfo = (TextView) layout3.findViewById(R.id.loginfo);
		clearLogBtn = (Button) layout3.findViewById(R.id.clear_log);
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
					fadePanel.setVisibility(View.GONE);
					break;
				case 1:
					addrPanel.setVisibility(View.GONE);
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
				int spos = Integer.valueOf(
						leafcursor.getString(leafcursor.getColumnIndex(MyDBHelper.LEAF_ADDR)));
				leafAddrSpinner.setSelection((spos - 1)/2);
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
		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			// TODO Auto-generated method stub
			Message msg = panelHandler.obtainMessage();
			msg.what = 0; // pointer rotate with the corresponding angle
			msg.arg1 = progress;
			panelHandler.sendMessage(msg);
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

				Log.d("DEBUG", "----------");

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
				sendMessage(message);
			}
		});

		// Initialize the BluetoothChatService to perform bluetooth connections
		mChatService = new BluetoothChatService(this, mHandler);

		// Initialize the buffer for outgoing messages
		mOutStringBuffer = new StringBuffer("");

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
			Log.d("DEBUG", "" + send);

			// Reset out string buffer to zero and clear the edit text field
			mOutStringBuffer.setLength(0);
			mOutEditText.setText(mOutStringBuffer);
		}
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
				mConversationArrayAdapter.add(mConnectedDeviceName + ":  "
						+ readMessage);
				dealMsgResponse(readMessage);
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
			case R.id.leaf_name_ok_btn:	// 更新从机名称
				int updateId = Integer.
					valueOf(leafcursor.getString(leafcursor.getColumnIndexOrThrow(MyDBHelper.LEAF_ID)));
				String addr = leafcursor.getString(leafcursor.getColumnIndexOrThrow(MyDBHelper.LEAF_ADDR));
				helper.update(updateId, 
						leafNameEdt.getText().toString(), addr);
				fillData2LeafList();
				break;
			case R.id.leaf_set_btn:		// 设置从机地址
				String address = leafAddrSpinner.getSelectedItem().toString();// 获取地址str
				// 设置shortAddress，成功后将载数据库中更新从机（insert/update）
				setShortAddress(address);
				break;
			case R.id.group_join_btn:	//设置组成员 
				
				break;
			case R.id.clear_log:
				LogInfo.setText("");
				break;
			}
		}

	};

	/* layout3 Spinner Selected Listener */
	public AdapterView.OnItemSelectedListener spinnerSelectedListener = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> arg0, View v, int arg2,
				long arg3) {
			// TODO Auto-generated method stub
			switch (v.getId()) {
			case R.id.leaf_addr_spinner:
				
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
		if(sp.getBoolean(PrefConfig.FIRST_RUN, true)){
			for(int i=0; i<16; i++){ //初始化16个从机，地址未设置
				helper.insert("Leaf" + i, "03");
			}
			sp.edit().putBoolean(PrefConfig.FIRST_RUN, false).commit();
		}
		fillData2LeafList();
	}
	
	/*
	 * ==========================================================================
	 * =
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
	protected void setShortAddress(final String address) {
		// TODO Auto-generated method stub
		new Thread() {
			public void run() {
				try {
					sendMessage("A3" + address);
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
					flag = PrefConfig.QUERY_SHORT_ADDR_SET; // 等待响应
					responseCheck = address;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();
	}

	/**
	 * 
	 */
	private void saveLeafAfterSet() {
		// TODO Auto-generated method stub
		String leafName = leafNameEdt.getText().toString();
		String leafAddr = leafAddrSpinner.getSelectedItem().toString();

		helper.insert(leafName, leafAddr);
	}

	public void fillData2LeafList() {
		leafcursor = helper.select();
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_list_item_1, leafcursor,
				new String[] { MyDBHelper.LEAF_NAME, MyDBHelper.LEAF_ADDR},
				new int[] { android.R.id.text1, android.R.id.text2 });
		leafList.setAdapter(adapter);
	}
	
	public void fillData2LeafSpinner(){
		
	}

	/**
	 * 
	 * @param leafAddr
	 * @param GroupId
	 */
	protected void addLeafToGroup(String leafAddr, String GroupId) {

	}
	
	protected void dealMsgResponse(String readMessage) {
		// TODO Auto-generated method stub
		LogInfo.append("\n> " + "receive str: " + readMessage);
		switch (flag) {
		case -1:
			break;
		case PrefConfig.QUERY_SHORT_ADDR_SET: // 设置从机短地址
			if (readMessage.contains(responseCheck)) {
				LogInfo.append("\n> " + getResources().getString(R.string.short_address_set_ok));
				// 将新设置的从机添加到sharedpref中

			} else {
				LogInfo.append("\n> " + getResources().getString( R.string.short_address_set_fail));
				// Toast.makeText(this,
				// getResources().getString(R.string.short_address_set_fail),
				// Toast.LENGTH_SHORT).show();
			}
			saveLeafAfterSet();
			break;
		}

		logScroll.post(new Runnable() {
			public void run() {
				logScroll.fullScroll(ScrollView.FOCUS_DOWN);
			}
		});
	}

}
package com.senssun.bluetooth.tools.babyscale;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckedTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.senssun.bluetooth.tools.R;
import com.senssun.bluetooth.tools.relative.Information;

import cn.senssun.ble.sdk.BleDevice;
import cn.senssun.ble.sdk.BleScan;
import cn.senssun.ble.sdk.entity.GrowMeasure;
import cn.senssun.ble.sdk.grow.BleGrowSDK;
import cn.senssun.ble.sdk.grow.GrowOnActionMethod.OnConnectState;
import cn.senssun.ble.sdk.grow.GrowOnActionMethod.OnDisplayDATA;
import cn.senssun.ble.sdk.grow.GrowOnActionMethod.OnInitService;
import cn.senssun.ble.sdk.grow.GrowOnActionMethod.OnUserInfoStatus;

import static com.senssun.bluetooth.tools.R.id.weight;

public class DeviceControlGrowthActivity extends Activity implements OnClickListener {
	BleDevice mDevice;
	boolean mConnected;
	TextView WeightNum,IfStable,unitType;
	private CheckedTextView mCheck_value;
	TextView his;
	BluetoothAdapter mAdapter;
	private String mAdress;
	private TextView mConnectionState,device_address;
	private SharedPreferences mySharedPreferences;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_baby_scale_control);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		mySharedPreferences = getSharedPreferences("sp", Activity.MODE_PRIVATE);

		 Intent intent = getIntent();
		 mAdress = intent.getStringExtra(BleScan.EXTRAS_DEVICE);


		WeightNum=(TextView)findViewById(R.id.WeightNum);
		mCheck_value= (CheckedTextView) findViewById(R.id.check_value);
		mConnectionState = (TextView) findViewById(R.id.connection_state);
		device_address = (TextView) findViewById(R.id.device_address);
		device_address.setText(mAdress);

		unitType=(TextView)findViewById(R.id.unitType);
		his=(TextView)findViewById(R.id.his);
		
		BleGrowSDK.getInstance().InitSDK(this);//初始化服务

		BleGrowSDK.getInstance().setOnInitService(new OnInitService() {
			
			@Override
			public void OnInit() {
				// TODO Auto-generated method stub
				BleGrowSDK.getInstance().Connect(mAdress);//选择你需要的设备连接
			}
		});

		BleGrowSDK.getInstance().setOnConnectState(new OnConnectState() {
			
			@Override
			public void OnState(boolean State) {
				// TODO Auto-generated method stub
				if(State){
					mConnected=true;
					mConnectionState.setText("连接成功");
					Toast.makeText(DeviceControlGrowthActivity.this, "连接", Toast.LENGTH_SHORT).show();
				}else{
					mConnected=false;
					mConnectionState.setText("连接失败");
					Toast.makeText(DeviceControlGrowthActivity.this, "断开", Toast.LENGTH_SHORT).show();
					BleGrowSDK.getInstance().Connect(mAdress);//选择你需要的设备连接
					if (mySharedPreferences.getBoolean(Information.DB.AutoCheck, false)) {
						if (weight > 0) {
							Handler mhadler = new Handler();
							mhadler.postDelayed(new Runnable() {
								@Override
								public void run() {
									Intent intent = new Intent();
									Bundle bundle = new Bundle();
									bundle.putString("CocktailSuc", mAdress);
									intent.putExtras(bundle);
									setResult(RESULT_OK, intent);
									finish();
								}
							}, 3000);
						}
					}
				}
				invalidateOptionsMenu();
			}
		});
		
		BleGrowSDK.getInstance().setOnUserInfoStatus(new OnUserInfoStatus() {
			
			@Override
			public void OnListener(int isStatus) {
				// TODO Auto-generated method stub
				switch (isStatus){
                case 0:
                	Toast.makeText(DeviceControlGrowthActivity.this, "新建用户成功", Toast.LENGTH_SHORT).show();
                	break;
                case 1:
                	Toast.makeText(DeviceControlGrowthActivity.this, "删除用户成功", Toast.LENGTH_SHORT).show();
                	break;
                case 2:
                	Toast.makeText(DeviceControlGrowthActivity.this, "选择用户", Toast.LENGTH_SHORT).show();
                	break;
                case 3:
                	Toast.makeText(DeviceControlGrowthActivity.this, "发送深同步历史成功", Toast.LENGTH_SHORT).show();
                	break;
                case 4:
                	Toast.makeText(DeviceControlGrowthActivity.this, "发送浅同步历史成功", Toast.LENGTH_SHORT).show();
                	break;
				}
				
			}
		});
		BleGrowSDK.getInstance().setOnDisplayDATA(new OnDisplayDATA() {
			
			@Override
			public void OnDATA(GrowMeasure growMeasure) {
				// TODO Auto-generated method stub
				if(growMeasure.isIfStable()){//判断数据是否稳定
					mCheck_value.setChecked(true);
				}else{
					mCheck_value.setChecked(false);
				}
				switch (growMeasure.getDataType().getValue()) {//判断秤体发送数据类型 0：一般测量数据  1：提取历史数据
				case 0:
					if(growMeasure.isSymbol()){//判断当前秤体是否为负数
						switch (growMeasure.getUnitType().getValue()) { //判断当前秤体选择的单位
						case 0:
							WeightNum.setText(String.valueOf(growMeasure.getWeightKg()));
							unitType.setText("kg");
							break;
						case 1:
							WeightNum.setText(String.valueOf(growMeasure.getWeightLb()));
							unitType.setText("lb");
							break;
						}
					}else{
						switch (growMeasure.getUnitType().getValue()) {
						case 0:
							WeightNum.setText("-"+growMeasure.getWeightKg());
							unitType.setText("kg");
							break;
						case 1:
							WeightNum.setText("-"+growMeasure.getWeightLb());
							unitType.setText("lb");
							break;
						}
					}
					break;
				case 1:
					his.setText(his.getText().toString()+" 用户序号："+growMeasure.getHistoryUserSerimal()+"\n"+
							"记录序号："+growMeasure.getHistoryDataSerimal()+ "记录总数："+growMeasure.getHistoryDataAmount()+" 日期："+growMeasure.getHistoryDate()+"\n"+
							"重量KG："+growMeasure.getHistoryWeightKg()+"\n"+
							"身高CM："+growMeasure.getHistoryHeightCm()+"\n");
					break;
				}

				((TextView)findViewById(R.id.HeightNum)).setText(growMeasure.getHeightCm()+"cm");
			}
		});
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.gatt_services, menu);
		if (mConnected) {
			menu.findItem(R.id.menu_connect).setVisible(false);
			menu.findItem(R.id.menu_disconnect).setVisible(true);
		} else {
			menu.findItem(R.id.menu_connect).setVisible(true);
			menu.findItem(R.id.menu_disconnect).setVisible(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.menu_connect:
			BleGrowSDK.getInstance().Connect(mAdress);//选择你需要的设备连接
			return true;
		case R.id.menu_disconnect:
			BleGrowSDK.getInstance().Disconnect();
			return true;
		case android.R.id.home:
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) { //serialNum:1~8
		switch (v.getId()) {
		case R.id.addUser:
			BleGrowSDK.getInstance().SendAddUser(1);
			break;
		case R.id.deleUser:
			BleGrowSDK.getInstance().SendDeleUser(1);
			break;
		case R.id.seleUser:
			BleGrowSDK.getInstance().SendSeleUser(1);
			break;
		case R.id.deepSys:
			BleGrowSDK.getInstance().SendDeepSys(String.valueOf(1));
			break;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		BleGrowSDK.getInstance().stopSDK(this);
	}
}

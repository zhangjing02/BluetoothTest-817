package com.senssun.bluetooth.tools.relative;


public interface Information {
	public interface DB{
		/*SharedPreferences
		 * */
		public static final String LimitRssi="LimitRssi";//限制RSSI
		public static final String AutoCheck="AutoCheck";//自动连接
		public static final String AutoCheck02="AutoCheck02";//自动连接

		public static final String AliCheck="AliCheck";//阿里自动测试

		public static final String MaxWeight = "maxWeight";//最大称量值
		public static final String MinWeight = "minWeight";//最少起秤量
		public static final String CloseTime = "closeTime";//关机时间
		public static final String Temperature = "temperature";//补偿温度
		public static final String UserSize = "UserSize";//用户数量

		public static final String Unit = "unit";//单位选择
		public static final String WeightMode = "WeightMode";//秤体基本功能
		public static final String PowerMode = "PowerMode";//电池电压检测方式
		public static final String StartUnit = "StartUnit";//开机单位

		public static final String RING_NUM = "ringNum";//铃声编号

		public static final String USER_NUM = "userNum";
		public static final String USER_SEX = "userSex";
		public static final String USER_HEIGHT = "userHeight";
		public static final String USER_AGE = "userAge";

		public static final String TEST_NUM = "testNum";
	}

}

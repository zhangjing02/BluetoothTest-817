package com.senssun.bluetooth.tools.relative;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import widget.TosGallery;
import widget.Utils;
import widget.WheelView;

public class WheelViewSelect03 {
	private static Activity activity;
	private static String[] mData;
	private static NumberAdapter numberAdapter;

	private static WheelView mWheel1 = null;


	private static	boolean mStart = false;


	static	final Html.ImageGetter imageGetter = new Html.ImageGetter() { 

		@Override
		public Drawable getDrawable(String source) { 
			Drawable drawable=null; 
			int rId=Integer.parseInt(source); 
			drawable=activity.getResources().getDrawable(rId); 
			drawable.setBounds(0, 0, 120, 60); 
			return drawable;
		};
	};

	public static  void viewProjectNum(WheelView wheel,Activity activitys) {
		activity=activitys;
		mData=new String[51];
		for(int i=0;i<51;i++ ){
			mData[i]=String.valueOf((i/10f));
		}
		mWheel1=wheel;
		mWheel1.setScrollCycle(true);
		numberAdapter=new NumberAdapter();
		mWheel1.setAdapter(numberAdapter);
	}

	private static void startScrolling() {
		mWheel1.post(new ScrollRunnable(mWheel1));
	}

	private static class ScrollRunnable implements Runnable {
		WheelView mWheelView;

		public ScrollRunnable(WheelView wheelView) {
			mWheelView = wheelView;
		}

		@Override
		public void run() {
			int position = mWheelView.getSelectedItemPosition();
			int count = mWheelView.getCount();
			position = (position + 1) % count;

			mWheelView.setSelection(position);

			if (mStart) {
				mWheelView.postDelayed(this, 0);
			}
		}
	}

	private static class NumberAdapter extends BaseAdapter {
		int mHeight = 30;
		int currPosition=0;

		public NumberAdapter() {
			mHeight = (int) Utils.pixelToDp(activity, mHeight);
		}

		@Override
		public int getCount() {
			return (null != mData) ? mData.length : 0;
		}

		@Override
		public Object getItem(int arg0) {
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			return 0;
		}
		public void change(int Position){
			currPosition=Position;
			notifyDataSetChanged();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView txtView = null;

			if (null == convertView) {
				convertView = new TextView(activity);
				convertView.setLayoutParams(new TosGallery.LayoutParams(-1, mHeight));

				txtView = (TextView) convertView;
				txtView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
				txtView.setTextColor(Color.BLACK);
				txtView.setGravity(Gravity.CENTER);
			}

			String text = String.valueOf(mData[position]);
			if (null == txtView) {
				txtView = (TextView) convertView;
			}
			//			if(currPosition<=mData.length-2){
			//				if(position==currPosition+mData.length-2){//改变上值
			//					txtView.setTextColor(activity.getResources().getColor(R.color.sele_col3));
			//				}
			//
			//				if(position==currPosition+mData.length-1){
			//					txtView.setTextColor(activity.getResources().getColor(R.color.sele_col2));
			//				}
			//			}
			//			if(currPosition>=mData.length-2){ 
			//				if(position==Math.abs(currPosition-mData.length+1)){//改变下值
			//					txtView.setTextColor(activity.getResources().getColor(R.color.sele_col2));
			//				}
			//				if(position==currPosition-mData.length+2){
			//					txtView.setTextColor(activity.getResources().getColor(R.color.sele_col3));
			//				}
			//			}
			//
			//			if(position==currPosition+2){//上两位
			//				txtView.setTextColor(activity.getResources().getColor(R.color.sele_col3));
			//			}
			//			if(position==currPosition+1){//上一位
			//				txtView.setTextColor(activity.getResources().getColor(R.color.sele_col2));
			//			}
			//			if(position==currPosition){//当前位
			//				txtView.setTextColor(activity.getResources().getColor(R.color.sele_col1));
			//			}
			//			if(position==currPosition-1){//下一位
			//				txtView.setTextColor(activity.getResources().getColor(R.color.sele_col2));
			//			}
			//			if(position==currPosition-2){//下两位
			//				txtView.setTextColor(activity.getResources().getColor(R.color.sele_col3));
			//			}

			txtView.setText(Html.fromHtml(text, imageGetter, null)); 
			//			txtView.setText(String.valueOf(position));

			return convertView;
		}
	}

    public interface OnSelectListening{
        void OnDrawListening();
    }

    OnSelectListening mOnSelectListening=null;
    public void setOnDrawListening(OnSelectListening e){
        mOnSelectListening=e;
    }
}

package com.example.mysensorlistener;

import java.util.LinkedList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MySensorListenerTest extends Activity {
	MySensorListener _listener = new MySensorListener();
	SensorManager _sm;

	private Button getBufFrontBtn;
	private Button getBufRearBtn;
	private Button getBufArrayAndClear;
	private TextView textViewOutput;;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_my_sensor_listener);

		getBufFrontBtn = (Button) findViewById(R.id.getBufFrontBtn);
		getBufRearBtn = (Button) findViewById(R.id.getBufRearBtn);
		getBufArrayAndClear = (Button) findViewById(R.id.getBufArrayAndClear);
		textViewOutput = (TextView) findViewById(R.id.textViewOutput);
		// textViewOutput.setMovementMethod(ScrollingMovementMethod.getInstance());

		getBufFrontBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				System.out.println("getBufFrontBtn.onClick");
				LinkedList<float[]> accBuf = _listener.getAccDataBuf();
				System.out.println("accBuf.size():= " + accBuf.size());

				String res = "Buf Front	float[3]:=\n";
				for (float value : accBuf.poll()) {
					System.out.println(value);
					res += value + "\n";
				}
				textViewOutput.setText(res);

			}
		});

		getBufRearBtn.setOnClickListener(new OnClickListener() {
			@TargetApi(9)
			@Override
			public void onClick(View v) {
				LinkedList<float[]> accBuf = _listener.getAccDataBuf();

				String res = "Buf End	Float[3]:=\n";
				for (float value : accBuf.pollLast()) {
					System.out.println(value);
					res += value + "\n";
				}
				textViewOutput.setText(res);

			}
		});
		getBufArrayAndClear.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				LinkedList<float[]> aBuf = _listener.getAccDataBuf();
				LinkedList<float[]> gBuf=_listener.getGravityDataBuf();
				LinkedList<float[]> mBuf=_listener.getMegDataBuf();

				System.out.println("aBuf.size():= "+aBuf.size());
				System.out.println("gBuf.size():= "+gBuf.size());
				System.out.println("mBuf.size():= "+mBuf.size());

				Object[] arr = aBuf.toArray();

//				String res = "arr.length:= " + arr.length + "\n";
//				for (Object obj : arr) {
//					res += ((float[]) obj)[0] + " | " + ((float[]) obj)[1]
//							+ " | " + ((float[]) obj)[2] + " | \n";
//				}

				StringBuilder res = new StringBuilder();
				res.append("arr.length:= " + arr.length + "\n");
				for(Object obj:arr){
//					res.append
					res.append(((float[]) obj)[0] + " | " + ((float[]) obj)[1]
					+ " | " + ((float[]) obj)[2] + " | \n");

				}

				textViewOutput.setText(res);
				aBuf.clear();
				gBuf.clear();
				mBuf.clear();

			}
		});

		_sm = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_my_sensor_listener, menu);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		System.out.println("onResume");
		_listener.registerWithSensorManager(_sm, SensorManager.SENSOR_DELAY_FASTEST);
	}

	@Override
	protected void onPause() {
		super.onPause();

		_listener.unregisterWithSensorManager(_sm);
	}

}


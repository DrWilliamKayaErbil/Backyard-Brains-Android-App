package com.backyardbrains.drawing;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.backyardbrains.audio.TriggerAverager.TriggerHandler;

public class TriggerViewThread extends OscilloscopeGLThread {

	private static final String TAG = TriggerViewThread.class.getCanonicalName();
	private float thresholdPixelHeight;

	TriggerViewThread(OscilloscopeGLSurfaceView view) {
		super(view);
		Log.d(TAG, "Creating TriggerViewThread");
	}

	public boolean isDrawThresholdLine() {
		return true;
	}

	/**
	 * Initialize GL bits, set up the GL area so that we're lookin at it
	 * properly, create a new {@link BybGLDrawable}, then commence drawing on it
	 * like the dickens.
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		
		setupSurfaceAndDrawable();
		mAudioService = null;
		bindAudioService(true);
		registerThresholdChangeReceiver(true);
		broadcastToggleTrigger();
		setDefaultThresholdValue();
		while (!mDone) {
			// grab current audio from audioservice
			if (!isServiceReady()) continue;
		
			// Reset our Audio buffer
			mBufferToDraws = null;
			// Read new mic data
			synchronized (mAudioService) {
				mBufferToDraws = mAudioService.getTriggerBuffer();
			}
			
			if (mBufferToDraws == null || mBufferToDraws.length <= 0) {
				glman.glClear();
				setGlWindow(glWindowHorizontalSize, glWindowHorizontalSize);
				if (isDrawThresholdLine()) {
					drawThresholdLine();
				}
				glman.swapBuffers();
				continue;
			}
			
			if (mBufferToDraws.length < glWindowHorizontalSize)
				glWindowHorizontalSize = mBufferToDraws.length;
			
			synchronized (parent) {
				setLabels(glWindowHorizontalSize);
			}

			glman.glClear();
			waveformShape.setBufferToDraw(mBufferToDraws);
			setGlWindow(glWindowHorizontalSize, mBufferToDraws.length);
			waveformShape.draw(glman.getmGL());
			if (isDrawThresholdLine()) {
				drawThresholdLine();
			}
			glman.swapBuffers();
			try {
				sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		broadcastToggleTrigger();
		bindAudioService(false);
		registerThresholdChangeReceiver(false);
		mConnection = null;
	}

	private void broadcastToggleTrigger() {
		Intent i = new Intent("BYBToggleTrigger").putExtra("triggerMode", true);
		parent.getContext().sendBroadcast(i);
	}

	protected void setLabels(final int samplesToShow) {
		setmVText();
		super.setLabels(samplesToShow);
	}

	private void setDefaultThresholdValue() {
		thresholdPixelHeight = glHeightToPixelHeight(getGlWindowVerticalSize()/4);
		setmVText();
	}

	protected void setGlWindow(final int samplesToShow, final int lengthOfSampleSet) {
		final int size = getGlWindowVerticalSize();
		glman.initGL(lengthOfSampleSet/2 - samplesToShow/2, lengthOfSampleSet/2 + samplesToShow/2, -size/2, size/2);
	}

	private void setmVText() {
		final float glHeight = pixelHeightToGlHeight(thresholdPixelHeight);
		final float yPerDiv = glHeight / 4 / 24.5f / 1000;
		if (mAudioService != null && mAudioServiceIsBound) {
			mAudioService.getTriggerHandler().post(new Runnable() {
				@Override public void run() {
					((TriggerHandler)mAudioService.getTriggerHandler()).setThreshold(glHeight*5/2);
				}
			});
		}
		parent.setmVText(yPerDiv);
	}

	private float glHeightToPixelHeight(float glHeight) {
		return (glHeight / getGlWindowVerticalSize()) * parent.getHeight();
	}

	private float pixelHeightToGlHeight(float pxHeight) {
		return pxHeight / parent.getHeight() * getGlWindowVerticalSize();
	}

	protected void drawThresholdLine() {
		final float thresholdLineLength = (mBufferToDraws == null) ? glWindowHorizontalSize : mBufferToDraws.length;
		float[] thresholdLine = new float[] { 0, getThresholdValue(),
				thresholdLineLength, getThresholdValue() };
		FloatBuffer thl = getFloatBufferFromFloatArray(thresholdLine);
		glman.getmGL().glEnableClientState(GL10.GL_VERTEX_ARRAY);
		glman.getmGL().glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
		glman.getmGL().glLineWidth(1.0f);
		glman.getmGL().glVertexPointer(2, GL10.GL_FLOAT, 0, thl);
		glman.getmGL().glDrawArrays(GL10.GL_LINES, 0, 4);
		glman.getmGL().glDisableClientState(GL10.GL_VERTEX_ARRAY);
	}// ((?!GC_)).*

	public float getThresholdValue() {
		return pixelHeightToGlHeight(thresholdPixelHeight);
	}

	public float getThresholdYValue() {
		return parent.getHeight() / 2 - thresholdPixelHeight;
	}

	public void adjustThresholdValue(float dy) {
		//if (dy < parent.getHeight() / 2)
			thresholdPixelHeight = parent.getHeight() / 2 - dy;
	}

	protected void registerThresholdChangeReceiver(boolean on) {
		if (on) {
			thChangeReceiver = new ThresholdChangeReceiver();
			parent.getContext().registerReceiver(thChangeReceiver,
					new IntentFilter("BYBThresholdChange"));
		} else {
			parent.getContext().unregisterReceiver(thChangeReceiver);
		}

	}

	private ThresholdChangeReceiver thChangeReceiver;

	private class ThresholdChangeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context,
				android.content.Intent intent) {
			adjustThresholdValue(intent.getFloatExtra("deltathreshold", 1));
		};
	}

}

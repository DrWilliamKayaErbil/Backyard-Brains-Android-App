package com.backyardbrains.drawing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.util.Log;

/**
 * An object capable of drawing itself to a provided GL10 object
 * 
 * @author Nathan Dotz <nate@backyardbrains.com>
 * @version 1
 * 
 */
class BybGLDrawable {
	/**
	 * Tag for use in LogCat
	 */
	private static final String TAG = "BYBGLShape";

	/**
	 * Scaling in Y domain to be used when drawing on a GL surface
	 */
	private final float Y_SCALING = .001f;

	/**
	 * Reference to the parent thread responsible for maintaining this object.
	 */
	private final OscilliscopeGLThread parent;

	/**
	 * Construct a drawable object, storing a reference to the parent thread.
	 * 
	 * @param oscilliscopeGLThread
	 *            the thread responsible for this object.
	 */
	BybGLDrawable(OscilliscopeGLThread oscilliscopeGLThread) {
		parent = oscilliscopeGLThread;
	}

	/**
	 * The array of 16-bit numbers that represents the data to be drawn
	 */
	private short[] mBufferToDraw;

	/**
	 * Takes an array of floats and returns a buffer representing the same
	 * floats
	 * 
	 * @param array
	 *            to be converted
	 * @return converted array as FloatBuffer
	 */
	FloatBuffer getFloatBufferFromFloatArray(float array[]) {
		ByteBuffer temp = ByteBuffer.allocateDirect(array.length * 4);
		temp.order(ByteOrder.nativeOrder());
		FloatBuffer buf = temp.asFloatBuffer();
		buf.put(array);
		buf.position(0);
		return buf;
	}

	/**
	 * Draw this object on the provided {@link GL10} object.
	 * 
	 * @param gl_obj
	 */
	public void draw(GL10 gl_obj) {
		// vertices = transform(vertices);
		// FloatBuffer mVertexBuffer = getFloatBufferFromFloatArray(vertices);
		FloatBuffer mVertexBuffer = getWaveformBuffer(mBufferToDraw);
		gl_obj.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl_obj.glVertexPointer(2, GL10.GL_FLOAT, 0, mVertexBuffer);
		gl_obj.glDrawArrays(GL10.GL_LINE_STRIP, 0, mVertexBuffer.limit() / 2);
	}

	/**
	 * Convert the local buffer-to-draw data over to a {@link FloatBuffer}
	 * structure suitable for feeding to
	 * {@link GL10#glDrawArrays(int, int, int)}
	 * 
	 * @param shortArrayToDraw
	 *            containing raw audio data
	 * @return {@link FloatBuffer} ready to be fed to
	 *         {@link GL10#glDrawArrays(int, int, int)}
	 */
	private FloatBuffer getWaveformBuffer(short[] shortArrayToDraw) {
		if (shortArrayToDraw == null) {
			Log.w(TAG, "Drawing fake line with null data");
			float[] array = { 0.0f, 0.0f, 1.0f, 0.0f, 2.0f, 0.0f};
			return getFloatBufferFromFloatArray(array);
		}
		// Log.d(TAG, "Received buffer to draw");

		float[] arr = new float[shortArrayToDraw.length * 2]; // array to fill
		int j = 0; // index of arr
		float interval = parent.x_width / shortArrayToDraw.length;
		for (int i = 0; i < shortArrayToDraw.length; i++) {
			arr[j++] = i * interval;
			arr[j++] = shortArrayToDraw[i] * Y_SCALING;
		}
		return getFloatBufferFromFloatArray(arr);
	}

	/**
	 * Called from the parent thread, this takes a {@link ByteBuffer} of audio
	 * data from the recording device and converts it into an array of 16-bit
	 * shorts, to later be processed by the drawing functions.
	 * 
	 * @param audioBuffer
	 *            {@link ByteBuffer} to be drawn
	 */
	public void setBufferToDraw(ByteBuffer audioBuffer) {
		if (audioBuffer != null) {
			audioBuffer.clear();
			mBufferToDraw = new short[audioBuffer.asShortBuffer().capacity()];
			audioBuffer.asShortBuffer().get(mBufferToDraw, 0,
					mBufferToDraw.length);
<<<<<<< HEAD
			Log.i(TAG, "Got audio data. Buffer length: " + audioBuffer.capacity());
=======
			Log.i(TAG, "Got audio data: " + audioBuffer.asShortBuffer().capacity());
>>>>>>> 0250b9b629e5469017363d6d32cfae4bf6d53a9e
		} else {
			Log.w(TAG, "Received null audioBuffer");
		}
	}
}
/*
 * Copyright (c) 2016. William Edward Woody
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>
 *
 */

package com.chaosinmotion.securechat.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * A class which tracks images in compressed (JPEG) format.
 * Created by woody on 4/28/16.
 */
public class JPEGImage
{
	private int imageWidth;
	private int imageHeight;
	private byte[] imageData;

	public JPEGImage(byte[] data, int start, int length)
	{
		imageData = new byte[length];
		System.arraycopy(data,start,imageData,0,length);

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeByteArray(imageData,0,length,options);

		imageWidth = options.outWidth;
		imageHeight = options.outHeight;

	}

	public JPEGImage(Bitmap bmap)
	{
		imageWidth = bmap.getWidth();
		imageHeight = bmap.getHeight();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bmap.compress(Bitmap.CompressFormat.JPEG,75,baos);
		try {
			baos.close();
		}
		catch (IOException e) {
			// Should never happen
		}

		imageData = baos.toByteArray();
	}

	public JPEGImage(Bitmap bmap, int width, int height)
	{
		int sizeWidth = bmap.getWidth();
		int sizeHeight = bmap.getHeight();

		if (sizeWidth > width) {
			sizeHeight = (sizeHeight * width)/sizeWidth;
			sizeWidth = width;
		}
		if (sizeHeight > height) {
			sizeWidth = (sizeWidth * height)/sizeHeight;
			sizeHeight = height;
		}

		Bitmap scale = Bitmap.createScaledBitmap(bmap,sizeWidth,sizeHeight,true);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		scale.compress(Bitmap.CompressFormat.JPEG,75,baos);
		try {
			baos.close();
		}
		catch (IOException e) {
			// Should never happen
		}
		imageData = baos.toByteArray();
		imageWidth = sizeWidth;
		imageHeight = sizeHeight;
	}

	private JPEGImage(int width, int height, byte[] data)
	{
		imageWidth = width;
		imageHeight = height;
		imageData = data;
	}

	/**
	 * Return image data
	 * @return
	 */
	public byte[] getBytes()
	{
		return imageData;
	}

	public int getWidth()
	{
		return imageWidth;
	}

	public int getHeight()
	{
		return imageHeight;
	}

	public JPEGImage resizeToFit(int maxDimension)
	{
		return resizeToFit(maxDimension,maxDimension);
	}

	public JPEGImage resizeToFit(int maxWidth, int maxHeight)
	{
		Size s = sizeInSize(maxWidth,maxHeight);
		if ((s.getWidth() == imageWidth) && (s.getHeight() == imageHeight)) return this;

		/*
		 *  We're going to have to rescale. Because we store a jpeg
		 *  internally we're going to have to decode into a bitmap,
		 *  rescale the bitmap, and re-encode.
		 */

		Bitmap orig = BitmapFactory.decodeByteArray(imageData,0,imageData.length);
		Bitmap scale = Bitmap.createScaledBitmap(orig,s.getWidth(),s.getHeight(),true);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		scale.compress(Bitmap.CompressFormat.JPEG,75,baos);
		try {
			baos.close();
		}
		catch (IOException e) {
			// Should never happen
		}

		JPEGImage ret = new JPEGImage(s.getWidth(),s.getHeight(),baos.toByteArray());

		orig.recycle();
		scale.recycle();

		return ret;
	}

	public Size sizeInSize(int width, int height)
	{
		int sizeWidth = imageWidth;
		int sizeHeight = imageHeight;

		if (sizeWidth > width) {
			sizeHeight = (sizeHeight * width)/sizeWidth;
			sizeWidth = width;
		}
		if (sizeHeight > height) {
			sizeWidth = (sizeWidth * height)/sizeHeight;
			sizeHeight = height;
		}
		return new Size(sizeWidth,sizeHeight);
	}

	public Size sizeForWidth(int width)
	{
		int sizeWidth = imageWidth;
		int sizeHeight = imageHeight;

		if (sizeWidth > width) {
			sizeHeight = (sizeHeight * width)/sizeWidth;
			sizeWidth = width;
		}
		if (sizeWidth < 1) {
			sizeWidth = 1;
			sizeHeight = 1;
		}
		return new Size(sizeWidth,sizeHeight);
	}

	public void drawInRect(Canvas canvas, Paint paint, int left, int top, int right, int bottom)
	{
		int drawWidth = imageWidth;
		int drawHeight = imageHeight;
		int width = right - left;
		int height = bottom - top;
		if ((width <= 0) || (height <= 0)) return;

		if (drawWidth > width) {
			drawHeight = (drawHeight * width)/drawWidth;
			drawWidth = width;
		}
		if (drawHeight > height) {
			drawWidth = (drawWidth * height)/drawHeight;
			drawHeight = height;
		}

		left += (left + right - drawWidth)/2;
		top += (top + bottom - drawHeight)/2;

		Bitmap orig = BitmapFactory.decodeByteArray(imageData,0,imageData.length);
		Rect src = new Rect(0,0,imageWidth,imageHeight);
		Rect dst = new Rect(left,top,left+drawWidth,top+drawHeight);
		canvas.drawBitmap(orig,src,dst,paint);
	}
}

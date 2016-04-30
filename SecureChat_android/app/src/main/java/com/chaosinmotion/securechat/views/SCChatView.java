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

package com.chaosinmotion.securechat.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.chaosinmotion.securechat.encapsulation.SCMessageObject;
import com.chaosinmotion.securechat.utils.Size;

/**
 * Created by woody on 4/23/16.
 */
public class SCChatView extends View
{
	private float density;
	private TextPaint messagePaint;
	private Paint bubblePaint;
	private TextPaint datePaint;
	private SCMessageObject message;
	private String date;
	private boolean receiveFlag;

	// TEST
	private int testDrawWidth;

	public SCChatView(Context context)
	{
		super(context);
		internalInit();
	}

	public SCChatView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		internalInit();
	}

	public SCChatView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		internalInit();
	}

	private void internalInit()
	{
		density = getResources().getDisplayMetrics().density;
		messagePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
		messagePaint.setColor(Color.BLACK);
		messagePaint.setTextSize((int)(density * 17));

		bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		bubblePaint.setColor(Color.LTGRAY);

		datePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
		datePaint.setColor(Color.LTGRAY);
		datePaint.setTextSize((int)(density * 13));

		message = new SCMessageObject("Hi.");
		date = "11/12/16/ 11:45 PMjy";
		receiveFlag = false;
		messagePaint.setColor(Color.WHITE);
		bubblePaint.setColor(0xFF406080);
	}

	public void setMessage(boolean rflag, SCMessageObject msg, String dt)
	{
		receiveFlag = !rflag;
		message = msg;
		date = dt;

		if (receiveFlag) {
			messagePaint.setColor(Color.BLACK);
			bubblePaint.setColor(Color.LTGRAY);
		} else {
			messagePaint.setColor(Color.WHITE);
			bubblePaint.setColor(0xFF406080);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		int width, height;
		int padding;
		int drawWidth;

		/*
		 *  Determine fitting width. We measure the maximum (unwrapped)
		 *  size of the text, then limit by the width boundaries.
		 */

		Paint.FontMetrics fm = datePaint.getFontMetrics();

		padding = (int)(density * 140);     // padding
		width = (int)(density * 200);
		int w = message.maximumWidth(messagePaint) + padding;
		if (width < w) width = w;

		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		if (widthMode == MeasureSpec.AT_MOST) {
			if (width > widthSize) width = widthSize;
		} else if (widthMode == MeasureSpec.EXACTLY) {
			width = widthSize;
		}

		/*
		 *  Given the width, determine the height.
		 */

		int minDrawWidth = (int)(density * 100);
		drawWidth = width - padding;
		if (drawWidth < minDrawWidth) drawWidth = minDrawWidth;

		height = message.sizeForWidth(messagePaint,drawWidth).getHeight();
		height += 30 * density;
		height += fm.descent - fm.ascent;

		testDrawWidth = (int)fm.descent;

		/*
		 *  Bound the height
		 */
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		if (heightMode == MeasureSpec.AT_MOST) {
			if (height > heightSize) height = heightSize;
		} else if (heightMode == MeasureSpec.EXACTLY) {
			height = heightSize;
		}

		setMeasuredDimension(width,height);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
		super.onLayout(changed, left, top, right, bottom);
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		/*
		 *  Determine actual width of the text to render.
		 */

		int padding = (int)(density * 140);
		int width = getWidth() - padding;

		Size size = message.sizeForWidth(messagePaint,width);

		/*
		 *  Calculate rectangle placement of the oval for chatting.
		 */

		int l,r,t,b;
		t = (int)(density * 10);            // 10 pixels off top
		if (receiveFlag) {
			l = (int)(density * 10);        // 10 off the left for received
		} else {
			l = (int)(getWidth() - size.getWidth() - 30 * density);
		}
		r = (int)(l + size.getWidth() + 20 * density);
		b = (int)(t + size.getHeight() + 10 * density);
		RectF rr = new RectF(l,t,r,b);
		float radius = 12 * density;

		canvas.drawRoundRect(rr,radius,radius,bubblePaint);

		/*
		 *  Draw the quote triangle (TODO)
		 */

		Path path = new Path();
		if (receiveFlag) {
			path.moveTo(l-(5 * density),b);
			path.lineTo(l+radius,b-radius);
			path.lineTo(l+radius,b);
			path.lineTo(l,b);
		} else {
			path.moveTo(r+(5 * density),b);
			path.lineTo(r-radius,b-radius);
			path.lineTo(r-radius,b);
			path.lineTo(r,b);
		}
		canvas.drawPath(path,bubblePaint);

		/*
		 *  Draw the embedded text
		 */

		int xpos = (int)(l + 10*density);
		int ypos = (int)(t + 5*density);
		message.drawWithRect(canvas,messagePaint,xpos, ypos,xpos + size.getWidth(), ypos + size.getHeight());

		/*
		 *  Draw the date
		 */

		int lpos;
		Paint.FontMetrics fm = datePaint.getFontMetrics();

		if (receiveFlag) {
			lpos = l;
		} else {
			lpos = (int)(getWidth() - 10 * density - datePaint.measureText(date));
		}
		int tpos = (int)(getHeight() - fm.descent) - 2;
		canvas.drawText(date,lpos,tpos,datePaint);
	}
}



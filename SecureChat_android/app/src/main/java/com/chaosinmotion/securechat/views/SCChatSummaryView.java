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
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.chaosinmotion.securechat.R;

/**
 * Displays the number of views within this view.
 * Created by woody on 4/20/16.
 */
public class SCChatSummaryView extends View
{
	private TextPaint paint;
	private boolean isSelf;
	private int deviceCount;

	public SCChatSummaryView(Context context)
	{
		super(context);
		internalInit();
	}

	public SCChatSummaryView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		internalInit();
	}

	public SCChatSummaryView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		internalInit();
	}

	public void setSelf(boolean f)
	{
		isSelf = f;
	}

	public void setDeviceCount(int dc)
	{
		deviceCount = dc;
	}

	private void internalInit()
	{
		float density = getResources().getDisplayMetrics().density;
		paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
		paint.setTextSize((int)(density * 13));
		paint.setColor(Color.GRAY);
		paint.setTextAlign(Paint.Align.CENTER);
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		String format;
		String str;
		if (isSelf) {
			if (deviceCount == 1) {
				format = getResources().getString(R.string.you_have_singular);
			} else {
				format = getResources().getString(R.string.you_have_plural);
			}
		} else {
			if (deviceCount == 1) {
				format = getResources().getString(R.string.user_has_singular);
			} else {
				format = getResources().getString(R.string.user_has_plural);
			}
		}
		str = String.format(format,deviceCount);

		Paint.FontMetrics fm = paint.getFontMetrics();
		int ypos = (int)(getHeight() - fm.ascent - fm.descent)/2;
		int xpos = (int)(getWidth()/2);
		canvas.drawText(str,xpos,ypos,paint);

		canvas.drawRect(0,getHeight()-1,getWidth(),getHeight(),paint);
	}
}

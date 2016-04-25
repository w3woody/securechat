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

package com.chaosinmotion.securechat.chatusers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.chaosinmotion.securechat.R;
import com.chaosinmotion.securechat.messages.SCDecryptCache;
import com.chaosinmotion.securechat.messages.SCMessageDatabase;

/**
 * Created by woody on 4/21/16.
 */
public class ChatUsersView extends View
{
	private float density;
	private TextPaint senderPaint;
	private TextPaint messagePaint;

	private SCMessageDatabase.Sender sender;
	private String senderName;
	private String senderMessage;

	public ChatUsersView(Context context)
	{
		super(context);
		internalInit();
	}

	public ChatUsersView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		internalInit();
	}

	public ChatUsersView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		internalInit();
	}

	private void internalInit()
	{
		density = getResources().getDisplayMetrics().density;

		senderPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
		senderPaint.setTextSize(density * 17);
		senderPaint.setColor(Color.BLACK);

		messagePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
		messagePaint.setTextSize(density * 15);
		messagePaint.setColor(Color.DKGRAY);
	}

	/**
	 * Set the sender data for this. This view handles decryption.
	 * @param s
	 */
	public void setSender(SCMessageDatabase.Sender s)
	{
		sender = s;
		senderName = s.getSenderName();
		senderMessage = SCDecryptCache.get().decrypt(s.getLastMessage(), s.getMessageID(), new SCDecryptCache.DecryptCallback()
		{
			@Override
			public void decryptedMessage(int messageID, String msg)
			{
				if (messageID == sender.getMessageID()) {
					senderMessage = msg;
					invalidate();
				}
			}
		});

		if (senderMessage == null) {
			senderMessage = getResources().getString(R.string.decrypt_label);
		}
		invalidate();
	}

	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		int width, height;

		/*
		 *  Determine fitting width, height
		 */

		width = 200;    // We don't care the width because we clip.

		Paint.FontMetrics fm = senderPaint.getFontMetrics();
		height = (int)(30 * density);
		height += fm.descent + fm.leading - fm.ascent;

		fm = messagePaint.getFontMetrics();
		height += 2 * (fm.descent + fm.leading - fm.ascent);

		/*
		 *  Now filter through our measure spec
		 */

		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		if (widthMode == MeasureSpec.AT_MOST) {
			if (width > widthSize) width = widthSize;
		} else if (widthMode == MeasureSpec.EXACTLY) {
			width = widthSize;
		}

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
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		/*
		 *  Draw the user name
		 */

		Paint.FontMetrics fm = senderPaint.getFontMetrics();

		int x = (int)(15 * density);
		int y = (int)(10 * density - fm.ascent);

		canvas.drawText(senderName,x,y,senderPaint);

		/*
		 *  Draw the message. This is wrapped as two lines of text.
		 */

		y += (int)(10 * density + fm.descent + fm.leading);
		fm = messagePaint.getFontMetrics();
		y -= fm.ascent;

		StaticLayout layout = new StaticLayout(senderMessage,
				messagePaint,getWidth() - x * 3, Layout.Alignment.ALIGN_NORMAL,1,0,true);
		x *= 2;
		int i,len = layout.getLineCount();
		if (len > 2) len = 2;
		for (i = 0; i < len; ++i) {
			int start = layout.getLineStart(i);
			int end = layout.getLineEnd(i);

			canvas.drawText(senderMessage,start,end,x,y,messagePaint);
			y += fm.descent + fm.leading - fm.ascent;
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
		super.onLayout(changed, left, top, right, bottom);
		invalidate();
	}
}

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

package com.chaosinmotion.securechat.encapsulation;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.chaosinmotion.securechat.utils.JPEGImage;
import com.chaosinmotion.securechat.utils.Size;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Represents a message in our system. Allows us to encapsulate different types.
 * Created by woody on 4/26/16.
 */
public class SCMessageObject
{
    /*
     *  TODO: Different types
     */
    private String message;
	private JPEGImage image;

    /**
     * Construct using raw serialized data
     * @param data
     */
    public SCMessageObject(byte[] data)
    {
        try {
	        if ((data.length > 2) && (data[0] == 0x01)) {
		        if (data[1] == 0x00) {
			        image = new JPEGImage(data,2,data.length-2);
		        } else if (data[1] == 0x01) {
			        message = new String(data,2,data.length-2,"UTF-8");
		        }
	        } else {
		        message = new String(data, "UTF-8");
	        }
        }
        catch (UnsupportedEncodingException e) {
            // Should never happen
        }
    }

    /**
     * Construct a new message object
     * @param msg
     */
    public SCMessageObject(String msg)
    {
        message = msg;
    }

	/**
	 * Construct new message object from bitmap
	 * @param bmap
	 */
    public SCMessageObject(Bitmap bmap)
    {
	    image = new JPEGImage(bmap);
    }

    /**
     * Serialize the message for transmitting
     * @return
     */
    public byte[] dataFromMessage()
    {
        try {
	        if (message != null) {
		        if ((message.length() >= 1) && (message.charAt(0) == 0x01)) {
			        // Must encapsulate. Normally should not happen
			        ByteArrayOutputStream baos = new ByteArrayOutputStream();
			        baos.write(0x01);
			        baos.write(0x01);
			        baos.write(message.getBytes("UTF-8"));
			        return baos.toByteArray();
		        } else {
			        return message.getBytes("UTF-8");
		        }
	        } else if (image != null) {
		        ByteArrayOutputStream baos = new ByteArrayOutputStream();
		        baos.write(0x01);
		        baos.write(0x00);
		        baos.write(image.getBytes());
		        return baos.toByteArray();
	        } else {
		        return null;
	        }
        }
        catch (IOException e) {
            return new byte[0];
        }
    }

    /**
     * Return a summary of the message suitable for displaying on the home page
     * @return
     */
    public String getSummaryMessageText()
    {
	    if (image != null) {
		    return "(photo)";   // TODO: what should this return?
	    } else if (message != null) {
		    return message;
	    } else {
		    return null;
	    }
    }

	/**
	 * Calculate the maximum width of this object.
	 * @return
	 */
	public int maximumWidth(Paint paint)
	{
		if (image != null) {
			return image.getWidth();
		} else if (message != null) {
			return (int)paint.measureText(message);
		} else {
			return 40;      // some random value.
		}
	}

	/**
	 * Calculate the size given the width
	 * @param width
	 * @return
	 */
	public Size sizeForWidth(TextPaint paint, int width)
	{
		if (image != null) {
			return image.sizeForWidth(width);
		} else if (message != null) {
			StaticLayout layout = new StaticLayout(message,paint,width, Layout.Alignment.ALIGN_NORMAL,1,0,true);
			int height = layout.getHeight();

			int maxWidth = 0;
			int i,len = layout.getLineCount();
			for (i = 0; i < len; ++i) {
				int w = (int)Math.ceil(layout.getLineWidth(i));
				if (w > maxWidth) {
					maxWidth = w;
				}
			}
			return new Size(maxWidth,height);

		} else {
			return new Size(width,width);
		}
	}

	/**
	 * Draw within the rectangle provided.
	 * @param canvas
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 * @param color
	 */
	public void drawWithRect(Canvas canvas, TextPaint paint, int left, int top, int right, int bottom)
	{
		if (image != null) {
			image.drawInRect(canvas,paint,left,top,right,bottom);
		} else if (message != null) {
			StaticLayout layout = new StaticLayout(message,paint,right-left, Layout.Alignment.ALIGN_NORMAL,1,0,true);
			canvas.save();
			canvas.translate(left,top);
			layout.draw(canvas);
			canvas.restore();
		}
	}
}

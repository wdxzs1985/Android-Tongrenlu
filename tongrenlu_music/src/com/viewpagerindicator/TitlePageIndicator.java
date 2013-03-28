/*
 * Copyright (C) 2011 Jake Wharton
 * Copyright (C) 2011 Patrik Akerfeldt
 * Copyright (C) 2011 Francisco Figueiredo Jr.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viewpagerindicator;

import info.tongrenlu.android.music.R;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * A TitlePageIndicator is a PageIndicator which displays the title of left view
 * (if exist), the title of the current select view (centered) and the title of
 * the right view (if exist). When the user scrolls the ViewPager then titles
 * are also scrolled.
 */
public class TitlePageIndicator extends View implements PageIndicator {
    /**
     * Percentage indicating what percentage of the screen width away from
     * center should the underline be fully faded. A value of 0.25 means that
     * halfway between the center of the screen and an edge.
     */
    private static final float SELECTION_FADE_PERCENTAGE = 0.25f;

    /**
     * Percentage indicating what percentage of the screen width away from
     * center should the selected text bold turn off. A value of 0.05 means that
     * 10% between the center and an edge.
     */
    private static final float BOLD_FADE_PERCENTAGE = 0.05f;

    /**
     * Title text used when no title is provided by the adapter.
     */
    private static final String EMPTY_TITLE = "";

    /**
     * Interface for a callback when the center item has been clicked.
     */
    public interface OnCenterItemClickListener {
        /**
         * Callback when the center item has been clicked.
         * 
         * @param position
         *            Position of the current center item.
         */
        void onCenterItemClick(int position);
    }

    public enum IndicatorStyle {
        None(0), Triangle(1), Underline(2);

        public final int value;

        private IndicatorStyle(final int value) {
            this.value = value;
        }

        public static IndicatorStyle fromValue(final int value) {
            for (final IndicatorStyle style : IndicatorStyle.values()) {
                if (style.value == value) {
                    return style;
                }
            }
            return null;
        }
    }

    public enum LinePosition {
        Bottom(0), Top(1);

        public final int value;

        private LinePosition(final int value) {
            this.value = value;
        }

        public static LinePosition fromValue(final int value) {
            for (final LinePosition position : LinePosition.values()) {
                if (position.value == value) {
                    return position;
                }
            }
            return null;
        }
    }

    private ViewPager mViewPager;
    private ViewPager.OnPageChangeListener mListener;
    private int mCurrentPage = -1;
    private float mPageOffset;
    private int mScrollState;
    private final Paint mPaintText = new Paint();
    private boolean mBoldText;
    private int mColorText;
    private int mColorSelected;
    private Path mPath = new Path();
    private final Rect mBounds = new Rect();
    private final Paint mPaintFooterLine = new Paint();
    private IndicatorStyle mFooterIndicatorStyle;
    private LinePosition mLinePosition;
    private final Paint mPaintFooterIndicator = new Paint();
    private float mFooterIndicatorHeight;
    private float mFooterIndicatorUnderlinePadding;
    private float mFooterPadding;
    private float mTitlePadding;
    private float mTopPadding;
    /** Left and right side padding for not active view titles. */
    private float mClipPadding;
    private float mFooterLineHeight;

    private static final int INVALID_POINTER = -1;

    private int mTouchSlop;
    private float mLastMotionX = -1;
    private int mActivePointerId = TitlePageIndicator.INVALID_POINTER;
    private boolean mIsDragging;

    private OnCenterItemClickListener mCenterItemClickListener;

    public TitlePageIndicator(final Context context) {
        this(context, null);
    }

    public TitlePageIndicator(final Context context, final AttributeSet attrs) {
        this(context, attrs, R.attr.vpiTitlePageIndicatorStyle);
    }

    public TitlePageIndicator(final Context context, final AttributeSet attrs,
            final int defStyle) {
        super(context, attrs, defStyle);
        if (this.isInEditMode()) {
            return;
        }

        // Load defaults from resources
        final Resources res = this.getResources();
        final int defaultFooterColor = res.getColor(R.color.default_title_indicator_footer_color);
        final float defaultFooterLineHeight = res.getDimension(R.dimen.default_title_indicator_footer_line_height);
        final int defaultFooterIndicatorStyle = res.getInteger(R.integer.default_title_indicator_footer_indicator_style);
        final float defaultFooterIndicatorHeight = res.getDimension(R.dimen.default_title_indicator_footer_indicator_height);
        final float defaultFooterIndicatorUnderlinePadding = res.getDimension(R.dimen.default_title_indicator_footer_indicator_underline_padding);
        final float defaultFooterPadding = res.getDimension(R.dimen.default_title_indicator_footer_padding);
        final int defaultLinePosition = res.getInteger(R.integer.default_title_indicator_line_position);
        final int defaultSelectedColor = res.getColor(R.color.default_title_indicator_selected_color);
        final boolean defaultSelectedBold = res.getBoolean(R.bool.default_title_indicator_selected_bold);
        final int defaultTextColor = res.getColor(R.color.default_title_indicator_text_color);
        final float defaultTextSize = res.getDimension(R.dimen.default_title_indicator_text_size);
        final float defaultTitlePadding = res.getDimension(R.dimen.default_title_indicator_title_padding);
        final float defaultClipPadding = res.getDimension(R.dimen.default_title_indicator_clip_padding);
        final float defaultTopPadding = res.getDimension(R.dimen.default_title_indicator_top_padding);

        // Retrieve styles attributes
        final TypedArray a = context.obtainStyledAttributes(attrs,
                                                            R.styleable.TitlePageIndicator,
                                                            defStyle,
                                                            0);

        // Retrieve the colors to be used for this view and apply them.
        this.mFooterLineHeight = a.getDimension(R.styleable.TitlePageIndicator_footerLineHeight,
                                                defaultFooterLineHeight);
        this.mFooterIndicatorStyle = IndicatorStyle.fromValue(a.getInteger(R.styleable.TitlePageIndicator_footerIndicatorStyle,
                                                                           defaultFooterIndicatorStyle));
        this.mFooterIndicatorHeight = a.getDimension(R.styleable.TitlePageIndicator_footerIndicatorHeight,
                                                     defaultFooterIndicatorHeight);
        this.mFooterIndicatorUnderlinePadding = a.getDimension(R.styleable.TitlePageIndicator_footerIndicatorUnderlinePadding,
                                                               defaultFooterIndicatorUnderlinePadding);
        this.mFooterPadding = a.getDimension(R.styleable.TitlePageIndicator_footerPadding,
                                             defaultFooterPadding);
        this.mLinePosition = LinePosition.fromValue(a.getInteger(R.styleable.TitlePageIndicator_linePosition,
                                                                 defaultLinePosition));
        this.mTopPadding = a.getDimension(R.styleable.TitlePageIndicator_topPadding,
                                          defaultTopPadding);
        this.mTitlePadding = a.getDimension(R.styleable.TitlePageIndicator_titlePadding,
                                            defaultTitlePadding);
        this.mClipPadding = a.getDimension(R.styleable.TitlePageIndicator_clipPadding,
                                           defaultClipPadding);
        this.mColorSelected = a.getColor(R.styleable.TitlePageIndicator_selectedColor,
                                         defaultSelectedColor);
        this.mColorText = a.getColor(R.styleable.TitlePageIndicator_android_textColor,
                                     defaultTextColor);
        this.mBoldText = a.getBoolean(R.styleable.TitlePageIndicator_selectedBold,
                                      defaultSelectedBold);

        final float textSize = a.getDimension(R.styleable.TitlePageIndicator_android_textSize,
                                              defaultTextSize);
        final int footerColor = a.getColor(R.styleable.TitlePageIndicator_footerColor,
                                           defaultFooterColor);
        this.mPaintText.setTextSize(textSize);
        this.mPaintText.setAntiAlias(true);
        this.mPaintFooterLine.setStyle(Paint.Style.FILL_AND_STROKE);
        this.mPaintFooterLine.setStrokeWidth(this.mFooterLineHeight);
        this.mPaintFooterLine.setColor(footerColor);
        this.mPaintFooterIndicator.setStyle(Paint.Style.FILL_AND_STROKE);
        this.mPaintFooterIndicator.setColor(footerColor);

        // final Drawable background =
        // a.getDrawable(R.styleable.TitlePageIndicator_android_background);
        // if (background != null) {
        // this.setBackgroundDrawable(background);
        // }

        a.recycle();

        final ViewConfiguration configuration = ViewConfiguration.get(context);
        this.mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
    }

    public int getFooterColor() {
        return this.mPaintFooterLine.getColor();
    }

    public void setFooterColor(final int footerColor) {
        this.mPaintFooterLine.setColor(footerColor);
        this.mPaintFooterIndicator.setColor(footerColor);
        this.invalidate();
    }

    public float getFooterLineHeight() {
        return this.mFooterLineHeight;
    }

    public void setFooterLineHeight(final float footerLineHeight) {
        this.mFooterLineHeight = footerLineHeight;
        this.mPaintFooterLine.setStrokeWidth(this.mFooterLineHeight);
        this.invalidate();
    }

    public float getFooterIndicatorHeight() {
        return this.mFooterIndicatorHeight;
    }

    public void setFooterIndicatorHeight(final float footerTriangleHeight) {
        this.mFooterIndicatorHeight = footerTriangleHeight;
        this.invalidate();
    }

    public float getFooterIndicatorPadding() {
        return this.mFooterPadding;
    }

    public void setFooterIndicatorPadding(final float footerIndicatorPadding) {
        this.mFooterPadding = footerIndicatorPadding;
        this.invalidate();
    }

    public IndicatorStyle getFooterIndicatorStyle() {
        return this.mFooterIndicatorStyle;
    }

    public void setFooterIndicatorStyle(final IndicatorStyle indicatorStyle) {
        this.mFooterIndicatorStyle = indicatorStyle;
        this.invalidate();
    }

    public LinePosition getLinePosition() {
        return this.mLinePosition;
    }

    public void setLinePosition(final LinePosition linePosition) {
        this.mLinePosition = linePosition;
        this.invalidate();
    }

    public int getSelectedColor() {
        return this.mColorSelected;
    }

    public void setSelectedColor(final int selectedColor) {
        this.mColorSelected = selectedColor;
        this.invalidate();
    }

    public boolean isSelectedBold() {
        return this.mBoldText;
    }

    public void setSelectedBold(final boolean selectedBold) {
        this.mBoldText = selectedBold;
        this.invalidate();
    }

    public int getTextColor() {
        return this.mColorText;
    }

    public void setTextColor(final int textColor) {
        this.mPaintText.setColor(textColor);
        this.mColorText = textColor;
        this.invalidate();
    }

    public float getTextSize() {
        return this.mPaintText.getTextSize();
    }

    public void setTextSize(final float textSize) {
        this.mPaintText.setTextSize(textSize);
        this.invalidate();
    }

    public float getTitlePadding() {
        return this.mTitlePadding;
    }

    public void setTitlePadding(final float titlePadding) {
        this.mTitlePadding = titlePadding;
        this.invalidate();
    }

    public float getTopPadding() {
        return this.mTopPadding;
    }

    public void setTopPadding(final float topPadding) {
        this.mTopPadding = topPadding;
        this.invalidate();
    }

    public float getClipPadding() {
        return this.mClipPadding;
    }

    public void setClipPadding(final float clipPadding) {
        this.mClipPadding = clipPadding;
        this.invalidate();
    }

    public void setTypeface(final Typeface typeface) {
        this.mPaintText.setTypeface(typeface);
        this.invalidate();
    }

    public Typeface getTypeface() {
        return this.mPaintText.getTypeface();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        if (this.mViewPager == null) {
            return;
        }
        final int count = this.mViewPager.getAdapter().getCount();
        if (count == 0) {
            return;
        }

        // mCurrentPage is -1 on first start and after orientation changed. If
        // so, retrieve the correct index from viewpager.
        if (this.mCurrentPage == -1 && this.mViewPager != null) {
            this.mCurrentPage = this.mViewPager.getCurrentItem();
        }

        // Calculate views bounds
        final ArrayList<Rect> bounds = this.calculateAllBounds(this.mPaintText);
        final int boundsSize = bounds.size();

        // Make sure we're on a page that still exists
        if (this.mCurrentPage >= boundsSize) {
            this.setCurrentItem(boundsSize - 1);
            return;
        }

        final int countMinusOne = count - 1;
        final float halfWidth = this.getWidth() / 2f;
        final int left = this.getLeft();
        final float leftClip = left + this.mClipPadding;
        final int width = this.getWidth();
        int height = this.getHeight();
        final int right = left + width;
        final float rightClip = right - this.mClipPadding;

        int page = this.mCurrentPage;
        float offsetPercent;
        if (this.mPageOffset <= 0.5) {
            offsetPercent = this.mPageOffset;
        } else {
            page += 1;
            offsetPercent = 1 - this.mPageOffset;
        }
        final boolean currentSelected = offsetPercent <= TitlePageIndicator.SELECTION_FADE_PERCENTAGE;
        final boolean currentBold = offsetPercent <= TitlePageIndicator.BOLD_FADE_PERCENTAGE;
        final float selectedPercent = (TitlePageIndicator.SELECTION_FADE_PERCENTAGE - offsetPercent)
                                      / TitlePageIndicator.SELECTION_FADE_PERCENTAGE;

        // Verify if the current view must be clipped to the screen
        final Rect curPageBound = bounds.get(this.mCurrentPage);
        final float curPageWidth = curPageBound.right - curPageBound.left;
        if (curPageBound.left < leftClip) {
            // Try to clip to the screen (left side)
            this.clipViewOnTheLeft(curPageBound, curPageWidth, left);
        }
        if (curPageBound.right > rightClip) {
            // Try to clip to the screen (right side)
            this.clipViewOnTheRight(curPageBound, curPageWidth, right);
        }

        // Left views starting from the current position
        if (this.mCurrentPage > 0) {
            for (int i = this.mCurrentPage - 1; i >= 0; i--) {
                final Rect bound = bounds.get(i);
                // Is left side is outside the screen
                if (bound.left < leftClip) {
                    final int w = bound.right - bound.left;
                    // Try to clip to the screen (left side)
                    this.clipViewOnTheLeft(bound, w, left);
                    // Except if there's an intersection with the right view
                    final Rect rightBound = bounds.get(i + 1);
                    // Intersection
                    if (bound.right + this.mTitlePadding > rightBound.left) {
                        bound.left = (int) (rightBound.left - w - this.mTitlePadding);
                        bound.right = bound.left + w;
                    }
                }
            }
        }
        // Right views starting from the current position
        if (this.mCurrentPage < countMinusOne) {
            for (int i = this.mCurrentPage + 1; i < count; i++) {
                final Rect bound = bounds.get(i);
                // If right side is outside the screen
                if (bound.right > rightClip) {
                    final int w = bound.right - bound.left;
                    // Try to clip to the screen (right side)
                    this.clipViewOnTheRight(bound, w, right);
                    // Except if there's an intersection with the left view
                    final Rect leftBound = bounds.get(i - 1);
                    // Intersection
                    if (bound.left - this.mTitlePadding < leftBound.right) {
                        bound.left = (int) (leftBound.right + this.mTitlePadding);
                        bound.right = bound.left + w;
                    }
                }
            }
        }

        // Now draw views
        final int colorTextAlpha = this.mColorText >>> 24;
        for (int i = 0; i < count; i++) {
            // Get the title
            final Rect bound = bounds.get(i);
            // Only if one side is visible
            if (bound.left > left
                && bound.left < right
                || bound.right > left
                && bound.right < right) {
                final boolean currentPage = i == page;
                final CharSequence pageTitle = this.getTitle(i);

                // Only set bold if we are within bounds
                this.mPaintText.setFakeBoldText(currentPage
                                                && currentBold
                                                && this.mBoldText);

                // Draw text as unselected
                this.mPaintText.setColor(this.mColorText);
                if (currentPage && currentSelected) {
                    // Fade out/in unselected text as the selected text fades
                    // in/out
                    this.mPaintText.setAlpha(colorTextAlpha
                                             - (int) (colorTextAlpha * selectedPercent));
                }

                // Except if there's an intersection with the right view
                if (i < boundsSize - 1) {
                    final Rect rightBound = bounds.get(i + 1);
                    // Intersection
                    if (bound.right + this.mTitlePadding > rightBound.left) {
                        final int w = bound.right - bound.left;
                        bound.left = (int) (rightBound.left - w - this.mTitlePadding);
                        bound.right = bound.left + w;
                    }
                }
                canvas.drawText(pageTitle,
                                0,
                                pageTitle.length(),
                                bound.left,
                                bound.bottom + this.mTopPadding,
                                this.mPaintText);

                // If we are within the selected bounds draw the selected text
                if (currentPage && currentSelected) {
                    this.mPaintText.setColor(this.mColorSelected);
                    this.mPaintText.setAlpha((int) ((this.mColorSelected >>> 24) * selectedPercent));
                    canvas.drawText(pageTitle,
                                    0,
                                    pageTitle.length(),
                                    bound.left,
                                    bound.bottom + this.mTopPadding,
                                    this.mPaintText);
                }
            }
        }

        // If we want the line on the top change height to zero and invert the
        // line height to trick the drawing code
        float footerLineHeight = this.mFooterLineHeight;
        float footerIndicatorLineHeight = this.mFooterIndicatorHeight;
        if (this.mLinePosition == LinePosition.Top) {
            height = 0;
            footerLineHeight = -footerLineHeight;
            footerIndicatorLineHeight = -footerIndicatorLineHeight;
        }

        // Draw the footer line
        this.mPath.reset();
        this.mPath.moveTo(0, height - footerLineHeight / 2f);
        this.mPath.lineTo(width, height - footerLineHeight / 2f);
        this.mPath.close();
        canvas.drawPath(this.mPath, this.mPaintFooterLine);

        final float heightMinusLine = height - footerLineHeight;
        switch (this.mFooterIndicatorStyle) {
        case Triangle:
            this.mPath.reset();
            this.mPath.moveTo(halfWidth, heightMinusLine
                                         - footerIndicatorLineHeight);
            this.mPath.lineTo(halfWidth + footerIndicatorLineHeight,
                              heightMinusLine);
            this.mPath.lineTo(halfWidth - footerIndicatorLineHeight,
                              heightMinusLine);
            this.mPath.close();
            canvas.drawPath(this.mPath, this.mPaintFooterIndicator);
            break;

        case Underline:
            if (!currentSelected || page >= boundsSize) {
                break;
            }

            final Rect underlineBounds = bounds.get(page);
            final float rightPlusPadding = underlineBounds.right
                                           + this.mFooterIndicatorUnderlinePadding;
            final float leftMinusPadding = underlineBounds.left
                                           - this.mFooterIndicatorUnderlinePadding;
            final float heightMinusLineMinusIndicator = heightMinusLine
                                                        - footerIndicatorLineHeight;

            this.mPath.reset();
            this.mPath.moveTo(leftMinusPadding, heightMinusLine);
            this.mPath.lineTo(rightPlusPadding, heightMinusLine);
            this.mPath.lineTo(rightPlusPadding, heightMinusLineMinusIndicator);
            this.mPath.lineTo(leftMinusPadding, heightMinusLineMinusIndicator);
            this.mPath.close();

            this.mPaintFooterIndicator.setAlpha((int) (0xFF * selectedPercent));
            canvas.drawPath(this.mPath, this.mPaintFooterIndicator);
            this.mPaintFooterIndicator.setAlpha(0xFF);
            break;
        }
    }

    @Override
    public boolean onTouchEvent(final android.view.MotionEvent ev) {
        if (super.onTouchEvent(ev)) {
            return true;
        }
        if (this.mViewPager == null
            || this.mViewPager.getAdapter().getCount() == 0) {
            return false;
        }

        final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;
        switch (action) {
        case MotionEvent.ACTION_DOWN:
            this.mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
            this.mLastMotionX = ev.getX();
            break;

        case MotionEvent.ACTION_MOVE: {
            final int activePointerIndex = MotionEventCompat.findPointerIndex(ev,
                                                                              this.mActivePointerId);
            final float x = MotionEventCompat.getX(ev, activePointerIndex);
            final float deltaX = x - this.mLastMotionX;

            if (!this.mIsDragging) {
                if (Math.abs(deltaX) > this.mTouchSlop) {
                    this.mIsDragging = true;
                }
            }

            if (this.mIsDragging) {
                this.mLastMotionX = x;
                if (this.mViewPager.isFakeDragging()
                    || this.mViewPager.beginFakeDrag()) {
                    this.mViewPager.fakeDragBy(deltaX);
                }
            }

            break;
        }

        case MotionEvent.ACTION_CANCEL:
        case MotionEvent.ACTION_UP:
            if (!this.mIsDragging) {
                final int count = this.mViewPager.getAdapter().getCount();
                final int width = this.getWidth();
                final float halfWidth = width / 2f;
                final float sixthWidth = width / 6f;
                final float leftThird = halfWidth - sixthWidth;
                final float rightThird = halfWidth + sixthWidth;
                final float eventX = ev.getX();

                if (eventX < leftThird) {
                    if (this.mCurrentPage > 0) {
                        if (action != MotionEvent.ACTION_CANCEL) {
                            this.mViewPager.setCurrentItem(this.mCurrentPage - 1);
                        }
                        return true;
                    }
                } else if (eventX > rightThird) {
                    if (this.mCurrentPage < count - 1) {
                        if (action != MotionEvent.ACTION_CANCEL) {
                            this.mViewPager.setCurrentItem(this.mCurrentPage + 1);
                        }
                        return true;
                    }
                } else {
                    // Middle third
                    if (this.mCenterItemClickListener != null
                        && action != MotionEvent.ACTION_CANCEL) {
                        this.mCenterItemClickListener.onCenterItemClick(this.mCurrentPage);
                    }
                }
            }

            this.mIsDragging = false;
            this.mActivePointerId = TitlePageIndicator.INVALID_POINTER;
            if (this.mViewPager.isFakeDragging()) {
                this.mViewPager.endFakeDrag();
            }
            break;

        case MotionEventCompat.ACTION_POINTER_DOWN: {
            final int index = MotionEventCompat.getActionIndex(ev);
            this.mLastMotionX = MotionEventCompat.getX(ev, index);
            this.mActivePointerId = MotionEventCompat.getPointerId(ev, index);
            break;
        }

        case MotionEventCompat.ACTION_POINTER_UP:
            final int pointerIndex = MotionEventCompat.getActionIndex(ev);
            final int pointerId = MotionEventCompat.getPointerId(ev,
                                                                 pointerIndex);
            if (pointerId == this.mActivePointerId) {
                final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                this.mActivePointerId = MotionEventCompat.getPointerId(ev,
                                                                       newPointerIndex);
            }
            this.mLastMotionX = MotionEventCompat.getX(ev,
                                                       MotionEventCompat.findPointerIndex(ev,
                                                                                          this.mActivePointerId));
            break;
        }

        return true;
    }

    /**
     * Set bounds for the right textView including clip padding.
     * 
     * @param curViewBound
     *            current bounds.
     * @param curViewWidth
     *            width of the view.
     */
    private void clipViewOnTheRight(final Rect curViewBound,
                                    final float curViewWidth,
                                    final int right) {
        curViewBound.right = (int) (right - this.mClipPadding);
        curViewBound.left = (int) (curViewBound.right - curViewWidth);
    }

    /**
     * Set bounds for the left textView including clip padding.
     * 
     * @param curViewBound
     *            current bounds.
     * @param curViewWidth
     *            width of the view.
     */
    private void clipViewOnTheLeft(final Rect curViewBound,
                                   final float curViewWidth,
                                   final int left) {
        curViewBound.left = (int) (left + this.mClipPadding);
        curViewBound.right = (int) (this.mClipPadding + curViewWidth);
    }

    /**
     * Calculate views bounds and scroll them according to the current index
     * 
     * @param paint
     * @return
     */
    private ArrayList<Rect> calculateAllBounds(final Paint paint) {
        final ArrayList<Rect> list = new ArrayList<Rect>();
        // For each views (If no values then add a fake one)
        final int count = this.mViewPager.getAdapter().getCount();
        final int width = this.getWidth();
        final int halfWidth = width / 2;
        for (int i = 0; i < count; i++) {
            final Rect bounds = this.calcBounds(i, paint);
            final int w = bounds.right - bounds.left;
            final int h = bounds.bottom - bounds.top;
            bounds.left = (int) (halfWidth - w / 2f + (i - this.mCurrentPage - this.mPageOffset)
                                                      * width);
            bounds.right = bounds.left + w;
            bounds.top = 0;
            bounds.bottom = h;
            list.add(bounds);
        }

        return list;
    }

    /**
     * Calculate the bounds for a view's title
     * 
     * @param index
     * @param paint
     * @return
     */
    private Rect calcBounds(final int index, final Paint paint) {
        // Calculate the text bounds
        final Rect bounds = new Rect();
        final CharSequence title = this.getTitle(index);
        bounds.right = (int) paint.measureText(title, 0, title.length());
        bounds.bottom = (int) (paint.descent() - paint.ascent());
        return bounds;
    }

    @Override
    public void setViewPager(final ViewPager view) {
        if (this.mViewPager == view) {
            return;
        }
        if (this.mViewPager != null) {
            this.mViewPager.setOnPageChangeListener(null);
        }
        if (view.getAdapter() == null) {
            throw new IllegalStateException("ViewPager does not have adapter instance.");
        }
        this.mViewPager = view;
        this.mViewPager.setOnPageChangeListener(this);
        this.invalidate();
    }

    @Override
    public void setViewPager(final ViewPager view, final int initialPosition) {
        this.setViewPager(view);
        this.setCurrentItem(initialPosition);
    }

    @Override
    public void notifyDataSetChanged() {
        this.invalidate();
    }

    /**
     * Set a callback listener for the center item click.
     * 
     * @param listener
     *            Callback instance.
     */
    public void setOnCenterItemClickListener(final OnCenterItemClickListener listener) {
        this.mCenterItemClickListener = listener;
    }

    @Override
    public void setCurrentItem(final int item) {
        if (this.mViewPager == null) {
            throw new IllegalStateException("ViewPager has not been bound.");
        }
        this.mViewPager.setCurrentItem(item);
        this.mCurrentPage = item;
        this.invalidate();
    }

    @Override
    public void onPageScrollStateChanged(final int state) {
        this.mScrollState = state;

        if (this.mListener != null) {
            this.mListener.onPageScrollStateChanged(state);
        }
    }

    @Override
    public void onPageScrolled(final int position,
                               final float positionOffset,
                               final int positionOffsetPixels) {
        this.mCurrentPage = position;
        this.mPageOffset = positionOffset;
        this.invalidate();

        if (this.mListener != null) {
            this.mListener.onPageScrolled(position,
                                          positionOffset,
                                          positionOffsetPixels);
        }
    }

    @Override
    public void onPageSelected(final int position) {
        if (this.mScrollState == ViewPager.SCROLL_STATE_IDLE) {
            this.mCurrentPage = position;
            this.invalidate();
        }

        if (this.mListener != null) {
            this.mListener.onPageSelected(position);
        }
    }

    @Override
    public void setOnPageChangeListener(final ViewPager.OnPageChangeListener listener) {
        this.mListener = listener;
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec,
                             final int heightMeasureSpec) {
        // Measure our width in whatever mode specified
        final int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);

        // Determine our height
        float height;
        final int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightSpecMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            height = MeasureSpec.getSize(heightMeasureSpec);
        } else {
            // Calculate the text bounds
            this.mBounds.setEmpty();
            this.mBounds.bottom = (int) (this.mPaintText.descent() - this.mPaintText.ascent());
            height = this.mBounds.bottom
                     - this.mBounds.top
                     + this.mFooterLineHeight
                     + this.mFooterPadding
                     + this.mTopPadding;
            if (this.mFooterIndicatorStyle != IndicatorStyle.None) {
                height += this.mFooterIndicatorHeight;
            }
        }
        final int measuredHeight = (int) height;

        this.setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    public void onRestoreInstanceState(final Parcelable state) {
        final SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        this.mCurrentPage = savedState.currentPage;
        this.requestLayout();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        final SavedState savedState = new SavedState(superState);
        savedState.currentPage = this.mCurrentPage;
        return savedState;
    }

    static class SavedState extends BaseSavedState {
        int currentPage;

        public SavedState(final Parcelable superState) {
            super(superState);
        }

        private SavedState(final Parcel in) {
            super(in);
            this.currentPage = in.readInt();
        }

        @Override
        public void writeToParcel(final Parcel dest, final int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(this.currentPage);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(final Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(final int size) {
                return new SavedState[size];
            }
        };
    }

    private CharSequence getTitle(final int i) {
        CharSequence title = this.mViewPager.getAdapter().getPageTitle(i);
        if (title == null) {
            title = TitlePageIndicator.EMPTY_TITLE;
        }
        return title;
    }
}

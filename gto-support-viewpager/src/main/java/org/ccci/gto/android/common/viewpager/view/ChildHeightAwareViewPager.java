package org.ccci.gto.android.common.viewpager.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

/**
 * Based on HeightWrappingViewPager.
 *
 * @see <a href="http://stackoverflow.com/a/14983747/4721910">http://stackoverflow.com/a/14983747/4721910</a>
 */
public class ChildHeightAwareViewPager extends HackyViewPager {
    public ChildHeightAwareViewPager(final Context context) {
        super(context);
    }

    public ChildHeightAwareViewPager(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // check child height if we don't have an exact height
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode != MeasureSpec.EXACTLY) {
            /**
             * The first super.onMeasure call made the pager take up all the available height. Since we really wanted
             * to dynamically wrap it, we need to remeasure it. Luckily, after that call the first child is now
             * available. So, we take the height from it.
             */

            // If the pager actually has any children, calculate the children height and use the largest as our height
            int height = getMeasuredHeight();
            final int count = getChildCount();
            if (count > 0) {
                // simplify the calculations by using exactly the previous measured width
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY);

                // measure the children
                measureChildren(widthMeasureSpec, heightMeasureSpec);

                // calculate the max height for all the (current) children
                final int paddingVertical = getPaddingTop() + getPaddingBottom();
                for (int i = 0; i < count; i++) {
                    final View child = getChildAt(i);
                    height = Math.max(height, child.getMeasuredHeight() + paddingVertical);
                }

                // re-measure ViewPager using the calculated height
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            }
        }
    }
}

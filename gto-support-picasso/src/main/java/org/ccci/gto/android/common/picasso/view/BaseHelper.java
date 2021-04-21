package org.ccci.gto.android.common.picasso.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Transformation;

import org.ccci.gto.android.common.base.model.Dimension;
import org.ccci.gto.android.common.picasso.R;
import org.ccci.gto.android.common.picasso.transformation.ScaleTransformation;

import java.util.ArrayList;
import java.util.List;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static org.ccci.gto.android.common.base.Constants.INVALID_DRAWABLE_RES;

public abstract class BaseHelper {
    @NonNull
    protected final ImageView mView;

    @NonNull
    private Dimension mSize = new Dimension(0, 0);
    @DrawableRes
    private int mPlaceholderResId = INVALID_DRAWABLE_RES;
    @Nullable
    private Drawable mPlaceholder = null;
    private final ArrayList<Transformation> mTransforms = new ArrayList<>();

    private int mBatching = 0;
    private boolean mNeedsUpdate = false;

    public BaseHelper(@NonNull final ImageView view, @Nullable final AttributeSet attrs, final int defStyleAttr,
                  final int defStyleRes) {
        mView = view;
        init(mView.getContext(), attrs, defStyleAttr, defStyleRes);
    }

    private void init(@NonNull final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr,
                      final int defStyleRes) {
        final TypedArray a =
                context.obtainStyledAttributes(attrs, R.styleable.PicassoImageView, defStyleAttr, defStyleRes);
        mPlaceholderResId = a.getResourceId(R.styleable.PicassoImageView_placeholder, INVALID_DRAWABLE_RES);
        a.recycle();
    }

    @UiThread
    public final void setPlaceholder(@DrawableRes final int placeholder) {
        final boolean changing = mPlaceholder != null || mPlaceholderResId != placeholder;
        mPlaceholder = null;
        mPlaceholderResId = placeholder;
        if (changing) {
            triggerUpdate();
        }
    }

    @UiThread
    public final void setPlaceholder(@Nullable final Drawable placeholder) {
        final boolean changing = mPlaceholderResId != INVALID_DRAWABLE_RES || placeholder != mPlaceholder;
        mPlaceholderResId = INVALID_DRAWABLE_RES;
        mPlaceholder = placeholder;
        if (changing) {
            triggerUpdate();
        }
    }

    @UiThread
    public final void setScaleType(@NonNull final ImageView.ScaleType type) {
        triggerUpdate();
    }

    @UiThread
    public final void addTransform(@NonNull final Transformation transformation) {
        mTransforms.add(transformation);
        triggerUpdate();
    }

    @UiThread
    public final void setTransforms(@Nullable final List<? extends Transformation> transformations) {
        mTransforms.clear();
        if (transformations != null) {
            mTransforms.addAll(transformations);
        }
        triggerUpdate();
    }

    @UiThread
    public final void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (oldw != w || oldh != h) {
            mSize = new Dimension(w, h);
            // onSizeChanged() is called during layout, so we need to defer until after layout is complete
            postTriggerUpdate();
        }
    }

    @UiThread
    public final void toggleBatchUpdates(final boolean enable) {
        if (enable) {
            mBatching++;
        } else {
            mBatching--;
            if (mBatching <= 0) {
                mBatching = 0;
                if (mNeedsUpdate) {
                    triggerUpdate();
                }
            }
        }
    }

    private void postTriggerUpdate() {
        mNeedsUpdate = true;
        mView.post(() -> {
            if (mNeedsUpdate) {
                triggerUpdate();
            }
        });
    }

    @UiThread
    protected final void triggerUpdate() {
        // short-circuit if we are in edit mode within a development tool
        if (mView.isInEditMode()) {
            return;
        }

        // if we are batching updates, track that we need an update, but don't trigger the update now
        if (mBatching > 0) {
            mNeedsUpdate = true;
            return;
        }

        // if we are currently in a layout pass, trigger an update once layout is complete
        if (mView.isInLayout()) {
            postTriggerUpdate();
            return;
        }

        // clear the needs update flag
        mNeedsUpdate = false;

        // create base request
        final RequestCreator update = onCreateUpdate(Picasso.get());

        // set placeholder & any transform options
        if (mPlaceholderResId != INVALID_DRAWABLE_RES) {
            update.placeholder(mPlaceholderResId);
        } else if (mPlaceholder != null) {
            update.placeholder(mPlaceholder);
        }

        if (mSize.width > 0 || mSize.height > 0) {
            onSetUpdateScale(update, mSize);
        }

        update.transform(mTransforms);

        // fetch or load based on the target size
        if (mSize.width > 0 || mSize.height > 0) {
            update.into(mView);
        } else {
            update.fetch();
        }
    }

    @NonNull
    @UiThread
    protected abstract RequestCreator onCreateUpdate(@NonNull final Picasso picasso);

    @UiThread
    protected void onSetUpdateScale(@NonNull final RequestCreator update, @NonNull final Dimension size) {
        // TODO: add some android integration tests for this behavior

        // is the view layout set to wrap content? if so we should just resize and not crop the image
        final ViewGroup.LayoutParams lp = mView.getLayoutParams();
        if (lp.width == WRAP_CONTENT || lp.height == WRAP_CONTENT) {
            if (lp.width == WRAP_CONTENT && lp.height == WRAP_CONTENT) {
                // Don't resize, let the view determine the size from the original image
            } else if (lp.width == WRAP_CONTENT && size.height > 0) {
                update.resize(0, size.height).onlyScaleDown();
            } else if (lp.height == WRAP_CONTENT && size.width > 0) {
                update.resize(size.width, 0).onlyScaleDown();
            }
            return;
        }

        // if we are planning on cropping the image when displaying it, lets crop it in Picasso first
        switch (mView.getScaleType()) {
            case CENTER_CROP:
                // centerCrop crops the image to the exact size specified by resize. So, if a dimension is 0 we
                // can't crop the image anyways.
                if (size.width > 0 && size.height > 0) {
                    update.resize(size.width, size.height);
                    update.onlyScaleDown();
                    update.centerCrop();
                }
                break;
            case CENTER_INSIDE:
            case FIT_CENTER:
            case FIT_START:
            case FIT_END:
                update.resize(size.width, size.height);
                update.onlyScaleDown();
                update.centerInside();
                break;
            default:
                update.transform(new ScaleTransformation(size.width, size.height));
                break;
        }
    }
}

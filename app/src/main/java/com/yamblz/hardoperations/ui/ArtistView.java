package com.yamblz.hardoperations.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.yamblz.hardoperations.R;
import com.yamblz.hardoperations.model.Artist;
import com.yamblz.hardoperations.utils.BitmapUtils;

import java.util.Collections;

/**
 * Created by i-sergeev on 06.07.16
 */
public class ArtistView extends View {
    private static final int WHITE_COLOR = 0xFFFFFF;
    private static final int PALETTE_POPULATION = 100;

    private TextPaint titlePaint;
    private TextPaint descriptionPaint;
    private int defaultTextColor;
    private int defaultBackgroundColor;

    private Artist artist;
    private Bitmap posterBitmap;
    private ImageLoadTarget imageLoadTarget;
    private Picasso picasso;

    private Palette palette;
    private Palette imagePalette;
    private boolean isPaletteLoading;
    private int imageHeight;
    private Resources resources;
    private int posterLRPosterPadding;
    private int posterTopPadding;
    private StaticLayout descriptionStaticLayout;
    private int posterTextMargin;
    private int textLRPadding;
    private float titleTextHeight;
    private StaticLayout titleStaticLayout;
    private int posterLRTextPadding;
    private int titleDescMargin;
    private Bitmap scaledBitmap;

    public ArtistView(Context context) {
        super(context);
        init(context);
    }

    public ArtistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ArtistView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @SuppressWarnings("unused")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ArtistView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    @NonNull
    private static Palette getDefaultPalette() {
        Palette.Swatch swatch = new Palette.Swatch(WHITE_COLOR, PALETTE_POPULATION);
        return Palette.from(Collections.singletonList(swatch));
    }

    private void init(@NonNull Context context) {
        picasso = Picasso.with(context);

        resources = getResources();

        //noinspection deprecation
        defaultTextColor = resources.getColor(R.color.default_text_color);
        //noinspection deprecation
        defaultBackgroundColor = resources.getColor(R.color.default_background_color);

        float titleFontSize = resources.getDimensionPixelSize(R.dimen.artist_card_title_font_size);
        titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setTypeface(Typeface.DEFAULT_BOLD);
        titlePaint.setTextSize(titleFontSize);
        titlePaint.setColor(defaultTextColor);

        float descriptionFontSize = resources.getDimensionPixelSize(R.dimen.artist_card_font_size);
        descriptionPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        descriptionPaint.setTextSize(descriptionFontSize);
        descriptionPaint.setColor(defaultTextColor);

        palette = getDefaultPalette();
        imageHeight = resources.getDimensionPixelOffset(R.dimen.poster_height);
        titleDescMargin = getResources().getDimensionPixelOffset(R.dimen.artist_card_title_desc_margin);
        posterLRPosterPadding = resources.getDimensionPixelOffset(R.dimen.artist_card_top_padding);
        posterTopPadding = resources.getDimensionPixelOffset(R.dimen.artist_card_top_padding);
        posterTextMargin = resources.getDimensionPixelOffset(R.dimen.artist_card_poster_text_margin);
        textLRPadding = resources.getDimensionPixelOffset(R.dimen.artist_card_lr_text_padding);
        posterLRTextPadding = resources.getDimensionPixelOffset(R.dimen.artist_card_lr_text_padding);
    }

    public void setArtist(Artist artist) {
        this.artist = artist;
        invalidate();
        requestLayout();

        if (imageLoadTarget != null) {
            Picasso.with(getContext()).cancelRequest(imageLoadTarget);
            imageLoadTarget = null;
        }
        imageLoadTarget = new ImageLoadTarget();
        picasso.load(artist.getCover().getBigImageUrl()).into(imageLoadTarget);
    }

    private void setPosterBitmap(Bitmap bitmap) {
        posterBitmap = bitmap;
        if (posterBitmap != null) {
            scaledBitmap = BitmapUtils.fitToCenterBitmap(posterBitmap,
                    getWidth() - (2 * posterLRPosterPadding),
                    imageHeight);
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (artist == null) {
            return;
        }

        if (!isPaletteLoading)
            loadPaletteAsync();

        //Draw background
        if (imagePalette != null)
            palette = imagePalette;

        canvas.drawRect(0, 0, getWidth(), getHeight(), getRectPaint(palette.getLightVibrantColor(
                defaultBackgroundColor)));

        int textColor = palette.getDarkMutedColor(defaultTextColor);
        titlePaint.setColor(palette.getDarkMutedColor(textColor));
        descriptionPaint.setColor(textColor);

        //draw poster
        if (posterBitmap == null) {
            canvas.drawRect(posterLRPosterPadding,
                    posterTopPadding,
                    getWidth() - posterLRPosterPadding,
                    imageHeight,
                    getRectPaint(WHITE_COLOR));
        } else {
            canvas.drawBitmap(scaledBitmap,
                    posterLRPosterPadding,
                    posterTopPadding,
                    getBitmapPaint());
        }

        canvas.save();
        canvas.translate(textLRPadding, posterTopPadding + imageHeight + posterTextMargin);
        titleStaticLayout.draw(canvas);
        canvas.restore();

        //draw description
        canvas.save();
        canvas.translate(textLRPadding,
                posterTopPadding + imageHeight + posterTextMargin + titleTextHeight + titleDescMargin);
        descriptionStaticLayout.draw(canvas);
        canvas.restore();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (artist == null) {
            setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int width = resolveSizeAndState(getSuggestedMinimumWidth(), widthMeasureSpec, 1);

        int textWidth = width - (2 * posterLRTextPadding);

        int height = 0;
        height += resources.getDimensionPixelOffset(R.dimen.poster_height);
        height += getTextHeight(artist.getName(), textWidth, titlePaint);
        height += getTextHeight(getArtistDescription(), textWidth, descriptionPaint);

        height += resources.getDimensionPixelOffset(R.dimen.artist_card_top_padding);
        height += resources.getDimensionPixelOffset(R.dimen.artist_card_bottom_padding);
        height += resources.getDimensionPixelOffset(R.dimen.artist_card_poster_text_margin);
        height += resources.getDimensionPixelOffset(R.dimen.artist_card_title_desc_margin);

        titleTextHeight = getTextHeight(artist.getName(), width, titlePaint);

        titleStaticLayout = getStaticLayout(artist.getName(),
                width - textLRPadding,
                titlePaint);

        descriptionStaticLayout = getStaticLayout(getArtistDescription(),
                width - textLRPadding,
                descriptionPaint);

        setMeasuredDimension(width, height);
    }

    private float getTextHeight(String text, int width, TextPaint textPaint) {
        return getStaticLayout(text, width, textPaint).getHeight();
    }

    private StaticLayout getStaticLayout(String text, int width, TextPaint textPaint) {
        if (text == null) {
            text = "";
        }
        return new StaticLayout(text, textPaint, width, Layout.Alignment.ALIGN_NORMAL, 1, 1, false);
    }

    private Paint getRectPaint(int color) {
        Paint rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectPaint.setColor(color);
        return rectPaint;
    }

    private Paint getBitmapPaint() {
        Paint bitmapPaint = new Paint();
        bitmapPaint.setAntiAlias(true);
        bitmapPaint.setFilterBitmap(true);
        bitmapPaint.setDither(true);
        return bitmapPaint;
    }

    private String getArtistDescription() {
        if (artist == null) {
            return "";
        }
        String descriptionText = artist.getDescription() + "\n";
        descriptionText += "\n" + getResources().getQuantityString(R.plurals.artistAlbums,
                artist.getAlbumsCount(),
                artist.getAlbumsCount());
        descriptionText += "\n" + getResources().getQuantityString(R.plurals.artistTracks,
                artist.getTracksCount(),
                artist.getTracksCount());
        return descriptionText;
    }

    private void loadPaletteAsync() {
        if (posterBitmap != null && !posterBitmap.isRecycled()) {
            isPaletteLoading = true;
            Palette.from(posterBitmap).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    imagePalette = palette;
                    invalidate();
                }
            });
        }
    }

    private final class ImageLoadTarget implements Target {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            imageLoadTarget = null;
            setPosterBitmap(bitmap);
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            imageLoadTarget = null;
            setPosterBitmap(null);
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
            setPosterBitmap(null);
        }
    }
}

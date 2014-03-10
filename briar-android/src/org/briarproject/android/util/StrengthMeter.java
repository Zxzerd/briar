package org.briarproject.android.util;

import static android.graphics.Color.BLACK;
import static android.graphics.Paint.Style.FILL;
import static android.graphics.Paint.Style.STROKE;
import static android.graphics.drawable.ClipDrawable.HORIZONTAL;
import static android.view.Gravity.LEFT;
import static org.briarproject.api.crypto.PasswordStrengthEstimator.QUITE_STRONG;
import static org.briarproject.api.crypto.PasswordStrengthEstimator.QUITE_WEAK;
import static org.briarproject.api.crypto.PasswordStrengthEstimator.STRONG;
import static org.briarproject.api.crypto.PasswordStrengthEstimator.WEAK;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.widget.ProgressBar;

public class StrengthMeter extends ProgressBar {

	private static final int MAX = 100;
	private static final int RED = Color.rgb(255, 0, 0);
	private static final int ORANGE = Color.rgb(255, 160, 0);
	private static final int YELLOW = Color.rgb(255, 255, 0);
	private static final int LIME = Color.rgb(180, 255, 0);
	private static final int GREEN = Color.rgb(0, 255, 0);

	private final ShapeDrawable bar;

	public StrengthMeter(Context context) {
		super(context, null, android.R.attr.progressBarStyleHorizontal);
		bar = new ShapeDrawable();
		bar.getPaint().setColor(RED);
		ClipDrawable clip = new ClipDrawable(bar, LEFT, HORIZONTAL);
		ShapeDrawable background = new ShapeDrawable();
		Paint p = background.getPaint();
		p.setStyle(FILL);
		p.setColor(getResources().getColor(android.R.color.transparent));
		p.setStyle(STROKE);
		p.setStrokeWidth(1);
		p.setColor(BLACK);
		Drawable[] layers = new Drawable[] { clip, background };
		setProgressDrawable(new LayerDrawable(layers));
		setIndeterminate(false);
	}

	@Override
	public int getMax() {
		return MAX;
	}

	public void setStrength(float strength) {
		if(strength < 0 || strength > 1) throw new IllegalArgumentException();
		int colour;
		if(strength < WEAK) colour = RED;
		else if(strength < QUITE_WEAK) colour = ORANGE;
		else if(strength < QUITE_STRONG) colour = YELLOW;
		else if(strength < STRONG) colour = LIME;
		else colour = GREEN;
		bar.getPaint().setColor(colour);
		setProgress((int) (strength * MAX));
	}
}
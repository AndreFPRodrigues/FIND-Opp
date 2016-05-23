package ul.fcul.lasige.findvictim.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.util.AttributeSet;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MyCanvas extends View {

    private Paint paint = new Paint();
    private Path path = new Path();
    private List<Path> paths;
    private ArrayList<Integer> pathColor;

    public MyCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
        File f = new File(Constants.SAVE_IMAGE);

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getAbsolutePath(), bounds);

        BitmapFactory.Options opts = new BitmapFactory.Options();
        Bitmap bm = BitmapFactory.decodeFile(f.getAbsolutePath(), opts);
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(f.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert exif != null;
        String orientString = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
        int orientation = orientString != null ? Integer.parseInt(orientString) :  ExifInterface.ORIENTATION_NORMAL;

        int rotationAngle = 0;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotationAngle = 90;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotationAngle = 180;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotationAngle = 270;

        Matrix matrix = new Matrix();
        matrix.setRotate(rotationAngle, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bm, 0, 0, bounds.outWidth, bounds.outHeight, matrix, true);

        //BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        //Bitmap bitmap = BitmapFactory.decodeFile(f.getAbsolutePath(),bmOptions);
        rotatedBitmap = Bitmap.createScaledBitmap(rotatedBitmap,size.x,size.y,true);
        BitmapDrawable ob = new BitmapDrawable(getResources(), rotatedBitmap);

        final int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            setBackgroundDrawable(ob);
        } else {
            setBackground(ob);
        }

        paths = new ArrayList<>();
        pathColor = new ArrayList<>();

        paint.setAntiAlias(true);
        paint.setStrokeWidth(6f);
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (int i = 0; i < paths.size() ; i++) {
            paint.setColor(pathColor.get(i));
            canvas.drawPath(paths.get(i), paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float eventX = event.getX();
        float eventY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(eventX, eventY);
                return true;
            case MotionEvent.ACTION_MOVE:
                path.lineTo(eventX, eventY);
                paths.add(path);
                pathColor.add(paint.getColor());
                break;
            case MotionEvent.ACTION_UP:
                path = new Path();
                break;
            default:
                return false;
        }

        invalidate();
        return true;
    }

    /**
     * Changes the stroke color
     * @param color color of the stroke
     */
    public void changeColor (int color) {
        paint.setColor(color);
    }

    /**
     * Cleans the draw
     */
    public void clean () {
        path = new Path();
        paths = new ArrayList<>();
        pathColor = new ArrayList<>();
        invalidate();
    }
}

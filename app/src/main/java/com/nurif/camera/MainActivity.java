package com.nurif.camera;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.File;
import java.io.InputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {
  private int PICK_FROM_CAMERA = 01;
  private int PICK_FROM_CAMERA_W_CROP = 02;
  private int PICK_FROM_GALLERY = 03;
  private int CROP_FROM_CAMERA = 10;

  private boolean setRotate = true;

  private Uri mImageCaptureUri;
  private String mCurrentPhotoPath;
  private File fileAvatar;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  }

  public void fromCamera(View view) {
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

    //Set directory
    takePictureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
        Uri.fromFile(setDirectory()));

    mCurrentPhotoPath = setDirectory().getAbsolutePath();

    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
      startActivityForResult(takePictureIntent, PICK_FROM_CAMERA);
    }
  }

  public void fromCameraCrop(View view) {
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

    //Set directory
    takePictureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
        Uri.fromFile(setDirectory()));

    mCurrentPhotoPath = setDirectory().getAbsolutePath();

    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
      startActivityForResult(takePictureIntent, PICK_FROM_CAMERA_W_CROP);
    }
  }

  public File setDirectory() {
    File fl = new File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
            + "/nurif");
    if (!fl.exists()) {
      if (!fl.mkdirs()) {
        Log.d("RESULTSPORT", "failed to create directory");
      }
    }

    return new File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
            + "/nurif/"
            + String.valueOf(System.currentTimeMillis())
            + ".jpg");
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (resultCode == RESULT_OK) {
      if (requestCode == PICK_FROM_CAMERA) {
        Bitmap bitmap;

        if (setRotate) {
          int rotate = 0;
          try {
            File imageFile = new File(mCurrentPhotoPath);
            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
              case ExifInterface.ORIENTATION_ROTATE_270:
                rotate = 270;
                break;
              case ExifInterface.ORIENTATION_ROTATE_180:
                rotate = 180;
                break;
              case ExifInterface.ORIENTATION_ROTATE_90:
                rotate = 90;
                break;
            }
          } catch (Exception e) {
            e.printStackTrace();
          }

          final BitmapFactory.Options options = new BitmapFactory.Options();
          options.inSampleSize = 5;
          Bitmap photo = BitmapFactory.decodeFile(mCurrentPhotoPath, options);

          int width = photo.getWidth();
          int height = photo.getHeight();
          photo = Bitmap.createScaledBitmap(photo, width, height, false);

          Matrix matrix = new Matrix();
          matrix.postRotate(rotate);
          bitmap =
              Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), matrix, false);
        } else {
          Bundle extras = data.getExtras();

          if (extras != null) {
            bitmap = (Bitmap) extras.get("data");
          } else{
            return;
          }
        }

        showImage(bitmap);

        /*PHOTO WILL BE SAVE IN DEAFULT PHOTOSD STORAGE*/
      } else if (requestCode == PICK_FROM_CAMERA_W_CROP) {
        mImageCaptureUri = data.getData();
        doCrop();
      } else if (requestCode == PICK_FROM_GALLERY) {
        try {
          BitmapFactory.Options options = new BitmapFactory.Options();
          final InputStream ist = getContentResolver().openInputStream(data.getData());
          Bitmap bitmap = BitmapFactory.decodeStream(ist, null, options);

          AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
          LayoutInflater inflater = this.getLayoutInflater();
          View dialogView = inflater.inflate(R.layout.image_layout, null);
          dialogBuilder.setView(dialogView);

          ImageView ivImage = (ImageView) dialogView.findViewById(R.id.iv_image);
          ivImage.setImageBitmap(bitmap);
          AlertDialog alertDialog = dialogBuilder.create();
          alertDialog.show();
        } catch (Exception ex) {
          Log.e("err", ex.getMessage());
          return;
        }
      } else if (requestCode == CROP_FROM_CAMERA) {
        Bundle extras = data.getExtras();

        if (extras != null) {
          Bitmap bitmap = extras.getParcelable("data");

          showImage(bitmap);
        }
      }
    }
  }

  private void doCrop() {
    Intent intent = new Intent("com.android.camera.action.CROP");
    intent.setType("image/*");
    List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, 0);
    int size = list.size();
    if (size == 0) {
      Toast.makeText(this, "Can not find image crop app", Toast.LENGTH_SHORT).show();
      intent.setData(mImageCaptureUri);
      intent.putExtra("return-data", true);
      intent.putExtra("aspectX", 1);
      intent.putExtra("aspectY", 1);
      intent.putExtra("crop", "true");
      intent.putExtra("outputX", 256);
      intent.putExtra("outputY", 256);
      startActivityForResult(intent, CROP_FROM_CAMERA);
      return;
    } else {
      intent.setData(mImageCaptureUri);
      intent.putExtra("aspectX", 1);
      intent.putExtra("aspectY", 1);
      intent.putExtra("outputX", 256);
      intent.putExtra("outputY", 256);
      intent.putExtra("crop", "true");
      intent.putExtra("return-data", true);
      ResolveInfo res = list.get(0);
      intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
      startActivityForResult(intent, CROP_FROM_CAMERA);
    }
  }

  public void showImage(Bitmap bitmap) {
    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
    LayoutInflater inflater = this.getLayoutInflater();
    View dialogView = inflater.inflate(R.layout.image_layout, null);
    dialogBuilder.setView(dialogView);

    ImageView ivImage = (ImageView) dialogView.findViewById(R.id.iv_image);
    ivImage.setImageBitmap(bitmap);
    AlertDialog alertDialog = dialogBuilder.create();
    alertDialog.show();
  }

  public void fromGalleryNoPermission(View view) {
    /*WITHOUT NEED PERMISSION*/

    Intent intent = new Intent();
    intent.setType("image/*");
    intent.setAction(Intent.ACTION_GET_CONTENT);
    startActivityForResult(Intent.createChooser(intent, "Complete action using"),
        PICK_FROM_GALLERY);

    Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
    i.setType("image/*");
    startActivityForResult(i, PICK_FROM_GALLERY);
  }

  public void fromGalleryPermission(View view) {
    /*NEED PERMISSION IN MANIFEST*/

    Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
    i.setType("image/*");
    startActivityForResult(i, PICK_FROM_GALLERY);
  }
}

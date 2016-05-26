package ul.fcul.lasige.findvictim.app;

import android.content.Context;
import android.os.Environment;

public class Constants {

    // start activities for result REQUEST_CODE
    public static final int EMAIL_REQUEST_CODE = 0;
    public static final int TAKE_PICTURE_REQUEST_CODE = 1;
    public static final int SEND_FILE_REQUEST_CODE = 2;

    public static final int ASK_REACH_PHONE_REQUEST_CODE = 3;
    public static final int ASK_OPTION_REQUEST_CODE = 4;
    public static final int GET_ANSWERED_OPTION_REQUEST_CODE = 5;
    public static final int POST_MESSAGE_REQUEST_CODE = 6;

    // ongoing alert, 0 - false, 1 - schedule, 2 - ongoing
    public static int ONGOING_ALERT = 0;

    // platform manually started/stopped
    public static boolean MANUALLY_STARTED = false;
    public static boolean MANUALLY_STOPPED = false;

    // voice commands time
    public static int ASK_STATE_TIME = 1;

    // SOS call
    public static final String CALL_112 = "tel:968938597";

    // shared preferences file
    public static final String VICTIM_PREF_FILE = "victimSharedPreferencesFile";

    // app folders
    public static final String ROOT_FOLDER = Environment.getExternalStorageDirectory().getAbsolutePath() + "/FindVictimApp";

    public static final String RECORDS_FOLDER =  "/records_folder";
    public static final String IMAGES_FOLDER = "/images_folder";
    public static final String RECEIVE_FOLDER = "/receive_folder";

    public static final String RECORD_FILE = "/voiceRecordMessageFile.3gp";
    public static final String RECORD_DRAWING_FILE = "/voiceRecordDrawingMessageFile.3gp";

    public static final String IMAGE_FILE = "/imageFile.png";
    public static final String DRAWING_FILE = "/drawingFile.png";

    public static final String SAVE_VOICE_MESSAGE = ROOT_FOLDER + RECORDS_FOLDER + RECORD_FILE;
    public static final String SAVE_DRAW_VOICE_MESSAGE = ROOT_FOLDER + RECORDS_FOLDER + RECORD_DRAWING_FILE;

    public static final String SAVE_IMAGE = ROOT_FOLDER + IMAGES_FOLDER + IMAGE_FILE;
    public static final String SAVE_DRAW_IMAGE = ROOT_FOLDER + IMAGES_FOLDER + DRAWING_FILE;
}

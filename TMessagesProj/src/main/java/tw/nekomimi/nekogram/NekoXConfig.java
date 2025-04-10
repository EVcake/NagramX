package tw.nekomimi.nekogram;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Typeface;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import static org.telegram.messenger.LocaleController.getString;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.Theme;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import tw.nekomimi.nekogram.database.NitritesKt;
import xyz.nextalone.nagram.NaConfig;

public class NekoXConfig {

    public static String FAQ_URL = "https://github.com/NextAlone/Nagram#faq";
    public static long[] officialChats = {
    };

    public static long[] developers = {
    };

    public static final int TITLE_TYPE_TEXT = 0;
    public static final int TITLE_TYPE_ICON = 1;
    public static final int TITLE_TYPE_MIX = 2;

    private static final String EMOJI_FONT_AOSP = "NotoColorEmoji.ttf";

    public static boolean loadSystemEmojiFailed = false;
    private static Typeface systemEmojiTypeface;

    public static SharedPreferences preferences = NitritesKt.openMainSharedPreference("nekox_config");

    public static boolean developerMode = preferences.getBoolean("developer_mode", true);

    public static boolean disableFlagSecure = NaConfig.INSTANCE.getDisableFlagSecure().Bool();

    public static boolean disableStatusUpdate = preferences.getBoolean("disable_status_update", false);
    public static boolean keepOnlineStatus = preferences.getBoolean("keepOnlineStatus", false);

    public static int customApi = preferences.getInt("custom_api", 0);
    public static int customAppId = preferences.getInt("custom_app_id", 0);
    public static String customAppHash = preferences.getString("custom_app_hash", "");

    private static Boolean hasDeveloper = null;
    
    public static int currentAppId() {
        switch (customApi) {
            case 0:
                return BuildConfig.APP_ID;
            case 1:
                return BuildVars.OFFICAL_APP_ID;
            case 2:
                return BuildVars.TGX_APP_ID;
            default:
                return customAppId;
        }
    }
    
    public static String currentAppHash() {
        switch (customApi) {
            case 0:
                return BuildConfig.APP_HASH;
            case 1:
                return BuildVars.OFFICAL_APP_HASH;
            case 2:
                return BuildVars.TGX_APP_HASH;
            default:
                return customAppHash;
        }
    }
    
    public static void saveCustomApi() {
        preferences.edit()
                .putInt("custom_api", customApi)
                .putInt("custom_app_id", customAppId)
                .putString("custom_app_hash", customAppHash)
                .apply();
    }

    public static void toggleDisableStatusUpdate() {
        preferences.edit().putBoolean("disable_status_update", disableStatusUpdate = !disableStatusUpdate).apply();
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateUserStatus, (Object) null);
    }

    public static void toggleKeepOnlineStatus() {
        preferences.edit().putBoolean("keepOnlineStatus", keepOnlineStatus = !keepOnlineStatus).apply();
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateUserStatus, (Object) null);
    }

    public static boolean isDeveloper() {
        if (hasDeveloper != null)
            return hasDeveloper;
        hasDeveloper = true; // BuildVars.DEBUG_VERSION;
        for (int acc : SharedConfig.activeAccounts) {
            long myId = UserConfig.getInstance(acc).clientUserId;
            if (ArrayUtil.contains(NekoXConfig.developers, myId)) {
                hasDeveloper = true;
                break;
            }
        }
        return hasDeveloper;
    }

    public static String formatLang(String name) {
        if (name == null || name.isEmpty()) {
            return getString(R.string.Default);
        } else {
            if (name.contains("-")) {
                return new Locale(StrUtil.subBefore(name, "-", false), StrUtil.subAfter(name, "-", false)).getDisplayName(LocaleController.getInstance().currentLocale);
            } else {
                return new Locale(name).getDisplayName(LocaleController.getInstance().currentLocale);
            }
        }
    }

    public static Typeface getSystemEmojiTypeface() {
        if (!loadSystemEmojiFailed && systemEmojiTypeface == null) {
            try {
                Pattern p = Pattern.compile(">(.*emoji.*)</font>", Pattern.CASE_INSENSITIVE);
                BufferedReader br = new BufferedReader(new FileReader("/system/etc/fonts.xml"));
                String line;
                while ((line = br.readLine()) != null) {
                    Matcher m = p.matcher(line);
                    if (m.find()) {
                        systemEmojiTypeface = Typeface.createFromFile("/system/fonts/" + m.group(1));
                        FileLog.d("emoji font file fonts.xml = " + m.group(1));
                        break;
                    }
                }
                br.close();
            } catch (Exception e) {
                FileLog.e(e);
            }
            if (systemEmojiTypeface == null) {
                try {
                    systemEmojiTypeface = Typeface.createFromFile("/system/fonts/" + EMOJI_FONT_AOSP);
                    FileLog.d("emoji font file = " + EMOJI_FONT_AOSP);
                } catch (Exception e) {
                    FileLog.e(e);
                    loadSystemEmojiFailed = true;
                }
            }
        }
        return systemEmojiTypeface;
    }

    public static int getNotificationColor() {
        int color = 0;
        Configuration configuration = ApplicationLoader.applicationContext.getResources().getConfiguration();
        boolean isDark = (configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (isDark) {
            color = 0xffffffff;
        } else {
            if (Theme.getActiveTheme().hasAccentColors()) {
                color = Theme.getActiveTheme().getAccentColor(Theme.getActiveTheme().currentAccentId);
            }
            if (Theme.getActiveTheme().isDark() || color == 0) {
                color = Theme.getColor(Theme.key_actionBarDefault);
            }
            // too bright
            if (AndroidUtilities.computePerceivedBrightness(color) >= 0.721f) {
                color = Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader) | 0xff000000;
            }
        }
        return color;
    }

    public static void setChannelAlias(long channelID, String name) {
        preferences.edit().putString(NekoConfig.channelAliasPrefix + channelID, name).apply();
    }

    public static void emptyChannelAlias(long channelID) {
        preferences.edit().remove(NekoConfig.channelAliasPrefix + channelID).apply();
    }

    public static String getChannelAlias(long channelID) {
        return preferences.getString(NekoConfig.channelAliasPrefix + channelID, null);
    }

    public static boolean lastOnlineState = preferences.getBoolean("last_online_state", true);

    public static void saveLastOnlineState(boolean online) {
        preferences.edit().putBoolean("last_online_state", lastOnlineState = online).apply();
    }

    public static void resetSettings() {
        NitritesKt.mkDatabase("nekox_config", true);
        preferences = NitritesKt.openMainSharedPreference("nekox_config");
    }
}

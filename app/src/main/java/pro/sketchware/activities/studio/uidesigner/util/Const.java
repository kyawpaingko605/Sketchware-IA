package pro.sketchware.activities.studio.uidesigner.util;

import android.graphics.drawable.Drawable;
import android.os.Environment;
import androidx.annotation.DrawableRes;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.webkit.WebView;
import pro.sketchware.activities.studio.uidesigner.DesignerApp;
import pro.sketchware.R;
import pro.sketchware.activities.studio.uidesigner.item.WidgetGroup;
import pro.sketchware.activities.studio.uidesigner.item.WidgetProperty;
import pro.sketchware.activities.studio.uidesigner.widget.WidgetButton;
import pro.sketchware.activities.studio.uidesigner.widget.WidgetImageView;
import pro.sketchware.activities.studio.uidesigner.widget.WidgetLinearLayout;
import pro.sketchware.activities.studio.uidesigner.widget.WidgetTextView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Const
{
    public static File APP_PATH = new File(Environment.getExternalStorageDirectory(), "AppUIDesigner");
    public static File LAYOUTS_PATH = new File(APP_PATH, "layout");
    public static File DRAWABLES_PATH = new File(APP_PATH, "drawable");
    public static File MIPMAP_PATH = new File(APP_PATH, "mipmap");
    public static File VALUES_PATH = new File(APP_PATH, "values");
    public static File COLORS_FILE = new File(VALUES_PATH, "colors.xml");

    public static Drawable getDrawable(@DrawableRes int resId)
    {
        return DesignerApp.getContext().getResources().getDrawable(resId); 
    }

    public static List<WidgetGroup> getWidgetsGroup()
    {
        List<WidgetGroup> list = new ArrayList<>();
        //Layouts
        list.add(new WidgetGroup("Layouts"));

        WidgetLinearLayout wl = new WidgetLinearLayout(DesignerApp.getContext());
        wl.setWidgetType(LinearLayout.class.getSimpleName());
        wl.setProperties(new ArrayList<WidgetProperty>());
        wl.setOrientation(LinearLayout.VERTICAL);
        list.add(new WidgetGroup("LinearLayout (vertical)", getDrawable(R.drawable.ic_mtrl_view_vertical), wl));

        wl = new WidgetLinearLayout(DesignerApp.getContext());
        wl.setWidgetType(LinearLayout.class.getSimpleName());
        wl.setProperties(new ArrayList<WidgetProperty>());
        wl.setOrientation(LinearLayout.HORIZONTAL);
        list.add(new WidgetGroup("LinearLayout (horizontal)", getDrawable(R.drawable.ic_mtrl_view_horizontal), wl));

        //Widgets
        list.add(new WidgetGroup("Widgets"));

        WidgetButton wb = new WidgetButton(DesignerApp.getContext());
        wb.setWidgetType(Button.class.getSimpleName());
        wb.setProperties(new ArrayList<WidgetProperty>());
        wb.getTextView().setText(wb.getWidgetType());
        list.add(new WidgetGroup("Button", getDrawable(R.drawable.ic_mtrl_button_click), wb));

        list.add(simpleWidget("EditText", EditText.class.getSimpleName(), R.drawable.ic_mtrl_edittext));
        list.add(simpleWidget("CheckBox", CheckBox.class.getSimpleName(), R.drawable.ic_mtrl_checkbox));
        list.add(simpleWidget("RadioButton", RadioButton.class.getSimpleName(), R.drawable.ic_mtrl_radio_btn));
        list.add(simpleWidget("Switch", Switch.class.getSimpleName(), R.drawable.ic_mtrl_switch));
        list.add(simpleWidget("SeekBar", SeekBar.class.getSimpleName(), R.drawable.ic_mtrl_seekbar));
        list.add(simpleWidget("ProgressBar", ProgressBar.class.getSimpleName(), R.drawable.ic_mtrl_progress_bar));
        list.add(simpleWidget("Spinner", Spinner.class.getSimpleName(), R.drawable.ic_mtrl_spinner));
        list.add(simpleWidget("ListView", ListView.class.getSimpleName(), R.drawable.ic_mtrl_list));

        //Views
        list.add(new WidgetGroup("Views"));

        WidgetTextView wt = new WidgetTextView(DesignerApp.getContext());
        wt.setWidgetType(TextView.class.getSimpleName());
        wt.setProperties(new ArrayList<WidgetProperty>());
        wt.getTextView().setText(wt.getWidgetType());
        list.add(new WidgetGroup("TextView", getDrawable(R.drawable.ic_mtrl_formattext), wt));

        WidgetImageView wi = new WidgetImageView(DesignerApp.getContext());
        wi.setWidgetType(ImageView.class.getSimpleName());
        wi.setProperties(new ArrayList<WidgetProperty>());
        list.add(new WidgetGroup("ImageView", getDrawable(R.drawable.ic_mtrl_image), wi));

        list.add(simpleWidget("WebView", WebView.class.getSimpleName(), R.drawable.ic_mtrl_web));
        list.add(simpleWidget("CalendarView", android.widget.CalendarView.class.getSimpleName(), R.drawable.ic_mtrl_calendar));
        list.add(simpleWidget("Space", Space.class.getSimpleName(), R.drawable.ic_mtrl_drag));

        return list;
    }

    private static WidgetGroup simpleWidget(String title, String type, int icon) {
        pro.sketchware.activities.studio.uidesigner.widget.Widget widget =
                new pro.sketchware.activities.studio.uidesigner.widget.Widget(DesignerApp.getContext());
        widget.setWidgetType(type);
        widget.setProperties(new ArrayList<WidgetProperty>());
        return new WidgetGroup(title, getDrawable(icon), widget);
    }
}

package pro.sketchware.activities.studio.uidesigner.widget;

import android.content.Context;
import android.graphics.Color;
import android.widget.LinearLayout;
import pro.sketchware.R;
import pro.sketchware.activities.studio.uidesigner.item.WidgetProperty;
import java.util.List;

public class Widget extends LinearLayout
{
    private String widgetId;
    private String widgetType;
    private List<WidgetProperty> properties;
    
    public Widget(Context context)
    {
        super(context);
    }

    public void setWidgetId(String widgetId)
    {
        this.widgetId = widgetId;
    }

    public String getWidgetId()
    {
        return widgetId;
    }

    public void setWidgetType(String widgetType)
    {
        this.widgetType = widgetType;
    }

    public String getWidgetType()
    {
        return widgetType;
    }

    public void setProperties(List<WidgetProperty> properties)
    {
        this.properties = properties;
    }

    public List<WidgetProperty> getProperties()
    {
        return properties;
    }
    
    public void select()
    {
        setBackgroundColor(getResources().getColor(R.color.studio_accent_soft));
    }

    public void unselect()
    {
        setBackgroundColor(Color.TRANSPARENT);
    }
}
